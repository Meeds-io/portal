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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * A class that wraps HttpServletResponse to commit all changes once the request
 * is handled by all possible handlers
 */
public class PortalHttpServletResponseWrapper extends HttpServletResponseWrapper {

  private String                          redirectLocation;

  private List<Map.Entry<String, String>> addedHeaders  = new ArrayList<>();

  private Map<String, String>             settedHeaders = new HashMap<>();

  private List<Cookie>                    cookies       = new ArrayList<>();

  private boolean                         isError       = false;

  private int                             error         = 0;

  private String                          errorMsg      = null;

  private boolean                         wrapMethods   = false;

  private HttpServletResponse             response;

  public PortalHttpServletResponseWrapper(HttpServletResponse response) {
    super(response);
    this.response = response;
  }

  @Override
  public void sendRedirect(String location) throws IOException {
    if (wrapMethods) {
      this.redirectLocation = location;
    } else {
      super.sendRedirect(location);
    }
  }

  @Override
  public void setHeader(String name, String value) {
    if (wrapMethods) {
      settedHeaders.put(name, value);
    } else {
      super.setHeader(name, value);
    }
  }

  @Override
  public void addCookie(Cookie cookie) {
    if (wrapMethods) {
      cookies.add(cookie);
    } else {
      super.addCookie(cookie);
    }
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
    if (wrapMethods) {
      isError = true;
      this.error = sc;
      this.errorMsg = msg;
    } else {
      super.sendError(sc, msg);
    }
  }

  @Override
  public void sendError(int sc) throws IOException {
    if (wrapMethods) {
      isError = true;
      this.error = sc;
    } else {
      super.sendError(sc);
    }
  }

  public void addHeader(String name, String value, boolean wrapMethods) {
    if (wrapMethods) {
      this.addHeader(name, value);
    } else {
      this.response.addHeader(name, value);
    }
  }

  @Override
  public void addHeader(String name, String value) {
    if (wrapMethods) {
      addedHeaders.add(new AbstractMap.SimpleEntry<>(name, value));
    } else {
      super.addHeader(name, value);
    }
  }

  public boolean isWrapMethods() {
    return wrapMethods;
  }

  public void setWrapMethods(boolean wrapMethods) {
    this.wrapMethods = wrapMethods;
  }

  public void commit() throws IOException {
    if (!wrapMethods) {
      return;
    }
    if (!addedHeaders.isEmpty()) {
      for (Entry<String, String> entry : addedHeaders) {
        response.addHeader(entry.getKey(), entry.getValue());
      }
      addedHeaders.clear();
    }
    if (!settedHeaders.isEmpty()) {
      for (Entry<String, String> entry : settedHeaders.entrySet()) {
        response.setHeader(entry.getKey(), entry.getValue());
      }
      settedHeaders.clear();
    }
    if (!cookies.isEmpty()) {
      for (Cookie cookie : cookies) {
        response.addCookie(cookie);
      }
      cookies.clear();
    }
    if (isError) {
      if (errorMsg == null) {
        response.sendError(error);
      } else {
        response.sendError(error, errorMsg);
      }
      isError = false;
    } else if (redirectLocation != null) {
      response.sendRedirect(redirectLocation);
      redirectLocation = null;
    }
  }
}
