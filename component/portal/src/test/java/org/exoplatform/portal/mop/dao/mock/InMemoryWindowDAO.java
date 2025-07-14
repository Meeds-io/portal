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
package org.exoplatform.portal.mop.dao.mock;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.jpa.mock.AbstractInMemoryDAO;
import org.exoplatform.portal.jdbc.entity.WindowEntity;
import org.exoplatform.portal.mop.dao.WindowDAO;

public class InMemoryWindowDAO extends AbstractInMemoryDAO<WindowEntity> implements WindowDAO {

  @Override
  public List<Long> findIdsByContentIds(List<String> contentIds, int offset, int limit) {
    return entities.values()
                   .stream()
                   .filter(entity -> contentIds.contains(entity.getContentId()))
                   .map(WindowEntity::getId)
                   .toList();
  }

  @Override
  public int updateContentId(String oldContentId, String newContentId) {
    List<WindowEntity> listToUpdate = entities.values()
                                              .stream()
                                              .filter(entity -> StringUtils.equals(oldContentId, entity.getContentId()))
                                              .toList();
    listToUpdate.forEach(entity -> entity.setContentId(newContentId));
    return listToUpdate.size();
  }

  @Override
  public int deleteByContentId(String oldContentId) {
    List<WindowEntity> list = entities.values()
                                              .stream()
                                              .filter(entity -> StringUtils.equals(oldContentId, entity.getContentId()))
                                              .toList();
    deleteAll(list);
    return list.size();
  }

}
