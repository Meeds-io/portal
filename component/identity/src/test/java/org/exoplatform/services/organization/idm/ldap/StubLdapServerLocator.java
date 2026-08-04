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

import java.util.Collections;
import java.util.List;

import javax.naming.NamingException;

/**
 * Test double that replaces the actual DNS SRV lookup and system clock with
 * fully controllable values, so tests never hit the network and never depend
 * on wall-clock time.
 */
class StubLdapServerLocator extends LdapServerLocator {

  private List<SrvRecord>  recordsToReturn = Collections.emptyList();

  private NamingException  failure;

  private int              lookupCount;

  private long             now;

  StubLdapServerLocator(String configuredProviderUrl) {
    super(configuredProviderUrl);
  }

  void setRecordsToReturn(List<SrvRecord> records) {
    this.recordsToReturn = records;
    this.failure = null;
  }

  void setFailure(NamingException failure) {
    this.failure = failure;
  }

  void setNow(long now) {
    this.now = now;
  }

  int getLookupCount() {
    return lookupCount;
  }

  @Override
  List<SrvRecord> lookupSrvRecords() throws NamingException {
    lookupCount++;
    if (failure != null) {
      throw failure;
    }
    return recordsToReturn;
  }

  @Override
  long currentTimeMillis() {
    return now;
  }
}
