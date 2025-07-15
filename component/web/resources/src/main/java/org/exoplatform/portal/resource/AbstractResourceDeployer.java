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
package org.exoplatform.portal.resource;

import java.net.MalformedURLException;
import java.net.URL;

import org.gatein.wci.WebApp;
import org.gatein.wci.WebAppEvent;
import org.gatein.wci.WebAppLifeCycleEvent;
import org.gatein.wci.WebAppListener;

public abstract class AbstractResourceDeployer implements WebAppListener {

    public static final String GATEIN_CONFIG_RESOURCE = "/WEB-INF/gatein-resources.xml";

    /**
     * @see org.gatein.wci.WebAppListener#onEvent(org.gatein.wci.WebAppEvent)
     */
    public void onEvent(WebAppEvent event) {
        if (event instanceof WebAppLifeCycleEvent) {
            WebAppLifeCycleEvent lifeCycleEvent = (WebAppLifeCycleEvent) event;
            WebApp webApp = null;
            URL url = null;
            switch (lifeCycleEvent.getType()) {
                case WebAppLifeCycleEvent.ADDED:
                    webApp = event.getWebApp();
                    url = getGateinResourcesXml(webApp);
                    if (url != null) {
                        add(webApp, url);
                    }
                    break;
                case WebAppLifeCycleEvent.REMOVED:
                    webApp = event.getWebApp();
                    try {
                      url = getGateinResourcesXml(webApp);
                    } catch (Exception e) {
                      // Could not access resources when Wildfly is stopping
                    }
                    if (url != null) {
                        remove(event.getWebApp());
                    }
                    break;
            }
        }
    }

    /**
     * Called on web application add event if the application contains {@value #GATEIN_CONFIG_RESOURCE} file.
     *
     * @param webApp
     * @param url
     */
    protected abstract void add(WebApp webApp, URL url);

    /**
     * Called on web application remove event if the application contains {@value #GATEIN_CONFIG_RESOURCE} file.
     *
     * @param webApp
     */
    protected abstract void remove(WebApp webApp);

    protected URL getGateinResourcesXml(final WebApp webApp) {
        try {
            return webApp.getServletContext().getResource(GATEIN_CONFIG_RESOURCE);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}