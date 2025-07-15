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
package org.exoplatform.component.test.web;

import static junit.framework.TestCase.*;

import java.io.File;
import java.net.URL;

import org.gatein.common.io.IOTools;

public class ServletContextTestCase {

    public void testFileGetResource() throws Exception {
        URL url = ServletContextTestCase.class.getClassLoader().getResource("org/exoplatform/component/test/web/");
        assertNotNull(url);
        File root = new File(url.toURI());
        assertTrue(root.exists());
        assertTrue(root.isDirectory());

        //
        ServletContextImpl servletContext = new ServletContextImpl(root, "/webapp", "webapp");

        //
        URL fooURL = servletContext.getResource("/foo.txt");
        assertNotNull(fooURL);
        assertEquals("foo", new String(IOTools.getBytes(fooURL.openStream())));

        //
        URL barURL = servletContext.getResource("/folder/bar.txt");
        assertNotNull(barURL);
        assertEquals("bar", new String(IOTools.getBytes(barURL.openStream())));

        //
        assertEquals(null, servletContext.getResource("/bar.txt"));
    }

    public void testClassGetResource() throws Exception {
        ServletContextImpl servletContext = new ServletContextImpl(getClass(), "/webapp", "webapp");

        //
        URL fooURL = servletContext.getResource("/foo.txt");
        assertNotNull(fooURL);
        assertEquals("foo", new String(IOTools.getBytes(fooURL.openStream())));

        //
        URL barURL = servletContext.getResource("/folder/bar.txt");
        assertNotNull(barURL);
        assertEquals("bar", new String(IOTools.getBytes(barURL.openStream())));

        //
        assertEquals(null, servletContext.getResource("/bar.txt"));
    }

    public void testContextPath() {
        ServletContextImpl servletContext = new ServletContextImpl(getClass(), "/webapp", "webapp");
        assertEquals("/webapp", servletContext.getContextPath());
    }
}
