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

import java.net.InetSocketAddress;
import java.util.List;

import javax.net.ssl.SSLSocketFactory;

/**
 * Test double exposing a controllable candidate address list and a
 * controllable TLS delegate, so {@link SniAwareLdapSocketFactory#connectWithFailover}
 * can be exercised end-to-end - including the TLS upgrade step - without
 * depending on real multi-homed DNS or a real certificate.
 */
class StubSniAwareLdapSocketFactory extends SniAwareLdapSocketFactory {

  private List<InetSocketAddress> candidates;

  private SSLSocketFactory        fakeDelegate;

  void setCandidates(List<InetSocketAddress> candidates) {
    this.candidates = candidates;
  }

  void setDelegate(SSLSocketFactory fakeDelegate) {
    this.fakeDelegate = fakeDelegate;
  }

  @Override
  List<InetSocketAddress> resolveCandidates(String host, int port) {
    return candidates;
  }

  @Override
  SSLSocketFactory delegate() {
    return fakeDelegate;
  }
}
