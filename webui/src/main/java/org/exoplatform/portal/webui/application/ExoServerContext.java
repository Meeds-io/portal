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
package org.exoplatform.portal.webui.application;

import java.io.IOException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.gatein.pc.portlet.impl.spi.AbstractServerContext;
import org.gatein.wci.RequestDispatchCallback;
import org.gatein.wci.ServletContainer;
import org.gatein.wci.ServletContainerFactory;

public class ExoServerContext extends AbstractServerContext {

    public ExoServerContext(HttpServletRequest clientRequest, HttpServletResponse clientResponse) {
        super(clientRequest, clientResponse);
    }

    @Override
    public void dispatch(ServletContext target, HttpServletRequest request, HttpServletResponse response,
            final Callable callable) throws Exception {
        ServletContainer container = ServletContainerFactory.getServletContainer();
        container.include(target, request, response, new RequestDispatchCallback() {
            @Override
            public Object doCallback(ServletContext dispatchedServletContext, HttpServletRequest dispatchedRequest,
                    HttpServletResponse dispatchedResponse, Object handback) throws ServletException, IOException {
                callable.call(dispatchedServletContext, dispatchedRequest, dispatchedResponse);

                // We don't use return value anymore
                return null;
            }
        }, null);
    }
}
