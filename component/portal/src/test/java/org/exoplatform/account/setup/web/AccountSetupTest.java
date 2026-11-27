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

import org.gatein.wci.ServletContainer;
import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.security.Credentials;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.exoplatform.container.PortalContainer;

public class AccountSetupTest {

  private MockedStatic<PortalContainer>         portalContainer;

  private MockedStatic<ServletContainerFactory> servletContainerFactory;

  private ServletContainer                      servletContainer;

  private AccountSetupService                   accountSetupService;

  private HttpServletRequest                    request;

  private HttpServletResponse                   response;

  private AccountSetup                          servlet;

  @Before
  public void setUp() {
    accountSetupService = mock(AccountSetupService.class);
    PortalContainer container = mock(PortalContainer.class);
    when(container.getComponentInstanceOfType(AccountSetupService.class)).thenReturn(accountSetupService);
    portalContainer = mockStatic(PortalContainer.class);
    portalContainer.when(PortalContainer::getInstance).thenReturn(container);
    portalContainer.when(PortalContainer::getCurrentPortalContainerName).thenReturn("portal");
    servletContainer = mock(ServletContainer.class);
    servletContainerFactory = mockStatic(ServletContainerFactory.class);
    servletContainerFactory.when(ServletContainerFactory::getServletContainer).thenReturn(servletContainer);
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    when(request.getParameter("setupbutton")).thenReturn("setup");
    servlet = new AccountSetup();
  }

  @After
  public void tearDown() {
    portalContainer.close();
    servletContainerFactory.close();
  }

  @Test
  public void doesNotRecordTheSetupWhenItMustBeSkipped() throws Exception {
    when(accountSetupService.mustSkipAccountSetup()).thenReturn(true);

    servlet.doGet(request, response);

    verify(accountSetupService, never()).setSkipSetup(true);
    verify(accountSetupService, never()).createAccount(any(), any(), any(), any(), any(), any());
    verify(response).sendRedirect("/portal");
  }

  @Test
  public void doesNotRecordTheSetupWhenTheSkipButtonIsUsedOnASetUpPlatform() throws Exception {
    when(request.getParameter("setupbutton")).thenReturn("skipform");
    when(accountSetupService.mustSkipAccountSetup()).thenReturn(true);

    servlet.doGet(request, response);

    verify(accountSetupService, never()).setSkipSetup(true);
    verify(response).sendRedirect("/portal");
  }

  @Test
  public void recordsTheSetupWhenTheSkipButtonIsUsed() throws Exception {
    when(request.getParameter("setupbutton")).thenReturn("skipform");

    servlet.doGet(request, response);

    verify(accountSetupService).setSkipSetup(true);
    verify(response).sendRedirect("/portal");
  }

  @Test
  public void doesNotLogInWhenTheAccountCreationIsRefused() throws Exception {
    when(accountSetupService.mustSkipAccountSetup()).thenReturn(false);
    when(accountSetupService.createAccount(any(), any(), any(), any(), any(), any())).thenReturn(false);

    servlet.doGet(request, response);

    servletContainerFactory.verify(ServletContainerFactory::getServletContainer, never());
    verify(response).sendRedirect("/portal");
  }

  @Test
  public void logsInTheCreatedAccount() throws Exception {
    when(request.getParameter("username")).thenReturn("admin");
    when(request.getParameter("password")).thenReturn("password1");
    when(accountSetupService.mustSkipAccountSetup()).thenReturn(false);
    when(accountSetupService.createAccount(any(), any(), any(), any(), any(), any())).thenReturn(true);

    servlet.doGet(request, response);

    verify(servletContainer).login(any(HttpServletRequest.class), eq(response), any(Credentials.class));
    verify(response).sendRedirect("/portal");
  }
}
