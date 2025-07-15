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
package org.exoplatform.portal.mop.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.PageType;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.rest.model.UserNodeBreadcrumbItem;
import org.exoplatform.portal.mop.rest.model.UserNodeRestEntity;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;

public class EntityBuilder {

  private EntityBuilder() {
    // Utils Class, thus private constructor
  }

  public static List<UserNodeRestEntity> toUserNodeRestEntity(Collection<UserNode> nodes,
                                                              boolean expand,
                                                              OrganizationService organizationService,
                                                              LayoutService layoutService,
                                                              UserACL userACL,
                                                              UserPortal userPortal,
                                                              boolean expandBreadcrumb) {
    if (nodes == null) {
      return Collections.emptyList();
    }
    List<UserNodeRestEntity> result = new ArrayList<>();
    for (UserNode userNode : nodes) {
      if (userNode == null) {
        continue;
      }
      UserNodeRestEntity resultNode = new UserNodeRestEntity(userNode);
      if (expand && userNode.getPageRef() != null) {
        Page userNodePage = layoutService.getPage(userNode.getPageRef());
        resultNode.setPageAccessPermissions(userNodePage.getAccessPermissions());
        if (!StringUtils.isBlank(userNodePage.getEditPermission())) {
          resultNode.setCanEditPage(userACL.hasEditPermission(userNodePage, ConversationState.getCurrent().getIdentity()));
          resultNode.setPageEditPermission(userNodePage.getEditPermission());
        }
        if (PageType.LINK.equals(PageType.valueOf(userNodePage.getType()))) {
          resultNode.setPageLink(userNodePage.getLink());
        }
      }
      if (expandBreadcrumb) {
        List<UserNodeBreadcrumbItem> userNodeBreadcrumbItemList = getUserNodeBreadcrumbList(userPortal, layoutService, userNode);
        Collections.reverse(userNodeBreadcrumbItemList);
        resultNode.setUserNodeBreadcrumbItemList(userNodeBreadcrumbItemList);
      }
      resultNode.setChildren(toUserNodeRestEntity(userNode.getChildren(),
                                                  expand,
                                                  organizationService,
                                                  layoutService,
                                                  userACL,
                                                  userPortal,
                                                  expandBreadcrumb));
      result.add(resultNode);
    }
    return result;
  }

  private static List<UserNodeBreadcrumbItem> getUserNodeBreadcrumbList(UserPortal userPortal,
                                                                        LayoutService layoutService,
                                                                        UserNode userNode) {
    UserNavigation userNodeNavigation = userNode.getNavigation();
    UserNode rootNavigationNode = userPortal.getNode(userNodeNavigation, Scope.ALL, UserNodeFilterConfig.builder().build(), null);
    userNode = findTargetNode(userNode.getId(), rootNavigationNode);
    List<UserNodeBreadcrumbItem> userNodeBreadcrumbItemList = new ArrayList<>();
    String portalName = PortalContainer.getCurrentPortalContainerName();
    while (userNode != null && !userNode.getName().equals("default")) {
      userNodeBreadcrumbItemList.add(computeUserNodeBreadcrumbItem(layoutService,
                                                                   userNode,
                                                                   portalName,
                                                                   userNodeNavigation.getKey().getName()));
      userNode = userNode.getParent();
    }
    return userNodeBreadcrumbItemList;
  }

  private static UserNodeBreadcrumbItem computeUserNodeBreadcrumbItem(LayoutService layoutService,
                                                                      UserNode node,
                                                                      String portalName,
                                                                      String siteName) {
    String nodeUri = null;
    if (node.getPageRef() != null) {
      Page userNodePage = layoutService.getPage(node.getPageRef());
      if (PageType.LINK.equals(PageType.valueOf(userNodePage.getType()))) {
        nodeUri = userNodePage.getLink();
      } else {
        if (siteName.contains(("/spaces/"))) {
          siteName = "g/" + siteName.replace("/", ":");
        }
        nodeUri = new StringBuilder("/").append(portalName)
                                        .append("/")
                                        .append(siteName)
                                        .append("/")
                                        .append(node.getURI())
                                        .toString();
      }
    }
    return new UserNodeBreadcrumbItem(node.getId(), node.getName(), node.getResolvedLabel(), nodeUri, node.getTarget());
  }

  private static UserNode findTargetNode(String nodeId, UserNode rootNode) {
    for (UserNode userNode : rootNode.getChildren()) {
      if (userNode.getId().equals(nodeId)) {
        return userNode;
      }
      UserNode targetUserNode = findTargetNode(nodeId, userNode);
      if (targetUserNode != null) {
        return targetUserNode;
      }
    }
    return null;
  }
}
