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
package org.exoplatform.web.controller.performance;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.metadata.ControllerDescriptor;
import org.exoplatform.web.controller.metadata.DescriptorBuilder;
import org.exoplatform.web.controller.router.RegexFactory;
import org.exoplatform.web.controller.router.RenderContext;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.controller.router.URIWriter;

public class Standalone extends TestCase {

    /** . */
    private static final QualifiedName HANDLER = QualifiedName.parse("gtn:handler");

    /** . */
    private static final QualifiedName SITETYPE = QualifiedName.parse("gtn:sitetype");

    /** . */
    private static final QualifiedName SITENAME = QualifiedName.parse("gtn:sitename");

    /** . */
    private static final QualifiedName PATH = QualifiedName.parse("gtn:path");

    public void testFoo() throws Exception {

        URL url = ControllerRendererDriver.class.getResource("controller.xml");
        DescriptorBuilder builder = new DescriptorBuilder();
        ControllerDescriptor descriptor = builder.build(url.openStream());
        Router router = descriptor.build(RegexFactory.JAVA);

        //
        Map<QualifiedName, String> map = new HashMap<QualifiedName, String>();
        map.put(HANDLER, "portal");
        map.put(SITETYPE, "portal");
        map.put(SITENAME, "classic");
        map.put(PATH, "page");

        //
        RenderContext context = new RenderContext(map);

        //
        URIWriter writer = new URIWriter(NullAppendable.INSTANCE);

        //
        String s = router.render(map);
        assertNotNull(s);

        //
        while (true) {
            writer.reset(NullAppendable.INSTANCE);
            router.render(context, writer);
        }
    }
}
