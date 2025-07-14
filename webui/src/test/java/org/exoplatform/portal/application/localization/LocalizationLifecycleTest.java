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
package org.exoplatform.portal.application.localization;

import static org.mockito.Mockito.mockStatic;

import java.util.Locale;

import static org.mockito.Mockito.*;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.LocalePolicy;
import org.exoplatform.web.application.Application;

import jakarta.servlet.http.HttpServletRequest;

@RunWith(MockitoJUnitRunner.class)
public class LocalizationLifecycleTest {

  private static final MockedStatic<LocaleContextInfoUtils> LOCALE_CONTEXT_INFO_UTILS = mockStatic(LocaleContextInfoUtils.class);

  @Mock
  LocaleContextInfo                                         localeContext;

  @Mock
  Application                                               app;

  @Mock
  ExoContainer                                              container;

  @Mock
  LocalePolicy                                              localePolicy;

  @Mock
  LocaleConfigService                                       localeConfigService;

  @Mock
  OrganizationService                                       organizationService;

  @Mock
  HttpServletRequest                                        request;

  @Mock
  PortalRequestContext                                      reqCtx;

  @Before
  public void setup() {
    LOCALE_CONTEXT_INFO_UTILS.when(() -> LocaleContextInfoUtils.buildLocaleContextInfo(request))
                             .thenReturn(localeContext);
    when(app.getApplicationServiceContainer()).thenReturn(container);
    when(container.getComponentInstanceOfType(LocalePolicy.class)).thenReturn(localePolicy);
    when(container.getComponentInstanceOfType(LocaleConfigService.class)).thenReturn(localeConfigService);
    when(container.getComponentInstanceOfType(OrganizationService.class)).thenReturn(organizationService);
    when(reqCtx.getRequest()).thenReturn(request);
  }

  @AfterClass
  public static void afterTestClass() {
    LOCALE_CONTEXT_INFO_UTILS.close();
  }

  @Test
  public void testSaveLocaleWhenLogin() throws Exception {
    LocalizationLifecycle localizationLifecycle = new LocalizationLifecycle();
    localizationLifecycle.onInit(app);
    localizationLifecycle.onStartRequest(app, null);
    LOCALE_CONTEXT_INFO_UTILS.verifyNoInteractions();

    when(localePolicy.determineLocale(localeContext)).thenReturn(Locale.FRENCH);
    localizationLifecycle.onStartRequest(app, reqCtx);
    verify(reqCtx, never()).setAttribute(anyString(), any());

    when(request.getRemoteUser()).thenReturn("test");
    localizationLifecycle.onStartRequest(app, reqCtx);
    verify(reqCtx, times(1)).setAttribute(LocalizationLifecycle.SAVE_PROFILE_LOCALE_ATTR, true);

    when(localePolicy.determineLocale(localeContext)).thenReturn(Locale.ENGLISH);
    when(localeContext.getUserProfileLocale()).thenReturn(Locale.ENGLISH);
    localizationLifecycle.onStartRequest(app, reqCtx);
    verify(reqCtx, times(1)).setAttribute(LocalizationLifecycle.SAVE_PROFILE_LOCALE_ATTR, true);

    when(localePolicy.determineLocale(localeContext)).thenReturn(Locale.FRENCH);
    localizationLifecycle.onStartRequest(app, reqCtx);
    verify(reqCtx, times(2)).setAttribute(LocalizationLifecycle.SAVE_PROFILE_LOCALE_ATTR, true);
  }

}
