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

import java.util.Collection;

import org.exoplatform.services.cache.ClusterCachedObjectSelector;
import org.exoplatform.services.cache.ObjectCacheInfo;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.cache.MembershipCacheKey;

/**
 * Selects the cached memberships of a group and the cached membership lists,
 * on every node of a cluster.
 */
public final class ClearMembershipCacheByGroupIdSelector
    extends ClusterCachedObjectSelector<MembershipCacheKey, Object> {
  private static final long serialVersionUID = 1L;

  private final String      groupId;

  public ClearMembershipCacheByGroupIdSelector(String groupId) {
    this.groupId = groupId;
  }

  @Override
  protected Object[] criteria() {
    return new Object[] { groupId };
  }

  @Override
  public boolean select(MembershipCacheKey key, ObjectCacheInfo<? extends Object> ocinfo) {
    Object obj = ocinfo.get();
    if (obj instanceof Membership) {
      Membership cachedMembership = (Membership) ocinfo.get();
      if (cachedMembership.getGroupId().equals(groupId)) {
        return true;
      }
    } else if (obj instanceof Collection) {
      // Delete all cached user's memberships when deleting a group
      return true;
    }
    return false;
  }
}
