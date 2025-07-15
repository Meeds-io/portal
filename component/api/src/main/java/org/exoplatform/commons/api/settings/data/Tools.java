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
package org.exoplatform.commons.api.settings.data;

import org.exoplatform.commons.api.settings.SettingValue;

/**
 * This class is useful to build the object path of {@link SettingValue} with
 * {@link Context} and {@link Scope}.
 * 
 * @LevelAPI Experimental
 */
public class Tools {
  /**
   * Builds a path of a specified scope in the database.
   * 
   * @param context The context with which the path is associated.
   * @param scope The scope with which the path is associated.
   * @return The scope path.
   * @LevelAPI Experimental
   */
  public static String buildScopePath(Context context, Scope scope) {

    StringBuilder path = new StringBuilder().append(buildContextPath(context));
    path.append("/").append(scope.getName().toLowerCase());
    if (scope.getId() != null) {
      path.append("/").append(scope.getId());
    }
    return path.toString();
  }

  /**
   * Builds a path of a specified context in the database.
   * 
   * @param context The context with which the path is associated.
   * @return The context path.
   * @LevelAPI Experimental
   */
  public static String buildContextPath(Context context) {
    StringBuilder path = new StringBuilder().append("settings/")
                                            .append(context.getName()
                                                           .toLowerCase());
    if (context.getId() != null && Context.USER.getName().equals(context.getName())) {
      path.append("/").append(context.getId());
    }
    return path.toString();
  }

}
