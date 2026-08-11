/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.services.organization.idm.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Resolves the LDAP/AD provider URL(s) used to open a JNDI {@code LdapContext}.
 * <p>
 * This class is only ever instantiated - and none of the properties below
 * have any effect - when {@code org.picketlink.idm.impl.store.ldap.ExoLDAPIdentityStoreImpl#FAILOVER_ENABLED_PROP}
 * ({@code exo.ldap.failover.enabled}) is set to {@code true}; that master
 * flag defaults to {@code false}, leaving the original PicketLink code path
 * untouched.
 * <p>
 * By default (feature disabled), this simply returns the statically configured
 * {@code providerURL} (i.e. the value of {@code exo.ldap.url}) untouched, so
 * existing single- or multi-server configurations keep working exactly as before.
 * <p>
 * When DNS SRV-based discovery is enabled, this class additionally resolves a
 * {@code _service._proto.domain} SRV record set (RFC 2782 - e.g. the standard
 * {@code _ldap._tcp.<domain>} record advertised by Active Directory domain
 * controllers), orders it by priority and applies a weighted-random ordering
 * within each priority tier, and prepends the resulting URL list to the
 * statically configured one (used as an ultimate fallback).
 * <p>
 * Because the JNDI LDAP provider (com.sun.jndi.ldap.LdapCtxFactory) natively
 * accepts a space-separated list of URLs in {@code java.naming.provider.url}
 * and tries them in order until one connects, returning a multi-URL string
 * from {@link #resolveProviderURL()} is sufficient to get connection failover
 * "for free". Re-ordering the list (weighted by SRV priority/weight) on every
 * call additionally spreads new connections across the available servers,
 * providing basic load-balancing.
 * <p>
 * Independently of SRV discovery, a main server plus explicit secondary/backup
 * servers can be configured statically via {@code exo.ldap.url} (main) and
 * {@code exo.ldap.failover.urls} (secondary, tried in the order given, after
 * the main server and after any SRV-resolved server).
 * <p>
 * Recognized configuration properties (all optional, all read via
 * {@link PropertyManager}):
 * <ul>
 * <li>{@code exo.ldap.failover.urls} - comma/space-separated list of
 * secondary/backup LDAP server URLs, tried (in order) after the main
 * {@code exo.ldap.url} server(s) and after any SRV-resolved server</li>
 * <li>{@code exo.ldap.srv.enabled} - enables SRV-based discovery (defaults to
 * {@code false})</li>
 * <li>{@code exo.ldap.srv.domain} - the DNS domain to query, e.g.
 * {@code example.com}. Mandatory when SRV lookup is enabled</li>
 * <li>{@code exo.ldap.srv.service} - the SRV service/proto prefix, defaults to
 * {@code _ldap._tcp} (use e.g. {@code _ldap._tcp.dc._msdcs} to target Active
 * Directory domain controllers specifically)</li>
 * <li>{@code exo.ldap.srv.scheme} - URL scheme used to build server URLs from
 * the resolved host/port, defaults to {@code ldap}</li>
 * <li>{@code exo.ldap.srv.refresh.period} - how long (ms) a successful
 * resolution is cached before being refreshed, defaults to 300000 (5 min)</li>
 * <li>{@code exo.ldap.srv.timeout} - DNS query timeout (ms), defaults to
 * 5000</li>
 * <li>{@code exo.ldap.srv.dns.servers} - comma-separated {@code host[:port]}
 * list of DNS servers to query; defaults to the JVM/OS-configured resolver
 * when unset</li>
 * </ul>
 */
public class LdapServerLocator {

  public static final String FAILOVER_URLS_PROP       = "exo.ldap.failover.urls";

  public static final String SRV_ENABLED_PROP         = "exo.ldap.srv.enabled";

  public static final String SRV_DOMAIN_PROP          = "exo.ldap.srv.domain";

  public static final String SRV_SERVICE_PROP         = "exo.ldap.srv.service";

  public static final String SRV_SCHEME_PROP          = "exo.ldap.srv.scheme";

  public static final String SRV_REFRESH_PERIOD_PROP  = "exo.ldap.srv.refresh.period";

  public static final String SRV_TIMEOUT_PROP         = "exo.ldap.srv.timeout";

  public static final String SRV_DNS_SERVERS_PROP     = "exo.ldap.srv.dns.servers";

  static final String        DEFAULT_SERVICE          = "_ldap._tcp";

  static final String        DEFAULT_SCHEME           = "ldap";

  static final long          DEFAULT_REFRESH_PERIOD_MS = 300000L;

  static final long          DEFAULT_TIMEOUT_MS       = 5000L;

  static final long          FAILURE_RETRY_BACKOFF_MS = 30000L;

  private static final Log   LOG                      = ExoLogger.getLogger(LdapServerLocator.class);

  private final boolean      srvEnabled;

  private final String       srvDomain;

  private final String       srvService;

  private final String       scheme;

  private final long         refreshPeriodMs;

  private final long         timeoutMs;

  private final String       dnsServersUrl;

  private final List<String> staticUrls;

  private final AtomicReference<List<SrvRecord>> cachedRecords = new AtomicReference<>();

  private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);

  private volatile boolean   lastAttemptFailed;

  private volatile long      lastAttemptAt;

  private volatile long      lastSuccessAt;

  public LdapServerLocator(String configuredProviderUrl) {
    this.srvEnabled = Boolean.parseBoolean(PropertyManager.getProperty(SRV_ENABLED_PROP));
    this.srvDomain = PropertyManager.getProperty(SRV_DOMAIN_PROP);
    this.srvService = StringUtils.defaultIfBlank(PropertyManager.getProperty(SRV_SERVICE_PROP), DEFAULT_SERVICE);
    this.scheme = StringUtils.defaultIfBlank(PropertyManager.getProperty(SRV_SCHEME_PROP), DEFAULT_SCHEME);
    this.refreshPeriodMs = parseLongProperty(SRV_REFRESH_PERIOD_PROP, DEFAULT_REFRESH_PERIOD_MS);
    this.timeoutMs = parseLongProperty(SRV_TIMEOUT_PROP, DEFAULT_TIMEOUT_MS);
    this.dnsServersUrl = buildDnsServersUrl(PropertyManager.getProperty(SRV_DNS_SERVERS_PROP));

    List<String> mainUrls = parseStaticUrls(configuredProviderUrl);
    List<String> secondaryUrls = parseStaticUrls(PropertyManager.getProperty(FAILOVER_URLS_PROP));
    List<String> combinedStaticUrls = new ArrayList<>(mainUrls.size() + secondaryUrls.size());
    combinedStaticUrls.addAll(mainUrls);
    for (String secondaryUrl : secondaryUrls) {
      if (!combinedStaticUrls.contains(secondaryUrl)) {
        combinedStaticUrls.add(secondaryUrl);
      }
    }
    this.staticUrls = combinedStaticUrls;

    if (this.srvEnabled && StringUtils.isBlank(this.srvDomain)) {
      LOG.warn("LDAP SRV lookup is enabled ({}=true) but {} is not set - disabling SRV-based failover/load-balancing, "
          + "falling back to the statically configured provider URL(s) only", SRV_ENABLED_PROP, SRV_DOMAIN_PROP);
    }
  }

  /**
   * @return the provider URL(s) to use for the next LDAP connection attempt,
   *         as a space-separated list suitable for
   *         {@code java.naming.provider.url}
   */
  public String resolveProviderURL() {
    List<String> urls = new ArrayList<>();
    if (isSrvLookupEnabled()) {
      for (String url : resolveSrvUrls()) {
        if (!urls.contains(url)) {
          urls.add(url);
        }
      }
    }
    for (String url : staticUrls) {
      if (!urls.contains(url)) {
        urls.add(url);
      }
    }
    if (urls.isEmpty()) {
      return null;
    }
    return String.join(" ", urls);
  }

  boolean isSrvLookupEnabled() {
    return srvEnabled && StringUtils.isNotBlank(srvDomain);
  }

  /**
   * @return {@code true} if at least one server is configured and every
   *         server this locator can ever return uses the {@code ldaps}
   *         scheme: the SRV-generated scheme (when SRV lookup is enabled) and
   *         every statically configured URL (main and secondary). Returns
   *         {@code false} when nothing is configured at all - there is no
   *         server to prove is {@code ldaps}, so this fails closed rather
   *         than vacuously true. Used to decide whether it is safe to
   *         register a TLS-only {@code java.naming.ldap.factory.socket} -
   *         that JNDI property is not scheme-scoped, so registering it while
   *         any configured server is plain {@code ldap://} would force a TLS
   *         handshake on a plaintext connection and break it.
   */
  public boolean isAllLdaps() {
    boolean hasKnownServer = false;
    if (isSrvLookupEnabled()) {
      hasKnownServer = true;
      if (!"ldaps".equalsIgnoreCase(scheme)) {
        return false;
      }
    }
    for (String url : staticUrls) {
      hasKnownServer = true;
      if (!StringUtils.startsWithIgnoreCase(url, "ldaps://")) {
        return false;
      }
    }
    // Nothing configured at all: there is no server to prove is ldaps, so do not
    // fail open and let a TLS-only socket factory register itself regardless.
    return hasKnownServer;
  }

  private List<String> resolveSrvUrls() {
    refreshIfNeeded();
    List<SrvRecord> records = cachedRecords.get();
    if (records == null || records.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> ordered = new ArrayList<>(records.size());
    for (SrvRecord srvRecord : orderByRfc2782(records)) {
      ordered.add(srvRecord.toUrl(scheme));
    }
    return ordered;
  }

  /**
   * Decides whether the SRV cache needs refreshing and, if so, triggers it -
   * synchronously the very first time (there is nothing to serve yet, so
   * callers need to wait once), and in the background for every later
   * refresh (the current, possibly stale, list is served immediately and
   * never blocks a connection attempt on a DNS round-trip). Only actual
   * resolution failures are subject to {@link #FAILURE_RETRY_BACKOFF_MS};
   * successful refreshes are only paced by {@link #refreshPeriodMs}.
   */
  private void refreshIfNeeded() {
    long now = currentTimeMillis();
    boolean neverResolved = cachedRecords.get() == null;
    boolean stale = !neverResolved && (now - lastSuccessAt) > refreshPeriodMs;
    boolean backoffElapsed = !lastAttemptFailed || (now - lastAttemptAt) > FAILURE_RETRY_BACKOFF_MS;

    if (!((neverResolved || stale) && backoffElapsed)) {
      return;
    }
    if (!refreshInProgress.compareAndSet(false, true)) {
      // A refresh (synchronous or background) is already running - the caller keeps using
      // whatever is currently cached (or the static fallback if nothing is cached yet).
      return;
    }
    if (neverResolved) {
      try {
        doRefresh(now);
      } finally {
        refreshInProgress.set(false);
      }
    } else {
      runAsync(() -> {
        try {
          doRefresh(now);
        } finally {
          refreshInProgress.set(false);
        }
      });
    }
  }

  private void doRefresh(long now) {
    lastAttemptAt = now;
    try {
      List<SrvRecord> resolved = lookupSrvRecords();
      cachedRecords.set(resolved);
      lastSuccessAt = now;
      lastAttemptFailed = false;
      if (LOG.isDebugEnabled()) {
        LOG.debug("Resolved {} LDAP SRV record(s) for '{}.{}': {}", resolved.size(), srvService, srvDomain, resolved);
      }
    } catch (Exception e) {
      lastAttemptFailed = true;
      int previousSize = cachedRecords.get() == null ? 0 : cachedRecords.get().size();
      LOG.warn("Failed to resolve LDAP SRV records for '{}.{}' - keeping previous list ({} entries)",
               srvService,
               srvDomain,
               previousSize,
               e);
      cachedRecords.compareAndSet(null, Collections.emptyList());
    }
  }

  /**
   * A single dedicated daemon thread, shared by every {@link LdapServerLocator}
   * instance, used to run background SRV refreshes. A blocking JNDI DNS query
   * (up to {@code exo.ldap.srv.timeout} times its retry count) has no business
   * parking a thread borrowed from the JVM-wide common {@link java.util.concurrent.ForkJoinPool}.
   */
  private static final ExecutorService REFRESH_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
    Thread thread = new Thread(runnable, "ldap-srv-refresh");
    thread.setDaemon(true);
    return thread;
  });

  /**
   * Runs {@code task} in the background. Package-private so tests can
   * substitute a same-thread execution and keep assertions deterministic.
   */
  void runAsync(Runnable task) {
    REFRESH_EXECUTOR.execute(task);
  }

  List<SrvRecord> lookupSrvRecords() throws NamingException {
    Hashtable<String, Object> env = new Hashtable<>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
    if (dnsServersUrl != null) {
      env.put(Context.PROVIDER_URL, dnsServersUrl);
    }
    env.put("com.sun.jndi.dns.timeout.initial", String.valueOf(timeoutMs));
    env.put("com.sun.jndi.dns.timeout.retries", "1");

    DirContext dirContext = new InitialDirContext(env);
    try {
      String lookupName = srvService + "." + srvDomain;
      Attributes attrs = dirContext.getAttributes(lookupName, new String[] { "SRV" });
      Attribute srvAttr = attrs == null ? null : attrs.get("SRV");
      List<SrvRecord> records = new ArrayList<>();
      if (srvAttr != null) {
        NamingEnumeration<?> values = srvAttr.getAll();
        while (values.hasMore()) {
          SrvRecord srvRecord = SrvRecord.parse(String.valueOf(values.next()));
          if (srvRecord != null) {
            records.add(srvRecord);
          }
        }
      }
      return records;
    } finally {
      try {
        dirContext.close();
      } catch (NamingException e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Failed to close DNS lookup context", e);
        }
      }
    }
  }

  /**
   * Orders SRV records per RFC 2782: ascending by priority, and within a
   * priority tier, a weighted-random order (higher weight = more likely to be
   * picked earlier). Re-evaluated on every call so successive connections are
   * spread across same-priority servers.
   */
  List<SrvRecord> orderByRfc2782(List<SrvRecord> records) {
    Map<Integer, List<SrvRecord>> byPriority = new TreeMap<>();
    for (SrvRecord srvRecord : records) {
      byPriority.computeIfAbsent(srvRecord.getPriority(), key -> new ArrayList<>()).add(srvRecord);
    }
    List<SrvRecord> ordered = new ArrayList<>(records.size());
    for (List<SrvRecord> tier : byPriority.values()) {
      ordered.addAll(weightedShuffle(tier));
    }
    return ordered;
  }

  private List<SrvRecord> weightedShuffle(List<SrvRecord> tier) {
    List<SrvRecord> remaining = new ArrayList<>(tier);
    List<SrvRecord> result = new ArrayList<>(remaining.size());
    ThreadLocalRandom random = ThreadLocalRandom.current();
    while (!remaining.isEmpty()) {
      int totalWeight = 0;
      for (SrvRecord srvRecord : remaining) {
        totalWeight += Math.max(srvRecord.getWeight(), 0);
      }
      int pickedIndex;
      if (totalWeight <= 0) {
        pickedIndex = random.nextInt(remaining.size());
      } else {
        // Inclusive upper bound per RFC 2782, so a weight-0 entry retains a (small) chance.
        int threshold = random.nextInt(totalWeight + 1);
        int cumulative = 0;
        pickedIndex = remaining.size() - 1;
        for (int i = 0; i < remaining.size(); i++) {
          cumulative += Math.max(remaining.get(i).getWeight(), 0);
          if (threshold < cumulative) {
            pickedIndex = i;
            break;
          }
        }
      }
      result.add(remaining.remove(pickedIndex));
    }
    return result;
  }

  private List<String> parseStaticUrls(String configuredProviderUrl) {
    if (StringUtils.isBlank(configuredProviderUrl)) {
      return Collections.emptyList();
    }
    List<String> urls = new ArrayList<>();
    for (String url : configuredProviderUrl.trim().split("[\\s,]+")) {
      if (StringUtils.isNotBlank(url)) {
        urls.add(url);
      }
    }
    return urls;
  }

  private String buildDnsServersUrl(String dnsServersProperty) {
    if (StringUtils.isBlank(dnsServersProperty)) {
      return null;
    }
    List<String> dnsUrls = new ArrayList<>();
    for (String server : dnsServersProperty.split(",")) {
      String trimmed = server.trim();
      if (StringUtils.isNotBlank(trimmed)) {
        dnsUrls.add("dns://" + trimmed);
      }
    }
    return dnsUrls.isEmpty() ? null : String.join(" ", dnsUrls);
  }

  private long parseLongProperty(String propertyName, long defaultValue) {
    String value = PropertyManager.getProperty(propertyName);
    if (StringUtils.isBlank(value)) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      LOG.warn("Invalid value '{}' for property {} - using default {}", value, propertyName, defaultValue);
      return defaultValue;
    }
  }

  long currentTimeMillis() {
    return System.currentTimeMillis();
  }
}
