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
package io.meeds.spring.web.transaction;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.impl.EnvironmentContext;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * A class to start a Kernel transaction before any call to Spring REST endpoint
 */
public class PortalTransactionFilter implements Filter {

  private static final Log                LOG = ExoLogger.getLogger(PortalTransactionFilter.class);

  private PortalContainer                 container;

  private List<ComponentRequestLifecycle> transactionalServices;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (container == null) {
      initFilter();
    }
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      chain.doFilter(request, response);
    } finally {
      Map<Object, Throwable> results = RequestLifeCycle.end();
      for (Entry<Object, Throwable> entry : results.entrySet()) {
        if (entry.getValue() != null) {
          LOG.error("An error occurred while calling the method endRequest on " + entry.getKey(), entry.getValue());
        }
      }
      EnvironmentContext.setCurrent(null);
      for (ComponentRequestLifecycle service : transactionalServices) {
        if (service.isStarted(container)) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("The service {} didn't called endRequest, uri = {}, http method = {}. Commit transaction anyway.",
                      service.getClass().getName(),
                      ((HttpServletRequest) request).getRequestURI(),
                      ((HttpServletRequest) request).getMethod());
          }
          service.endRequest(container);
          if (service.isStarted(container)) {
            LOG.error("The service {} didn't ended properly even after calling endRequest",
                      service.getClass().getName(),
                      ((HttpServletRequest) request).getRequestURI(),
                      ((HttpServletRequest) request).getMethod());
          }
        }
      }
    }
  }

  private void initFilter() {
    container = PortalContainer.getInstance();
    transactionalServices = container.getComponentInstancesOfType(ComponentRequestLifecycle.class);
  }

}