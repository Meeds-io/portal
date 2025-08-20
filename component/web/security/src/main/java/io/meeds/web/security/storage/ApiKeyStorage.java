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

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.meeds.web.security.dao.ApiKeyDAO;
import io.meeds.web.security.entity.ApiKeyEntity;

@Component
public class ApiKeyStorage {

  @Autowired
  private ApiKeyDAO apiKeyDao;

  public void saveKey(String userName, String encryptedPassword) {
    ApiKeyEntity apiKeyEntity = apiKeyDao.findByUserName(userName);
    if (apiKeyEntity == null) {
      apiKeyEntity = new ApiKeyEntity(null, userName, encryptedPassword, new Date());
    } else {
      apiKeyEntity.setEncryptedPassword(encryptedPassword);
      apiKeyEntity.setCreationDate(new Date());
    }
    apiKeyDao.save(apiKeyEntity);
  }

  public String getKey(String userName) {
    ApiKeyEntity apiKeyEntity = apiKeyDao.findByUserName(userName);
    if (apiKeyEntity == null) {
      return null;
    } else {
      return apiKeyEntity.getEncryptedPassword();
    }

  }

}
