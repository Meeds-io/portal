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
package io.meeds.web.security.storage;

import java.util.List;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.web.security.security.TokenExistsException;

import io.meeds.web.security.dao.TokenDAO;
import io.meeds.web.security.entity.TokenEntity;
import io.meeds.web.security.model.TokenData;

public class PortalTokenStorage {

  private TokenDAO tokenDAO;

  public PortalTokenStorage(TokenDAO tokenDAO) {
    this.tokenDAO = tokenDAO;
  }

  @ExoTransactional
  public void createToken(TokenData data) throws TokenExistsException {
    TokenEntity existing = this.tokenDAO.findByTokenId(data.getTokenId());
    if (existing != null) {
      throw new TokenExistsException();
    }
    TokenEntity entity = new TokenEntity();
    entity.setTokenId(data.getTokenId());
    entity.setTokenHash(data.getHash());
    entity.setUsername(data.getUsername());
    entity.setExpirationTime(data.getExpirationTime());
    entity.setTokenType(data.getTokenType());
    tokenDAO.create(entity);
  }

  @ExoTransactional
  public TokenData getToken(String tokenId) {
    TokenEntity entity = this.tokenDAO.findByTokenId(tokenId);
    if (entity != null) {
      return new TokenData(entity.getTokenId(),
                           entity.getTokenHash(),
                           entity.getUsername(),
                           entity.getExpirationTime(),
                           entity.getTokenType());
    }
    return null;
  }

  @ExoTransactional
  public void deleteToken(String tokenId) {
    TokenEntity entity = this.tokenDAO.findByTokenId(tokenId);
    if (entity != null) {
      this.tokenDAO.delete(entity);
    }
  }

  @ExoTransactional
  public void deleteTokenOfUser(String user) {
    List<TokenEntity> entities = this.tokenDAO.findByUsername(user);
    if (entities != null && !entities.isEmpty()) {
      this.tokenDAO.deleteAll(entities);
    }
  }

  @ExoTransactional
  public void deleteAll() {
    this.tokenDAO.deleteAll();
  }

  @ExoTransactional
  public void cleanExpired() {
    this.tokenDAO.cleanExpired();
  }

  @ExoTransactional
  public long size() {
    return this.tokenDAO.count();
  }

  @ExoTransactional
  public void deleteTokensByUsernameAndType(String username, String tokenType) {
    this.tokenDAO.deleteTokensByUsernameAndType(username, tokenType);
  }

}
