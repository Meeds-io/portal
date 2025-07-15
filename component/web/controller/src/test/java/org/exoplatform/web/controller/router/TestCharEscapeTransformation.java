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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.exoplatform.component.test.BaseGateInTest;
import org.exoplatform.web.controller.regexp.RENode;
import org.exoplatform.web.controller.regexp.REParser;
import org.exoplatform.web.controller.regexp.RERenderer;

public class TestCharEscapeTransformation extends BaseGateInTest {

    private void match(String pattern, String test, String expectedValue) throws Exception {
        REParser parser = new REParser(pattern);
        CharEscapeTransformation escaper = new CharEscapeTransformation('/', '_');
        RENode.Disjunction re = parser.parseDisjunction();
        re.accept(escaper);
        Pattern p = Pattern.compile(RERenderer.render(re, new StringBuilder()).toString());
        Matcher matcher = p.matcher(test);
        assertTrue(matcher.find());
        assertEquals(expectedValue, matcher.group());
    }

    public void testMatch() throws Exception {
        match(".*", "_", "_");
        match(".*", "_/", "_");
        match(".*", "_/_", "_");
        match("/", "_/", "_");
        match("/*", "_/_", "_");
        match("[/a]*", "_a_/_", "_a_");
        match("[,-1&&[^/]]*", "_/_", "");
    }
}
