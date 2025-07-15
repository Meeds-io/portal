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
package org.exoplatform.portal.mop.dao;

import java.util.Collections;
import java.util.List;

import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.portal.jdbc.entity.WindowEntity;

public class WindowDAOImpl extends AbstractDAO<WindowEntity> implements WindowDAO {

  @Override
  @ExoTransactional
  public WindowEntity find(Long id) {
    return super.find(id);
  }

  @Override
  @ExoTransactional
  public List<Long> findIdsByContentIds(List<String> contentIds, int offset, int limit) {
    if (contentIds == null || contentIds.isEmpty()) {
      return Collections.emptyList();
    }
    TypedQuery<Long> query = getEntityManager().createNamedQuery("WindowEntity.findByContentIds", Long.class);
    query.setParameter("contentIds", contentIds);
    if (limit > 0) {
      query.setFirstResult(offset);
      query.setMaxResults(limit);
    }
    query.setParameter("contentIds", contentIds);
    return query.getResultList();
  }

  @Override
  @ExoTransactional
  public int updateContentId(String oldContentId, String newContentId) {
    Query query = getEntityManager().createNamedQuery("WindowEntity.updateContentId");
    query.setParameter("oldContentId", oldContentId);
    query.setParameter("newContentId", newContentId);
    return query.executeUpdate();
  }

  @Override
  @ExoTransactional
  public int deleteByContentId(String contentId) {
    Query query = getEntityManager().createNamedQuery("WindowEntity.deleteByContentId");
    query.setParameter("contentId", contentId);
    return query.executeUpdate();
  }

  @Override
  @ExoTransactional
  public List<WindowEntity> findByIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return Collections.emptyList();
    }
    TypedQuery<WindowEntity> query = getEntityManager().createNamedQuery("WindowEntity.findByIds", WindowEntity.class);
    query.setParameter("ids", ids);
    return query.getResultList();
  }
  
  @Override
  @ExoTransactional
  public void deleteById(Long id) {
    WindowEntity window = find(id);
    if (window != null) {
      delete(window);
    }
  }

}
