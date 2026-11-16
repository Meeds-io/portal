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
package org.exoplatform.web;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

/**
 * A request whose user cannot be read is answered with 503 and never reaches
 * the chain; a failure of the chain itself is not swallowed.
 */
public class CacheUserProfileFilterTest {

  private HttpServletRequest  request;

  private HttpServletResponse response;

  private FilterChain         chain;

  private UserHandler         userHandler;

  @Before
  public void setUp() {
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    chain = mock(FilterChain.class);
    OrganizationService organizationService = mock(OrganizationService.class);
    userHandler = mock(UserHandler.class);
    when(organizationService.getUserHandler()).thenReturn(userHandler);
    ExoContainer container = mock(ExoContainer.class);
    when(container.getComponentInstanceOfType(OrganizationService.class)).thenReturn(organizationService);
    when(container.getComponentInstanceOfType(ConversationRegistry.class)).thenReturn(mock(ConversationRegistry.class));
    ExoContainerContext.setCurrentContainer(container);
    ConversationState.setCurrent(new ConversationState(new Identity("john")));
  }

  @After
  public void tearDown() {
    ConversationState.setCurrent(null);
    ExoContainerContext.setCurrentContainer(null);
  }

  @Test
  public void testAFailedUserReadIsAnswered503() throws Exception {
    when(userHandler.findUserByName("john", UserStatus.ANY)).thenThrow(new IllegalStateException("store down"));

    new CacheUserProfileFilter().doFilter(request, response, chain);

    verify(response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    verify(chain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
  }

  @Test
  public void testAFailureOfTheChainIsNotSwallowed() throws Exception {
    org.exoplatform.services.organization.User user = mock(org.exoplatform.services.organization.User.class);
    when(user.isEnabled()).thenReturn(true);
    when(userHandler.findUserByName("john", UserStatus.ANY)).thenReturn(user);
    ServletException failure = new ServletException("chain");
    doThrow(failure).when(chain).doFilter(request, response);

    ServletException thrown = assertThrows(ServletException.class,
                                           () -> new CacheUserProfileFilter().doFilter(request, response, chain));

    assertSame(failure, thrown);
    verify(response, never()).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
  }

}
