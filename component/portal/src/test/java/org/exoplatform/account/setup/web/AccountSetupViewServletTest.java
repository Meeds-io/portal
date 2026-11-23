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
package org.exoplatform.account.setup.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.exoplatform.container.PortalContainer;

public class AccountSetupViewServletTest {

  private MockedStatic<PortalContainer> portalContainer;

  private AccountSetupService           accountSetupService;

  private PortalContainer               container;

  private HttpServletRequest            request;

  private HttpServletResponse           response;

  private RequestDispatcher             dispatcher;

  private AccountSetupViewServlet       servlet;

  @Before
  public void setUp() {
    accountSetupService = mock(AccountSetupService.class);
    container = mock(PortalContainer.class);
    when(container.getComponentInstanceOfType(AccountSetupService.class)).thenReturn(accountSetupService);
    portalContainer = mockStatic(PortalContainer.class);
    portalContainer.when(PortalContainer::getInstance).thenReturn(container);
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    ServletContext context = mock(ServletContext.class);
    dispatcher = mock(RequestDispatcher.class);
    when(container.getPortalContext()).thenReturn(context);
    when(context.getRequestDispatcher("/WEB-INF/jsp/welcome-screens/accountSetup.jsp")).thenReturn(dispatcher);
    // The cache says "not set up": only the stored state may decide
    when(accountSetupService.mustSkipAccountSetup()).thenReturn(false);
    servlet = new AccountSetupViewServlet();
  }

  @After
  public void tearDown() {
    portalContainer.close();
  }

  @Test
  public void redirectsWhenTheStoredStateSaysTheSetupIsDone() throws Exception {
    when(accountSetupService.getStoredSetupDone()).thenReturn(true);

    servlet.doGet(request, response);

    verify(response).sendRedirect("/");
    verify(dispatcher, never()).forward(any(), any());
  }

  @Test
  public void answersUnavailableWhenTheStoredStateCannotBeRead() throws Exception {
    when(accountSetupService.getStoredSetupDone()).thenReturn(null);

    servlet.doGet(request, response);

    verify(response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    verify(response, never()).sendRedirect("/");
    verify(dispatcher, never()).forward(any(), any());
  }

  @Test
  public void showsTheScreenWhenTheStoredStateSaysTheSetupIsNotDone() throws Exception {
    when(accountSetupService.getStoredSetupDone()).thenReturn(false);

    servlet.doGet(request, response);

    verify(dispatcher).forward(any(HttpServletRequest.class), eq(response));
    verify(response, never()).sendRedirect("/");
  }
}
