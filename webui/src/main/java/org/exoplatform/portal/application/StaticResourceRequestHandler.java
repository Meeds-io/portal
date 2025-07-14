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
package org.exoplatform.portal.application;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebRequestHandler;

/**
 * @author <a href="mailto:phuong.vu@exoplatform.com">Vu Viet Phuong</a>
 * @deprecated not needed anymore since the resources will be served directly
 *             from Web Archives using {@link ResourceRequestFilter}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class StaticResourceRequestHandler extends WebRequestHandler {

    @Override
    public String getHandlerName() {
        return "staticResource";
    }

    @Override
    public boolean execute(ControllerContext context) throws Exception {
        context.getResponse().setHeader("Cache-Control", "max-age=2592000,s-maxage=2592000");
        PortalContainer portalContainer = PortalContainer.getInstance();
        ServletContext mergedContext = portalContainer.getPortalContext();

        HttpServletRequest req = context.getRequest();
        HttpServletResponse res = context.getResponse();
        mergedContext.getNamedDispatcher("default").forward(req, res);
        return true;
    }

    @Override
    protected boolean getRequiresLifeCycle() {
        return false;
    }
}
