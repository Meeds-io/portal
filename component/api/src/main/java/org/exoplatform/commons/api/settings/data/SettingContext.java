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

import java.io.Serializable;

/**
 * Associates setting properties with a specified context (GLOBAL/USER). This is
 * used to specify context of setting properties at the Context level when
 * working with database and cache or dispatching the setting event.
 */
public class SettingContext implements Serializable {

  private static final long serialVersionUID = 437625857263645213L;

  /**
   * Context of the setting object.
   */
  protected Context         context;

  /**
   * Path of the context.
   */
  protected String          contextPath;

  /**
   * Creates a SettingContext object with a specified context type.
   * 
   * @param context The context type.
   * @LevelAPI Experimental
   */
  public SettingContext(Context context) {
    super();
    this.context = context;
    this.contextPath = Tools.buildContextPath(context);
  }

  /**
   * Compares a specified object with the SettingContext for equality.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (this == obj) {
      return true;
    }

    if (obj instanceof SettingContext dest) {
      return this.getContextPath().equals(dest.getContextPath());
    }
    return false;
  }

  /**
   * Returns the hash code value for the SettingContext object.
   */
  @Override
  public int hashCode() {
    return contextPath.hashCode();
  }

  /**
   * Gets path of the SettingContext object.
   * 
   * @return The setting context path.
   * @LevelAPI Experimental
   */
  public String getContextPath() {
    return contextPath;
  }

  /**
   * Gets a context object associated with the SettingContext object.
   * 
   * @return The context object.
   * @LevelAPI Experimental
   */
  public Context getContext() {
    return context;
  }
}
