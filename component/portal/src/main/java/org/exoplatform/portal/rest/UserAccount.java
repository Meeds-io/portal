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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * An account as an administrator describes it to create it: the password is
 * carried from the client to the store and never read back.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {

  /**
   * The login, blank when the platform generates it
   */
  private String  userName;

  private String  firstName;

  private String  lastName;

  private String  email;

  /**
   * Blank when the user sets their own password from the onboarding email
   */
  @ToString.Exclude
  private String  password;

  /**
   * The preferred language, a supported locale name, blank for none
   */
  private String  language;

  /**
   * The role as sent by the client, see {@link UserAccountRole#parse(String)}
   */
  private String  role;

  private boolean enabled = true;

}
