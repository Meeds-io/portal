/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package org.exoplatform.services.organization.idm.cache;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.services.cache.ClusterCachedObjectSelector;
import org.exoplatform.services.cache.ObjectCacheInfo;

/**
 * Selects the cached entries of a group, of its subgroups and, when asked, the
 * children lists of the group and of its parent, on every node of a cluster.
 */
public final class ClearGroupCacheByGroupIdSelector extends ClusterCachedObjectSelector<Serializable, Object> {
  private static final long serialVersionUID = 1L;

  private final String      groupId;

  private final String      childrenKey;

  private final String      parentCachedChildrenKey;

  public ClearGroupCacheByGroupIdSelector(String groupId, String parentId, boolean clearCachedChildrenList) {
    this.groupId = groupId;
    this.childrenKey = clearCachedChildrenList ? CacheableGroupHandlerImpl.computeChildrenKey(groupId) : null;
    this.parentCachedChildrenKey = clearCachedChildrenList ? CacheableGroupHandlerImpl.computeChildrenKey(parentId) : null;
  }

  @Override
  protected Object[] criteria() {
    return new Object[] { groupId, childrenKey, parentCachedChildrenKey };
  }

  @Override
  public boolean select(Serializable key, ObjectCacheInfo<? extends Object> ocinfo) {
    String keyString = key.toString();
    if (key.equals(groupId) || keyString.startsWith(groupId + "/")
        || (StringUtils.isNotBlank(childrenKey) && (keyString.equals(childrenKey) || keyString.startsWith(childrenKey + "/")))
        || (StringUtils.isNotBlank(parentCachedChildrenKey) && (keyString.equals(parentCachedChildrenKey)))) {
      return true;
    }
    return false;
  }
}
