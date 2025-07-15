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
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.web.AbstractFilter;

/**
 * This filter allows the rest of the platform to add their own filters without changing the web.xml file. It is based on
 * {@link ExtensibleFilter} which is a component that supports plugin.
 *
 * Created by The eXo Platform SAS Author : Nicolas Filotto nicolas.filotto@exoplatform.com 25 sept. 2009
 */
public class GenericFilter extends AbstractFilter {

    private Pattern contextPathPattern;

    /**
     * @see jakarta.servlet.Filter#destroy()
     */
    public void destroy() {
    }

    @Override
    protected void afterInit(FilterConfig config) throws ServletException {
        ServletContext servletContext = this.getServletContext();
        contextPathPattern = Pattern.compile("[/]*" + servletContext.getContextPath() + "[/]*");
    }

    /**
     * This filter calls <code>doFilter</code> of the {@link ExtensibleFilter} of the current eXo container if it cans be found
     * otherwise it releases filter
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        ExoContainer container = getContainer();
        ExtensibleFilter filter = (ExtensibleFilter) container.getComponentInstanceOfType(ExtensibleFilter.class);
        if (filter == null) {
            chain.doFilter(request, response);
        } else {
            String path = contextPathPattern.matcher(((HttpServletRequest) request).getRequestURI()).replaceFirst("/");
            filter.doFilter(request, response, chain, path);
        }
    }
}
