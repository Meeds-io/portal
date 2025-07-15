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
package org.exoplatform.settings.jpa.dao.mock;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.jpa.mock.AbstractInMemoryDAO;
import org.exoplatform.settings.jpa.SettingsDAO;
import org.exoplatform.settings.jpa.entity.SettingsEntity;

public class InMemorySettingsDAO extends AbstractInMemoryDAO<SettingsEntity> implements SettingsDAO {

  @Override
  public List<SettingsEntity> getSettingsByContextTypeAndName(String contextType,
                                                              String contextName) {
    return entities.values()
                   .stream()
                   .filter(entity -> isSameContextName(entity, contextName) && isSameContextType(entity, contextType))
                   .toList();
  }

  @Override
  public List<SettingsEntity> getSettingsByContextAndScope(String contextType,
                                                           String contextName,
                                                           String scopeType,
                                                           String scopeName) {
    return entities.values()
                   .stream()
                   .filter(entity -> isSameScopeName(entity, scopeName)
                       && isSameScopeType(entity, scopeType)
                       && isSameContextName(entity, contextName)
                       && isSameContextType(entity, contextType))
                   .toList();
  }

  @Override
  public SettingsEntity getSettingByContextAndScopeAndKey(String contextType,
                                                          String contextName,
                                                          String scopeType,
                                                          String scopeName,
                                                          String key) {
    return entities.values()
                   .stream()
                   .filter(entity -> isSameScopeName(entity, scopeName)
                       && isSameScopeType(entity, scopeType)
                       && isSameContextName(entity, contextName)
                       && isSameContextType(entity, contextType)
                       && StringUtils.equals(key, entity.getName()))
                   .findFirst()
                   .orElse(null);
  }

  @Override
  public long countSettingsByNameAndValueAndScope(String scopeType, String scopeName, String key, String value) {
    return entities.values()
                   .stream()
                   .filter(entity -> isSameScopeName(entity, scopeName)
                       && isSameScopeType(entity, scopeType)
                       && StringUtils.equals(key, entity.getName())
                       && StringUtils.equals(value, entity.getValue()))
                   .count();
  }

  private boolean isSameContextType(SettingsEntity entity, String contextType) {
    return StringUtils.isBlank(contextType) || StringUtils.equals(contextType, getContextType(entity));
  }

  private boolean isSameContextName(SettingsEntity entity, String contextName) {
    return StringUtils.isBlank(contextName) || StringUtils.equals(contextName, getContextName(entity));
  }

  private boolean isSameScopeType(SettingsEntity entity, String scopeType) {
    return StringUtils.isBlank(scopeType) || StringUtils.equals(scopeType, getScopeType(entity));
  }

  private boolean isSameScopeName(SettingsEntity entity, String scopeName) {
    return StringUtils.isBlank(scopeName) || StringUtils.equals(scopeName, getScopeName(entity));
  }

  private String getContextType(SettingsEntity entity) {
    return entity == null || entity.getContext() == null ? null : entity.getContext().getType();
  }

  private String getContextName(SettingsEntity entity) {
    return entity == null || entity.getContext() == null ? null : entity.getContext().getName();
  }

  private String getScopeType(SettingsEntity entity) {
    return entity == null || entity.getScope() == null ? null : entity.getScope().getType();
  }

  private String getScopeName(SettingsEntity entity) {
    return entity == null || entity.getScope() == null ? null : entity.getScope().getName();
  }

}
