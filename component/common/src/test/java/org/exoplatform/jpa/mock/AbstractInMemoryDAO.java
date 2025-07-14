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
package org.exoplatform.jpa.mock;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.exoplatform.commons.api.persistence.GenericDAO;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class AbstractInMemoryDAO<E> implements GenericDAO<E, Long> {

  private static final Log                   LOG          = ExoLogger.getLogger(AbstractInMemoryDAO.class);

  protected static final AtomicLong          ID_GENERATOR = new AtomicLong();

  protected Class<E>                         modelClass;

  protected static Map<String, Map<Long, ?>> allEntities = new HashMap<>();

  protected Map<Long, E>                     entities;

  @SuppressWarnings("unchecked")
  public AbstractInMemoryDAO() {
    ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
    this.modelClass = (Class<E>) genericSuperclass.getActualTypeArguments()[0];
    this.entities = (Map<Long, E>) allEntities.computeIfAbsent(this.getClass().getName(), key -> new HashMap<>());
  }

  @Override
  public Long count() {
    return Long.valueOf(entities.size());
  }

  @Override
  public E find(Long id) {
    return entities.get(id);
  }

  @Override
  public List<E> findAll() {
    return new ArrayList<>(entities.values());
  }

  public List<E> findByIds(List<Long> ids) {
    return entities.values().stream().filter(entity -> ids.contains(getId(entity))).toList();
  }

  public void deleteById(Long id) {
    E entity = entities.get(id);
    delete(entity);
  }

  @Override
  public E create(E entity) {
    long id = ID_GENERATOR.incrementAndGet();
    setId(entity, id);
    entities.put(id, entity);
    return entity;
  }

  @Override
  public void createAll(List<E> entities) {
    entities.forEach(this::create);
  }

  @Override
  public E update(E entity) {
    Long id = getId(entity);
    entities.put(id, entity);
    return entity;
  }

  @Override
  public void updateAll(List<E> entities) {
    entities.forEach(this::update);
  }

  @Override
  public void delete(E entity) {
    if (entity == null) {
      return;
    }
    entities.remove(getId(entity));
  }

  @Override
  public void deleteAll(List<E> entities) {
    remove(entities);
  }

  @Override
  public void deleteAll() {
    remove(entities.values());
  }

  private Long getId(E entity) {
    try {
      return (Long) modelClass.getDeclaredMethod("getId").invoke(entity);
    } catch (Exception e) {
      LOG.warn("Can't get id of entity {}", modelClass, e);
      return null;
    }
  }

  private void setId(E entity, long id) {
    try {
      modelClass.getDeclaredMethod("setId", Long.class).invoke(entity, id);
    } catch (Exception e) {
      LOG.warn("Can't set id of entity {}", modelClass, e);
    }
  }

  private void remove(Collection<E> items) {
    @SuppressWarnings("unchecked")
    E[] array = (E[]) items.toArray();
    for (E item : array) {
      delete(item);
    }
  }

}
