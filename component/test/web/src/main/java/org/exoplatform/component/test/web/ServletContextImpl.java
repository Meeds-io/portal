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

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;

import jakarta.servlet.*;
import jakarta.servlet.ServletRegistration.Dynamic;
import jakarta.servlet.descriptor.JspConfigDescriptor;

import org.gatein.common.NotYetImplemented;

/**
 * URL based implementation. Disclaimer : does not work with jar URLs.
 *
 */
public class ServletContextImpl implements ServletContext {

    /** . */
    private final URL base;

    /** The path of this context. */
    private final String path;

    /** . */
    private final String name;

    public ServletContextImpl(File root, String path, String name) throws MalformedURLException {
        this(root.toURI().toURL(), path, name);
    }

    public ServletContextImpl(Class<?> root, String path, String name) {
        this(root.getResource(""), path, name);
    }

    public ServletContextImpl(URL base, String path, String name) {
        if (base == null) {
            throw new NullPointerException("No null base URL accepted");
        }
        if (path == null) {
            throw new NullPointerException("No null path accepted");
        }
        if (name == null) {
            throw new NullPointerException("No null name accepted");
        }

        //
        this.base = base;
        this.path = path;
        this.name = name;
    }

    public String getContextPath() {
        return path;
    }

    public URL getResource(String path) throws MalformedURLException {
        if (path.length() == 0 || path.charAt(0) != '/') {
            throw new MalformedURLException(path + "does not start with /");
        }
        try {
            URI relative = new URI(path.substring(1));
            URI uri = base.toURI().resolve(relative);
            if (new File(uri).exists()) {
                return uri.toURL();
            } else {
                return null;
            }
        } catch (Exception e) {
            MalformedURLException ex = new MalformedURLException("Cannot build URL");
            ex.initCause(e);
            throw ex;
        }
    }

    public InputStream getResourceAsStream(String path) {
        try {
            URL url = getResource(path);
            if (url != null) {
                return url.openStream();
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    public String getServletContextName() {
        return name;
    }

    public ServletContext getContext(String uripath) {
        throw new NotYetImplemented();
    }

    public int getMajorVersion() {
        throw new NotYetImplemented();
    }

    public int getMinorVersion() {
        throw new NotYetImplemented();
    }

    public String getMimeType(String file) {
        throw new NotYetImplemented();
    }

    public Set getResourcePaths(String path) {
        throw new NotYetImplemented();
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        throw new NotYetImplemented();
    }

    public RequestDispatcher getNamedDispatcher(String name) {
        throw new NotYetImplemented();
    }

    public Servlet getServlet(String name) throws ServletException {
        throw new NotYetImplemented();
    }

    public Enumeration getServlets() {
        throw new NotYetImplemented();
    }

    public Enumeration getServletNames() {
        throw new NotYetImplemented();
    }

    public void log(String msg) {
    }

    public void log(Exception exception, String msg) {
    }

    public void log(String message, Throwable throwable) {
    }

    public String getRealPath(String path) {
        throw new NotYetImplemented();
    }

    public String getServerInfo() {
        throw new NotYetImplemented();
    }

    public String getInitParameter(String name) {
        throw new NotYetImplemented();
    }

    public Enumeration getInitParameterNames() {
        throw new NotYetImplemented();
    }

    public Object getAttribute(String name) {
        throw new NotYetImplemented();
    }

    public Enumeration getAttributeNames() {
        throw new NotYetImplemented();
    }

    public void setAttribute(String name, Object object) {
        throw new NotYetImplemented();
    }

    public void removeAttribute(String name) {
        throw new NotYetImplemented();
    }

    @Override
    public int getEffectiveMajorVersion() {
      throw new NotYetImplemented();
    }

    @Override
    public int getEffectiveMinorVersion() {
      throw new NotYetImplemented();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
      throw new NotYetImplemented();
    }

    @Override
    public Dynamic addServlet(String servletName, String className) {
      throw new NotYetImplemented();
    }

    @Override
    public Dynamic addServlet(String servletName, Servlet servlet) {
      throw new NotYetImplemented();
    }

    @Override
    public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
      throw new NotYetImplemented();
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
      throw new NotYetImplemented();
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
      throw new NotYetImplemented();
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
      throw new NotYetImplemented();
    }

    @Override
    public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
      throw new NotYetImplemented();
    }

    @Override
    public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
      throw new NotYetImplemented();
    }

    @Override
    public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
      throw new NotYetImplemented();
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
      throw new NotYetImplemented();
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
      throw new NotYetImplemented();
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
      throw new NotYetImplemented();
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
      throw new NotYetImplemented();
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
      throw new NotYetImplemented();
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
      throw new NotYetImplemented();
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
      throw new NotYetImplemented();
    }

    @Override
    public void addListener(String className) {
      throw new NotYetImplemented();
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
      throw new NotYetImplemented();
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
      throw new NotYetImplemented();
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
      throw new NotYetImplemented();
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
      throw new NotYetImplemented();
    }

    @Override
    public ClassLoader getClassLoader() {
      throw new NotYetImplemented();
    }

    @Override
    public void declareRoles(String... roleNames) {
      throw new NotYetImplemented();
    }

    @Override
    public String getVirtualServerName() {
      throw new NotYetImplemented();
    }

    @Override
    public Dynamic addJspFile(String servletName, String jspFile) {
      throw new NotYetImplemented();
    }

    @Override
    public int getSessionTimeout() {
      throw new NotYetImplemented();
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {
      throw new NotYetImplemented();
    }

    @Override
    public String getRequestCharacterEncoding() {
      throw new NotYetImplemented();
    }

    @Override
    public void setRequestCharacterEncoding(String encoding) {
      throw new NotYetImplemented();
    }

    @Override
    public String getResponseCharacterEncoding() {
      throw new NotYetImplemented();
    }

    @Override
    public void setResponseCharacterEncoding(String encoding) {
      throw new NotYetImplemented();
    }
    
}
