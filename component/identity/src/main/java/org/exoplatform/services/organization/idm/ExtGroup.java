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
package org.exoplatform.services.organization.idm;

import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.impl.GroupImpl;

public class ExtGroup extends GroupImpl implements Comparable<Object> {
  private static final long serialVersionUID = -7379104016885124958L;

  public ExtGroup() {
  }

  public ExtGroup(String name) {
    super(name);
  }

  public String toString() {
    return "Group[" + getId() + "|" + getGroupName() + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ExtGroup)) {
      return false;
    }

    ExtGroup extGroup = (ExtGroup) o;

    if (getDescription() != null ? !getDescription().equals(extGroup.getDescription()) : extGroup.getDescription() != null) {
      return false;
    }
    if (getGroupName() != null ? !getGroupName().equals(extGroup.getGroupName()) : extGroup.getGroupName() != null) {
      return false;
    }
    if (getId() != null ? !getId().equals(extGroup.getId()) : extGroup.getId() != null) {
      return false;
    }
    if (getLabel() != null ? !getLabel().equals(extGroup.getLabel()) : extGroup.getLabel() != null) {
      return false;
    }
    if (getParentId() != null ? !getParentId().equals(extGroup.getParentId()) : extGroup.getParentId() != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = getId() != null ? getId().hashCode() : 0;
    result = 31 * result + (getParentId() != null ? getParentId().hashCode() : 0);
    result = 31 * result + (getGroupName() != null ? getGroupName().hashCode() : 0);
    result = 31 * result + (getLabel() != null ? getLabel().hashCode() : 0);
    result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
    return result;
  }

  public int compareTo(Object o) {
    if (!(o instanceof Group)) {
      return 0;
    }

    Group group = (Group) o;

    return getGroupName().compareTo(group.getGroupName());

  }
}
