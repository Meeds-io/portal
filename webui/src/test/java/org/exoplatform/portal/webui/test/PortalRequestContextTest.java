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
package org.exoplatform.portal.webui.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ResourceBundle;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.utils.ExpressionUtil;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.web.application.Application;

import junit.framework.TestCase;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PortalRequestContextTest extends TestCase {

  private static MockedStatic<ExpressionUtil> EXPRESSION_UTIL;

  @BeforeClass
  public static void beforeClass() {
    EXPRESSION_UTIL = mockStatic(ExpressionUtil.class);
  }

  @AfterClass
  public static void afterClass() {
    EXPRESSION_UTIL.close();
  }

  @Test
  public void testGetTitle() throws Exception {

    PortalRequestContext prc = mock(PortalRequestContext.class);
    HttpServletRequest request = mock(HttpServletRequest.class, Mockito.CALLS_REAL_METHODS);
    ExoContainer container = mock(ExoContainer.class);
    UserNode userNode = mock(UserNode.class);
    UserPortalConfigService configService = mock(UserPortalConfigService.class);
    PortalRequestContext.portalConfigService = configService;
    UIPortal uiPortal = mock(UIPortal.class);
    PageState state = mock(PageState.class);
    PageContext page = mock(PageContext.class);
    Application app = mock(Application.class);

    ResourceBundle bundle = ResourceBundle.getBundle("test");

    when(prc.getUiPortal()).thenReturn(uiPortal);
    EXPRESSION_UTIL.when(() -> ExpressionUtil.getExpressionValue(bundle, "title")).thenCallRealMethod();

    doCallRealMethod().when(prc).setPageTitle(anyString());

    when(prc.getTitle()).thenCallRealMethod();

    when(container.getComponentInstanceOfType(UserPortalConfigService.class)).thenReturn(configService);
    when(configService.getPage(any(), any())).thenReturn(page);
    when(page.getState()).thenReturn(state);
    when(state.getDisplayName()).thenReturn("title");
    when(userNode.getResolvedLabel()).thenReturn("test");
    when(app.getApplicationServiceContainer()).thenReturn(container);
    when(prc.getApplication()).thenReturn(app);
    when(prc.getApplicationResourceBundle()).thenReturn(bundle);
    when(uiPortal.getSelectedUserNode()).thenReturn(userNode);
    when(prc.getRequest()).thenReturn(request);

    request.setAttribute(PortalRequestContext.REQUEST_TITLE, "title");
    assertEquals("test", prc.getTitle());

    prc.setPageTitle("otherTest");
    assertEquals("otherTest", prc.getTitle());

  }
}
