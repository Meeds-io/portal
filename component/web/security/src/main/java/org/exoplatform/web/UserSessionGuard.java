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

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.web.HttpSessionStateKey;
import org.exoplatform.web.login.LoginUtils;

/**
 * Reads the user of a conversation once, when the conversation is built, and
 * ends the session of a user that is disabled or no longer exists. A change of
 * the user makes every node build its conversations again (ConversationRegistry
 * invalidateUser), so each node reads the user again at the next request.
 */
public final class UserSessionGuard {

  private static final Log LOG = ExoLogger.getLogger(UserSessionGuard.class);

  private UserSessionGuard() {
  }

  /**
   * @param request the current request
   * @param response the current response
   * @param state the conversation of the request
   * @param organizationService the organization service
   * @param conversationRegistry the conversation registry
   * @return true when the user is disabled or deleted: the session is ended
   *         and the response written, the caller stops the request
   * @throws IOException when the response cannot be written
   */
  public static boolean endSessionOfAGoneUser(HttpServletRequest request,
                                              HttpServletResponse response,
                                              ConversationState state,
                                              OrganizationService organizationService,
                                              ConversationRegistry conversationRegistry) throws IOException {
    if (state == null
        || state.getIdentity() == null
        || IdentityConstants.ANONIM.equals(state.getIdentity().getUserId())
        || state.getAttribute(CacheUserProfileFilter.USER_PROFILE) != null) {
      return false;
    }
    User user = findUser(organizationService, state.getIdentity().getUserId());
    if (user != null && user.isEnabled()) {
      state.setAttribute(CacheUserProfileFilter.USER_PROFILE, user);
      return false;
    }
    HttpSession session = request.getSession(false);
    if (session != null) {
      conversationRegistry.unregister(new HttpSessionStateKey(session), false);
      session.invalidate();
    }
    try {
      request.logout();
    } catch (ServletException e) {
      LOG.debug("Cannot log out the request of a disabled or deleted user", e);
    }
    LoginUtils.clearRememberMeCookie(request, response);
    if (isPortalNavigation(request)) {
      response.sendRedirect(request.getContextPath() + "/");
    } else {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
    return true;
  }

  /**
   * Only a GET of the portal webapp is a page navigation redirected to the
   * portal root; any other request is answered with 401.
   */
  private static boolean isPortalNavigation(HttpServletRequest request) {
    return "GET".equalsIgnoreCase(request.getMethod())
           && ("/" + PortalContainer.getCurrentPortalContainerName()).equals(request.getContextPath());
  }

  private static User findUser(OrganizationService organizationService, String userId) {
    boolean begun = organizationService instanceof ComponentRequestLifecycle;
    if (begun) {
      RequestLifeCycle.begin((ComponentRequestLifecycle) organizationService);
    }
    try {
      return organizationService.getUserHandler().findUserByName(userId, UserStatus.ANY);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot read the user " + userId, e);
    } finally {
      if (begun) {
        RequestLifeCycle.end();
      }
    }
  }

}
