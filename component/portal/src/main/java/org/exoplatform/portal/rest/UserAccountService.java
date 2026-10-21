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

/**
 * Administrator operations on user accounts, reached by the REST resources
 * of this module at call time through the container, since its
 * implementation lives in the web security module, above this one in the
 * module graph.
 */
public interface UserAccountService {

  /**
   * Name of the one-time-password method that confirms an administrator
   * session: a code sent by email, generated and validated for this purpose
   * only, so that a code issued for another flow never confirms a session
   */
  String OTP_METHOD = "userAccountEmail";

  /**
   * Validates and consumes a one-time-password code sent to the user by the
   * {@link #OTP_METHOD} method. A code validates once: a second validation of
   * the same code, even within its validity window, is refused.
   *
   * @param username user who received the code
   * @param otpCode code typed by the user
   * @throws IllegalAccessException when the code is blank, wrong, expired,
   *           already consumed, or when the user exceeded the allowed
   *           attempts
   */
  void confirmSession(String username, String otpCode) throws IllegalAccessException;

}
