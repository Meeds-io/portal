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
package org.exoplatform.web;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.RenderContext;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.controller.router.URIWriter;

public class ControllerContext {

    /** . */
    private final HttpServletRequest request;

    /** . */
    private final HttpServletResponse response;

    /** . */
    private final WebAppController controller;

    /** . */
    private final Router router;

    /** . */
    private final Map<QualifiedName, String> parameters;

    /** . */
    private final String contextName;

    /** . */
    private final RenderContext renderContext;

    public ControllerContext(WebAppController controller, Router router, HttpServletRequest request,
            HttpServletResponse response, Map<QualifiedName, String> parameters) {
        this.controller = controller;
        this.request = request;
        this.response = response;
        this.parameters = parameters;
        this.contextName = request.getContextPath().substring(1);
        this.router = router;
        this.renderContext = new RenderContext();
    }

    public WebAppController getController() {
        return controller;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public String getParameter(QualifiedName parameter) {
        return parameters.get(parameter);
    }

    public void renderURL(Map<QualifiedName, String> parameters, URIWriter uriWriter) throws IOException {
        renderContext.reset(parameters);
        uriWriter.append('/');
        uriWriter.appendSegment(contextName);
        router.render(renderContext, uriWriter);
    }
}
