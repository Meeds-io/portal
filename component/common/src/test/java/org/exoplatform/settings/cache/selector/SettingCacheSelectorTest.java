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
package org.exoplatform.settings.cache.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.api.settings.data.SettingContext;
import org.exoplatform.commons.api.settings.data.SettingKey;
import org.exoplatform.commons.api.settings.data.SettingScope;

/**
 * The selection of the settings of a context or a scope, which travels to the other nodes of a cluster.
 */
class SettingCacheSelectorTest {

  private static final SettingKey JOHN_APPLICATION = new SettingKey(Context.USER.id("john"), Scope.APPLICATION.id("app"), "k");

  private static final SettingKey JOHN_SPACE       = new SettingKey(Context.USER.id("john"), Scope.SPACE.id("space"), "k");

  private static final SettingKey MARY_APPLICATION = new SettingKey(Context.USER.id("mary"), Scope.APPLICATION.id("app"), "k");

  @Test
  void scopeSelectionSelectsTheKeysOfItsScope() {
    SettingCacheSelector selector = new SettingCacheSelector(new SettingScope(Context.USER.id("john"),
                                                                              Scope.APPLICATION.id("app")));

    assertTrue(selector.select(JOHN_APPLICATION, null));
    assertFalse(selector.select(JOHN_SPACE, null));
    assertFalse(selector.select(MARY_APPLICATION, null));
  }

  @Test
  void contextSelectionSelectsEveryScopeOfItsContext() {
    SettingCacheSelector selector = new SettingCacheSelector(new SettingContext(Context.USER.id("john")));

    assertTrue(selector.select(JOHN_APPLICATION, null));
    assertTrue(selector.select(JOHN_SPACE, null));
    assertFalse(selector.select(MARY_APPLICATION, null));
  }

  @Test
  void aContextSelectionIsNotTheSelectionOfOneOfItsScopes() {
    SettingCacheSelector context = new SettingCacheSelector(new SettingContext(Context.USER.id("john")));
    SettingCacheSelector scope = new SettingCacheSelector(new SettingScope(Context.USER.id("john"),
                                                                           Scope.APPLICATION.id("app")));

    assertNotEquals(context, scope);
    assertNotEquals(scope, context);
  }

  @Test
  void aSelectionTravelsAsTheSameSelection() throws Exception {
    SettingCacheSelector selector = new SettingCacheSelector(new SettingScope(Context.USER.id("john"),
                                                                              Scope.APPLICATION.id("app")));

    SettingCacheSelector received = (SettingCacheSelector) overTheWire(selector);

    assertEquals(selector, received);
    assertEquals(selector.hashCode(), received.hashCode());
    assertTrue(received.select(JOHN_APPLICATION, null));
    assertFalse(received.select(JOHN_SPACE, null));
  }

  private static Object overTheWire(Object value) throws IOException, ClassNotFoundException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(value);
    }
    try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      return in.readObject();
    }
  }
}
