/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.portal.theme.storage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The storages speak DTOs: a JPA entity never crosses their public API.
 */
public class ThemeStorageApiTest {

  private static final String ENTITY_PACKAGE = "io.meeds.portal.theme.entity";

  @Test
  void noPublicStorageMethodExposesAnEntity() {
    List<String> leaks = new ArrayList<>();
    for (Class<?> storage : List.of(ThemeStorage.class, ThemeFileStorage.class)) {
      for (Method method : storage.getDeclaredMethods()) {
        if (!Modifier.isPublic(method.getModifiers())) {
          continue;
        }
        List<Type> types = new ArrayList<>(List.of(method.getGenericParameterTypes()));
        types.add(method.getGenericReturnType());
        types.addAll(List.of(method.getGenericExceptionTypes()));
        if (types.stream().anyMatch(ThemeStorageApiTest::mentionsEntity)) {
          leaks.add(storage.getSimpleName() + "." + method.getName());
        }
      }
    }
    assertTrue(leaks.isEmpty(), "entity types in a public signature: " + leaks);
  }

  private static boolean mentionsEntity(Type type) {
    if (type instanceof Class<?> clazz) {
      return clazz.getName().startsWith(ENTITY_PACKAGE) || clazz.isArray() && mentionsEntity(clazz.getComponentType());
    } else if (type instanceof ParameterizedType parameterized) {
      return mentionsEntity(parameterized.getRawType()) || List.of(parameterized.getActualTypeArguments())
                                                                .stream()
                                                                .anyMatch(ThemeStorageApiTest::mentionsEntity);
    }
    return false;
  }

}
