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
package org.exoplatform.web.controller.router;

import junit.framework.TestCase;

public class TestBitStack extends TestCase {

    public void testSimple() {
        BitStack bs = new BitStack();
        assertEquals(0, bs.getDepth());
        bs.init(2);
        bs.push();
        assertEquals(1, bs.getDepth());
        bs.set(1);
        assertFalse(bs.isEmpty());
        bs.push();
        assertEquals(2, bs.getDepth());
        bs.set(0);
        assertTrue(bs.isEmpty());
        bs.pop();
        assertEquals(1, bs.getDepth());
        assertFalse(bs.isEmpty());
        bs.pop();
        assertEquals(0, bs.getDepth());
    }

    public void testReuse() {
        BitStack bs = new BitStack();
        bs.init(2);
        bs.push();
        bs.set(0);
        bs.push();
        bs.set(1);
        assertTrue(bs.isEmpty());
        bs.pop();
        bs.push();
        assertFalse(bs.isEmpty());
    }

    public void testState() {
        BitStack bs = new BitStack();
        try {
            bs.set(0);
            fail();
        } catch (IllegalStateException e) {
        }
        try {
            bs.pop();
            fail();
        } catch (IllegalStateException e) {
        }
    }

}
