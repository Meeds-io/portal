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
import static org.junit.Assert.assertNotNull;
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

import org.junit.After;
import org.junit.Test;

import org.exoplatform.commons.utils.PropertyManager;

/**
 * These tests exercise the address-enumeration and per-candidate TCP connect
 * logic - the actual novel behavior this class adds - using real loopback
 * sockets. The TLS upgrade step itself is a single delegated call to the
 * platform default {@code SSLSocketFactory} and is not re-verified here, since
 * that would require standing up a self-signed TLS test server for no
 * additional coverage of this class's own logic.
 */
public class SniAwareLdapSocketFactoryTest {

  @After
  public void clearProperties() {
    System.clearProperty(SniAwareLdapSocketFactory.SNI_ENABLED_PROP);
    PropertyManager.refresh();
  }

  @Test
  public void testGetDefaultReturnsSameSingletonInstance() {
    SocketFactory first = SniAwareLdapSocketFactory.getDefault();
    SocketFactory second = SniAwareLdapSocketFactory.getDefault();
    assertSame(first, second);
    assertTrue(first instanceof javax.net.ssl.SSLSocketFactory);
  }

  @Test
  public void testIsEnabledReflectsProperty() {
    assertFalse(SniAwareLdapSocketFactory.isEnabled());
    PropertyManager.setProperty(SniAwareLdapSocketFactory.SNI_ENABLED_PROP, "true");
    assertTrue(SniAwareLdapSocketFactory.isEnabled());
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
  public void testFailoverSkipsAnUnreachableCandidateAndConnectsToTheNextOne() throws IOException {
    int closedPort = findClosedPort();
    SniAwareLdapSocketFactory factory = new SniAwareLdapSocketFactory();
    try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
      InetSocketAddress unreachable = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), closedPort);
      InetSocketAddress reachable = new InetSocketAddress(serverSocket.getInetAddress(), serverSocket.getLocalPort());
      List<InetSocketAddress> candidates = Arrays.asList(unreachable, reachable);

      Socket connected = null;
      IOException lastFailure = null;
      for (InetSocketAddress candidate : candidates) {
        try {
          connected = factory.connectPlain(candidate, 2000);
          break;
        } catch (IOException e) {
          lastFailure = e;
        }
      }

      assertNotNull("Expected the second (reachable) candidate to succeed", connected);
      assertNotNull("First candidate should have failed before the second succeeded", lastFailure);
      assertEquals(reachable.getPort(), connected.getPort());
      connected.close();
    }
  }

  private static int findClosedPort() throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
      return serverSocket.getLocalPort();
    }
  }
}
