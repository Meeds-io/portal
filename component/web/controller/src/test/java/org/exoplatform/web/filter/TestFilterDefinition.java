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
package org.exoplatform.web.filter;

import java.util.ArrayList;
import java.util.Arrays;

import org.exoplatform.component.test.AbstractGateInTest;


public class TestFilterDefinition extends AbstractGateInTest {

    public void testMatch() {
        FilterDefinition def = new FilterDefinition(null, null);
        try {
            def.getMapping();
            fail("An IllegalArgumentException is expected");
        } catch (IllegalArgumentException e) {
        }
        def = new FilterDefinition(null, new ArrayList<String>());
        try {
            def.getMapping();
            fail("An IllegalArgumentException is expected");
        } catch (IllegalArgumentException e) {
        }
        def = new FilterDefinition(null, Arrays.asList(".*\\.gif"));
        assertTrue(def.getMapping().match("/foo/foo.gif"));
        assertFalse(def.getMapping().match("/foo/foo.giff"));
        def = new FilterDefinition(null, Arrays.asList(".*\\.gif", ".*\\.giff"));
        assertTrue(def.getMapping().match("/foo/foo.gif"));
        assertTrue(def.getMapping().match("/foo/foo.giff"));
        assertFalse(def.getMapping().match("/foo/foo.giffo"));
    }
}
