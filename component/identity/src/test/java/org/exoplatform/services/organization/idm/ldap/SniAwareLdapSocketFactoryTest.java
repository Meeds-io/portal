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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.junit.After;
import org.junit.Test;

import org.exoplatform.commons.utils.PropertyManager;

/**
 * Exercises the address-enumeration and per-candidate TCP connect logic using
 * real loopback sockets, and {@link SniAwareLdapSocketFactory#connectWithFailover}
 * itself - including the TLS upgrade step - against a fake delegate
 * ({@link StubSniAwareLdapSocketFactory}), so no real certificate/handshake is
 * needed to cover the skip-and-continue, close-on-failed-upgrade and
 * rethrow-when-exhausted behavior.
 */
public class SniAwareLdapSocketFactoryTest {

  @After
  public void clearProperties() {
    System.clearProperty(SniAwareLdapSocketFactory.SNI_ENABLED_PROP);
    System.clearProperty(SniAwareLdapSocketFactory.SNI_CONNECT_TIMEOUT_PROP);
    System.clearProperty(SniAwareLdapSocketFactory.JNDI_LDAP_CONNECT_TIMEOUT_SYSTEM_PROP);
    SniAwareLdapSocketFactory.hintCustomJndiConnectTimeout(null);
    PropertyManager.refresh();
  }

  @Test
  public void testGetDefaultReturnsSameSingletonInstance() {
    SocketFactory first = SniAwareLdapSocketFactory.getDefault();
    SocketFactory second = SniAwareLdapSocketFactory.getDefault();
    assertSame(first, second);
    assertTrue(first instanceof SSLSocketFactory);
  }

  @Test
  public void testFactoryImplementsComparatorSoJndiPoolingStaysEnabled() {
    // com.sun.jndi.ldap.LdapPoolManager only allows pooling with a custom socket
    // factory when its class implements java.util.Comparator.
    assertTrue(SniAwareLdapSocketFactory.getDefault() instanceof java.util.Comparator);
    SniAwareLdapSocketFactory factory = new SniAwareLdapSocketFactory();
    assertEquals(0, factory.compare(factory, new SniAwareLdapSocketFactory()));
  }

  @Test
  public void testIsEnabledReflectsProperty() {
    assertFalse(SniAwareLdapSocketFactory.isEnabled());
    PropertyManager.setProperty(SniAwareLdapSocketFactory.SNI_ENABLED_PROP, "true");
    assertTrue(SniAwareLdapSocketFactory.isEnabled());
  }

  @Test
  public void testConnectTimeoutDefaultsToTenSecondsWhenNothingConfigured() {
    assertEquals(SniAwareLdapSocketFactory.DEFAULT_CONNECT_TIMEOUT_MS, SniAwareLdapSocketFactory.connectTimeoutMs());
  }

  @Test
  public void testConnectTimeoutFallsBackToTheJndiSystemPropertyWhenSniTimeoutIsUnset() {
    System.setProperty(SniAwareLdapSocketFactory.JNDI_LDAP_CONNECT_TIMEOUT_SYSTEM_PROP, "45000");
    assertEquals(45000, SniAwareLdapSocketFactory.connectTimeoutMs());
  }

  @Test
  public void testConnectTimeoutPrefersTheExplicitSniPropertyOverTheJndiOne() {
    System.setProperty(SniAwareLdapSocketFactory.JNDI_LDAP_CONNECT_TIMEOUT_SYSTEM_PROP, "45000");
    PropertyManager.setProperty(SniAwareLdapSocketFactory.SNI_CONNECT_TIMEOUT_PROP, "5000");
    assertEquals(5000, SniAwareLdapSocketFactory.connectTimeoutMs());
  }

  @Test
  public void testConnectTimeoutFallsBackToTheCustomJndiConnectionParametersHint() {
    // This is how com.sun.jndi.ldap.connect.timeout is actually shipped by default: as a
    // customJNDIConnectionParameters entry, not a system property.
    SniAwareLdapSocketFactory.hintCustomJndiConnectTimeout("30000");
    assertEquals(30000, SniAwareLdapSocketFactory.connectTimeoutMs());
  }

  @Test
  public void testConnectTimeoutPrefersTheCustomJndiConnectionParametersHintOverTheSystemProperty() {
    System.setProperty(SniAwareLdapSocketFactory.JNDI_LDAP_CONNECT_TIMEOUT_SYSTEM_PROP, "45000");
    SniAwareLdapSocketFactory.hintCustomJndiConnectTimeout("30000");
    assertEquals(30000, SniAwareLdapSocketFactory.connectTimeoutMs());
  }

  @Test
  public void testConnectTimeoutPrefersTheExplicitSniPropertyOverTheCustomJndiConnectionParametersHint() {
    SniAwareLdapSocketFactory.hintCustomJndiConnectTimeout("30000");
    PropertyManager.setProperty(SniAwareLdapSocketFactory.SNI_CONNECT_TIMEOUT_PROP, "5000");
    assertEquals(5000, SniAwareLdapSocketFactory.connectTimeoutMs());
  }

  @Test
  public void testResolveCandidatesPairsEachAddressWithTargetPort() throws IOException {
    SniAwareLdapSocketFactory factory = new SniAwareLdapSocketFactory();
    List<InetSocketAddress> candidates = factory.resolveCandidates("127.0.0.1", 1234);
    assertEquals(1, candidates.size());
    assertEquals(1234, candidates.get(0).getPort());
    assertEquals(InetAddress.getByName("127.0.0.1"), candidates.get(0).getAddress());
  }

