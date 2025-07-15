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
package org.exoplatform.commons.utils;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.List;

import org.gatein.common.util.ParameterValidation;

public class ListAccessImpl<E> implements ListAccess<E>, Serializable {

  /** . */
  private final List<E>  list;

  /** . */
  private final Class<E> elementType;

  public ListAccessImpl(Class<E> elementType, List<E> list) {
    ParameterValidation.throwIllegalArgExceptionIfNull(elementType, "element type");
    ParameterValidation.throwIllegalArgExceptionIfNull(list, "elements");
    this.elementType = elementType;
    this.list = list;
  }

  @SuppressWarnings("unchecked")
  public E[] load(int index, int length) throws Exception {
    E[] array = (E[]) Array.newInstance(elementType, length);
    return list.stream()
               .skip(index)
               .limit(length)
               .toList()
               .toArray(array);
  }

  public int getSize() throws Exception {
    return list.size();
  }
}
