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
 * A creation the store did not complete as requested. Its message is the code
 * a client reads: {@link UserAccountService#USER_CREATION_FAILED} and
 * {@link UserAccountService#USER_USERNAME_GENERATION_EXHAUSTED} when nothing
 * was created, {@link UserAccountService#USER_CREATED_INCOMPLETE} and
 * {@link UserAccountService#USER_CREATED_INCOMPLETE_ENABLED} followed by the
 * login and the parts the read-back found missing.
 */
public class UserAccountCreationException extends Exception {

  private static final long serialVersionUID = 4297134067621889235L;

  public UserAccountCreationException(String code) {
    super(code);
  }

  public UserAccountCreationException(String code, Throwable cause) {
    super(code, cause);
  }

}
