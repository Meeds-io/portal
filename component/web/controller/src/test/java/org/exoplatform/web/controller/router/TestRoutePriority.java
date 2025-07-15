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


public class TestRoutePriority extends AbstractTestController {

    public void testExactMatchingAfterWildcard() throws Exception {

        Router router = router().add(route("/{foo}")).add(route("/foo").with(routeParam("foo").withValue("b"))).build();

        assertEquals(Collections.singletonMap(Names.FOO, "foo"), router.route("/foo"));
        assertEquals("/foo", router.render(Collections.singletonMap(Names.FOO, "foo")));
        assertEquals("/b", router.render(Collections.singletonMap(Names.FOO, "b")));
    }

    public void testExactMatchingBeforeWildcard() throws Exception {

        Router router = router().add(route("/foo").with(routeParam("foo").withValue("b"))).add(route("/{foo}")).build();

        assertEquals(Collections.singletonMap(Names.FOO, "b"), router.route("/foo"));
        assertEquals("/foo", router.render(Collections.singletonMap(Names.FOO, "b")));
        assertEquals("/foo", router.render(Collections.singletonMap(Names.FOO, "foo")));
    }
}
