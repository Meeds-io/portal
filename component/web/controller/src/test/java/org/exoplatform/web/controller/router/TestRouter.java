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

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.exoplatform.web.WebAppController;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.metadata.ControllerDescriptor;
import org.exoplatform.web.controller.metadata.DescriptorBuilder;

public class TestRouter extends AbstractTestController {

    public void testMatcher() throws Exception {

        URL routerURL = TestRouter.class.getClassLoader().getResource("controller.xml");
        ControllerDescriptor routerDesc = new DescriptorBuilder().build(routerURL.openStream());
        Router router = new Router(routerDesc);

        //
        Iterator<Map<QualifiedName, String>> matcher = router.matcher("/", null);
        assertTrue(matcher.hasNext());
        Map<QualifiedName, String> params = matcher.next();
        Map<QualifiedName, String>  expected = new HashMap<QualifiedName, String>();
        expected.put(WebAppController.HANDLER_PARAM, "default");
        assertEquals(expected, params);

        assertFalse(matcher.hasNext());

        //
        matcher = router.matcher("/abc", null);

        assertTrue(matcher.hasNext());
        params = matcher.next();
        assertEquals("portal", params.get(WebAppController.HANDLER_PARAM));
        expected = new HashMap<QualifiedName, String>();
        expected.put(WebAppController.HANDLER_PARAM, "portal");
        expected.put(Names.GTN_SITETYPE, "portal");
        expected.put(Names.GTN_SITENAME, "abc");
        expected.put(Names.GTN_LANG, "");
        expected.put(Names.GTN_PATH, "");
        assertEquals(expected, params);

        assertFalse(matcher.hasNext());

        //
        matcher = router.matcher("/fr/exist/point", null);

        assertTrue(matcher.hasNext());
        params = matcher.next();
        assertEquals("portal", params.get(WebAppController.HANDLER_PARAM));
        expected = new HashMap<QualifiedName, String>();
        expected.put(WebAppController.HANDLER_PARAM, "portal");
        expected.put(Names.GTN_SITETYPE, "portal");
        expected.put(Names.GTN_LANG, "fr");
        expected.put(Names.GTN_SITENAME, "exist");
        expected.put(Names.GTN_PATH, "point");
        assertEquals(expected, params);

        assertFalse(matcher.hasNext());

        //
        matcher = router.matcher("/download", null);

        assertTrue(matcher.hasNext());
        params = matcher.next();
        assertEquals("download", params.get(WebAppController.HANDLER_PARAM));
        expected = new HashMap<QualifiedName, String>();
        expected.put(WebAppController.HANDLER_PARAM, "download");
        assertEquals(expected, params);

        assertTrue(matcher.hasNext());
        params = matcher.next();
        assertEquals("portal", params.get(WebAppController.HANDLER_PARAM));
        expected = new HashMap<QualifiedName, String>();
        expected.put(WebAppController.HANDLER_PARAM, "portal");
        expected.put(Names.GTN_SITETYPE, "portal");
        expected.put(Names.GTN_LANG, "");
        expected.put(Names.GTN_SITENAME, "download");
        expected.put(Names.GTN_PATH, "");
        assertEquals(expected, params);

        assertFalse(matcher.hasNext());

        //
        matcher = router.matcher("/en/classic/home", null);

        assertTrue(matcher.hasNext());
        params = matcher.next();
        expected = new HashMap<QualifiedName, String>();
        expected.put(WebAppController.HANDLER_PARAM, "portal");
        expected.put(Names.GTN_SITETYPE, "portal");
        expected.put(Names.GTN_LANG, "en");
        expected.put(Names.GTN_SITENAME, "classic");
        expected.put(Names.GTN_PATH, "home");
        assertEquals(expected, params);

        assertFalse(matcher.hasNext());

        //
        matcher = router.matcher("/g/classic/register",
                                 new HashMap<String, String[]>(Collections.singletonMap("lang", new String[] { "fr" })));
        assertTrue(matcher.hasNext());
        params = matcher.next();
        expected = new HashMap<QualifiedName, String>();
        expected.put(WebAppController.HANDLER_PARAM, "portal");
        expected.put(Names.GTN_SITETYPE, "group");
        expected.put(Names.GTN_LANG, "fr");
        expected.put(Names.GTN_SITENAME, "classic");
        expected.put(Names.GTN_PATH, "register");
        assertEquals(expected, params);

        assertTrue(matcher.hasNext());
        params = matcher.next();
        expected = new HashMap<QualifiedName, String>();
        expected.put(WebAppController.HANDLER_PARAM, "portal");
        expected.put(Names.GTN_SITETYPE, "portal");
        expected.put(Names.GTN_LANG, "");
        expected.put(Names.GTN_SITENAME, "g");
        expected.put(Names.GTN_PATH, "classic/register");
        assertEquals(expected, params);

        assertFalse(matcher.hasNext());
    }
}
