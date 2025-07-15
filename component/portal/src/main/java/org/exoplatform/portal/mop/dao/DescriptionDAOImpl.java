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

import java.util.Map;

import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.portal.jdbc.entity.DescriptionEntity;
import org.exoplatform.portal.jdbc.entity.DescriptionState;

public class DescriptionDAOImpl extends GenericDAOJPAImpl<DescriptionEntity, Long> implements DescriptionDAO {
  
  @Override
  @ExoTransactional
  public int deleteByRefId(String refId) {
    DescriptionEntity desc = getByRefId(refId);
    if (desc != null) {
      this.delete(desc);
      return 1; 
    } else {
      return 0;      
    }
  }

  @Override
  @ExoTransactional
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
  @ExoTransactional
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

  @Override
  public DescriptionEntity getByRefId(String refId) {
    if (refId == null) {
      return null;
    }
    
    TypedQuery<DescriptionEntity> query = getEntityManager().createNamedQuery("DescriptionEntity.getByRefId", DescriptionEntity.class);
    query.setParameter("refId", refId);
    
    try {
      return query.getSingleResult();      
    } catch (NoResultException ex) {
      return null;
    }
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
