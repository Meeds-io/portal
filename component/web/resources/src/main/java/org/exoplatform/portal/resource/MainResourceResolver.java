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

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.ServletContext;

import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;

public class MainResourceResolver implements ResourceResolver {

    /** . */
    final Map<String, SimpleResourceContext> contexts;

    /** . */
    final List<ResourceResolver> resolvers;

    /** . */
    private final Log                          log = ExoLogger.getLogger(MainResourceResolver.class);

    public MainResourceResolver() {
        this.contexts = new ConcurrentHashMap<>();
        this.resolvers = new Vector<>();
    }

    /**
     * Register a servlet request context
     * <p>
     * Append a servlet context to the map of contexts if servlet context name is not existing
     *
     * @param servletContext servlet context which want to append
     * @return
     */
    SimpleResourceContext registerContext(ServletContext servletContext) {
        String key = "/" + servletContext.getServletContextName();
        SimpleResourceContext ctx = contexts.get(key);
        if (ctx == null) {
            ctx = new SimpleResourceContext(key, servletContext);
            contexts.put(ctx.getContextPath(), ctx);
        }
        return ctx;
    }

    /**
     * Remove a servlet context from map of contexts
     *
     * @param servletContext
     */
    public void removeServletContext(ServletContext servletContext) {
      String key = "/" + servletContext.getServletContextName();
      SimpleResourceContext ctx = contexts.get(key);
      if (ctx != null) {
        contexts.remove(ctx.getContextPath());
      } else {
        log.warn("Cannot find servlet context module");
      }
    }

    public Resource resolve(String path) {
        if (path == null) {
            throw new NullPointerException("No null path is accepted");
        }

        //
        for (ResourceResolver resolver : resolvers) {
            Resource res = resolver.resolve(path);
            if (res != null) {
                return res;
            }
        }

        //
        int i1 = path.indexOf("/", 2);
        if (i1 == -1) {
            throw new AssertionError();
        }
        String targetedContextPath = path.substring(0, i1);
        SimpleResourceContext context = contexts.get(targetedContextPath);

        //
        if (context == null) {
            log.warn("Could not resolve " + targetedContextPath + " resource for path " + path);
            return null;
        } else {
            return context.getResource(path.substring(i1));
        }

    }
}
