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
package io.meeds.portal.plugin;

import org.exoplatform.services.security.Identity;

public interface AclPlugin {

  static final String VIEW_PERMISSION_TYPE   = "VIEW";

  static final String EDIT_PERMISSION_TYPE   = "EDIT";

  static final String DELETE_PERMISSION_TYPE = "DELETE";

  String getObjectType();

  /**
   * @param objectId Object identifier
   * @param permissionType Permission Type, can be : VIEW, EDIT or DELETE. This
   *          parameter can be a custom permission type too, which may be
   *          compatible with some plugins only
   * @param identity User ACL Identity. This will be null when anonymous user
   * @return true if the user can view the identified object
   */
  boolean hasPermission(String objectId, String permissionType, Identity identity);

}
