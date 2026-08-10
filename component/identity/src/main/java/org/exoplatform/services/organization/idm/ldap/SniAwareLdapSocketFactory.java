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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * SSL socket factory for {@code ldaps://} connections whose host name (e.g. a
 * CNAME, or a name backed by round-robin DNS) resolves to more than one
 * server address.
 * <p>
 * The JDK's default socket handling only ever connects to a single address
 * for a host name and never retries other addresses behind the same name if
 * that first attempt fails. This factory instead enumerates every address
 * behind the configured host name (via {@link InetAddress#getAllByName}) and
 * tries each in turn, while still performing the TLS handshake - SNI and
 * certificate hostname verification - against the <em>original host name</em>
 * rather than the numeric address actually connected to.
 * <p>
 * This distinction matters: verifying against a raw IP address instead of the
 * configured host name would silently defeat hostname verification (most
 * server certificates are not issued for IP addresses), which is why the
 * original host name is always passed through to the delegate factory's
 * {@link SSLSocketFactory#createSocket(Socket, String, int, boolean)}.
 * <p>
 * Enabled by setting {@code exo.ldap.sni.enabled=true}; the connect timeout
 * (ms) used for each candidate address can be tuned with
 * {@code exo.ldap.sni.connect.timeout}. When unset, it defaults to the JVM
 * system property {@code com.sun.jndi.ldap.connect.timeout} if present (the
 * standard, documented way this timeout is normally configured), else 10000.
 * Note that this only covers the system-property form of that setting: a
 * value supplied only through the store's {@code customJNDIConnectionParameters}
 * option (i.e. as a JNDI env entry rather than a system property) is not
 * visible from this factory and must be mirrored into
 * {@code exo.ldap.sni.connect.timeout} explicitly if SNI support is enabled.
 * <p>
 * Registered via the standard JNDI LDAP {@code java.naming.ldap.factory.socket}
 * connection property, whose contract requires a public static
 * {@code getDefault()} method (mirroring {@link SSLSocketFactory#getDefault()}) -
 * the JNDI LDAP provider obtains the factory instance by reflectively invoking
 * that method, not the class constructor.
 * <p>
 * Implements {@link Comparator} (comparing purely on class identity) because
 * {@code com.sun.jndi.ldap.LdapPoolManager} only allows pooling LDAP
 * connections that use a custom socket factory when that factory's class
 * implements {@code java.util.Comparator} - without it, enabling this
 * feature would silently disable {@code com.sun.jndi.ldap.connect.pool}.
 */
public class SniAwareLdapSocketFactory extends SSLSocketFactory implements Comparator<SniAwareLdapSocketFactory> {

  public static final String SNI_ENABLED_PROP             = "exo.ldap.sni.enabled";

  public static final String SNI_CONNECT_TIMEOUT_PROP     = "exo.ldap.sni.connect.timeout";

  public static final String SOCKET_FACTORY_JNDI_PROPERTY = "java.naming.ldap.factory.socket";

  static final int           DEFAULT_CONNECT_TIMEOUT_MS   = 10000;

  private static final Log   LOG                          = ExoLogger.getLogger(SniAwareLdapSocketFactory.class);

  private static final SniAwareLdapSocketFactory INSTANCE = new SniAwareLdapSocketFactory();

  // Package-private (rather than private) only so tests in this package can exercise
  // resolveCandidates()/connectPlain() directly; production code must go through getDefault().
  SniAwareLdapSocketFactory() {
  }

  /**
   * Required by the JNDI LDAP {@code java.naming.ldap.factory.socket}
   * contract: the configured class is instantiated by invoking this static
   * method, not its constructor.
   */
  public static SocketFactory getDefault() {
    return INSTANCE;
  }

  public static boolean isEnabled() {
    return Boolean.parseBoolean(PropertyManager.getProperty(SNI_ENABLED_PROP));
  }

  /**
   * All instances are considered equal: this is a stateless singleton, and
   * the comparison only exists to satisfy
   * {@code com.sun.jndi.ldap.LdapPoolManager}'s pooling eligibility check.
   */
  @Override
  public int compare(SniAwareLdapSocketFactory first, SniAwareLdapSocketFactory second) {
    return 0;
  }

  @Override
  public Socket createSocket(String host, int port) throws IOException {
    return connectWithFailover(host, port);
  }

  @Override
  public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
    // Local-address binding is not used by the JNDI LDAP provider in practice; fall back to the default behavior.
    return delegate().createSocket(host, port, localAddress, localPort);
  }

  @Override
  public Socket createSocket(InetAddress host, int port) throws IOException {
    // No host name available to enumerate alternate addresses from - delegate directly.
    return delegate().createSocket(host, port);
  }

  @Override
  public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
    return delegate().createSocket(address, port, localAddress, localPort);
  }

  @Override
  public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
    return delegate().createSocket(s, host, port, autoClose);
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return delegate().getDefaultCipherSuites();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return delegate().getSupportedCipherSuites();
  }

  /**
   * Package-private (rather than private) so tests can drive this method
   * directly - including the TLS upgrade step - against a fake delegate
   * factory, instead of only re-testing a copy of its retry loop.
   */
  Socket connectWithFailover(String host, int port) throws IOException {
    int timeoutMs = connectTimeoutMs();
    List<InetSocketAddress> candidates = resolveCandidates(host, port);
    IOException lastFailure = null;
    for (InetSocketAddress candidate : candidates) {
      Socket plainSocket;
      try {
        plainSocket = connectPlain(candidate, timeoutMs);
      } catch (IOException e) {
        lastFailure = e;
        continue;
      }
      try {
        // Upgrade to TLS using the ORIGINAL host name (not the numeric address actually
        // connected to), so SNI and certificate hostname verification target the real name.
        return delegate().createSocket(plainSocket, host, port, true);
      } catch (IOException e) {
        lastFailure = e;
        closeQuietly(plainSocket);
      }
    }
    if (lastFailure != null) {
      throw lastFailure;
    }
    throw new UnknownHostException("No addresses resolved for host " + host);
  }

  /**
   * Enumerates every candidate address behind {@code host}, paired with the
   * target port. Package-private so tests can verify the mapping without
   * depending on real multi-homed DNS entries.
   */
  List<InetSocketAddress> resolveCandidates(String host, int port) throws UnknownHostException {
    InetAddress[] addresses = InetAddress.getAllByName(host);
    List<InetSocketAddress> candidates = new ArrayList<>(addresses.length);
    for (InetAddress address : addresses) {
      candidates.add(new InetSocketAddress(address, port));
    }
    return candidates;
  }

  /**
   * Opens a plain TCP connection to a single candidate address. Package-private
   * so the failover/skip-on-failure iteration can be exercised with real
   * loopback sockets in tests, without requiring a TLS handshake.
   */
  Socket connectPlain(InetSocketAddress candidate, int timeoutMs) throws IOException {
    Socket socket = new Socket();
    try {
      socket.connect(candidate, timeoutMs);
    } catch (IOException e) {
      closeQuietly(socket);
      throw e;
    }
    return socket;
  }

  private static void closeQuietly(Socket socket) {
    try {
      socket.close();
    } catch (IOException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to close a socket candidate discarded during LDAPS failover", e);
      }
    }
  }

  static final String JNDI_LDAP_CONNECT_TIMEOUT_SYSTEM_PROP = "com.sun.jndi.ldap.connect.timeout";

  static int connectTimeoutMs() {
    String value = PropertyManager.getProperty(SNI_CONNECT_TIMEOUT_PROP);
    if (StringUtils.isNotBlank(value)) {
      return parseTimeout(value, SNI_CONNECT_TIMEOUT_PROP, defaultConnectTimeoutMs());
    }
    return defaultConnectTimeoutMs();
  }

  /**
   * Falls back to the JVM system property {@code com.sun.jndi.ldap.connect.timeout}
   * when {@code exo.ldap.sni.connect.timeout} is unset, so switching SNI on
   * does not silently replace a timeout the admin already configured the
   * standard way.
   */
  private static int defaultConnectTimeoutMs() {
    String jndiValue = System.getProperty(JNDI_LDAP_CONNECT_TIMEOUT_SYSTEM_PROP);
    if (StringUtils.isNotBlank(jndiValue)) {
      return parseTimeout(jndiValue, JNDI_LDAP_CONNECT_TIMEOUT_SYSTEM_PROP, DEFAULT_CONNECT_TIMEOUT_MS);
    }
    return DEFAULT_CONNECT_TIMEOUT_MS;
  }

  private static int parseTimeout(String value, String propertyName, int fallback) {
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      LOG.warn("Invalid value '{}' for property {} - using default {}", value, propertyName, fallback);
      return fallback;
    }
  }

  /**
   * Package-private (rather than private/static) so tests can substitute a
   * fake TLS delegate and exercise {@link #connectWithFailover(String, int)}
   * without a real certificate/handshake.
   */
  SSLSocketFactory delegate() {
    return (SSLSocketFactory) SSLSocketFactory.getDefault();
  }
}
