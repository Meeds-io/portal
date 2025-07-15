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
package org.exoplatform.component.test.web;

import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpSession;

import org.gatein.common.NotYetImplemented;
import org.gatein.wci.WebApp;

public class WebAppImpl implements WebApp {

    /** . */
    private final ServletContext servletContext;

    /** . */
    private final ClassLoader loader;

    public WebAppImpl(ServletContext servletContext, ClassLoader loader) throws NullPointerException {
        if (servletContext == null) {
            throw new NullPointerException("No null servlet context allowed");
        }
        if (loader == null) {
            throw new NullPointerException("No null loader accepted");
        }

        //
        this.servletContext = servletContext;
        this.loader = loader;
    }

    public WebAppImpl(Class<?> base, String path, String name) {
        this.servletContext = new ServletContextImpl(base, path, name);
        this.loader = base.getClassLoader();
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public ClassLoader getClassLoader() {
        return loader;
    }

    public String getContextPath() {
        return servletContext.getContextPath();
    }

    public boolean importFile(String parentDirRelativePath, String name, InputStream source, boolean overwrite)
            throws IOException {
        throw new NotYetImplemented();
    }

    public boolean invalidateSession(String sessId) {
        throw new NotYetImplemented();
    }

    @Override
    public HttpSession getHttpSession(String s) {
        throw new NotYetImplemented();
    }

    @Override
    public void fireRequestDestroyed(ServletRequest servletRequest) {
        //Do Nothing
    }

    @Override
    public void fireRequestInitialized(ServletRequest servletRequest) {
        //Do Nothing
    }
}
