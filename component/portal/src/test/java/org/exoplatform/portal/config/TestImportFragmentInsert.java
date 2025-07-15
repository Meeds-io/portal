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
package org.exoplatform.portal.config;

import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.navigation.NodeContext;

public class TestImportFragmentInsert extends AbstractImportFragmentTest {

  @Override
  protected ImportMode getMode() {
    return ImportMode.INSERT;
  }

  @Override
  protected final void afterOnePhaseBoot(NodeContext<?> root) {
    assertEquals(1, root.getNodeSize());
    NodeContext<?> foo = root.get("foo");
    assertNotNull(foo);
    assertEquals("foo_icon", foo.getState().getIcon());
    assertEquals(1, foo.getNodeSize());
    NodeContext<?> bar = foo.get("bar");
    assertNotNull(bar);
    assertEquals("bar_icon", bar.getState().getIcon());
    assertEquals(0, bar.getNodeSize());
  }

  @Override
  protected final void afterTwoPhaseOverrideReboot(NodeContext<?> root) {
      assertEquals(1, root.getNodeSize());
      NodeContext<?> foo = root.get("foo");
      assertNotNull(foo);
      assertEquals("foo_icon", foo.getState().getIcon());
      assertEquals(1, foo.getNodeSize());
  }
}
