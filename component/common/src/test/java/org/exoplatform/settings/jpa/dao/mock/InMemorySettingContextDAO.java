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

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.jpa.mock.AbstractInMemoryDAO;
import org.exoplatform.settings.jpa.SettingContextDAO;
import org.exoplatform.settings.jpa.entity.ContextEntity;

public class InMemorySettingContextDAO extends AbstractInMemoryDAO<ContextEntity> implements SettingContextDAO {

  private InMemorySettingsDAO memorySettingsDAO;

  public InMemorySettingContextDAO(InMemorySettingsDAO memorySettingsDAO) {
    this.memorySettingsDAO = memorySettingsDAO;
  }

  @Override
  public ContextEntity getContextByTypeAndName(String contextType, String contextName) {
    return entities.values()
                   .stream()
                   .filter(entity -> (StringUtils.isBlank(contextName) || StringUtils.equals(contextName, entity.getName()))
                       && (StringUtils.isBlank(contextType) || StringUtils.equals(contextType, entity.getType())))
                   .findFirst()
                   .orElse(null);
  }

  @Override
  public List<ContextEntity> getEmptyContextsByScopeAndContextType(String contextType,
                                                                   String scopeType,
                                                                   String scopeName,
                                                                   String settingName,
                                                                   int offset,
                                                                   int limit) {
    Stream<ContextEntity> resultStream = entities.values()
                                                 .stream()
                                                 .filter(entity -> (StringUtils.isBlank(contextType)
                                                     || StringUtils.equals(contextType, entity.getType()))
                                                     && isEmptyContext(entity, scopeType, scopeName, settingName));
    if (limit > 0) {
      resultStream = resultStream.limit((long) offset + limit);
    }
    List<ContextEntity> result = resultStream.toList();
    if (offset > 0) {
      result = result.size() > offset ? result.subList(offset, result.size())
                                      : Collections.emptyList();
    }
    return result;
  }

  @Override
  public List<ContextEntity> getContextsByTypeAndSettingNameAndScope(String contextType,
                                                                     String scopeType,
                                                                     String scopeName,
                                                                     String settingName,
                                                                     int offset,
                                                                     int limit) {
    Stream<ContextEntity> resultStream = entities.values()
                                                 .stream()
                                                 .filter(entity -> (StringUtils.isBlank(contextType)
                                                     || StringUtils.equals(contextType, entity.getType()))
                                                     && memorySettingsDAO.getSettingByContextAndScopeAndKey(entity.getType(),
                                                                                                            entity.getName(),
                                                                                                            scopeType,
                                                                                                            scopeName,
                                                                                                            settingName) != null);
    if (limit > 0) {
      resultStream = resultStream.limit((long) offset + limit);
    }
    List<ContextEntity> result = resultStream.toList();
    if (offset > 0) {
      result = result.size() > offset ? result.subList(offset, result.size())
                                      : Collections.emptyList();
    }
    return result;
  }

  @Override
  public long countContextsByType(String contextType) {
    return entities.values()
                   .stream()
                   .filter(entity -> StringUtils.isBlank(contextType) || StringUtils.equals(contextType, entity.getType()))
                   .count();
  }

  @Override
  public List<String> getContextNamesByType(String contextType, int offset, int limit) {
    return entities.values()
                   .stream()
                   .filter(entity -> (StringUtils.isBlank(contextType) || StringUtils.equals(contextType, entity.getType())))
                   .map(ContextEntity::getName)
                   .distinct()
                   .toList();
  }

  private boolean isEmptyContext(ContextEntity context, String scopeType, String scopeName, String settingName) {
    if (StringUtils.isBlank(scopeName)) {
      return memorySettingsDAO.getSettingsByContextTypeAndName(context.getType(), context.getName())
                              .stream()
                              .filter(setting -> setting.getScope() != null
                                  && StringUtils.equals(setting.getScope().getType(), scopeType)
                                  && StringUtils.isBlank(setting.getScope().getName())
                                  && StringUtils.equals(setting.getName(), settingName))
                              .count() == 0;
    } else {
      return memorySettingsDAO.getSettingByContextAndScopeAndKey(context.getType(),
                                                                 context.getName(),
                                                                 scopeType,
                                                                 scopeName,
                                                                 settingName) == null;
    }
  }

}
