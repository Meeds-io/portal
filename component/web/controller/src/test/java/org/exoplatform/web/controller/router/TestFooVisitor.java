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

import java.util.List;

import junit.framework.TestCase;

import org.exoplatform.web.controller.regexp.RENode;
import org.exoplatform.web.controller.regexp.REParser;

public class TestFooVisitor extends TestCase {

    private static void assertSatisfied(String expression, String... expected) throws Exception {
        RENode root = new REParser(expression).parse();
        root.accept(new CaptureGroupTransformation());
        List<ValueResolverFactory.Alternative> alternatives = new ValueResolverFactory().foo(root);
        assertEquals(expected.length, alternatives.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], alternatives.get(i).getResolvingExpression().toString());
        }
    }

    public void testDisjunctions() throws Exception {
        assertSatisfied("a|.", "(a)", "(.)");
        assertSatisfied("a(.)b|.", "a(.)b", "(.)");
    }

    public void testDisjunction() throws Exception {
        assertSatisfied("(?:a|b)(.)b", "a(.)b");
    }

    public void testAlternative() throws Exception {
        assertSatisfied("a(.)b", "a(.)b");
    }

    public void testCharacterClassRange() throws Exception {
        assertSatisfied("a(.)[b-c]", "a(.)b");
    }

    public void testCharacterClassOr() throws Exception {
        assertSatisfied("a(.)[bc]", "a(.)b");
    }

    public void testCharacterClassAnd() throws Exception {
        assertSatisfied("a(.)[b&&b]", "a(.)b");
    }

    public void testQuantifier() throws Exception {
        assertSatisfied("a(.)b{2,3}", "a(.)bb");
    }

    public void testCharacterClassNegateChar() throws Exception {
        assertSatisfied("a(.)[^a]", "a(.) ");
    }

    public void testCharacterClassNegateOr() throws Exception {
        assertSatisfied("a(.)[^ !]", "a(.)\"");
    }

    public void testCharacterClassNegateAnd() throws Exception {
        assertSatisfied("a(.)[^a&&b]", "a(.) ");
    }

    public void testAny() throws Exception {
        assertSatisfied("a(.).", "a(.)a");
    }

    public void testComplex() throws Exception {
        assertSatisfied("a(.)[a-z&&f-t&&p-q]", "a(.)p");
    }
}
