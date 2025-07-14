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

public class TestImportNavigationConserve extends AbstractImportNavigationTest {

    @Override
    protected ImportMode getMode() {
        return ImportMode.CONSERVE;
    }

    @Override
    protected final void afterOnePhaseBoot(NodeContext<?> root) {
        assertEquals(4, root.getNodeCount());
        NodeContext<?> foo = root.get("foo");
        assertNotNull(foo);
        assertEquals("foo_icon_2", foo.getState().getIcon());
        assertEquals(1, foo.getNodeCount());
        NodeContext<?> juu = foo.get("juu");
        assertNotNull(juu);
        assertEquals("juu_icon", juu.getState().getIcon());
        assertEquals(0, juu.getNodeCount());
        NodeContext<?> bar = root.get("bar");
        assertNotNull(bar);
        assertEquals("bar_icon", bar.getState().getIcon());
        assertEquals(0, bar.getNodeCount());
        NodeContext<?> daa = root.get("daa");
        assertNotNull(daa);
        assertEquals("daa_icon", daa.getState().getIcon());
        assertEquals(0, daa.getNodeCount());
        // CONSERVE is changed into MERGE when first startup
        assertEquals(2, root.get("baz").getIndex());
        assertEquals(0, root.get("foo").getIndex());
        assertEquals(3, root.get("daa").getIndex());
    }

    @Override
    protected final void afterTwoPhasesBoot(NodeContext<?> root) {
        assertEquals(3, root.getNodeCount());
        assertNotNull(root.get("foo"));
        assertNotNull(root.get("daa"));
        assertEquals(0, root.get("baz").getIndex());
        assertEquals(1, root.get("foo").getIndex());
        assertEquals(2, root.get("daa").getIndex());
    }

    @Override
    protected final void afterTwoPhaseOverrideReboot(NodeContext<?> root) {
        assertState(root);
    }

    protected void assertState(NodeContext<?> root) {
        assertEquals(3, root.getNodeCount());
        NodeContext<?> foo = root.get("foo");
        assertNotNull(foo);
        assertEquals("foo_icon_1", foo.getState().getIcon());
        assertEquals(1, foo.getNodeCount());
        NodeContext<?> juu = foo.get("juu");
        assertNotNull(juu);
        assertEquals("juu_icon", juu.getState().getIcon());
        assertEquals(0, juu.getNodeCount());
        NodeContext<?> daa = root.get("daa");
        assertNotNull(daa);
        assertEquals("daa_icon", daa.getState().getIcon());
        assertEquals(0, daa.getNodeCount());
    }
}