  @Test
  public void testConnectPlainSucceedsAgainstAListeningServer() throws IOException {
    SniAwareLdapSocketFactory factory = new SniAwareLdapSocketFactory();
    try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
      InetSocketAddress target = new InetSocketAddress(serverSocket.getInetAddress(), serverSocket.getLocalPort());
      Socket client = factory.connectPlain(target, 2000);
      try {
        assertTrue(client.isConnected());
      } finally {
        client.close();
      }
    }
  }

  @Test
  public void testConnectPlainThrowsForAnUnreachablePort() throws IOException {
    int closedPort = findClosedPort();
    SniAwareLdapSocketFactory factory = new SniAwareLdapSocketFactory();
    InetSocketAddress target = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), closedPort);
    try {
      factory.connectPlain(target, 2000);
      fail("Expected an IOException connecting to a port with nothing listening");
    } catch (IOException expected) {
      // expected: connection refused
    }
  }

  @Test
  public void testConnectWithFailoverSkipsAnUnreachableCandidateAndConnectsToTheNextOne() throws IOException {
    int closedPort = findClosedPort();
    try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
      InetSocketAddress unreachable = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), closedPort);
      InetSocketAddress reachable = new InetSocketAddress(serverSocket.getInetAddress(), serverSocket.getLocalPort());

      StubSniAwareLdapSocketFactory factory = new StubSniAwareLdapSocketFactory();
      factory.setCandidates(Arrays.asList(unreachable, reachable));
      PassthroughTlsDelegate delegate = new PassthroughTlsDelegate();
      factory.setDelegate(delegate);

      Socket result = factory.connectWithFailover("ignored-host", 0);
      try {
        assertTrue(result.isConnected());
        assertEquals(reachable.getPort(), result.getPort());
        assertEquals("The TLS upgrade should only ever be attempted on the reachable candidate", 1, delegate.getUpgradeCount());
      } finally {
        result.close();
      }
    }
  }

  @Test
  public void testConnectWithFailoverRetriesTheNextCandidateWhenTheTlsUpgradeFails() throws IOException {
    try (ServerSocket first = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        ServerSocket second = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
      InetSocketAddress firstAddress = new InetSocketAddress(first.getInetAddress(), first.getLocalPort());
      InetSocketAddress secondAddress = new InetSocketAddress(second.getInetAddress(), second.getLocalPort());

      StubSniAwareLdapSocketFactory factory = new StubSniAwareLdapSocketFactory();
      factory.setCandidates(Arrays.asList(firstAddress, secondAddress));
      PassthroughTlsDelegate delegate = new PassthroughTlsDelegate();
      delegate.failNextUpgrade();
      factory.setDelegate(delegate);

      Socket result = factory.connectWithFailover("ignored-host", 0);
      try {
        assertTrue(result.isConnected());
        assertEquals("The first candidate's TLS upgrade failed, so the second candidate must be used",
                     secondAddress.getPort(),
                     result.getPort());
        assertEquals(2, delegate.getUpgradeCount());
      } finally {
        result.close();
      }
    }
  }

  @Test
  public void testConnectWithFailoverThrowsTheLastFailureWhenEveryCandidateFails() throws IOException {
    int firstClosedPort = findClosedPort();
    int secondClosedPort = findClosedPort();
    InetSocketAddress first = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), firstClosedPort);
    InetSocketAddress second = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), secondClosedPort);

    StubSniAwareLdapSocketFactory factory = new StubSniAwareLdapSocketFactory();
    factory.setCandidates(Arrays.asList(first, second));
    factory.setDelegate(new PassthroughTlsDelegate());

    try {
      factory.connectWithFailover("ignored-host", 0);
      fail("Expected an IOException when every candidate is unreachable");
    } catch (IOException expected) {
      // expected
    }
  }

  private static int findClosedPort() throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
      return serverSocket.getLocalPort();
    }
  }

  /**
   * Fake TLS delegate that simulates a successful handshake by returning the
   * given plain socket unchanged, optionally failing the next single upgrade
   * attempt on demand.
   */
  private static final class PassthroughTlsDelegate extends SSLSocketFactory {

    private boolean shouldFailNextUpgrade;

    private int     upgradeCount;

    void failNextUpgrade() {
      this.shouldFailNextUpgrade = true;
    }

    int getUpgradeCount() {
      return upgradeCount;
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
      upgradeCount++;
      if (shouldFailNextUpgrade) {
        shouldFailNextUpgrade = false;
        throw new IOException("simulated TLS handshake failure");
      }
      return socket;
    }

    @Override
    public String[] getDefaultCipherSuites() {
      return new String[0];
    }

    @Override
    public String[] getSupportedCipherSuites() {
      return new String[0];
    }

    @Override
    public Socket createSocket(String host, int port) {
      throw new UnsupportedOperationException("not used by this test");
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) {
      throw new UnsupportedOperationException("not used by this test");
    }

    @Override
    public Socket createSocket(InetAddress host, int port) {
      throw new UnsupportedOperationException("not used by this test");
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) {
      throw new UnsupportedOperationException("not used by this test");
    }
  }
}
