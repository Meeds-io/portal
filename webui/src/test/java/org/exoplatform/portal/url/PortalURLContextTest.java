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
package org.exoplatform.portal.url;

import static org.exoplatform.web.controller.metadata.DescriptorBuilder.pathParam;
import static org.exoplatform.web.controller.metadata.DescriptorBuilder.route;
import static org.exoplatform.web.controller.metadata.DescriptorBuilder.routeParam;
import static org.exoplatform.web.controller.metadata.DescriptorBuilder.router;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;

import junit.framework.TestCase;

@RunWith(MockitoJUnitRunner.class)
public class PortalURLContextTest extends TestCase {

    private PortalURLContext urlContext;

    @Before
    public void begin() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/portal");

        HttpServletResponse response = mock(HttpServletResponse.class);
        WebAppController webAppController = mock(WebAppController.class);


        Router router = router().add(
                route("/public/{gtn:lang}/{gtn:sitename}{gtn:path}").with(routeParam("gtn:sitetype").withValue("portal"))
                        .with(routeParam("gtn:handler").withValue("portal"))
                        .with(pathParam("gtn:lang").matchedBy("([A-Za-z]{2}(-[A-Za-z]{2})?)?").preservePath())
                        .with(pathParam("gtn:path").matchedBy(".*").preservePath())).build();

        Map<QualifiedName, String> parameters = new HashMap<>();
        ControllerContext controllerContext = new ControllerContext(webAppController, router, request, response, parameters);
        SiteKey key = SiteKey.portal("classic");

        urlContext = new PortalURLContext(controllerContext, key);
    }

    @Test
    public void testXSS() {
        NodeURL url = new NodeURL(urlContext);
        url.setAuthorityUse(true);
        url.setSchemeUse(true);
        url.setLocale(Locale.FRENCH);
        url.setResource(new NavigationResource(SiteKey.portal("classic"),
                "/home/&apos;%29&semi;alert%28&apos;xss-vulnerability&apos;%29;alert%28&apos;"));

        String expected = "http://localhost:8080/portal/public/fr/classic/home/&amp;apos;%2529&amp;semi;alert%2528&amp;apos;xss-vulnerability&amp;apos;%2529;alert%2528&amp;apos;";
        assertEquals(expected, url.toString());
    }
}
