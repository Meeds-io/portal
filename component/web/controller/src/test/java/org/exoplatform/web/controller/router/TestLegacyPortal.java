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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.exoplatform.web.controller.QualifiedName;

public class TestLegacyPortal extends TestCase {

    /** . */
    private Router router;

    @Override
    protected void setUp() throws Exception {
        this.router = router().add(
                route("/")
                        .with(routeParam("gtn:handler").withValue("portal"))
                        .sub(route("/public/{gtn:sitename}{gtn:path}").with(routeParam("gtn:access").withValue("public"),
                                pathParam("gtn:path").matchedBy(".*").preservePath()))
                        .sub(route("/private/{gtn:sitename}{gtn:path}").with(routeParam("gtn:access").withValue("private"),
                                pathParam("gtn:path").matchedBy(".*").preservePath()))).build();
    }

    public void testPrivateClassicComponent() throws Exception {
        Map<QualifiedName, String> expectedParameters = new HashMap<QualifiedName, String>();
        expectedParameters.put(Names.GTN_HANDLER, "portal");
        expectedParameters.put(Names.GTN_SITENAME, "classic");
        expectedParameters.put(Names.GTN_ACCESS, "private");
        expectedParameters.put(Names.GTN_PATH, "");

        //
        assertEquals(expectedParameters, router.route("/private/classic"));
        assertEquals("/private/classic", router.render(expectedParameters));
    }

    public void testPrivateClassic() throws Exception {
        Map<QualifiedName, String> expectedParameters = new HashMap<QualifiedName, String>();
        expectedParameters.put(Names.GTN_HANDLER, "portal");
        expectedParameters.put(Names.GTN_SITENAME, "classic");
        expectedParameters.put(Names.GTN_ACCESS, "private");
        expectedParameters.put(Names.GTN_PATH, "");

        //
        assertEquals(expectedParameters, router.route("/private/classic"));
        assertEquals("/private/classic", router.render(expectedParameters));
    }

    public void testPrivateClassicSlash() throws Exception {
        Map<QualifiedName, String> expectedParameters = new HashMap<QualifiedName, String>();
        expectedParameters.put(Names.GTN_HANDLER, "portal");
        expectedParameters.put(Names.GTN_SITENAME, "classic");
        expectedParameters.put(Names.GTN_ACCESS, "private");
        expectedParameters.put(Names.GTN_PATH, "/");

        //
        assertEquals(expectedParameters, router.route("/private/classic/"));
        assertEquals("/private/classic/", router.render(expectedParameters));
    }

    public void testPrivateClassicSlashComponent() throws Exception {
        Map<QualifiedName, String> expectedParameters = new HashMap<QualifiedName, String>();
        expectedParameters.put(Names.GTN_HANDLER, "portal");
        expectedParameters.put(Names.GTN_SITENAME, "classic");
        expectedParameters.put(Names.GTN_ACCESS, "private");
        expectedParameters.put(Names.GTN_PATH, "/");

        //
        assertEquals(expectedParameters, router.route("/private/classic/"));
        assertEquals("/private/classic/", router.render(expectedParameters));
    }

    public void testPrivateClassicHome() throws Exception {
        Map<QualifiedName, String> expectedParameters = new HashMap<QualifiedName, String>();
        expectedParameters.put(Names.GTN_HANDLER, "portal");
        expectedParameters.put(Names.GTN_SITENAME, "classic");
        expectedParameters.put(Names.GTN_ACCESS, "private");
        expectedParameters.put(Names.GTN_PATH, "/home");

        //
        assertEquals(expectedParameters, router.route("/private/classic/home"));
        assertEquals("/private/classic/home", router.render(expectedParameters));
    }

    public void testPrivateClassicHomeComponent() throws Exception {
        Map<QualifiedName, String> expectedParameters = new HashMap<QualifiedName, String>();
        expectedParameters.put(Names.GTN_HANDLER, "portal");
        expectedParameters.put(Names.GTN_SITENAME, "classic");
        expectedParameters.put(Names.GTN_ACCESS, "private");
        expectedParameters.put(Names.GTN_PATH, "/home");

        //
        assertEquals(expectedParameters, router.route("/private/classic/home"));
        assertEquals("/private/classic/home", router.render(expectedParameters));
    }
}
