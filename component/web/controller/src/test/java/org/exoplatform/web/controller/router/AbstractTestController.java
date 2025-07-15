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

import java.util.Arrays;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.exoplatform.web.controller.QualifiedName;

public abstract class AbstractTestController extends TestCase {
    public static void assertEquals(Map<QualifiedName, String> expectedParameters, Map<QualifiedName, String> parameters) {
        assertNotNull("Was not expecting a null parameter set", parameters);
        Assert.assertEquals(expectedParameters.keySet(), parameters.keySet());
        for (Map.Entry<QualifiedName, String> expectedEntry : expectedParameters.entrySet()) {
            Assert.assertEquals(expectedEntry.getValue(), parameters.get(expectedEntry.getKey()));
        }
    }

    public static void assertMapEquals(Map<String, String[]> expectedParameters, Map<String, String[]> parameters) {
        assertNotNull("Was not expecting a null parameter set", parameters);
        Assert.assertEquals(expectedParameters.keySet(), parameters.keySet());
        for (Map.Entry<String, String[]> expectedEntry : expectedParameters.entrySet()) {
            Assert.assertEquals(Arrays.asList(expectedEntry.getValue()), Arrays.asList(parameters.get(expectedEntry.getKey())));
        }
    }
}
