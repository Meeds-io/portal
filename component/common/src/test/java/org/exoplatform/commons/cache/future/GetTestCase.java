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
package org.exoplatform.commons.cache.future;

import static org.junit.Assert.assertThrows;

import java.util.concurrent.Callable;

import junit.framework.TestCase;

public class GetTestCase extends TestCase {
    public void testGet() {
        FutureMap<String, String, Callable<String>> futureCache = new FutureMap<>(
                new StringLoader());
        assertEquals("foo_value_", futureCache.get(new Callable<String>() {
            public String call() throws Exception {
                return "foo_value_";
            }
        }, "foo"));
        assertEquals("foo_value_", futureCache.data.get("foo"));
    }

    public void testNullValue() {
        FutureMap<String, String, Callable<String>> futureCache = new FutureMap<>(
                new StringLoader());
        assertEquals(null, futureCache.get(new Callable<String>() {
            public String call() throws Exception {
                return null;
            }
        }, "foo"));
        assertFalse(futureCache.data.containsKey("foo"));
    }

    public void testThrowException() {
        FutureMap<String, String, Callable<String>> futureCache = new FutureMap<>(
                new StringLoader());
        assertThrows(IllegalStateException.class, () -> futureCache.get(new Callable<String>() {
            public String call() throws Exception {
                throw new Exception("DON'T FREAK OUT");
            }
        }, "foo"));
        assertFalse(futureCache.data.containsKey("foo"));
    }

    public void testReentrancy() {
        final FutureMap<String, String, Callable<String>> futureCache = new FutureMap<>(
                new StringLoader());
        String res = futureCache.get(new Callable<String>() {
            public String call() throws Exception {
                try {
                    futureCache.get(new Callable<String>() {
                        public String call() throws Exception {
                            // Should not go there
                            throw new AssertionError();
                        }
                    }, "foo");
                    return "fail";
                } catch (IllegalStateException expected) {
                    return "pass";
                }
            }
        }, "foo");
        assertEquals("pass", res);
    }
}
