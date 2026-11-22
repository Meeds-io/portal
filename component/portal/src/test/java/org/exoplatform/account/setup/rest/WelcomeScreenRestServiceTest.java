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
package org.exoplatform.account.setup.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import org.exoplatform.account.setup.web.AccountSetupService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;

public class WelcomeScreenRestServiceTest {

  private AccountSetupService      accountSetupService;

  private UserHandler              userHandler;

  private WelcomeScreenRestService restService;

  @Before
  public void setUp() {
    accountSetupService = mock(AccountSetupService.class);
    OrganizationService organizationService = mock(OrganizationService.class);
    userHandler = mock(UserHandler.class);
    when(organizationService.getUserHandler()).thenReturn(userHandler);
    restService = new WelcomeScreenRestService(accountSetupService, organizationService);
  }

  @Test
  public void checkUsernameAnswersWhenPlatformNeverSetUp() throws Exception {
    when(accountSetupService.mustSkipAccountSetup()).thenReturn(false);
    when(userHandler.findUserByName("root")).thenReturn(mock(User.class));

    Response response = restService.checkUsername("root");
    assertEquals(200, response.getStatus());
    assertEquals("{\"userExists\":true}", response.getEntity());

    response = restService.checkUsername("john");
    assertEquals(200, response.getStatus());
    assertEquals("{\"userExists\":false}", response.getEntity());
  }

  @Test
  public void checkUsernameRefusedWhenPlatformSetUp() throws Exception {
    when(accountSetupService.mustSkipAccountSetup()).thenReturn(true);
    when(userHandler.findUserByName("root")).thenReturn(mock(User.class));

    Response response = restService.checkUsername("root");
    assertEquals(403, response.getStatus());
    assertNull(response.getEntity());
    verify(userHandler, never()).findUserByName("root");
  }
}
