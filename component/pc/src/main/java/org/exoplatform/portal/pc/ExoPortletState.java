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
package org.exoplatform.portal.pc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExoPortletState implements Serializable {

  private static final long                   serialVersionUID = -6281095288237675907L;

  /** . */
  private final String                        portletId;

  /** . */
  private final HashMap<String, List<String>> state;

  public ExoPortletState(String portletId) {
    this.portletId = portletId;
    this.state = new HashMap<>();
  }

  public String getPortletId() {
    return portletId;
  }

  public Map<String, List<String>> getState() {
    return state;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ExoPortletState that) {
      return portletId.equals(that.portletId) && state.equals(that.state);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return portletId.hashCode() ^ state.hashCode();
  }

  @Override
  public String toString() {
    return "ExoPortletState[portletId=" + portletId + ",state=" + state + "]";
  }
}
