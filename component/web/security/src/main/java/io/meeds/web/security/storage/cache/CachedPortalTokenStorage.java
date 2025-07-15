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

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.web.security.security.TokenExistsException;

import io.meeds.web.security.dao.TokenDAO;
import io.meeds.web.security.model.TokenData;
import io.meeds.web.security.storage.PortalTokenStorage;

import lombok.SneakyThrows;

public class CachedPortalTokenStorage extends PortalTokenStorage {

  public static final String                        CACHE_NAME = "portal.tokens";

  public static final TokenData                     NULL_TOKEN = new TokenData();

  private ExoCache<String, TokenData>               cache;

  private FutureExoCache<String, TokenData, Object> futureCache;

  public CachedPortalTokenStorage(CacheService cacheService,
                                  TokenDAO tokenDAO) {
    super(tokenDAO);
    cache = cacheService.getCacheInstance(CACHE_NAME);
    Loader<String, TokenData, Object> loader = new Loader<>() {
      @Override
      public TokenData retrieve(Object context, String tokenId) throws Exception {
        TokenData token = CachedPortalTokenStorage.super.getToken(tokenId);
        return token == null ? NULL_TOKEN : token;
      }
    };
    this.futureCache = new FutureExoCache<>(loader, cache);
  }

  @Override
  public void createToken(TokenData data) throws TokenExistsException {
    try {
      super.createToken(data);
    } finally {
      if (data.getTokenId() != null) {
        clearKey(data.getTokenId());
      }
    }
  }

  @Override
  @ExoTransactional
  public void deleteTokensByUsernameAndType(String username, String tokenType) {
    try {
      super.deleteTokensByUsernameAndType(username, tokenType);
    } finally {
      clearByUser(username);
    }
  }

  @Override
  public TokenData getToken(String tokenId) {
    TokenData token = futureCache.get(null, tokenId);
    return token == null || token.getTokenId() == null ? null : token;
  }

  @Override
  public void deleteToken(String tokenId) {
    try {
      super.deleteToken(tokenId);
    } finally {
      clearKey(tokenId);
    }
  }

  @Override
  public void deleteTokenOfUser(String username) {
    try {
      super.deleteTokenOfUser(username);
    } finally {
      clearByUser(username);
    }
  }

  @Override
  @ExoTransactional
  public void deleteAll() {
    try {
      super.deleteAll();
    } finally {
      clearAll();
    }
  }

  @Override
  public void cleanExpired() {
    try {
      super.cleanExpired();
    } finally {
      clearAll();
    }
  }

  @SneakyThrows
  private void clearByUser(String username) {
    cache.select(new PortalTokenCacheSelector(username));
  }

  private void clearKey(String key) {
    futureCache.remove(key);
  }

  private void clearAll() {
    futureCache.clear();
  }

}
