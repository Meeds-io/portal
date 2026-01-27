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
package org.exoplatform.services.organization.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

import org.exoplatform.commons.utils.ListAccess;

public class InMemoryListAccess<T> implements ListAccess<T> {

  private Class<T> modelClass;

  private List<T>  values;

  private T[]      defaultResult;

  public InMemoryListAccess(List<T> values, T[] defaultResult) {
    this.defaultResult = defaultResult;
    List<T> retrievedValues = values == null ? Collections.emptyList() : values.stream().filter(Objects::nonNull).toList();
    if (CollectionUtils.isNotEmpty(retrievedValues)) {
      T firstElement = retrievedValues.get(0);
      if (firstElement instanceof Cloneable) {
        this.values = retrievedValues.stream().map(ObjectUtils::clone).filter(Objects::nonNull).toList();
      } else {
        this.values = new ArrayList<>(retrievedValues);
      }
    } else {
      this.values = Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public T[] load(int index, int length) {
    Class<T> tClass = getModelClass();
    if (tClass == null || index >= values.size()) {
      return defaultResult;
    }
    if (index + length > values.size()) {
      length = values.size() - index;
    }
    return values.subList(index, index + length)
                 .toArray((T[]) java.lang.reflect.Array.newInstance(tClass, values.size()));
  }

  @SuppressWarnings("unchecked")
  private Class<T> getModelClass() {
    if (modelClass == null && CollectionUtils.isNotEmpty(values)) {
      List<Class<?>> classes = new ArrayList<>();
      for (T t : values) {
        if (t == null) {
          continue; // NOSONAR
        } else {
          List<Class<?>> tClasses = new ArrayList<>(Arrays.asList(t.getClass().getInterfaces()));
          tClasses.add(t.getClass());
          tClasses.add(t.getClass().getSuperclass());
          if (classes.isEmpty()) {
            classes.addAll(tClasses);
          } else {
            classes.retainAll(tClasses);
          }
        }
      }
      classes.remove(Object.class);
      if (!classes.isEmpty()) {
        modelClass = (Class<T>) classes.getLast();
      }
    }
    return modelClass;
  }

  public int getSize() {
    return values.size();
  }

}
