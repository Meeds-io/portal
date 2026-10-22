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

import org.apache.commons.lang3.StringUtils;

/**
 * The role an administrator gives to an account at its creation. It is the
 * only client value that decides the account's memberships, among these three
 * fixed values: no client value names a group or a membership type.
 */
public enum UserAccountRole {

  /**
   * A member of the platform users group, the membership every new account
   * receives
   */
  INTERNAL,

  /**
   * A member of the platform externals group instead of the platform users
   * group
   */
  EXTERNAL,

  /**
   * An internal user holding the membership the platform requires to grant
   * administration rights
   */
  ADMINISTRATOR;

  /**
   * @param value the role as sent by a client, case-insensitive
   * @return the role, {@link #INTERNAL} when the value is blank
   * @throws IllegalArgumentException with message {@code ROLE:INVALID} when
   *           the value names none of the roles
   */
  public static UserAccountRole parse(String value) {
    if (StringUtils.isBlank(value)) {
      return INTERNAL;
    }
    for (UserAccountRole role : values()) {
      if (role.name().equalsIgnoreCase(value.trim())) {
        return role;
      }
    }
    throw new IllegalArgumentException(UserAccountService.ROLE_INVALID);
  }

}
