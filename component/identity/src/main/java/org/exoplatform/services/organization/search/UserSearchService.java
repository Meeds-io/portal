/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package org.exoplatform.services.organization.search;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;

/**
 * This Service is used to centralize the search of users
 */
public interface UserSearchService {

  /**
   * Search users by a term and having status = ENABLED
   * 
   * @param term query string that will be used to search in user name
   * @return {@link ListAccess} of {@link User}
   * @throws Exception
   */
  default ListAccess<User> searchUsers(String term) throws Exception {
    return this.searchUsers(term, UserStatus.ENABLED);
  }

  /**
   * Search users by a term and its status: ENABLED, DISABLED or ALL
   * 
   * @param term query string that will be used to search in user name
   * @param userStatus user status of type {@link UserStatus}
   * @return {@link ListAccess} of {@link User}
   * @throws Exception
   */
  ListAccess<User> searchUsers(String term, UserStatus userStatus) throws Exception;
}
