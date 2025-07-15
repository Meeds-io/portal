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
package org.exoplatform.web.handler;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.URIWriter;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;

public class DefaultRequestHandlerTest {

  private static final Log LOG = ExoLogger.getLogger(DefaultRequestHandlerTest.class);

  @Test
  public void testGetDefaultSite() {
    NodeURL url = mock(NodeURL.class);
    when(url.toString()).thenCallRealMethod();
    when(url.setResource(any(NavigationResource.class))).thenCallRealMethod();
    UserPortalConfigService portalConfigService = mock(UserPortalConfigService.class);
    ControllerContext context = mock(ControllerContext.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpServletRequest request = mock(HttpServletRequest.class);

    try {
      when(context.getResponse()).thenReturn(response);
      when(context.getRequest()).thenReturn(request);
      when(request.getRemoteUser()).thenReturn("root");
      when(portalConfigService.getDefaultPath(request.getRemoteUser())).thenReturn("/portal/site2/node");
      when(response.encodeRedirectURL(anyString())).thenAnswer(new Answer<>() {
        public String answer(InvocationOnMock invocation) {
          return invocation.getArgument(0, String.class);
        }
      });

      DefaultRequestHandler defaultRequestHandler = new DefaultRequestHandler(portalConfigService);
      defaultRequestHandler.execute(context);
      verify(response).sendRedirect("/portal/site2/node");
    } catch (Exception e) {
      LOG.error("Error while executing method", e);
      fail(e.getMessage());
    }
  }

  @Test
  public void testGetUserDefaultUri() {
    UserPortalConfigService portalConfigService = mock(UserPortalConfigService.class);
    ControllerContext context = mock(ControllerContext.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpServletRequest request = mock(HttpServletRequest.class);

    try {
      when(context.getResponse()).thenReturn(response);
      when(context.getRequest()).thenReturn(request);
      DefaultRequestHandler defaultRequestHandler = new DefaultRequestHandler(portalConfigService);
      defaultRequestHandler.execute(context);
      verify(response).sendRedirect("/portal/login");

      when(request.getRemoteUser()).thenReturn("user");
      when(portalConfigService.getMetaPortal()).thenReturn("classic");

      when(response.encodeRedirectURL(anyString())).thenAnswer(new Answer<>() {
        public String answer(InvocationOnMock invocation) {
          return invocation.getArgument(0, String.class);
        }
      });
      defaultRequestHandler.execute(context);
      verify(response).sendRedirect("/portal/classic/page-not-found");

      when(portalConfigService.getDefaultPath(eq("user"))).thenReturn("/portal/home");
      defaultRequestHandler.execute(context);
      verify(response).sendRedirect("/portal/home");
    } catch (Exception e) {
      LOG.error("Error while executing method", e);
      fail(e.getMessage());
    }
  }

  @Test
  public void testGetNonDefaultSite() {
    NodeURL url = mock(NodeURL.class);
    when(url.toString()).thenCallRealMethod();
    UserPortalConfigService portalConfigService = mock(UserPortalConfigService.class);
    ControllerContext context = mock(ControllerContext.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpServletRequest request = mock(HttpServletRequest.class);

    try {
      when(context.getResponse()).thenReturn(response);
      when(context.getRequest()).thenReturn(request);
      when(request.getRemoteUser()).thenReturn("root");
      when(request.getRemoteUser()).thenReturn("user");
      when(portalConfigService.getDefaultPath(anyString())).thenReturn("/portal/site1");
      doAnswer(invocation -> {
        @SuppressWarnings("unchecked")
        Map<QualifiedName, String> parameters = invocation.getArgument(0, Map.class);
        URIWriter uriWriter = invocation.getArgument(1, URIWriter.class);
        uriWriter.append("/portal/");
        uriWriter.append(parameters.get(NodeURL.REQUEST_SITE_NAME));
        return null;
      }).when(context).renderURL(any(), any());

      when(response.encodeRedirectURL(anyString())).thenAnswer(new Answer<String>() {
        public String answer(InvocationOnMock invocation) {
          return invocation.getArgument(0, String.class);
        }
      });
       DefaultRequestHandler defaultRequestHandler = new DefaultRequestHandler(portalConfigService);
      defaultRequestHandler.execute(context);
      verify(response).sendRedirect(eq("/portal/site1"));
    } catch (Exception e) {
      LOG.error("Error while executing method", e);
      fail(e.getMessage());
    }
  }

  @Test
  public void testGetLoginPageWhenNoSite() {
    UserPortalConfigService portalConfigService = mock(UserPortalConfigService.class);
    ControllerContext context = mock(ControllerContext.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpServletRequest request = mock(HttpServletRequest.class);

    try {
      when(context.getResponse()).thenReturn(response);
      when(context.getRequest()).thenReturn(request);

      when(response.encodeRedirectURL(anyString())).thenAnswer(invocation -> invocation.getArgument(0, String.class));

      DefaultRequestHandler defaultRequestHandler = new DefaultRequestHandler(portalConfigService);
      defaultRequestHandler.execute(context);
      verify(response).sendRedirect(eq("/portal/login"));
    } catch (Exception e) {
      LOG.error("Error while executing method", e);
      fail(e.getMessage());
    }
  }

}
