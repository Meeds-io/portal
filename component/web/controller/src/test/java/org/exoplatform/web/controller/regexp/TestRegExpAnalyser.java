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
package org.exoplatform.web.controller.regexp;

import org.exoplatform.component.test.BaseGateInTest;

public class TestRegExpAnalyser extends BaseGateInTest {

    private void assertAnalyse(String expectedPattern, String pattern) {
        try {
            RENode.Disjunction disjunction = new REParser(pattern).parseDisjunction();
            assertEquals(expectedPattern, RERenderer.render(disjunction, new StringBuilder()).toString());
        } catch (Exception e) {
            fail(e);
        }
    }

    public void testCharacterClass() {
        assertAnalyse("[a]", "[a]");
        assertAnalyse("[ab]", "[ab]");
        assertAnalyse("[ab]", "[a[b]]");
        assertAnalyse("[abc]", "[abc]");
        assertAnalyse("[abc]", "[[a]bc]");
        assertAnalyse("[abc]", "[a[b]c]");
        assertAnalyse("[abc]", "[ab[c]]");
        assertAnalyse("[abc]", "[[ab]c]");
        assertAnalyse("[abc]", "[a[bc]]");
        assertAnalyse("[abc]", "[[abc]]");
        assertAnalyse("[^a]", "[^a]");
    }

    public void testGroupContainer() {
        assertAnalyse("(a)", "(a)");
        assertAnalyse("(a(?:b))", "(a(?:b))");
        assertAnalyse("(?:a(b))", "(?:a(b))");
        assertAnalyse("(a)(?:b)", "(a)(?:b)");
        assertAnalyse("(a(b))", "(a(b))");
        assertAnalyse("(a)(b)", "(a)(b)");

        //
        assertAnalyse("(?=a)", "(?=a)");
        assertAnalyse("(?!a)", "(?!a)");
        assertAnalyse("(?<=a)", "(?<=a)");
        assertAnalyse("(?<!a)", "(?<!a)");
    }

    public void testBilto() {
        assertAnalyse("[a]+", "[a]+");
    }
}
