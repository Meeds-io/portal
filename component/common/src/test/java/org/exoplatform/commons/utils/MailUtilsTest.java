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
package org.exoplatform.commons.utils;

import static org.exoplatform.commons.utils.MailUtils.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.branding.BrandingService;

@RunWith(MockitoJUnitRunner.class)
public class MailUtilsTest {

  private static final MockedStatic<ExoContainerContext> EXO_CONTAINER_CONTEXT = mockStatic(ExoContainerContext.class);

  private static ExoContainer                            container             = mock(ExoContainer.class);

  @Mock
  private BrandingService                                brandingService;

  @Mock
  private SettingService                                 settingService;

  @BeforeClass
  public static void beforeRunBare() {
    EXO_CONTAINER_CONTEXT.when(() -> ExoContainerContext.getCurrentContainer()).thenReturn(container);
  }

  @AfterClass
  public static void afterRunBare() {
    EXO_CONTAINER_CONTEXT.close();
  }

  @Before
  public void before() {
    when(container.getComponentInstanceOfType(BrandingService.class)).thenReturn(brandingService);
    when(container.getComponentInstanceOfType(SettingService.class)).thenReturn(settingService);
  }

  @SuppressWarnings({
      "rawtypes", "unchecked"
  })
  @Test
  public void testGetSenderName() {
    String senderName = MailUtils.getSenderName();
    assertNull(senderName);

    String companyName = "Company Name";
    when(brandingService.getCompanyName()).thenReturn(companyName);
    senderName = MailUtils.getSenderName();
    assertEquals(companyName, senderName);

    companyName = "Company Name From Settings";
    SettingValue settingValue = SettingValue.create(companyName);
    when(settingService.get(Context.GLOBAL, Scope.GLOBAL.id(null), SENDER_NAME_PARAM)).thenReturn(settingValue);
    senderName = MailUtils.getSenderName();
    assertEquals(companyName, senderName);
  }

  @SuppressWarnings({
      "rawtypes", "unchecked"
  })
  @Test
  public void testGetSenderEmail() {
    String senderEmail = MailUtils.getSenderEmail();
    assertEquals(DEFAULT_FROM_EMAIL, senderEmail);

    String companyEmail = "email.property@test.com";
    System.setProperty("gatein.email.smtp.from", companyEmail);
    senderEmail = MailUtils.getSenderEmail();
    assertEquals(companyEmail, senderEmail);

    companyEmail = "email.settings@test.com";
    SettingValue settingValue = SettingValue.create(companyEmail);
    when(settingService.get(Context.GLOBAL, Scope.GLOBAL.id(null), SENDER_EMAIL_PARAM)).thenReturn(settingValue);
    senderEmail = MailUtils.getSenderEmail();
    assertEquals(companyEmail, senderEmail);
  }

}
