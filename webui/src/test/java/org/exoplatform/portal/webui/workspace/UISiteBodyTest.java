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
package org.exoplatform.portal.webui.workspace;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.page.UISiteBody;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;

@RunWith(MockitoJUnitRunner.class)
public class UISiteBodyTest {

  private static final MockedStatic<RequestContext> PORTAL_REQUEST_CONTEXT = mockStatic(RequestContext.class);

  @Mock
  private PortalRequestContext                      pcontext;

  @AfterClass
  public static void afterRunBare() {
    PORTAL_REQUEST_CONTEXT.close();
  }

  @Before
  public void setUp() {
    PORTAL_REQUEST_CONTEXT.when(RequestContext::getCurrentInstance).thenReturn(pcontext);
  }

  @Test
  public void testStandaloneRender() throws Exception {
    AtomicInteger pageBodyRenderCount = new AtomicInteger(0);
    AtomicInteger overallRenderCount = new AtomicInteger(0);

    UISiteBody uiSiteBody = new UISiteBody() {
      @Override
      public void processRender(WebuiRequestContext context) throws Exception {
        getUIComponent();
      }

      @Override
      protected UIComponent getSiteComponent() {
        overallRenderCount.incrementAndGet();
        return null;
      }

      @Override
      protected UIComponent getPageComponent() {
        pageBodyRenderCount.incrementAndGet();
        return null;
      }

    };
    uiSiteBody.processRender(pcontext);

    assertEquals(1, overallRenderCount.get());
    assertEquals(0, pageBodyRenderCount.get());

    when(pcontext.isShowMaxWindow()).thenReturn(true);
    uiSiteBody.processRender(pcontext);

    assertEquals(1, overallRenderCount.get());
    assertEquals(1, pageBodyRenderCount.get());
  }

}
