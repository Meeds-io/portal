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
package org.exoplatform.component.test;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class BaseGateInTest extends TestCase {

    public BaseGateInTest() {
    }

    public BaseGateInTest(String name) {
        super(name);
    }

    public static <T> T assertInstanceOf(Object o, Class<T> expectedType) {
        if (expectedType != null) {
            if (expectedType.isInstance(o)) {
                fail();
                return null;
            } else {
                return expectedType.cast(o);
            }
        } else {
            fail("Need an expected type");
            return null;
        }
    }

    public static void fail(String msg, Throwable t) {
        throw failure(msg, t);
    }

    public static void fail(Throwable t) {
        AssertionFailedError afe = new AssertionFailedError();
        afe.initCause(t);
        throw afe;
    }

    public static Error failure(Throwable t) {
        AssertionFailedError afe = new AssertionFailedError();
        afe.initCause(t);
        return afe;
    }

    public static Error failure(String msg, Throwable t) {
        AssertionFailedError afe = new AssertionFailedError(msg);
        afe.initCause(t);
        return afe;
    }

}
