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
package org.exoplatform.portal.mop.storage.cache.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;

import org.exoplatform.portal.mop.SiteKey;

/**
 * The selections of the navigation caches reach the other nodes of a cluster: each one is received as the selection
 * that was made, and differs from the selection of another site, node or description.
 */
public class TestCacheSelectors extends TestCase {

  public void testNavigationSelectionIsReceivedAsMade() throws Exception {
    NavigationCacheSelector selector = new NavigationCacheSelector(SiteKey.portal("classic"));

    assertEquals(selector, overTheWire(selector));
    assertFalse(selector.equals(new NavigationCacheSelector(SiteKey.portal("other"))));
  }

  public void testNodeSelectionIsReceivedAsMade() throws Exception {
    NavigationDataCacheSelector selector = new NavigationDataCacheSelector(SiteKey.portal("classic"), 2L);

    assertEquals(selector, overTheWire(selector));
    assertEquals(new NavigationDataCacheSelector(null, 2L), overTheWire(new NavigationDataCacheSelector(null, 2L)));
    assertFalse(selector.equals(new NavigationDataCacheSelector(SiteKey.portal("classic"), 3L)));
    assertFalse(selector.equals(new NavigationDataCacheSelector(SiteKey.portal("other"), 2L)));
  }

  public void testDescriptionSelectionIsReceivedAsMade() throws Exception {
    DescriptionCacheSelector selector = new DescriptionCacheSelector("1");

    assertEquals(selector, overTheWire(selector));
    assertFalse(selector.equals(new DescriptionCacheSelector("2")));
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
