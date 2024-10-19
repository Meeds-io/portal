/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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

import java.util.ArrayList;
import java.util.Collection;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupEventListener;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.OrganizationService;

import io.meeds.common.ContainerTransactional;

/**
 * This listener is used to initialize/destroy the Group Site Config of a group
 * when it is created/deleted
 */
public class GroupPortalConfigListener extends GroupEventListener {

  private UserPortalConfigService portalConfigService;

  private LayoutService           layoutService;

  private OrganizationService     orgService;

  private String                  groupNamePattern;

  public GroupPortalConfigListener(InitParams params,
                                   UserPortalConfigService portalConfigService,
                                   OrganizationService orgService,
                                   LayoutService layoutService) {
    this.portalConfigService = portalConfigService;
    this.orgService = orgService;
    this.layoutService = layoutService;
    if (params != null) {
      ValueParam groupPatternValueParam = params.getValueParam("group.name.pattern");
      if (groupPatternValueParam != null) {
        groupNamePattern = groupPatternValueParam.getValue();
      }
    }
  }

  @Override
  @ContainerTransactional
  public void preDelete(Group group) throws Exception {
    String groupId = group.getId().trim();

    // Remove all descendant navigations
    removeGroupNavigation(group);
    portalConfigService.removeUserPortalConfig(PortalConfig.GROUP_TYPE, groupId);
  }

  @Override
  @ContainerTransactional
  public void postSave(Group group, boolean isNew) throws Exception {
    if (!isNew) {
      return;
    }

    String groupId = group.getId();
    if (groupNamePattern != null
        && groupId.startsWith(groupNamePattern)
        && layoutService.getPortalConfig(SiteKey.group(groupId)) == null) {
      portalConfigService.createGroupSite(groupId);
    }
  }

  private void removeGroupNavigation(Group group) throws Exception {
    GroupHandler groupHandler = orgService.getGroupHandler();
    Collection<String> descendantGroups = getDescendantGroups(group, groupHandler);
    Collection<String> deletedNavigationGroups = new ArrayList<>();
    deletedNavigationGroups.addAll(descendantGroups);
    deletedNavigationGroups.add(group.getId());
    for (String childGroup : deletedNavigationGroups) {
      SiteKey key = SiteKey.group(childGroup);
      NavigationService navService = portalConfigService.getNavigationService();
      NavigationContext nav = navService.loadNavigation(key);
      if (nav != null) {
        navService.destroyNavigation(nav);
      }
    }
  }

  private Collection<String> getDescendantGroups(Group group, GroupHandler groupHandler) throws Exception {
    Collection<Group> groupCollection = groupHandler.findGroups(group);
    Collection<String> col = new ArrayList<>();
    for (Group childGroup : groupCollection) {
      col.add(childGroup.getId());
      col.addAll(getDescendantGroups(childGroup, groupHandler));
    }
    return col;
  }
}
