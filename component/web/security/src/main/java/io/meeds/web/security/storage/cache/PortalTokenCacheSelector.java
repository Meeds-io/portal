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
package io.meeds.web.security.storage.cache;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.services.cache.CachedObjectSelector;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.ObjectCacheInfo;

import io.meeds.web.security.model.TokenData;

public class PortalTokenCacheSelector implements CachedObjectSelector<String, TokenData> {

  private String username;

  public PortalTokenCacheSelector(String username) {
    this.username = username;
  }

  @Override
  public boolean select(String tokenId, ObjectCacheInfo<? extends TokenData> ocinfo) {
    return tokenId == null
           || ocinfo == null
           || ocinfo.get() == null
           || StringUtils.equals(ocinfo.get().getUsername(), username);
  }

  @Override
  public void onSelect(ExoCache<? extends String, ? extends TokenData> cache,
                       String tokenId,
                       ObjectCacheInfo<? extends TokenData> ocinfo) throws Exception {
    cache.remove(tokenId);
  }

}
