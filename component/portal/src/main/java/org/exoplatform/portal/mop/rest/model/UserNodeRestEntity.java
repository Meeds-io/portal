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
package org.exoplatform.portal.mop.rest.model;

import java.util.List;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.user.UserNode;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserNodeRestEntity {

  private List<UserNodeRestEntity> subNodes;

  private boolean                  canEditPage;

  private String[]                 pageAccessPermissions;

  private String                   pageEditPermission;

  private String                   pageLink;

  private String                   id;

  private String                   name;

  private Visibility               visibility;

  private String                   label;

  private String                   labelKey;

  private String                   uri;

  private String                   icon;

  private String                   target;

  private SiteKey                  siteKey;

  private PageKey                  pageKey;

  private long                     startPublicationTime;

  private long                     endPublicationTime;

  private long                     updatedDate;

  public UserNodeRestEntity(UserNode userNode) {
    setUserNode(userNode);
  }

  public void setUserNode(UserNode userNode) {
    if (userNode == null) {
      return;
    }
    this.label = userNode.getResolvedLabel();
    this.labelKey = userNode.getLabel();
    this.id = userNode.getId();
    this.uri = userNode.getURI();
    this.icon = userNode.getIcon();
    this.visibility = userNode.getVisibility();
    this.name = userNode.getName();
    this.startPublicationTime = userNode.getStartPublicationTime();
    this.endPublicationTime = userNode.getEndPublicationTime();
    this.siteKey = userNode.getNavigation() == null ? null : userNode.getNavigation().getKey();
    this.pageKey = userNode.getPageRef();
    this.target = userNode.getTarget();
    this.updatedDate = userNode.getUpdatedDate();
  }

  private List<UserNodeBreadcrumbItem> userNodeBreadcrumbItemList;

  public void setChildren(List<UserNodeRestEntity> subNodes) {
    this.subNodes = subNodes;
  }

  public List<UserNodeRestEntity> getChildren() {
    return subNodes;
  }

}
