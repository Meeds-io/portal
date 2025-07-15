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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.exoplatform.portal.resource.compressor.ResourceCompressor;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.gatein.wci.WebApp;

/**
 * An abstract class for resource services in Portal like {@link SkinService} and {@link JavascriptConfigService}
 *
 */
public abstract class AbstractResourceService {

    /** . */
    protected final MainResourceResolver mainResolver;

    /** . */
    protected final ResourceCompressor compressor;

    /** . */
    protected final Map<String, WebApp> contexts;

    protected AbstractResourceService(ResourceCompressor compressor) {
        this.compressor = compressor;
        this.mainResolver = new MainResourceResolver();
        this.contexts = new ConcurrentHashMap<>();
    }

    /**
     * Add a resource resolver to plug external resolvers.
     *
     * @param resolver a resolver to add
     */
    public void addResourceResolver(ResourceResolver resolver) {
      if (!mainResolver.resolvers.contains(resolver)) {
        mainResolver.resolvers.add(resolver);
      }
    }

    public void registerContext(WebApp app) {
        mainResolver.registerContext(app.getServletContext());
        contexts.put(app.getContextPath(), app);
    }

    public void unregisterServletContext(WebApp app) {
        mainResolver.removeServletContext(app.getServletContext());
        contexts.remove(app.getContextPath());
    }
}
