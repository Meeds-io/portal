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

public class TestDuplicateParam extends AbstractTestController {

    public void testPathParamDuplicatesRequestParam() throws Exception {
        try {
            router().add(route("/").with(requestParam("foo").named("a")).sub(route("/{foo}"))).build();
            fail();
        } catch (MalformedRouteException e) {
        }

        //
        try {
            router().add(route("/").with(requestParam("foo").named("a")).sub(route("/bar").sub(route("/{foo}")))).build();
            fail();
        } catch (MalformedRouteException e) {
        }
    }

    public void testPathParamDuplicatesRouteParam() throws Exception {
        try {
            router().add(route("/").with(routeParam("foo").withValue("bar")).sub(route("/{foo}"))).build();
            fail();
        } catch (MalformedRouteException e) {
        }

        //
        try {
            router().add(route("/").with(routeParam("foo").withValue("bar")).sub(route("/bar").sub(route("/{foo}")))).build();
            fail();
        } catch (MalformedRouteException e) {
        }
    }

    public void testPathParamDuplicatesPathParam() throws Exception {
        try {
            router().add(route("/{foo}").sub(route("/{foo}"))).build();
            fail();
        } catch (MalformedRouteException e) {
        }

        //
        try {
            router().add(route("/{foo}").sub(route("/bar").sub(route("/{foo}")))).build();
            fail();
        } catch (MalformedRouteException e) {
        }
    }

    public void testRequestParamDuplicatesRequestParam() throws Exception {
        try {
            router().add(
                    route("/").with(requestParam("foo").named("a")).sub(route("/bar").with(routeParam("foo").withValue("b"))))
                    .build();
            fail();
        } catch (MalformedRouteException e) {
        }

        //
        try {
            router().add(
                    route("/").with(requestParam("foo").named("a")).sub(
                            route("/bar").sub(route("/foo").with(routeParam("foo").withValue("b"))))).build();
            fail();
        } catch (MalformedRouteException e) {
        }
    }

    public void testRequestParamDuplicatesRouteParam() throws Exception {
        try {
            router().add(
                    route("/").with(routeParam("foo").withValue("bar")).sub(
                            route("/bar").with(routeParam("foo").withValue("b")))).build();
            fail();
        } catch (MalformedRouteException e) {
        }

        //
        try {
            router().add(
                    route("/").with(routeParam("foo").withValue("bar")).sub(
                            route("/bar").sub(route("/foo").with(routeParam("foo").withValue("b"))))).build();
            fail();
        } catch (MalformedRouteException e) {
        }
    }

    public void testRequestParamDuplicatesPathParam() throws Exception {
        try {
            router().add(route("/{foo}").sub(route("/bar").with(routeParam("foo").withValue("b")))).build();
            fail();
        } catch (MalformedRouteException e) {
        }

        //
        try {
            router().add(route("/{foo}").sub(route("/bar").sub(route("/foo").with(routeParam("foo").withValue("b"))))).build();
            fail();
        } catch (MalformedRouteException e) {
        }
    }

    public void testRouteParamDuplicatesRequestParam() throws Exception {
        try {
            router().add(
                    route("/").with(requestParam("foo").named("a")).sub(route("/bar").with(routeParam("foo").withValue("b"))))
                    .build();
            fail();
        } catch (MalformedRouteException e) {
        }

        //
        try {
            router().add(
                    route("/").with(requestParam("foo").named("a")).sub(
                            route("/bar").sub(route("/foo").with(routeParam("foo").withValue("b"))))).build();
            fail();
        } catch (MalformedRouteException e) {
        }
    }

    public void testRouteParamDuplicatesRouteParam() throws Exception {
        try {
            router().add(
                    route("/").with(routeParam("foo").withValue("bar")).sub(
                            route("/bar").with(routeParam("foo").withValue("b")))).build();
            fail();
        } catch (MalformedRouteException e) {
        }

        //
        try {
            router().add(
                    route("/").with(routeParam("foo").withValue("bar")).sub(
                            route("/bar").sub(route("/foo").with(routeParam("foo").withValue("b"))))).build();
            fail();
        } catch (MalformedRouteException e) {
        }
    }

    public void testRouteParamDuplicatesPathParam() throws Exception {
        try {
            router().add(route("/{foo}").sub(route("/bar").with(routeParam("foo").withValue("b")))).build();
            fail();
        } catch (MalformedRouteException e) {
        }

        //
        try {
            router().add(route("/{foo}").sub(route("/bar").sub(route("/foo").with(routeParam("foo").withValue("b"))))).build();
            fail();
        } catch (MalformedRouteException e) {
        }
    }
}
