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

public class TestPatternBuilder extends TestCase {

    public void testEscapeReservedChar() throws Exception {
        assertLiteral('^');
        assertLiteral('*');
        assertLiteral('$');
        assertLiteral('[');
        assertLiteral(']');
        assertLiteral('.');
        assertLiteral('|');
        assertLiteral('+');
        assertLiteral('(');
        assertLiteral(')');
        assertLiteral('?');
    }

    private void assertLiteral(char c) {
        PatternBuilder pb = new PatternBuilder();
        pb.expr("^");
        pb.literal(c);
        pb.expr("$");
        Regex pattern = RegexFactory.JAVA.compile(pb.build());
        assertTrue(pattern.matcher().matches(Character.toString(c)));
    }
}
