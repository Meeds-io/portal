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
package org.exoplatform.portal.config;

import java.util.Set;

import org.exoplatform.portal.config.model.PortalConfig;

/**
 * @deprecated Site Templates has been changed to be stored in database to make
 *             it dynamically managed by UI rather than statis pages and
 *             navigation from source files
 */
@Deprecated(forRemoval = true, since = "7.0")
public class SiteConfigTemplates {
  private String      location;

  private Set<String> portalTemplates;

  private Set<String> groupTemplates;

  private Set<String> userTemplates;

  /**
   * @return the location
   */
  public String getLocation() {
    return location;
  }

  /**
   * @param locationPath the locationPath to set
   */
  public void setLocation(String locationPath) {
    this.location = locationPath;
  }

  /**
   * @return the type
   */
  public Set<String> getTemplates(String type) {
    if (type.equals(PortalConfig.PORTAL_TYPE)) {
      return portalTemplates;
    } else if (type.equals(PortalConfig.GROUP_TYPE)) {
      return groupTemplates;
    } else {
      return userTemplates;
    }
  }
}
