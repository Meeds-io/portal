/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
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
package org.exoplatform.portal.mop.user;

import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.mop.SiteType;

import lombok.Setter;

public class UserNavigationComparator implements Comparator<UserNavigation> {

  @Setter
  private String globalPortal;

  public int compare(UserNavigation nav1,
                     UserNavigation nav2) {
    if (nav1.getKey().getType() == SiteType.PORTAL && StringUtils.equals(globalPortal, nav1.getKey().getName())) {
      return 1;
    } else if (nav2.getKey().getType() == SiteType.PORTAL && StringUtils.equals(globalPortal, nav2.getKey().getName())) {
      return -1;
    }
    int priority1 = nav1.getPriority();
    int priority2 = nav2.getPriority();
    if (priority1 == priority2) {
      return 0;
    } else if (priority1 == PageNavigation.UNDEFINED_PRIORITY) {
      return 1;
    } else if (priority2 == PageNavigation.UNDEFINED_PRIORITY) {
      return -1;
    } else {
      return priority1 - priority2;
    }
  }

}
