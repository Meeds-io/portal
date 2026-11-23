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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.exoplatform.container.PortalContainer;

public class AccountSetupViewServletTest {

  private MockedStatic<PortalContainer> portalContainer;

  private AccountSetupService           accountSetupService;

  private PortalContainer               container;

  private HttpServletRequest            request;

  private HttpServletResponse           response;

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
    servlet = new AccountSetupViewServlet();
  }

  @After
  public void tearDown() {
    portalContainer.close();
  }

  @Test
  public void redirectsWhenTheStoredStateDoesNotAllowTheSetup() throws Exception {
    when(accountSetupService.isAccountSetupAllowed()).thenReturn(false);

    servlet.doGet(request, response);

    verify(response).sendRedirect("/");
    verify(container, never()).getPortalContext();
  }

  @Test
  public void showsTheScreenWhenTheStoredStateAllowsTheSetup() throws Exception {
    when(accountSetupService.isAccountSetupAllowed()).thenReturn(true);
    jakarta.servlet.ServletContext context = mock(jakarta.servlet.ServletContext.class);
    RequestDispatcher dispatcher = mock(RequestDispatcher.class);
    when(container.getPortalContext()).thenReturn(context);
    when(context.getRequestDispatcher("/WEB-INF/jsp/welcome-screens/accountSetup.jsp")).thenReturn(dispatcher);

    servlet.doGet(request, response);

    verify(dispatcher).forward(org.mockito.ArgumentMatchers.any(HttpServletRequest.class), org.mockito.ArgumentMatchers.eq(response));
    verify(response, never()).sendRedirect("/");
  }
}
