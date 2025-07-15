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
package org.exoplatform.web.url;

import java.util.HashMap;
import java.util.Map;


public class URLFactoryService extends URLFactory {

    /** . */
    private final Map<ResourceType<?, ?>, URLFactoryPlugin> plugins;

    public URLFactoryService() {
        this.plugins = new HashMap<ResourceType<?, ?>, URLFactoryPlugin>();
    }

    @Override
    public <R, U extends PortalURL<R, U>> U newURL(ResourceType<R, U> resourceType, URLContext context)
            throws NullPointerException {
        if (resourceType == null) {
            throw new NullPointerException("No null resource type accepted");
        }

        // Can't really make that checked
        @SuppressWarnings("unchecked")
        URLFactoryPlugin<R, U> plugin = (URLFactoryPlugin<R, U>) plugins.get(resourceType);

        //
        return plugin != null ? plugin.newURL(context) : null;
    }

    public void addPlugin(URLFactoryPlugin plugin) {
        plugins.put(plugin.getResourceType(), plugin);
    }
}
