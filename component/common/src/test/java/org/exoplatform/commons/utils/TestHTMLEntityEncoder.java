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
package org.exoplatform.commons.utils;

import junit.framework.TestCase;

public class TestHTMLEntityEncoder extends TestCase {
    private HTMLEntityEncoder htmlEncoder = HTMLEntityEncoder.getInstance();

    public void testHTMLEncoding() {
        assertEquals("&lt;h1&gt;HELLO WORLD&lt;&#x2f;h1&gt;", htmlEncoder.encode("<h1>HELLO WORLD</h1>"));
        assertEquals("&lt;h1&gt;HELLO WORLD&lt;&#x2f;h1&gt;", htmlEncoder.encodeHTML("<h1>HELLO WORLD</h1>"));

        assertEquals("alert&#x28;&#x27;HELLO WORLD&#x27;&#x29;", htmlEncoder.encode("alert('HELLO WORLD')"));
        assertEquals("alert&#x28;&#x27;HELLO WORLD&#x27;&#x29;", htmlEncoder.encodeHTML("alert('HELLO WORLD')"));

        assertEquals(
                "&lt;a href&#x3d;&quot;http&#x3a;&#x2f;&#x2f;example.com&#x2f;&#x3f;name1&#x3d;value1&amp;name2&#x3d;value2&amp;name3&#x3d;a&#x2b;b&quot;&gt;link&lt;&#x2f;a&gt;",
                htmlEncoder.encode("<a href=\"http://example.com/?name1=value1&name2=value2&name3=a+b\">link</a>"));
        assertEquals(
                "&lt;a href&#x3d;&quot;http&#x3a;&#x2f;&#x2f;example.com&#x2f;&#x3f;name1&#x3d;value1&amp;name2&#x3d;value2&amp;name3&#x3d;a&#x2b;b&quot;&gt;link&lt;&#x2f;a&gt;",
                htmlEncoder.encodeHTML("<a href=\"http://example.com/?name1=value1&name2=value2&name3=a+b\">link</a>"));
    }

    public void testHTMLAttributeEncoding() {
        assertEquals("&lt;h1&gt;HELLO&#x20;WORLD&lt;&#x2f;h1&gt;", htmlEncoder.encodeHTMLAttribute("<h1>HELLO WORLD</h1>"));

        assertEquals("alert&#x28;&#x27;HELLO&#x20;WORLD&#x27;&#x29;", htmlEncoder.encodeHTMLAttribute("alert('HELLO WORLD')"));

        assertEquals(
                "&lt;a&#x20;href&#x3d;&quot;http&#x3a;&#x2f;&#x2f;example.com&#x2f;&#x3f;name1&#x3d;value1&amp;name2&#x3d;value2&amp;name3&#x3d;a&#x2b;b&quot;&gt;link&lt;&#x2f;a&gt;",
                htmlEncoder.encodeHTMLAttribute("<a href=\"http://example.com/?name1=value1&name2=value2&name3=a+b\">link</a>"));
    }

    public void testEmoticonsHTMLEncoding() {
        // Emoticon in the Unicode BMP (Basic Multilingual Plane) plane - Code point from 0000 to ​FFFF (16 bits)
        assertEquals("&lt;h1&gt;HELLO WORLD &#x2714;&lt;&#x2f;h1&gt;", htmlEncoder.encode("<h1>HELLO WORLD ✔</h1>"));
        // Emoticon in the Unicode BMP (Basic Multilingual Plane) plane with a variant
        assertEquals("&lt;h1&gt;HELLO WORLD &#x2714;&#xfe0f;&lt;&#x2f;h1&gt;", htmlEncoder.encode("<h1>HELLO WORLD ✔️</h1>"));
        // Emoticon in the supplementary planes - Code point from 10000 to 10FFFF - Represented in Java as 2 surrogates code points
        assertEquals("&lt;h1&gt;HELLO WORLD &#x1f44d;&lt;&#x2f;h1&gt;", htmlEncoder.encode("<h1>HELLO WORLD \uD83D\uDC4D</h1>"));
    }
}
