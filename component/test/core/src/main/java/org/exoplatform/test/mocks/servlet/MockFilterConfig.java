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

import java.util.Enumeration;

import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;

public class MockFilterConfig implements FilterConfig {

  private ServletContext servletContext;

  public MockFilterConfig(ServletContext servletContext) {
    super();
    this.servletContext = servletContext;
  }

  public String getFilterName() {
    return "mock-filter";
  }

  public String getInitParameter(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  public Enumeration getInitParameterNames() {
    // TODO Auto-generated method stub
    return null;
  }

  public ServletContext getServletContext() {
    return servletContext;
  }

}
