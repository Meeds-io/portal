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
package org.exoplatform.services.organization;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.picketlink.idm.api.IdentitySession;
import org.picketlink.idm.api.PersistenceManager;
import org.picketlink.idm.api.Transaction;
import org.picketlink.idm.common.exception.IdentityException;

import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;
import org.exoplatform.services.organization.idm.UserDAOImpl;
import org.exoplatform.services.organization.idm.UserImpl;

/**
 * The insert of a new login reaches the store through the session's own
 * save, which throws, never through the service's flush, which swallows:
 * a login the store refuses fails the creation before any attribute is
 * written, so a concurrent creation of the same login never writes on the
 * account that won
 */
@RunWith(MockitoJUnitRunner.class)
public class TestUserDAOImplCreateUser {

  @Mock
  private PicketLinkIDMOrganizationServiceImpl orgService;

  @Mock
  private PicketLinkIDMService                 idmService;

  @Mock
  private IdentitySession                      session;

  @Mock
  private PersistenceManager                   persistenceManager;

  @Mock
  private Transaction                          transaction;

  @Test
  public void testARefusedInsertFailsTheCreationBeforeTheAttributes() throws Exception {
    when(idmService.getIdentitySession()).thenReturn(session);
    when(session.getPersistenceManager()).thenReturn(persistenceManager);
    when(session.getTransaction()).thenReturn(transaction);
    when(transaction.isActive()).thenReturn(true);
    doThrow(new IdentityException("Unique constraint UK_JBID_IO_01 violated")).when(session).save();
    UserDAOImpl userDAO = new UserDAOImpl(orgService, idmService);
    UserImpl user = new UserImpl("jdoe");
    user.setEmail("jdoe@example.com");

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> userDAO.createUser(user, false));

    assertTrue(e.getMessage(), e.getMessage().startsWith("Identity operation error"));
    verify(persistenceManager).createUser("jdoe");
    verify(session).save();
    verify(orgService).recoverFromIDMError();
    verify(session, never()).getAttributesManager();
    verify(persistenceManager, never()).removeUser(anyString(), org.mockito.ArgumentMatchers.anyBoolean());
  }

  /**
   * With no IDM transaction active there is nothing to save, as the
   * service's flush treats it: the creation goes on to its attributes
   */
  @Test
  public void testNoActiveTransactionSavesNothingAndGoesOn() throws Exception {
    when(idmService.getIdentitySession()).thenReturn(session);
    when(session.getPersistenceManager()).thenReturn(persistenceManager);
    when(session.getTransaction()).thenReturn(transaction);
    when(transaction.isActive()).thenReturn(false);
    UserDAOImpl userDAO = new UserDAOImpl(orgService, idmService);

    try {
      userDAO.createUser(new UserImpl("jdoe"), false);
    } catch (Exception e) {
      // The attribute writes need the store's managers, not mocked here:
      // the creation went past the guarded insert
    }

    verify(persistenceManager).createUser("jdoe");
    verify(session, never()).save();
    verify(session).getAttributesManager();
  }

}
