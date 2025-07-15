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
package org.exoplatform.portal.rest.model;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.services.organization.Group;

public class GroupRestEntity {

  private String                id;

  private String                parentId;

  private String                groupName;

  private String                label;

  private String                description;

  private List<GroupRestEntity> children;

  public GroupRestEntity() {
  }

  public GroupRestEntity(Group group) {
    this.id = group.getId();
    this.parentId = group.getParentId();
    this.groupName = group.getGroupName();
    this.label = group.getLabel();
    this.description = group.getDescription();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<GroupRestEntity> getChildren() {
    return children;
  }

  public void setChildren(List<GroupRestEntity> children) {
    this.children = children;
  }

  public void addChild(GroupRestEntity child) {
    if (this.children == null) {
      this.children = new ArrayList<>();
    }
    if (!this.children.contains(child)) {
      this.children.add(child);
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    GroupRestEntity other = (GroupRestEntity) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }

}
