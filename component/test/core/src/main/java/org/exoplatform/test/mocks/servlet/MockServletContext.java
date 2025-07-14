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
package org.exoplatform.test.mocks.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletRegistration.Dynamic;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;

public class MockServletContext implements ServletContext {

  private String       name_;

  private HashMap      initParams_;

  private HashMap      attributes_;

  private String       contextPath_;

  private StringBuffer logBuffer = new StringBuffer();

  public MockServletContext() {
    this("portlet_app_1");
  }

  public MockServletContext(String name) {
    this(name, "/" + name);
  }

  public MockServletContext(String name, String path) {
    this.name_ = name;
    this.contextPath_ = path;
    this.initParams_ = new HashMap<String, String>();
    this.attributes_ = new HashMap<String, Object>();
    this.attributes_.put("jakarta.servlet.context.tempdir", path);
  }

  public void setName(String name) {
    name_ = name;
  }

  public String getLogBuffer() {
    try {
      return logBuffer.toString();
    } finally {
      logBuffer = new StringBuffer();
    }
  }

  public ServletContext getContext(String s) {
    return null;
  }

  public int getMajorVersion() {
    return 3;
  }

  public int getMinorVersion() {
    return 0;
  }

  public String getMimeType(String s) {
    return "text/html";
  }

  public Set getResourcePaths(String s) {
    Set set = new HashSet();
    set.add("/test1");
    set.add("/WEB-INF");
    set.add("/test2");
    return set;
  }

  public URL getResource(String s) throws MalformedURLException {
    String path = "file:" + contextPath_ + s;
    URL url = new URL(path);
    return url;
  }

  public InputStream getResourceAsStream(String s) {
    try {
      return getResource(s).openStream();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public RequestDispatcher getRequestDispatcher(String s) {
    return null;
  }

  public RequestDispatcher getNamedDispatcher(String s) {
    return null;
  }

  public Servlet getServlet(String s) throws ServletException {
    return null;
  }

  public Enumeration getServlets() {
    return null;
  }

  public Enumeration getServletNames() {
    return null;
  }

  public void log(String s) {
    logBuffer.append(s);
  }

  public void log(Exception e, String s) {
    logBuffer.append(s + e.getMessage());
  }

  public void log(String s, Throwable throwable) {
    logBuffer.append(s + throwable.getMessage());
  }

  public void setContextPath(String s) {
    contextPath_ = s;
  }

  public String getRealPath(String s) {
    return contextPath_ + s;
  }

  public String getServerInfo() {
    return null;
  }

  public boolean setInitParameter(String name, String value) {
    initParams_.put(name, value);
    return true;
  }

  public String getInitParameter(String name) {
    return (String) initParams_.get(name);
  }

  public Enumeration getInitParameterNames() {
    Vector keys = new Vector(initParams_.keySet());
    return keys.elements();
  }

  public Object getAttribute(String name) {
    return attributes_.get(name);
  }

  public Enumeration getAttributeNames() {
    Vector keys = new Vector(attributes_.keySet());
    return keys.elements();
  }

  public void setAttribute(String name, Object value) {
    attributes_.put(name, value);
  }

  public void removeAttribute(String name) {
    attributes_.remove(name);
  }

  public String getServletContextName() {
    return name_;
  }

  public String getContextPath() {
    return contextPath_;
  }

  // servelt 3.0.1 api

  public int getEffectiveMajorVersion() {
    return 3;
  }

  public int getEffectiveMinorVersion() {
    return 0;
  }

  public Dynamic addServlet(String servletName, String className) {
    return null;
  }

  public Dynamic addServlet(String servletName, Servlet servlet) {
    return null;
  }

  public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
    return null;
  }

  public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
    return null;
  }

  public ServletRegistration getServletRegistration(String servletName) {
    return null;
  }

  public Map<String, ? extends ServletRegistration> getServletRegistrations() {
    return null;
  }

  public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
    return null;
  }

  public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
    return null;
  }

  public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
    return null;
  }

  public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
    return null;
  }

  public FilterRegistration getFilterRegistration(String filterName) {
    return null;
  }

  public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
    return null;
  }

  public SessionCookieConfig getSessionCookieConfig() {
    return null;
  }

  public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
  }

  public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
    return null;
  }

  public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
    return null;
  }

  public void addListener(String className) {
  }

  public <T extends EventListener> void addListener(T t) {
  }

  public void addListener(Class<? extends EventListener> listenerClass) {
  }

  public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
    return null;
  }

  public JspConfigDescriptor getJspConfigDescriptor() {
    return null;
  }

  public ClassLoader getClassLoader() {
    return null;
  }

  public void declareRoles(String... roleNames) {
  }

  @Override
  public Dynamic addJspFile(String servletName, String jspFile) {
    return null;
  }

  @Override
  public String getVirtualServerName() {
    return null;
  }

  @Override
  public int getSessionTimeout() {
    return 0;
  }

  @Override
  public void setSessionTimeout(int sessionTimeout) {
  }

  @Override
  public String getRequestCharacterEncoding() {
    return null;
  }

  @Override
  public void setRequestCharacterEncoding(String encoding) {

  }

  @Override
  public String getResponseCharacterEncoding() {
    return null;
  }

  @Override
  public void setResponseCharacterEncoding(String encoding) {

  }
}
