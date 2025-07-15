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
package org.exoplatform.settings.jpa.dao;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.settings.jpa.entity.ContextEntity;

import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class SettingContextDAO extends GenericDAOJPAImpl<ContextEntity, Long> implements org.exoplatform.settings.jpa.SettingContextDAO {
  private static final Log LOG = ExoLogger.getLogger(SettingContextDAO.class);

  @ExoTransactional
  @Override
  public ContextEntity getContextByTypeAndName(String contextType, String contextName) {
    TypedQuery<ContextEntity> query;
    if (StringUtils.isBlank(contextName)) {
      query = getEntityManager().createNamedQuery("SettingsContextEntity.getContextByTypeWithNullName", ContextEntity.class)
                                .setParameter("contextType", contextType);
    } else {
      query = getEntityManager().createNamedQuery("SettingsContextEntity.getContextByTypeAndName", ContextEntity.class)
                                .setParameter("contextName", contextName)
                                .setParameter("contextType", contextType);
    }
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    } catch (NonUniqueResultException e1) {
      LOG.warn("Non unique result for settings context of type {} and name {}. First result will be returned",
                contextType,
                contextName);
      return query.getResultList().get(0);
    }
  }

  @ExoTransactional
  @Override
  public List<ContextEntity> getEmptyContextsByScopeAndContextType(String contextType,
                                                                   String scopeType,
                                                                   String scopeName,
                                                                   String settingName,
                                                                   int offset,
                                                                   int limit) {
    TypedQuery<ContextEntity> query;
    if (StringUtils.isBlank(scopeName)) {
      query =
            getEntityManager().createNamedQuery("SettingsContextEntity.getEmptyContextsByScopeWithNullNameAndContextType", ContextEntity.class)
                              .setParameter("contextType", contextType)
                              .setParameter("scopeType", scopeType)
                              .setParameter("settingName", settingName);
    } else {
      query = getEntityManager().createNamedQuery("SettingsContextEntity.getEmptyContextsByScopeAndContextType", ContextEntity.class)
                                .setParameter("contextType", contextType)
                                .setParameter("scopeType", scopeType)
                                .setParameter("scopeName", scopeName)
                                .setParameter("settingName", settingName);
    }
    if (limit != 0) {
      query.setMaxResults(limit).setFirstResult(offset);
    }
    return query.getResultList();
  }

  @ExoTransactional
  @Override
  public List<ContextEntity> getContextsByTypeAndSettingNameAndScope(String contextType,
                                                                     String scopeType,
                                                                     String scopeName,
                                                                     String settingName,
                                                                     int offset,
                                                                     int limit) {
    TypedQuery<ContextEntity> query;
    if (StringUtils.isBlank(scopeName)) {
      query = getEntityManager()
                                .createNamedQuery("SettingsContextEntity.getContextsByTypeAndScopeWithNullNameAndSettingName",
                                                  ContextEntity.class)
                                .setFirstResult(offset)
                                .setMaxResults(limit)
                                .setParameter("contextType", contextType)
                                .setParameter("scopeType", scopeType)
                                .setParameter("settingName", settingName);
    } else {
      query = getEntityManager().createNamedQuery("SettingsContextEntity.getContextsByTypeAndScopeAndSettingName", ContextEntity.class)
                                .setFirstResult(offset)
                                .setMaxResults(limit)
                                .setParameter("contextType", contextType)
                                .setParameter("scopeType", scopeType)
                                .setParameter("scopeName", scopeName)
                                .setParameter("settingName", settingName);
    }
    return query.getResultList();
  }

  @ExoTransactional
  @Override
  public long countContextsByType(String contextType) {
    TypedQuery<Long> query = getEntityManager().createNamedQuery("SettingsContextEntity.countContextsByType", Long.class)
                                               .setParameter("contextType", contextType);
    return query.getSingleResult().longValue();
  }

  @ExoTransactional
  @Override
  public List<String> getContextNamesByType(String contextType, int offset, int limit) {
    TypedQuery<String> query = getEntityManager().createNamedQuery("SettingsContextEntity.getContextNamesByType", String.class)
                                                        .setParameter("contextType", contextType)
                                                        .setFirstResult(offset)
                                                        .setMaxResults(limit);
    return query.getResultList();
  }
}
