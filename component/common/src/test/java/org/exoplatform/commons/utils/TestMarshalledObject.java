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

import org.exoplatform.commons.serialization.MarshalledObject;
import org.exoplatform.component.test.AbstractGateInTest;

public class TestMarshalledObject extends AbstractGateInTest {

    public void testSerialization() {
        String from = "foo";
        MarshalledObject<String> marshalled = MarshalledObject.marshall(from);
        String to = marshalled.unmarshall();
        assertEquals(to, from);
    }

    public void testNPE() {
        try {
            MarshalledObject.marshall(null);
            fail();
        } catch (NullPointerException e) {
        }
    }

    public void testHashCode() {
        MarshalledObject<String> marshalled1 = MarshalledObject.marshall("foo");
        assertEquals(marshalled1.hashCode(), marshalled1.hashCode());
        MarshalledObject<String> marshalled2 = MarshalledObject.marshall("foo");
        assertEquals(marshalled1.hashCode(), marshalled2.hashCode());
        assertEquals(marshalled2.hashCode(), marshalled1.hashCode());
        MarshalledObject<String> marshalled3 = MarshalledObject.marshall("bar");
        assertNotSame(marshalled1.hashCode(), marshalled3.hashCode());
        assertNotSame(marshalled3.hashCode(), marshalled1.hashCode());
    }

    public void testEquals() {
        MarshalledObject<String> marshalled1 = MarshalledObject.marshall("foo");
        assertTrue(marshalled1.equals(marshalled1));
        MarshalledObject<String> marshalled2 = MarshalledObject.marshall("foo");
        assertTrue(marshalled1.equals(marshalled2));
        assertTrue(marshalled2.equals(marshalled1));
        MarshalledObject<String> marshalled3 = MarshalledObject.marshall("bar");
        assertFalse(marshalled1.equals(marshalled3));
        assertFalse(marshalled3.equals(marshalled1));
    }
}
