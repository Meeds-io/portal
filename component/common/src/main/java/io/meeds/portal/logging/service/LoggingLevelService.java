/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.portal.logging.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import io.meeds.portal.logging.constant.LoggingLevel;
import io.meeds.portal.logging.model.LoggerLevel;
import io.meeds.portal.logging.model.LoggerLevelPage;
import io.meeds.portal.logging.model.LoggingLevelStatus;
import io.meeds.portal.logging.model.StoredLevels;
import io.meeds.portal.logging.storage.LoggingLevelStorage;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

/**
 * Changes the level of a logger while the server runs, without a restart and
 * without touching {@code logback.xml}, which stays the base: an override
 * replaces the level the file gave until it is reset, and a reset gives the
 * file's level back.
 * <p>
 * The overrides are stored, so a restart applies them again. Each node
 * compares the version of the stored overrides to the one it applied, and
 * applies the stored ones when they differ: that is how a change reaches the
 * other nodes of a cluster. The same refresh gives a logger its override back
 * when {@code logback.xml} was reloaded (logback resets every level then), so
 * after a reload an override is missing for one refresh interval at most.
 */
@Service
@RequiredArgsConstructor
public class LoggingLevelService {

  private static final Log                LOG          = ExoLogger.getLogger(LoggingLevelService.class);

