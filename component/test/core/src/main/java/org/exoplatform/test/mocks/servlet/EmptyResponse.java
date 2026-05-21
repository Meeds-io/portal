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
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public class EmptyResponse implements HttpServletResponse {

  public void addCookie(Cookie cookie) {
  }

  public boolean containsHeader(String s) {
    return false;
  }

  public String encodeURL(String s) {
    return null;
  }

  public String encodeRedirectURL(String s) {
    return null;
  }

  public String encodeUrl(String s) {
    return null;
  }

  public String encodeRedirectUrl(String s) {
    return null;
  }

  public void sendError(int i, String s) throws IOException {
  }

  public void sendError(int i) throws IOException {
  }

  public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException {
  }

  public void setDateHeader(String s, long l) {
  }

  public void addDateHeader(String s, long l) {
  }

  public void setHeader(String s, String s1) {
  }

  public void addHeader(String s, String s1) {
  }

  public void setIntHeader(String s, int i) {
  }

  public void addIntHeader(String s, int i) {
  }

  public void setStatus(int i) {
  }

  public void setStatus(int i, String s) {
  }

  public String getCharacterEncoding() {
    return null;
  }

  public ServletOutputStream getOutputStream() throws IOException {
    return null;
  }

  public PrintWriter getWriter() throws IOException {
    return null;
  }

  public void setContentLength(int i) {
  }

  public void setContentType(String s) {
  }

  public void setBufferSize(int i) {
  }

  public int getBufferSize() {
    return 0;
  }

  public void flushBuffer() throws IOException {
  }

  public void resetBuffer() {
  }

  public boolean isCommitted() {
    return false;
  }

  public void reset() {
  }

  public void setLocale(Locale locale) {
  }

  public Locale getLocale() {
    return null;
  }

  public void setCharacterEncoding(String charset) {
  }

  public String getContentType() {
    return null;
  }

  // Servlet 3.0.1 API

  public int getStatus() {
    return 0;
  }

  public String getHeader(String name) {
    return null;
  }

  public Collection<String> getHeaders(String name) {
    return null;
  }

  public Collection<String> getHeaderNames() {
    return null;
  }

  @Override
  public void setContentLengthLong(long len) {

  }
}
