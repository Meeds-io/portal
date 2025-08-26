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
package org.exoplatform.web.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.exoplatform.container.*;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;


/**
 * This class allows the rest of the platform to define new filters thanks to the external plugins.
 */
public class ExtensibleFilter {

    private static final Log LOG = ExoLogger.getLogger(ExtensibleFilter.class);

    /**
     * List of all the sub filters
     */
    private volatile List<FilterDefinition> filters = Collections.unmodifiableList(new ArrayList<FilterDefinition>());

    /**
     * Adds new {@link FilterDefinition}
     */
    public void addFilterDefinitions(FilterDefinitionPlugin plugin) {
        addFilterDefinitions(plugin.getFilterDefinitions());
    }

    /**
     * Adds new {@link FilterDefinition}
     */
    void addFilterDefinitions(List<FilterDefinition> pluginFilters) {
        if (pluginFilters == null || pluginFilters.isEmpty()) {
            // No filter to add
            return;
        }
        synchronized (this) {
            List<FilterDefinition> result = new ArrayList<FilterDefinition>(filters);
            result.addAll(pluginFilters);
            this.filters = Collections.unmodifiableList(result);
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain, String path) throws IOException,
            ServletException {
        ExtensibleFilterChain efChain = new ExtensibleFilterChain(chain, filters, path);
        efChain.doFilter(request, response);
    }

    private static class ExtensibleFilterChain implements FilterChain {

        private final FilterChain parentChain;

        private final Iterator<FilterDefinition> filters;

        private final String path;

        private ExtensibleFilterChain(FilterChain parentChain, List<FilterDefinition> filters, String path_) {
            this.parentChain = parentChain;
            this.filters = filters.iterator();
            this.path = path_;
        }

        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            PortalContainer exoContainer = PortalContainer.getInstanceIfPresent();
            if (exoContainer != null) {
              ExoContainerContext.setCurrentContainer(exoContainer);
              RequestLifeCycle.begin(exoContainer);
            }
            try {
              while (filters.hasNext()) {
                FilterDefinition filterDef = filters.next();
                if (filterDef.getMapping().match(path)) {
                  filterDef.getFilter().doFilter(request, response, this);
                  return;
                }
              }
            } finally {
              if (exoContainer != null) {
                RequestLifeCycle.end();
                List<ComponentRequestLifecycle> transactionalServices = exoContainer.getComponentInstancesOfType(ComponentRequestLifecycle.class);
                for (ComponentRequestLifecycle service : transactionalServices) {
                  if (service.isStarted(exoContainer)) {
                    if (LOG.isDebugEnabled()) {
                      LOG.debug("The service {} didn't called endRequest. Commit transaction anyway.",
                                service.getClass().getName());
                    }
                    service.endRequest(exoContainer);
                    if (service.isStarted(exoContainer)) {
                      LOG.error("The service {} didn't ended properly even after calling endRequest",
                                service.getClass().getName());
                    }
                  }
                }
              }
            }
            parentChain.doFilter(request, response);
        }
    }
}
