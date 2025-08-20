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
package io.meeds.web.security.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.exoplatform.container.PortalContainer;

import io.meeds.web.security.plugin.RootWebappFilterPlugin;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RootWebappFilter extends HttpFilter {

  private static final long            serialVersionUID = -1644590354419007074L;

  private List<RootWebappFilterPlugin> rootWebappFilterPlugins;                 // NOSONAR

  @Override
  protected void doFilter(HttpServletRequest httpRequest,
                          HttpServletResponse httpResponse,
                          FilterChain chain) throws IOException,
                                             ServletException {
    RootWebappFilterPlugin rootWebappFilterPlugin = getRootWebappFilterPlugins().stream()
                                                                                .filter(p -> p.matches(httpRequest, httpResponse))
                                                                                .findFirst()
                                                                                .orElse(null);
    if (rootWebappFilterPlugin == null) {
      super.doFilter(httpRequest, httpResponse, chain);
    } else {
      rootWebappFilterPlugin.doFilter(httpRequest, httpResponse, chain);
    }
  }

  public List<RootWebappFilterPlugin> getRootWebappFilterPlugins() {
    if (rootWebappFilterPlugins == null) {
      rootWebappFilterPlugins = PortalContainer.getInstance().getComponentInstancesOfType(RootWebappFilterPlugin.class);
      if (rootWebappFilterPlugins == null) {
        rootWebappFilterPlugins = Collections.emptyList();
      }
    }
    return rootWebappFilterPlugins;
  }

}
