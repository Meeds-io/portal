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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;

public class MockServletRequest implements HttpServletRequest {

  private Map         parameters;

  private Map         attributes;

  private HttpSession session;

  private Locale      locale;

  private boolean     secure;

  private Map         headers;

  private String      enc           = "ISO-8859-1";

  private String      pathInfo_;

  private String      requestURI_;

  private URL         url;

  private String      method        = "GET";

  private String      contextPath   = "";

  private String      remoteUser    = "REMOTE USER FROM MOCK";

  private boolean     authenticated = true;

  public MockServletRequest(HttpSession session, Locale locale) {

    this(session, locale, false);

  }

  public MockServletRequest(HttpSession session, Locale locale, boolean secure) {

    this(session, null, null, locale, secure);
  }

  public MockServletRequest(HttpSession session, URL url, String contextPath, Locale locale, boolean secure) {
    this.session = session;
    this.locale = locale;
    headers = new HashMap();
    Collection headersMultiple = new ArrayList();
    headersMultiple.add("header-value3-1");
    headersMultiple.add("header-value3-2");
    headersMultiple.add("header-value3-3");
    headers.put("header1", "header-value1");
    headers.put("header2", "header-value2");
    headers.put("header3", headersMultiple);
    parameters = new HashMap();
    attributes = new HashMap();
    this.secure = secure;
    if (url == null) {
      try {
        this.url = new URL("http://exoplatform.com:80/context/path?q=v");
        this.contextPath = "/context";
      } catch (MalformedURLException e) {
      }
    } else {
      this.url = url;
      this.contextPath = contextPath;
    }
  }

  public void reset() {
    parameters = new HashMap();
    attributes = new HashMap();
  }

  public String getAuthType() {
    return authenticated ? DIGEST_AUTH : null;
  }

  public Cookie[] getCookies() {
    return new Cookie[0];
  }

  public long getDateHeader(String s) {
    return 0;
  }

  public String getHeader(String s) {
    return (String) headers.get(s);
  }

  public Enumeration getHeaders(String s) {
    if (headers.get(s) instanceof Collection)
      return Collections.enumeration((Collection) headers.get(s));
    else {
      Vector v = new Vector();
      v.add(headers.get(s));
      return v.elements();
    }
  }

  public Enumeration getHeaderNames() {
    return Collections.enumeration(headers.keySet());
  }

  public int getIntHeader(String s) {
    return 0;
  }

  public String getMethod() {

    return method;
  }

  public String getPathInfo() {
    return pathInfo_;
  }

  public void setPathInfo(String s) {
    pathInfo_ = s;
  }

  public String getPathTranslated() {
    return null;
  }

  public String getContextPath() {
    return contextPath;
  }

  public String getQueryString() {
    return url.getQuery();
  }

  public String getRemoteUser() {
    return authenticated ? remoteUser : null;
  }

  public void setRemoteUser(String remoteUser) {
    this.remoteUser = remoteUser;
  }

  public boolean isUserInRole(String s) {
    if ("auth-user".equals(s))
      return true;
    else
      return false;
  }

  public Principal getUserPrincipal() {
    return authenticated ? new MockPrincipal() : null;
  }

  public String getRequestedSessionId() {
    return null;
  }

  public String getRequestURI() {
    if (this.requestURI_ == null)
      return url.getPath();
    else
      return requestURI_;
  }

  public void setRequestURI(String s) {
    this.requestURI_ = s;
  }

  public StringBuffer getRequestURL() {
    return new StringBuffer(url.toString());
  }

  public String getServletPath() {
    return url.getPath();
  }

  public HttpSession getSession(boolean b) {
    return session;
  }

  public HttpSession getSession() {
    return session;
  }

  public boolean isRequestedSessionIdValid() {
    return false;
  }

  public boolean isRequestedSessionIdFromCookie() {
    return false;
  }

  public boolean isRequestedSessionIdFromURL() {
    return false;
  }

