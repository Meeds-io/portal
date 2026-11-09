/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package org.exoplatform.web.application.javascript;

import junit.framework.TestCase;

public class TestMinifyDefault extends TestCase {

  public void testExplicitMinifyWins() {
    assertTrue(JavascriptConfigParser.isMinify("true", "/a.bundle.js", false));
    assertFalse(JavascriptConfigParser.isMinify("false", "/a.js", true));
  }

  public void testPrebuiltServedAsBuilt() {
    assertFalse(JavascriptConfigParser.isMinify(null, "/a.bundle.js", false));
    assertFalse(JavascriptConfigParser.isMinify(null, "/lib/b.min.js", false));
  }

  public void testPrebuiltMinifiedWhenAsked() {
    assertTrue(JavascriptConfigParser.isMinify(null, "/a.bundle.js", true));
    assertTrue(JavascriptConfigParser.isMinify(null, "/lib/b.min.js", true));
  }

  public void testOtherScriptsMinified() {
    assertTrue(JavascriptConfigParser.isMinify(null, "/a.js", false));
    assertTrue(JavascriptConfigParser.isMinify(null, "/bundle.js.map.js", false));
    assertTrue(JavascriptConfigParser.isMinify(null, null, false));
  }
}
