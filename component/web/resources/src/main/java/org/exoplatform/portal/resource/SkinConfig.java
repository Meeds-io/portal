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
package org.exoplatform.portal.resource;

import java.util.List;

/**
 * Extends a skin with additional information. Created by The eXo Platform SAS
 * Jan 19, 2007
 */
public interface SkinConfig extends Skin {

  /**
   * Returns the skin name
   *
   * @return the skin name
   */
  String getName();

  /**
   * Returns the skin module.
   *
   * @return the module
   */
  String getModule();

  /**
   * Returns the css path.
   *
   * @return the css path
   */
  String getCSSPath();

  /**
   * @return the dependent PortalSkins to load with the current Skin
   */
  default List<String> getAdditionalModules() {
    return null; // NOSONAR
  }

  /**
   * @return true is the current PortalSkin is filtered
   */
  default boolean isFiltered() {
    return false;
  }

  /**
   * Sets Skin type
   * 
   * @param type
   */
  default void setType(String type) {
    // Nothing to do
  }

  /**
   * @param cssPath
   */
  default void setCSSPath(String cssPath) {
    throw new UnsupportedOperationException();
  }

  /**
   * @param priority
   */
  default void setPriority(int priority) {
    throw new UnsupportedOperationException();
  }

}
