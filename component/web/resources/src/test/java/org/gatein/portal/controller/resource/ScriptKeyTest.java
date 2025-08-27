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
package org.gatein.portal.controller.resource;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.util.Locale;



public class ScriptKeyTest extends TestCase {

    final ResourceId id = new ResourceId(ResourceScope.PORTAL, "platformNavigation/UIGroupsNavigationPortlet");
    final boolean minified = false ;
    final Locale locale = null;

    public void testEqualsMethod() {
        ScriptKey scriptKey = new ScriptKey(id, minified, locale);
        ScriptKey scriptKey1 = new ScriptKey(id, false, locale);
        ScriptKey scriptKey2 = new ScriptKey(id, true,locale);

        assertTrue(scriptKey.equals(scriptKey1));
        assertFalse(scriptKey.equals(scriptKey2));

    }
}