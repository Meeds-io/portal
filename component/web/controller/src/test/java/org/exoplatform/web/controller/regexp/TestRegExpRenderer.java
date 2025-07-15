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

import java.io.IOException;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class TestRegExpRenderer extends TestCase {

    static void assertRender(RENode re, String expected) {
        try {
            String rendered;
            if (re != null) {
                rendered = RERenderer.render(re, new StringBuilder()).toString();
            } else {
                rendered = "";
            }
            assertEquals(expected, rendered);
        } catch (IOException e) {
            AssertionFailedError afe = new AssertionFailedError();
            afe.initCause(e);
            throw afe;
        }
    }

    static RENode.Disjunction disjunction(String regexp) {
        try {
            REParser parser = new REParser(regexp);
            RENode.Disjunction re = parser.parseDisjunction();
            assertTrue(parser.isDone());
            return re;
        } catch (SyntaxException e) {
            AssertionFailedError afe = new AssertionFailedError();
            afe.initCause(e);
            throw afe;
        }
    }

    static RENode.Alternative alternative(String regexp) {
        try {
            REParser parser = new REParser(regexp);
            RENode.Alternative re = parser.parseAlternative();
            assertTrue(parser.isDone());
            return re;
        } catch (SyntaxException e) {
            AssertionFailedError afe = new AssertionFailedError();
            afe.initCause(e);
            throw afe;
        }
    }

    static RENode.Expr expression(String regexp) {
        try {
            REParser parser = new REParser(regexp);
            RENode.Expr re = parser.parseExpression();
            assertTrue(parser.isDone());
            return re;
        } catch (SyntaxException e) {
            AssertionFailedError afe = new AssertionFailedError();
            afe.initCause(e);
            throw afe;
        }
    }

    static RENode.CharacterClass characterClass(String regexp) {
        try {
            REParser parser = new REParser(regexp);
            RENode.CharacterClass re = parser.parseCharacterClass();
            assertTrue(parser.isDone());
            return re;
        } catch (SyntaxException e) {
            AssertionFailedError afe = new AssertionFailedError();
            afe.initCause(e);
            throw afe;
        }
    }

    public void testSimple() {
        assertRender(expression("."), ".");
        assertRender(expression("^"), "^");
        assertRender(expression("\\."), "\\.");
    }

    public void testDisjunction() {
        assertRender(disjunction(""), "");
        assertRender(disjunction("|"), "|");
        assertRender(disjunction("a|"), "a|");
        assertRender(disjunction("|a"), "|a");
        assertRender(disjunction("a|b"), "a|b");
    }

    public void testAlternative() {
        assertRender(alternative(""), "");
        assertRender(alternative("ab"), "ab");
        assertRender(alternative("abc"), "abc");
    }

    public void testCharacterClass() {
        assertRender(characterClass(""), "");
        assertRender(characterClass("[a]"), "[a]");
        assertRender(characterClass("[ab]"), "[ab]");
        assertRender(characterClass("[.\\]]"), "[\\.\\]]");
    }
}
