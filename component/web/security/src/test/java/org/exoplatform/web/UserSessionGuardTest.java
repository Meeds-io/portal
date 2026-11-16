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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.StateKey;

/**
 * A conversation built for a user disabled or deleted, on this node or on
 * another one, ends the session instead of serving the request.
 */
public class UserSessionGuardTest {

  private HttpServletRequest   request;

  private HttpServletResponse  response;

  private HttpSession          session;

  private OrganizationService  organizationService;

  private UserHandler          userHandler;

  private ConversationRegistry conversationRegistry;

  private org.mockito.MockedStatic<PortalContainer> portalContainer;

  @After
  public void tearDown() {
    portalContainer.close();
  }

  @Before
  public void setUp() {
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    session = mock(HttpSession.class);
    when(session.getId()).thenReturn("session");
    when(request.getSession(false)).thenReturn(session);
    when(request.getContextPath()).thenReturn("/portal");
    portalContainer = org.mockito.Mockito.mockStatic(PortalContainer.class);
    portalContainer.when(PortalContainer::getCurrentPortalContainerName).thenReturn("portal");
    when(request.getRequestURI()).thenReturn("//other.example/dw");
    organizationService = mock(OrganizationService.class);
    userHandler = mock(UserHandler.class);
    when(organizationService.getUserHandler()).thenReturn(userHandler);
    conversationRegistry = mock(ConversationRegistry.class);
  }

  @Test
  public void testTheSessionOfADisabledUserEnds() throws Exception {
    User user = mock(User.class);
    when(user.isEnabled()).thenReturn(false);
    when(userHandler.findUserByName("john", UserStatus.ANY)).thenReturn(user);
    when(request.getMethod()).thenReturn("GET");

    assertTrue(guard(new ConversationState(new Identity("john"))));

    verify(conversationRegistry).unregister(any(StateKey.class), org.mockito.ArgumentMatchers.eq(false));
    verify(session).invalidate();
    ArgumentCaptor<Cookie> cookie = ArgumentCaptor.forClass(Cookie.class);
    verify(response).addCookie(cookie.capture());
    assertTrue(cookie.getValue().getMaxAge() == 0);
    verify(response).sendRedirect("/portal/");
  }

  @Test
  public void testAGetOutsideThePortalWebappIsRefusedNotRedirected() throws Exception {
    when(request.getMethod()).thenReturn("GET");
    when(request.getContextPath()).thenReturn("/rest");

    assertTrue(guard(new ConversationState(new Identity("john"))));

    verify(session).invalidate();
    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
    verify(response, never()).sendRedirect(anyString());
  }

  @Test
  public void testTheSessionOfADeletedUserEndsAndAWriteIsRefused() throws Exception {
    when(request.getMethod()).thenReturn("POST");

    assertTrue(guard(new ConversationState(new Identity("john"))));

    verify(session).invalidate();
    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  public void testAnEnabledUserIsReadOncePerConversation() throws Exception {
    User user = mock(User.class);
    when(user.isEnabled()).thenReturn(true);
    when(userHandler.findUserByName("john", UserStatus.ANY)).thenReturn(user);
    ConversationState state = new ConversationState(new Identity("john"));

    assertFalse(guard(state));
    assertFalse(guard(state));

    assertSame(user, state.getAttribute(CacheUserProfileFilter.USER_PROFILE));
    verify(userHandler, org.mockito.Mockito.times(1)).findUserByName(anyString(), any(UserStatus.class));
    verify(session, never()).invalidate();
  }

  @Test
  public void testAnAnonymousRequestIsLeftAlone() throws Exception {
    assertFalse(guard(new ConversationState(new Identity(IdentityConstants.ANONIM))));
    assertFalse(guard(null));

    verify(userHandler, never()).findUserByName(anyString(), any(UserStatus.class));
  }

  private boolean guard(ConversationState state) throws Exception {
    return UserSessionGuard.endSessionOfAGoneUser(request, response, state, organizationService, conversationRegistry);
  }

}
