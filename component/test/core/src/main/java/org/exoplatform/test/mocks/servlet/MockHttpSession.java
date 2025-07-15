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
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;

public class MockHttpSession implements HttpSession {

  private Map map = new HashMap();

  public long getCreationTime() {
    return 0;
  }

  public String getId() {
    return "MockSessionId";
  }

  public long getLastAccessedTime() {
    return 0;
  }

  public ServletContext getServletContext() {
    return null;
  }

  public void setMaxInactiveInterval(int i) {
  }

  public int getMaxInactiveInterval() {
    return 0;
  }

  public Object getAttribute(String s) {
    return map.get(s);
  }

  public Object getValue(String s) {
    return null;
  }

  public Enumeration getAttributeNames() {
    return new Vector(map.keySet()).elements();
  }

  public String[] getValueNames() {
    return new String[0];
  }

  public void setAttribute(String s, Object o) {
    map.put(s, o);
  }

  public void putValue(String s, Object o) {
  }

  public void removeAttribute(String s) {
    map.remove(s);
  }

  public void removeValue(String s) {
  }

  public void invalidate() {
  }

  public boolean isNew() {
    return false;
  }

}
