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
package org.exoplatform.portal.rest;

import java.util.LinkedHashMap;
import java.util.List;

public class CollectionEntity<T> extends LinkedHashMap<String, Object> {

  private static final long serialVersionUID = -2654776238599704700L;

  public CollectionEntity(List<T> entities, int offset, int limit, int size) {
    setEntities(entities);
    setOffset(offset);
    setLimit(limit);
    setSize(size);
  }

  public int getSize() {
    Integer size = (Integer) get("size");
    return (size == null) ? 0 : size;
  }

  public void setSize(int size) {
    put("size", size);
  }

  public int getLimit() {
    return (Integer) get("limit");
  }

  public void setLimit(int limit) {
    put("limit", limit);
  }

  public int getOffset() {
    return (Integer) get("offset");
  }

  public void setOffset(int offset) {
    put("offset", offset);
  }

  @SuppressWarnings("unchecked")
  public List<T> getEntities() {
    return (List<T>) get("entities");
  }

  public void setEntities(List<T> entities) {
    put("entities", entities);
  }

}
