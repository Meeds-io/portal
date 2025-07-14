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
package org.exoplatform.portal.application.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.picocontainer.Startable;

public class ContextualPropertyManagerImpl implements ContextualPropertyManager, Startable {

    private ArrayList<AbstractContextualPropertyProviderPlugin> propertyProviderPlugins;

    public ContextualPropertyManagerImpl() throws Exception {
        propertyProviderPlugins = new ArrayList<AbstractContextualPropertyProviderPlugin>();
    }

    public Map<QName, String[]> getProperties(UIPortlet portletWindow) {
        Map<QName, String[]> whatPortletWindowSee = new HashMap<QName, String[]>();

        // No need to use an iterator here
        for (int i = 0; i < propertyProviderPlugins.size(); i++) {
            AbstractContextualPropertyProviderPlugin plugin = propertyProviderPlugins.get(i);
            plugin.getProperties(portletWindow, whatPortletWindowSee);
        }

        //
        return whatPortletWindowSee;
    }

    public void start() {
    }

    public void stop() {
    }

    public void addPlugin(ComponentPlugin plugin) {
        if (plugin instanceof AbstractContextualPropertyProviderPlugin) {
            propertyProviderPlugins.add((AbstractContextualPropertyProviderPlugin) plugin);
        }
    }
}
