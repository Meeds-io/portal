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
package org.gatein.portal.init;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.web.AbstractFilter;

/**
 * A filter for checking a flag if Root Container is properly initialized.
 * If not, filter will send 503 response to user avoiding accessing portal whe is being started.
 *
 */
public class PortalCheckInitFilter extends AbstractFilter {

    private static volatile boolean isPortalStarted = false;
    private static final String ERROR_MSG = "Server is starting, please try in a few seconds...";

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
            ServletException {
        if (!isPortalStarted) {
            ExoContainer container = getContainer();
            if (container != null) {
                if (container instanceof PortalContainer) {
                    isPortalStarted = ((PortalContainer)container).isStarted();
                }
            }
            if (!isPortalStarted) {
                HttpServletResponse httpResp = (HttpServletResponse)resp;
                httpResp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, ERROR_MSG);
            } else {
                chain.doFilter(req, resp);
            }
        } else {
            chain.doFilter(req, resp);
        }
    }

    @Override
    public void destroy() {
     // nothing to do
    }

}