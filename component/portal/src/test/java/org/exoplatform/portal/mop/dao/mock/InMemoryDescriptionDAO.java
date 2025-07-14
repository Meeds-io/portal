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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.jpa.mock.AbstractInMemoryDAO;
import org.exoplatform.portal.jdbc.entity.DescriptionEntity;
import org.exoplatform.portal.jdbc.entity.DescriptionState;
import org.exoplatform.portal.mop.dao.DescriptionDAO;

public class InMemoryDescriptionDAO extends AbstractInMemoryDAO<DescriptionEntity> implements DescriptionDAO {

  @Override
  public DescriptionEntity getByRefId(String refId) {
    return entities.values()
                   .stream()
                   .filter(description -> StringUtils.equals(refId, description.getReferenceId()))
                   .findFirst()
                   .orElse(null);
  }

  @Override
  public int deleteByRefId(String refId) {
    DescriptionEntity entity = getByRefId(refId);
    delete(entity);
    return entity == null ? 0 : 1;
  }

  @Override
  public DescriptionEntity saveDescriptions(String refId, Map<String, DescriptionState> states) {
    if (refId == null) {
      throw new IllegalArgumentException("refId , states must not be null");
    }

    DescriptionEntity entity = getOrCreate(refId);
    entity.setLocalized(states);
    if (entity.getId() == null) {
      this.create(entity);
    } else {
      this.update(entity);
    }

    return entity;
  }

  @Override
  public DescriptionEntity saveDescription(String refId, DescriptionState state) {
    if (refId == null) {
      throw new IllegalArgumentException("refId , states must not be null");
    }

    DescriptionEntity entity = getOrCreate(refId);
    entity.setState(state);
    if (entity.getId() == null) {
      this.create(entity);
    } else {
      this.update(entity);
    }
    return entity;
  }

  private DescriptionEntity getOrCreate(String refId) {
    DescriptionEntity entity = getByRefId(refId);
    if (entity == null) {
      entity = new DescriptionEntity();
      entity.setReferenceId(refId);
    }
    return entity;
  }
}