  /** One segment of a name: a Java identifier, hyphens allowed. No repetition of a group, which can overflow the stack. */
  private static final Pattern            SEGMENT_PATTERN = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$\\-]*");

  /** The name of the root logger, which logback also answers to in another case. */
  private static final String             ROOT = org.slf4j.Logger.ROOT_LOGGER_NAME;

  private static final int                MAX_NAME     = 200;

  private static final int                MAX_PAGE     = 500;

  private static final int                MAX_OVERRIDES = 500;

  private final LoggingLevelStorage       storage;

  private final LoggingLevelConfiguration configuration;

  private final Map<String, LoggingLevel> overrides    = new LinkedHashMap<>();

  /** When each override was last changed, in milliseconds; a logger that is not there has no known time. */
  private final Map<String, Long>         changedAt    = new HashMap<>();

  /** The clock, which the tests move. */
  LongSupplier                            clock        = System::currentTimeMillis;

  /** The level {@code logback.xml} gave each overridden logger, empty for none: a missing key is a logger nothing is known of. */
  private final Map<String, Optional<Level>> baselines = new HashMap<>();

  private LoggerContextResetListener      resetListener;

  /** Set by logback's reset, which holds its locks: only read under this service's lock. */
  private volatile boolean                contextReset;

  private volatile boolean                refreshFailing;

  /** Set when the context is gone: a refresh already under way must not put the levels back. */
  private volatile boolean                stopped;

  private volatile String                 appliedVersion;

  @PostConstruct
  public void init() {
    if (!configuration.isEnabled()) {
      LOG.info("Logging levels are disabled (meeds.logging.levels.enabled): logback.xml is the only authority");
      return;
    }
    stopped = false;
    resetListener = new LoggerContextResetListener(this::contextWasReset);
    context().addListener(resetListener);
    refresh();
  }

  @PreDestroy
  public synchronized void destroy() {
    // logback lives in the server's libraries and outlives this context: it
    // must not keep the levels of a context that is gone, and the next one
    // applies the stored overrides again
    if (resetListener != null) {
      context().removeListener(resetListener);
      resetListener = null;
    }
    stopped = true;
    new ArrayList<>(overrides.keySet()).forEach(this::restore);
    overrides.clear();
    changedAt.clear();
    baselines.clear();
  }

  /**
   * Lists the loggers the application created: the ones an administrator
   * changed first, then the others, each sorted by name.
   *
   * @param  query        a part of the logger name, in any case; {@code null}
   *                      or empty for every logger
   * @param  explicitOnly {@code true} to keep the loggers that have a level of
   *                      their own, the ones {@code logback.xml} or an
   *                      administrator set
   * @param  offset       the index of the first logger to return
   * @param  limit        the number of loggers to return, between 1 and 500
   * @return              the page
   * @throws IllegalAccessException with the message code
   *                                  {@code logging.disabled}
   */
  public synchronized LoggerLevelPage getLoggers(String query, boolean explicitOnly, int offset, int limit) throws IllegalAccessException {
    requireEnabled();
    forgetBaselinesAfterReset();
    String filter = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    List<LoggerLevel> matches = new ArrayList<>();
    for (Logger logger : context().getLoggerList()) {
      if ((!explicitOnly || logger.getLevel() != null)
          && logger.getName().toLowerCase(Locale.ROOT).contains(filter)) {
        matches.add(toLoggerLevel(logger, overrides.containsKey(logger.getName())));
      }
    }
    // what an administrator changed first, so it is found without scrolling
    matches.sort(Comparator.comparing((LoggerLevel logger) -> !logger.overridden())
                           .thenComparing(LoggerLevel::name, LoggingLevelService::compareNames));
    int pageSize = Math.clamp(limit, 1, MAX_PAGE);
    int from = Math.clamp(offset, 0, matches.size());
    int to = Math.min(from + pageSize, matches.size());
    return new LoggerLevelPage(new ArrayList<>(matches.subList(from, to)), from, pageSize, matches.size());
  }

  /**
   * Sets the level of a logger, a new one when no code created it yet: that is
   * how a package is added.
   *
   * @param  name     the logger name, a package or a class, or {@code ROOT}
   * @param  level    the level name, in any case
   * @param  username the administrator, for the audit line
   * @throws IllegalArgumentException with the message code
   *                                  {@code logging.name.invalid},
   *                                  {@code logging.level.invalid} or, for a
   *                                  new logger when 500 are already
   *                                  changed, {@code logging.overrides.tooMany}
   * @throws IllegalAccessException with the message code
   *                                  {@code logging.disabled} or
   *                                  {@code logging.readOnly}
   */
  public synchronized void setLevel(String name, String level, String username) throws IllegalAccessException {
    requireWritable();
    String loggerName = validateName(name);
    LoggingLevel newLevel = LoggingLevel.fromName(level);
    syncStoredOverrides();
    if (!overrides.containsKey(loggerName) && overrides.size() >= MAX_OVERRIDES) {
      throw new IllegalArgumentException("logging.overrides.tooMany");
    }
    Map<String, LoggingLevel> next = new LinkedHashMap<>(overrides);
    next.put(loggerName, newLevel);
    Map<String, Long> nextTimes = new HashMap<>(changedAt);
    long now = clock.getAsLong();
    nextTimes.put(loggerName, now);
    // stored first: a write that fails changes nothing on this node either
    String version = storage.saveOverrides(next, nextTimes);
    Logger logger = context().getLogger(loggerName);
    audited(() -> {
      // Only the first override remembers the level of logback.xml, which can be
      // none (containsKey, not putIfAbsent: a null level is a level): the level
      // a logger has once overridden is ours, never the one to give back.
      baselines.computeIfAbsent(loggerName, key -> Optional.ofNullable(logger.getLevel()));
      logger.setLevel(newLevel.getLevel());
    }, "Logger '{}' set to {} at runtime by '{}'", loggerName, newLevel, username);
    overrides.put(loggerName, newLevel);
    changedAt.put(loggerName, now);
    appliedVersion = version;
  }

  /**
   * Gives a logger the level {@code logback.xml} set.
   *
   * @param  name                   the logger name
   * @param  username               the administrator, for the audit line
   * @throws ObjectNotFoundException when the logger has no override
   * @throws IllegalAccessException  with the message code
   *                                  {@code logging.disabled} or
   *                                  {@code logging.readOnly}
   */
  public synchronized void resetLevel(String name, String username) throws ObjectNotFoundException, IllegalAccessException {
    requireWritable();
    String loggerName = validateName(name);
    syncStoredOverrides();
    if (!overrides.containsKey(loggerName)) {
      throw new ObjectNotFoundException("logging.override.notFound");
    }
    Map<String, LoggingLevel> next = new LinkedHashMap<>(overrides);
    next.remove(loggerName);
    String version = storage.saveOverrides(next, changedAt);
    audited(() -> restore(loggerName), "Logger '{}' back to its logback.xml level by '{}'", loggerName, username);
    overrides.remove(loggerName);
    changedAt.remove(loggerName);
    appliedVersion = version;
  }

  /**
   * Gives every overridden logger the level {@code logback.xml} set.
   *
   * @param  username               the administrator, for the audit line
   * @throws IllegalAccessException with the message code
   *                                  {@code logging.disabled} or
   *                                  {@code logging.readOnly}
   */
  public synchronized void resetAll(String username) throws IllegalAccessException {
    requireWritable();
    syncStoredOverrides();
    String version = storage.saveOverrides(new LinkedHashMap<>());
    audited(() -> {
      for (String name : new ArrayList<>(overrides.keySet())) {
        restore(name);
      }
    }, "Every logger back to its logback.xml level by '{}'", username);
    overrides.clear();
    changedAt.clear();
    appliedVersion = version;
  }

  /**
   * Applies the stored overrides when another node changed them, and gives a
   * logger its override back when a reload of {@code logback.xml} replaced it.
   * Run on every node by {@code LoggingLevelRefreshJob}, and never throws: a store that does not answer keeps
   * the levels as they are and is reported once.
   */
  public void refresh() {
    if (!configuration.isEnabled()) {
      return;
    }
    try {
      // read outside the lock: a store that is slow does not hold the list of the page
      String appliedAtRead = appliedVersion;
      StoredLevels stored = storage.read();
      applyStored(stored, appliedAtRead);
      refreshFailing = false;
    } catch (RuntimeException e) {
      if (refreshFailing) {
        LOG.debug("The stored logging levels are still not applied", e);
      } else {
        LOG.warn("Unable to apply the stored logging levels, the levels in place are kept", e);
        refreshFailing = true;
      }
    }
  }

  /**
   * @return what a system administrator allows: the page shows it
   */
  public LoggingLevelStatus getStatus() {
    return new LoggingLevelStatus(configuration.isEnabled(), configuration.isReadOnly(), configuration.getRevertAfterMinutes());
  }

  /**
   * Logback reset its levels: what was remembered of {@code logback.xml} is
   * out of date. Runs under logback's locks, so it only sets a flag.
   */
  void contextWasReset() {
    contextReset = true;
  }

  /**
   * Reads the stored overrides when another node wrote them since this one
   * applied them. A write starts from them, so two administrators on two nodes
   * do not erase each other's change, but for the time of one read.
   */
  private void syncStoredOverrides() {
    forgetBaselinesAfterReset();
    StoredLevels stored = storage.read();
    if (stored != null && !stored.version().equals(appliedVersion)) {
      adopt(stored);
    }
  }

  /**
   * Applies what {@link #refresh()} read, unless this node wrote since it read:
   * what it read is then older than what this node applied.
   *
   * @param stored        what the store held when it was read
   * @param appliedAtRead the version this node had applied when it read
   */
  synchronized void applyStored(StoredLevels stored, String appliedAtRead) {
    if (stopped) {
      return;
    }
    forgetBaselinesAfterReset();
    if (stored != null
        && Objects.equals(appliedAtRead, appliedVersion)
        && !stored.version().equals(appliedVersion)) {
      adopt(stored);
    }
    overrides.forEach(this::reapply);
    giveBackTheOldChanges();
  }

  /**
   * Gives back the level of {@code logback.xml} to the changes older than
   * {@code meeds.logging.levels.revertAfterMinutes}, whatever the read only
   * mode: a forgotten DEBUG must not stay on. The result is stored, so the
   * other nodes follow; a node that finds the same changes at the same time
   * stores the same result.
   */
  private void giveBackTheOldChanges() {
    long limit = configuration.getRevertAfterMinutes() * 60_000L;
    if (limit <= 0) {
      return;
    }
    long now = clock.getAsLong();
    List<String> old = dueChanges(now, limit);
    if (old.isEmpty()) {
      return;
    }
    // A write, like the others: it starts from what another node stored, which this node read a moment ago
    // or not at all, and it must not erase a change made since
    syncStoredOverrides();
    old = dueChanges(now, limit);
    if (old.isEmpty()) {
      return;
    }
    Map<String, LoggingLevel> next = new LinkedHashMap<>(overrides);
    old.forEach(next::remove);
    String version = storage.saveOverrides(next, changedAt);
    old.forEach(name -> {
      audited(() -> restore(name), "Logger '{}' back to its logback.xml level, {} minutes after it was changed", name,
              configuration.getRevertAfterMinutes());
      overrides.remove(name);
      changedAt.remove(name);
    });
    appliedVersion = version;
  }

  /** Takes the overrides another node stored, and puts them in place at once. */
  private void adopt(StoredLevels stored) {
    for (String name : new ArrayList<>(overrides.keySet())) {
      if (!stored.overrides().containsKey(name)) {
        restore(name);
      }
    }
    overrides.clear();
    overrides.putAll(stored.overrides());
    changedAt.clear();
    changedAt.putAll(stored.changedAt());
    appliedVersion = stored.version();
    overrides.forEach(this::reapply);
  }

  private List<String> dueChanges(long now, long limit) {
    return overrides.keySet().stream().filter(name -> {
      Long time = changedAt.get(name);
      return time != null && now - time >= limit;
    }).toList();
  }

  /**
   * Puts an override in place on this node: at the start, for a level another
   * node changed, and where a reload of {@code logback.xml} gave the logger
   * another level.
   */
  private void reapply(String name, LoggingLevel level) {
    Logger logger = context().getLogger(name);
    Level current = logger.getLevel();
    // Remembered once: what logback.xml gives is known when nothing is
    // remembered (a reload forgets it first). A level that is not the override
    // may be the one of an earlier override, that no longer is the one to give back.
    baselines.computeIfAbsent(name, key -> Optional.ofNullable(current));
    if (!level.getLevel().equals(current)) {
      logger.setLevel(level.getLevel());
    }
  }

  /**
   * Runs a change of levels and writes its audit line once: before the change
   * when INFO is on then (a change that silences the service's own logger,
   * {@code io.meeds} or {@code ROOT} records itself), after it otherwise (a
   * change that gives the logs back).
   */
  private void audited(Runnable change, String message, Object... arguments) {
    boolean written = LOG.isInfoEnabled();
    if (written) {
      LOG.info(message, arguments);
    }
    change.run();
    if (!written) {
      LOG.info(message, arguments);
    }
  }

  private void restore(String name) {
    if (!baselines.containsKey(name)) {
      // nothing is known of logback.xml for it (it was just reloaded, and the
      // logger holds the level of the new file): the level in place is kept
      return;
    }
    Logger logger = context().getLogger(name);
    logger.setLevel(baselines.remove(name).orElse(null));
  }

  /**
   * After a reset of logback the levels are the ones of the file again, and
   * what was remembered is out of date.
   */
  private void forgetBaselinesAfterReset() {
    if (contextReset) {
      contextReset = false;
      baselines.clear();
    }
  }

  private void requireEnabled() throws IllegalAccessException {
    if (!configuration.isEnabled()) {
      throw new IllegalAccessException("logging.disabled");
    }
  }

  private void requireWritable() throws IllegalAccessException {
    requireEnabled();
    if (configuration.isReadOnly()) {
      throw new IllegalAccessException("logging.readOnly");
    }
  }

  private LoggerLevel toLoggerLevel(Logger logger, boolean overridden) {
    LoggingLevel level = LoggingLevel.fromLevel(logger.getLevel());
    LoggingLevel effective = LoggingLevel.fromLevel(logger.getEffectiveLevel());
    LoggingLevel configured = overridden ? LoggingLevel.fromLevel(baselines.getOrDefault(logger.getName(), Optional.empty()).orElse(null)) : level;
    Long changed = overridden ? changedAt.get(logger.getName()) : null;
    long limit = configuration.getRevertAfterMinutes() * 60_000L;
    Long revertsAt = changed == null || limit <= 0 ? null : changed + limit;
    return new LoggerLevel(logger.getName(), level, effective, configured, overridden, revertsAt, isValidName(logger.getName()));
  }

  private static int compareNames(String first, String second) {
    if (ROOT.equals(first)) {
      return ROOT.equals(second) ? 0 : -1;
    } else if (ROOT.equals(second)) {
      return 1;
    }
    return first.compareTo(second);
  }

  /**
   * @return whether the name can be changed here: the loggers logback lists
   *         include some that no code of the platform names, such as Tomcat's
   *         container loggers (bridged from {@code java.util.logging}), whose
   *         names the rule of {@link #validateName(String)} refuses
   */
  private static boolean isValidName(String name) {
    try {
      // validateName trims what an administrator typed; a logger whose own name has a space around it is another logger
      return name.equals(name.trim()) && validateName(name) != null;
    } catch (IllegalArgumentException _) {
      return false;
    }
  }

  private static String validateName(String name) {
    String trimmed = name == null ? null : name.trim();
    if (trimmed != null && ROOT.equalsIgnoreCase(trimmed)) {
      // logback takes root, Root and ROOT for the one root logger
      return ROOT;
    }
    if (trimmed == null
        || trimmed.isEmpty()
        || trimmed.length() > MAX_NAME
        || !isDottedName(trimmed)) {
      throw new IllegalArgumentException("logging.name.invalid");
    }
    return trimmed;
  }

  private static boolean isDottedName(String name) {
    // -1 keeps the empty segments of "a..b" and "a.": they are refused
    for (String segment : name.split("\\.", -1)) {
      if (!SEGMENT_PATTERN.matcher(segment).matches()) {
        return false;
      }
    }
    return true;
  }

  private static LoggerContext context() {
    ILoggerFactory factory = LoggerFactory.getILoggerFactory();
    if (factory instanceof LoggerContext loggerContext) {
      return loggerContext;
    }
    throw new IllegalStateException("logging.unsupported");
  }

}
