/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package org.gatein.portal.idm.impl.repository;

import java.util.Map;

import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.spi.store.IdentityStoreSession;

/**
 * This class is used by external store API to store opened sessions (DB and/or
 * LDAP) in to close them at the end of transactions
 */
public class ExoRepositoryIdentityStoreSessionImpl implements IdentityStoreSession {

  protected final Map<String, IdentityStoreSession> identityStoreSessionMappings;

  protected final Map<String, Object>               sessionOptions;

  public ExoRepositoryIdentityStoreSessionImpl(Map<String, IdentityStoreSession> identityStoreSessionMappings,
                                               Map<String, Object> sessionOptions) {
    if (identityStoreSessionMappings == null) {
      throw new IllegalArgumentException();
    }
    this.identityStoreSessionMappings = identityStoreSessionMappings;
    this.sessionOptions = sessionOptions;
  }

  public Map<String, IdentityStoreSession> getIdentityStoreSessionMappings() {
    return identityStoreSessionMappings;
  }

  public Map<String, Object> getSessionOptions() {
    return sessionOptions;
  }

  public void addIdentityStoreSessionMapping(String id, IdentityStoreSession identityStoreSession) {
    identityStoreSessionMappings.put(id, identityStoreSession);
  }

  public void removeIdentityStoreSessionMapping(String id) {
    identityStoreSessionMappings.remove(id);
  }

  public Object getSessionContext() throws IdentityException {
    return null;
  }

  public IdentityStoreSession getIdentityStoreSession(String storeId) {
    return identityStoreSessionMappings.get(storeId);
  }

  public void close() throws IdentityException {
    for (IdentityStoreSession identityStoreSession : identityStoreSessionMappings.values()) {
      identityStoreSession.close();
    }
  }

  public void save() throws IdentityException {
    for (IdentityStoreSession iss : identityStoreSessionMappings.values()) {
      iss.save();
    }
  }

  public void clear() throws IdentityException {
    for (IdentityStoreSession iss : identityStoreSessionMappings.values()) {
      iss.clear();
    }
  }

  public boolean isOpen() {
    for (IdentityStoreSession identityStoreSession : identityStoreSessionMappings.values()) {
      if (identityStoreSession.isOpen()) {
        return true;
      }
    }
    return false;
  }

  public boolean isTransactionSupported() {
    for (IdentityStoreSession identityStoreSession : identityStoreSessionMappings.values()) {
      if (identityStoreSession.isTransactionSupported()) {
        return true;
      }
    }
    return false;
  }

  public void startTransaction() {
    for (IdentityStoreSession identityStoreSession : identityStoreSessionMappings.values()) {
      if (identityStoreSession.isTransactionSupported()) {
        identityStoreSession.startTransaction();
      }
    }
  }

  public void commitTransaction() {
    for (IdentityStoreSession identityStoreSession : identityStoreSessionMappings.values()) {
      if (identityStoreSession.isTransactionSupported()) {
        identityStoreSession.commitTransaction();
      }
    }
  }

  public void rollbackTransaction() {
    for (IdentityStoreSession identityStoreSession : identityStoreSessionMappings.values()) {
      if (identityStoreSession.isTransactionSupported()) {
        identityStoreSession.rollbackTransaction();
      }
    }
  }

  public boolean isTransactionActive() {
    for (IdentityStoreSession identityStoreSession : identityStoreSessionMappings.values()) {
      if (identityStoreSession.isTransactionActive()) {
        return true;
      }
    }
    return false;
  }
}
