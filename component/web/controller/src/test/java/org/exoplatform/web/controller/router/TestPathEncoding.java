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

import static org.exoplatform.web.controller.metadata.DescriptorBuilder.*;

import java.util.Collections;

import org.exoplatform.web.controller.QualifiedName;

public class TestPathEncoding extends AbstractTestController {

    public void testSegment1() throws Exception {
        Router router = router().add(route("/?")).build();
        assertEquals("/%3F", router.render(Collections.<QualifiedName, String> emptyMap()));
    }

    public void testSegment2() throws Exception {
        Router router = router().add(route("/?{p}?")).build();
        assertEquals("/%3Fa%3F", router.render(Collections.singletonMap(Names.P, "a")));
    }

    public void testSegment3() throws Exception {
        Router router = router().add(route("/{p}")).build();
        assertEquals("/%C2%A2", router.render(Collections.singletonMap(Names.P, "\u00A2")));
    }

    public void testParamDefaultForm() throws Exception {
        Router router = router().add(route("/{p}").with(pathParam("p").matchedBy(".+"))).build();

        // Route
        assertEquals(Collections.singletonMap(Names.P, "/"), router.route("/_"));
        assertEquals(Collections.singletonMap(Names.P, "_"), router.route("/%5F"));
        assertEquals(Collections.singletonMap(Names.P, "_/"), router.route("/%5F_"));
        assertEquals(Collections.singletonMap(Names.P, "/_"), router.route("/_%5F"));
        assertEquals(Collections.singletonMap(Names.P, "?"), router.route("/%3F"));

        // Render
        assertEquals("/_", router.render(Collections.singletonMap(Names.P, "/")));
        assertEquals("/%5F", router.render(Collections.singletonMap(Names.P, "_")));
        assertEquals("/%5F_", router.render(Collections.singletonMap(Names.P, "_/")));
        assertEquals("/_%5F", router.render(Collections.singletonMap(Names.P, "/_")));
        assertEquals("/%3F", router.render(Collections.singletonMap(Names.P, "?")));
    }

    public void testAlternativeSepartorEscape() throws Exception {
        Router router = router().separatorEscapedBy(':').add(route("/{p}").with(pathParam("p").matchedBy(".+"))).build();

        // Route
        assertEquals(Collections.singletonMap(Names.P, "/"), router.route("/:"));
        assertEquals(Collections.singletonMap(Names.P, "_"), router.route("/_"));
        assertEquals(Collections.singletonMap(Names.P, ":"), router.route("/%3A"));

        // Render
        assertEquals("/:", router.render(Collections.singletonMap(Names.P, "/")));
        assertEquals("/_", router.render(Collections.singletonMap(Names.P, "_")));
        assertEquals("/%3A", router.render(Collections.singletonMap(Names.P, ":")));
    }

    public void testBug() throws Exception {
        Router router = router().add(route("/{p}").with(pathParam("p").matchedBy("[^_]+"))).build();

        // This is a *known* bug
        assertNull(router.route("/_"));

        // This is expected
        assertEquals("/_", router.render(Collections.singletonMap(Names.P, "/")));

        // This is expected
        assertNull(router.route("/%5F"));
        assertEquals("", router.render(Collections.singletonMap(Names.P, "_")));
    }

    public void testParamPreservePath() throws Exception {
        Router router = router().add(route("/{p}").with(pathParam("p").matchedBy("[^/]+").preservePath())).build();

        // Route
        assertEquals(Collections.singletonMap(Names.P, "_"), router.route("/_"));
        assertNull(router.route("//"));

        // Render
        assertEquals("", router.render(Collections.singletonMap(Names.P, "/")));
    }

    public void testD() throws Exception {
        Router router = router().add(route("/{p}").with(pathParam("p").matchedBy("/[a-z]+/[a-z]+/?"))).build();

        // Route
        assertEquals(Collections.singletonMap(Names.P, "/platform/administrator"), router.route("/_platform_administrator"));
        assertEquals(Collections.singletonMap(Names.P, "/platform/administrator"), router.route("/_platform_administrator/"));
        assertEquals(Collections.singletonMap(Names.P, "/platform/administrator/"), router.route("/_platform_administrator_"));
        assertEquals(Collections.singletonMap(Names.P, "/platform/administrator/"), router.route("/_platform_administrator_/"));

        // Render
        assertEquals("/_platform_administrator", router.render(Collections.singletonMap(Names.P, "/platform/administrator")));
        assertEquals("/_platform_administrator_", router.render(Collections.singletonMap(Names.P, "/platform/administrator/")));
        assertEquals("", router.render(Collections.singletonMap(Names.P, "/platform/administrator//")));
    }

    public void testWildcardPathParamWithPreservePath() throws Exception {
        Router router = router().add(route("/{p}").with(pathParam("p").matchedBy(".*").preservePath())).build();

        // Render
        assertEquals("/", router.render(Collections.singletonMap(Names.P, "")));
        assertEquals("//", router.render(Collections.singletonMap(Names.P, "/")));
        assertEquals("/a", router.render(Collections.singletonMap(Names.P, "a")));
        assertEquals("/a/b", router.render(Collections.singletonMap(Names.P, "a/b")));

        // Route
        assertEquals(Collections.singletonMap(Names.P, ""), router.route("/"));
        assertEquals(Collections.singletonMap(Names.P, "/"), router.route("//"));
        assertEquals(Collections.singletonMap(Names.P, "a"), router.route("/a"));
        assertEquals(Collections.singletonMap(Names.P, "a/b"), router.route("/a/b"));
    }

    public void testWildcardParamPathWithDefaultForm() throws Exception {
        Router router = router().add(route("/{p}").with(pathParam("p").matchedBy(".*"))).build();

        //
        assertEquals("/_", router.render(Collections.singletonMap(Names.P, "/")));
    }

}
