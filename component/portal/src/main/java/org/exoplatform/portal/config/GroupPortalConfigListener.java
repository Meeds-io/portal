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

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupEventListener;

import io.meeds.common.ContainerTransactional;

/**
 * This listener is used to remove the Group Site Config of a group when it is deleted
 */
public class GroupPortalConfigListener extends GroupEventListener {

  private LayoutService     layoutService;

  private NavigationService navigationService;

  public GroupPortalConfigListener(LayoutService layoutService,
                                   NavigationService navigationService) {
    this.navigationService = navigationService;
    this.layoutService = layoutService;
  }

  @Override
  @ContainerTransactional
  public void preDelete(Group group) throws Exception {
    SiteKey siteKey = SiteKey.group(group.getId());
    PortalConfig portalConfig = layoutService.getPortalConfig(siteKey);
    if (portalConfig != null) {
      // Remove all descendant navigations
      navigationService.destroyNavigation(siteKey);
      layoutService.removePages(siteKey);
      layoutService.remove(portalConfig);
    }
  }

}
