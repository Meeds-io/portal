/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.commons.addons;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.serialize.PortletApplication;

/**
 * This class is a workaround, eXo Kernel can't config an object field as array
 * of String <br>
 * This class also help to simplify the xml configuration
 */
public class PortletModel extends PortletApplication {
  private String contentId;
  
  private List<String>        permissions;

  private Map<String, Object> portletPrefs;

  public List<String> getPermissions() {
    if (permissions != null) {
      return Collections.unmodifiableList(permissions);
    } else {
      return Arrays.asList(UserACL.EVERYONE);
    }
  }

  public Map<String, Object> getPortletPrefs() {
    if (portletPrefs != null) {
      return new HashMap<String, Object>(portletPrefs);      
    } else {
      return null;
    }
  }
  
  public String getContentId() {
    return contentId;
  }
}