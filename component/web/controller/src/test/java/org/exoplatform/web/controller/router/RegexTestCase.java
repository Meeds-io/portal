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

public class RegexTestCase extends TestCase {

    public void testLiteral() {
        Regex regex = JRegexFactory.INSTANCE.compile("abc");
        Regex.Match[] matches = regex.matcher().find("abc");
        assertEquals(1, matches.length);
        assertEquals(0, matches[0].getStart());
        assertEquals(3, matches[0].getEnd());
        assertEquals("abc", matches[0].getValue());
    }

    public void testSimpleGroup1() {
        Regex regex = JRegexFactory.INSTANCE.compile("(abc)");
        Regex.Match[] matches = regex.matcher().find("abc");
        assertEquals(2, matches.length);
        assertEquals(0, matches[0].getStart());
        assertEquals(3, matches[0].getEnd());
        assertEquals("abc", matches[0].getValue());
        assertEquals(0, matches[1].getStart());
        assertEquals(3, matches[1].getEnd());
        assertEquals("abc", matches[1].getValue());
    }

    public void testSimpleGroup2() {
        Regex regex = JRegexFactory.INSTANCE.compile("a(b)c");
        Regex.Match[] matches = regex.matcher().find("abc");
        assertEquals(2, matches.length);
        assertEquals(0, matches[0].getStart());
        assertEquals(3, matches[0].getEnd());
        assertEquals("abc", matches[0].getValue());
        assertEquals(1, matches[1].getStart());
        assertEquals(2, matches[1].getEnd());
        assertEquals("b", matches[1].getValue());
    }

    public void testNonCapturingGroup() {
        Regex regex = JRegexFactory.INSTANCE.compile("a(?:b)c");
        Regex.Match[] matches = regex.matcher().find("abc");
        assertEquals(1, matches.length);
        assertEquals(0, matches[0].getStart());
        assertEquals(3, matches[0].getEnd());
        assertEquals("abc", matches[0].getValue());
    }
}
