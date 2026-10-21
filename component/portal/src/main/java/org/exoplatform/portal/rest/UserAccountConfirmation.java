/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package org.exoplatform.portal.rest;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.ExoContainerContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Server-side guard of the administrator actions on user accounts: an action
 * is applied only from a session the administrator confirmed with a one-time
 * password. The confirmation is a marker on the HTTP session of the webapp
 * serving the request, set by the confirmation endpoint only, and read here
 * together with the confirmation settings (the switch and the technical
 * accounts): no header and no credential is ever considered.
 */
public final class UserAccountConfirmation {

  /**
   * Session attribute holding the user name whose actions are confirmed
   */
  public static final String SESSION_ATTRIBUTE = "meeds.userAccount.actionsConfirmed";

  /**
   * Body of the 403 answered to an unconfirmed session
   */
  public static final String OTP_REQUIRED      = "OTP_REQUIRED";

  private UserAccountConfirmation() {
    // Static helper
  }

  /**
   * @param request current request, null when none is bound
   * @return true when the request may perform a guarded action: the
   *         confirmation is switched off, or the user is an exempted technical
   *         account, or the session carries the confirmation of this very
   *         user
   */
  public static boolean isConfirmed(HttpServletRequest request) {
    return isConfirmed(request, getSettings());
  }

  static boolean isConfirmed(HttpServletRequest request, UserAccountConfirmationSettings settings) {
    if (settings != null && !settings.isEnabled()) {
      return true;
    }
    if (request == null) {
      return false;
    }
    String remoteUser = request.getRemoteUser();
    if (StringUtils.isBlank(remoteUser)) {
      return false;
    }
    HttpSession session = request.getSession(false);
    if (session != null && StringUtils.equals(remoteUser, (String) session.getAttribute(SESSION_ATTRIBUTE))) {
      return true;
    }
    return settings != null && settings.hasExemptedUsers() && settings.isExempted(remoteUser);
  }

  /**
   * Marks the request's session as confirmed for the designated user
   *
   * @param request current request
   * @param username user who confirmed the one-time password
   */
  public static void confirm(HttpServletRequest request, String username) {
    request.getSession(true).setAttribute(SESSION_ATTRIBUTE, username);
  }

  /**
   * @return the response answered to a guarded request from an unconfirmed
   *         session
   */
  public static Response otpRequired() {
    return Response.status(Response.Status.FORBIDDEN).entity(OTP_REQUIRED).build();
  }

  private static UserAccountConfirmationSettings getSettings() {
    return ExoContainerContext.getService(UserAccountConfirmationSettings.class);
  }

}
