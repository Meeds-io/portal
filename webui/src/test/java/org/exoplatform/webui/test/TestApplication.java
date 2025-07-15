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
package org.exoplatform.webui.test;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.exoplatform.component.test.AbstractGateInTest;

/**
 * Author : Nhu Dinh Thuan nhudinhthuan@yahoo.com May 5, 2006
 */
public class TestApplication extends AbstractGateInTest {

    public void testApplication() throws Exception {
        Map<String, String> initParams = new HashMap<String, String>();
        initParams.put("webui.configuration", "webui.configuration");

        String basedir = System.getProperty("basedir");
        String webuiConfig = basedir + "/src/test/resources/webui-configuration.xml";
        Map<String, URL> resources = new HashMap<String, URL>();
        resources.put("webui.configuration", new File(webuiConfig).toURL());
        initParams.put("webui.configuration", new File(webuiConfig).toURL().toString());

        MockApplication mock = new MockApplication(initParams, resources, null);
        mock.onInit();
        mock.onDestroy();
    }

}
