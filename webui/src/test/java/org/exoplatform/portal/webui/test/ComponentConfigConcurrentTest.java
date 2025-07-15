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
package org.exoplatform.portal.webui.test;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.component.test.AbstractGateInTest;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.webui.config.Event;

/**
 * Unit test for concurrent read of event from UI component configuration.
 *
 */
public class ComponentConfigConcurrentTest extends AbstractGateInTest {
    private static final int WORKERS_COUNT = 50;

    private MockApplication mockApplication;

    public void testConcurrentReadOfComponentEventConfig() throws Exception {
        // Init configuration and mock WebUI application
        Map<String, String> initParams = new HashMap<String, String>();
        initParams.put("webui.configuration", "webui.configuration");

        Map<String, URL> resources = new HashMap<String, URL>();
        String webuiConfig = getClass().getClassLoader().getResource("webui-configuration.xml").getFile();
        URI uri = new File(webuiConfig).toURI();
        resources.put("webui.configuration", uri.toURL());
        initParams.put("webui.configuration", uri.toURL().toString());

        mockApplication = new MockApplication(initParams, resources, null);
        mockApplication.onInit();

        // init workers list
        List<Worker> workers = new ArrayList<Worker>(WORKERS_COUNT);

        PortalContainer container = PortalContainer.getInstance();
        // test obtain event configuration concurrently with more worker threads
        for (int i = 0; i < WORKERS_COUNT; i++) {
            Worker worker = new Worker(container, "Worker-" + i);
            workers.add(worker);
            worker.start();
        }

        // Wait for all workers to finish
        for (Worker worker : workers) {
            worker.join(100);
        }

        // Go throguh all workers and throw error if some worker has null eventConfig
        for (Worker worker : workers) {
            assertNotNull("event configuration is null in worker " + worker.getName(), worker.eventConfig);
        }

        // destroy mock application
        mockApplication.onDestroy();
    }

    private class Worker extends Thread {
        private final PortalContainer container;

        private Event eventConfig = null;

        public Worker(PortalContainer container, String name) {
            super(name);
            this.container = container;
        }

        public void run() {
            ExoContainerContext.setCurrentContainer(container);
            try {
                UIPortal uiPortal = mockApplication.createUIComponent(UIPortal.class, null, null, null);
                eventConfig = uiPortal.getComponentConfig().getUIComponentEventConfig("Logout");

                // log message now if eventConfig is null, so that we know about all failed workers. Test will be failed later.
                if (eventConfig == null) {
                    log.error("eventConfig is null for worker " + getName());
                }
            } catch (Exception e) {
                log.error("Exception occured during concurrent test in worker " + getName(), e);
            } finally {
              ExoContainerContext.setCurrentContainer(null);
            }

        }

    }

}