  public boolean isRequestedSessionIdFromUrl() {
    return false;
  }

  public Object getAttribute(String s) {
    return attributes.get(s);
  }

  public Enumeration getAttributeNames() {
    return new Vector(attributes.keySet()).elements();
  }

  public String getCharacterEncoding() {
    return enc;
  }

  public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
    enc = s;
  }

  public int getContentLength() {
    return 0;
  }

  public String getContentType() {
    return null;
  }

  public ServletInputStream getInputStream() throws IOException {
    return null;
  }

  public String getParameter(String s) {
    return (String) parameters.get(s);
  }

  public void setParameter(String s, Object value) {
    parameters.put(s, value);
  }

  public Enumeration getParameterNames() {
    return new Vector(parameters.keySet()).elements();
  }

  public String[] getParameterValues(String s) {

    ArrayList<String> arr = new ArrayList<String>();
    Iterator it = parameters.keySet().iterator();
    while (it.hasNext()) {

      String pname = (String) it.next();
      if (pname.equals(s))
        arr.add((String) parameters.get(s));
    }
    return arr.toArray(new String[arr.size()]);

  }

  public Map getParameterMap() {
    return parameters;
  }

  public String getProtocol() {
    return null;
  }

  public String getScheme() {
    return url.getProtocol();
  }

  public String getServerName() {
    return url.getHost();
  }

  public int getServerPort() {
    return url.getPort();
  }

  public BufferedReader getReader() throws IOException {
    return null;
  }

  public String getRemoteAddr() {
    return null;
  }

  public String getRemoteHost() {
    return null;
  }

  public void setAttribute(String s, Object o) {
    attributes.put(s, o);
  }

  public void removeAttribute(String s) {
    attributes.remove(s);
  }

  public Locale getLocale() {
    return locale;
  }

  public Enumeration getLocales() {
    Vector v = new Vector();
    v.add(locale);
    return v.elements();
  }

  public boolean isSecure() {
    return secure;
  }

  public RequestDispatcher getRequestDispatcher(String s) {

    return null;
  }

  public String getRealPath(String s) {
    return null;
  }

  // servlet 2.4 method
  public int getLocalPort() {
    return 0;
  }

  public String getLocalAddr() {
    return "127.0.0.1";
  }

  public String getLocalName() {
    return "localhost";
  }

  public int getRemotePort() {
    return 0;
  }

  // servlet 3.0.1 api

  public ServletContext getServletContext() {
    return new MockServletContext();
  }

  public AsyncContext startAsync() throws IllegalStateException {
    throw new IllegalStateException("Asynchronous request is not supported");
  }

  public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
    throw new IllegalStateException("Asynchronous request is not supported");
  }

  public boolean isAsyncStarted() {
    return false;
  }

  public boolean isAsyncSupported() {
    return false;
  }

  public AsyncContext getAsyncContext() {
    throw new IllegalStateException("Request is not in asynchronous mode");
  }

  public DispatcherType getDispatcherType() {
    return DispatcherType.REQUEST;
  }

  public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
    return authenticated;
  }

  public void login(String username, String password) throws ServletException {
    if (authenticated) {
      throw new ServletException("Non-null caller identity had already been established");
    }

    authenticated = true;
  }

  public void logout() throws ServletException {
    authenticated = false;
  }

  public Collection<Part> getParts() throws IOException, ServletException {
    throw new ServletException("Request is not of type multipart/form-data");
  }

  public Part getPart(String name) throws IOException, ServletException {
    throw new ServletException("Request is not of type multipart/form-data");
  }

  @Override
  public long getContentLengthLong() {
    return 0;
  }

  @Override
  public String getRequestId() {
    return null;
  }

  @Override
  public String getProtocolRequestId() {
    return null;
  }

  @Override
  public ServletConnection getServletConnection() {
    return null;
  }

  @Override
  public String changeSessionId() {
    return null;
  }

  @Override
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
    return null;
  }
}
