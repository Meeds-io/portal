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
package org.exoplatform.web.security.jpa.mock;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.jpa.mock.AbstractInMemoryDAO;

import io.meeds.web.security.dao.TokenDAO;
import io.meeds.web.security.entity.TokenEntity;

public class InMemoryTokenDAO extends AbstractInMemoryDAO<TokenEntity> implements TokenDAO {

  @Override
  public TokenEntity findByTokenId(String tokenId) {
    return entities.values()
                   .stream()
                   .filter(entity -> StringUtils.equals(entity.getTokenId(), tokenId))
                   .findFirst()
                   .orElse(null);
  }

  @Override
  public List<TokenEntity> findByUsername(String username) {
    return entities.values()
                   .stream()
                   .filter(entity -> StringUtils.equals(entity.getUsername(), username))
                   .toList();
  }

  @Override
  public void deleteTokensByUsernameAndType(String username, String tokenType) {
    List<TokenEntity> tokenList = entities.values()
                                          .stream()
                                          .filter(entity -> StringUtils.equals(entity.getUsername(), username)
                                              && StringUtils.equals(entity.getTokenType(), tokenType))
                                          .toList();
    deleteAll(tokenList);
  }

  @Override
  public void cleanExpired() {
    List<TokenEntity> tokenList = entities.values()
                                          .stream()
                                          .filter(entity -> entity.getExpirationTime() != null
                                              && entity.getExpirationTime().getTime() <= System.currentTimeMillis())
                                          .toList();
    deleteAll(tokenList);
  }

}
