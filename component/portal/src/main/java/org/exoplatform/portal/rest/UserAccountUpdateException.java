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
package org.exoplatform.portal.rest;

/**
 * An update the store did not complete as requested. Its message is the code
 * a client reads: {@link UserAccountService#USER_PASSWORD_NOT_STORED} and
 * {@link UserAccountService#USER_PASSWORD_NOT_VERIFIABLE} when the password
 * change could not be proven and nothing else was written,
 * {@link UserAccountService#USER_UPDATE_INCOMPLETE} when the read-back after
 * the writes found a field other than requested.
 */
public class UserAccountUpdateException extends Exception {

  private static final long serialVersionUID = -6402911538047326151L;

  public UserAccountUpdateException(String code) {
    super(code);
  }

  public UserAccountUpdateException(String code, Throwable cause) {
    super(code, cause);
  }

}
