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
package org.exoplatform.portal.mop.storage.cache.model;

import java.util.Objects;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.services.cache.CachedObjectSelector;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.ObjectCacheInfo;

import lombok.Data;

@Data
public class NavigationDataCacheSelector implements CachedObjectSelector<Long, NodeData> {

  private SiteKey key;

  private Long    nodeId;

  public NavigationDataCacheSelector(SiteKey key, Long nodeId) {
    this.key = key;
    this.nodeId = nodeId;
  }

  @Override
  public boolean select(final Long nodeKey, final ObjectCacheInfo<? extends NodeData> ocinfo) {
    return Objects.equals(nodeId, nodeKey)
        || Objects.equals(key, ocinfo.get().getState().getSiteKey())
        || Objects.equals(String.valueOf(nodeId), ocinfo.get().getParentId());
  }

  @Override
  public void onSelect(ExoCache<? extends Long, ? extends NodeData> cache,
                       Long key,
                       ObjectCacheInfo<? extends NodeData> ocinfo) throws Exception {
    cache.remove(key);
  }
}
