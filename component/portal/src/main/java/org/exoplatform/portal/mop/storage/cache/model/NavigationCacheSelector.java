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
import org.exoplatform.portal.mop.navigation.NavigationData;
import org.exoplatform.services.cache.CachedObjectSelector;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.ObjectCacheInfo;

import lombok.Data;

@Data
public class NavigationCacheSelector implements CachedObjectSelector<SiteKey, NavigationData> {

  private SiteKey key;

  public NavigationCacheSelector(SiteKey key) {
    this.key = key;
  }

  @Override
  public boolean select(final SiteKey siteKey, final ObjectCacheInfo<? extends NavigationData> ocinfo) {
    return Objects.equals(key, siteKey) || Objects.equals(key, ocinfo.get().getSiteKey());
  }

  @Override
  public void onSelect(ExoCache<? extends SiteKey, ? extends NavigationData> cache,
                       SiteKey key,
                       ObjectCacheInfo<? extends NavigationData> ocinfo) throws Exception {
    cache.remove(key);
  }

}
