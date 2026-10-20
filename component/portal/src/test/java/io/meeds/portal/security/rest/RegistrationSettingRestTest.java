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
package io.meeds.portal.security.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.exoplatform.portal.rest.UserAccountConfirmation;
import org.exoplatform.portal.rest.services.BaseRestServicesTestCase;
import org.exoplatform.services.rest.impl.ContainerResponse;

import io.meeds.portal.security.model.RegistrationSetting;
import io.meeds.portal.security.service.SecuritySettingService;

public class RegistrationSettingRestTest extends BaseRestServicesTestCase {

  private static final String    ADMIN = "admin";

  private SecuritySettingService securitySettingService;

  @Override
  protected Class<?> getComponentClass() {
    return RegistrationSettingRest.class;
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    securitySettingService = mock(SecuritySettingService.class);
    getContainer().unregisterComponent(SecuritySettingService.class);
    getContainer().registerComponentInstance(SecuritySettingService.class, securitySettingService);
    startUserSession(ADMIN);
  }

  @Override
  public void tearDown() throws Exception {
    getContainer().unregisterComponent(SecuritySettingService.class);
    super.tearDown();
  }

  public void testUpdateRegistrationSettingRequiresConfirmedSession() throws Exception {
    String body = "{\"type\":\"OPEN\",\"extraGroupIds\":[\"/platform/administrators\"]}";

    ContainerResponse response = getResponse("PUT", "/registration/settings", body, unconfirmedContext(ADMIN));
    assertEquals(403, response.getStatus());
    assertEquals(UserAccountConfirmation.OTP_REQUIRED, response.getEntity());
    verify(securitySettingService, never()).saveRegistrationSetting(any(RegistrationSetting.class));

    response = getResponse("PUT", "/registration/settings", body, confirmedContext(ADMIN));
    assertEquals(204, response.getStatus());
    verify(securitySettingService).saveRegistrationSetting(any(RegistrationSetting.class));
  }

  public void testGetRegistrationSettingIsNotGuarded() throws Exception {
    ContainerResponse response = launcher.service("GET", "/registration/settings", "", null, null, unconfirmedContext(ADMIN));
    assertEquals(200, response.getStatus());
  }

}
