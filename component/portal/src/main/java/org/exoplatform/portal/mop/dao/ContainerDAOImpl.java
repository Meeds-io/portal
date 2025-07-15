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

import jakarta.persistence.TypedQuery;

import org.exoplatform.portal.jdbc.entity.ContainerEntity;

public class ContainerDAOImpl extends AbstractDAO<ContainerEntity> implements ContainerDAO {

  @Override
  public List<ContainerEntity> findByIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return Collections.emptyList();
    }
    TypedQuery<ContainerEntity> query = getEntityManager().createNamedQuery("ContainerEntity.findByIds", ContainerEntity.class);
    query.setParameter("ids", ids);
    return query.getResultList();
  }

  @Override
  public void deleteById(Long id) {
    ContainerEntity containerEntity = find(id);
    if (containerEntity != null) {
      delete(containerEntity);
    }
  }

}
