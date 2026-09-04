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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public class MockHttpServletResponse implements HttpServletResponse {

  private PrintWriter                  writer;

  private ByteArrayOutputStream        stream;

  private ByteArrayServletOutputStream output;

  public MockHttpServletResponse() {
    stream = new ByteArrayOutputStream();
    writer = new PrintWriter(stream);
    output = new ByteArrayServletOutputStream(stream);
  }

  public String getOutputContent() {
    return new String(stream.toByteArray());
  }

  public void flushBuffer() throws IOException {
    // TODO Auto-generated method stub

  }

  public int getBufferSize() {
    // TODO Auto-generated method stub
    return 0;
  }

  public ServletOutputStream getOutputStream() throws IOException {
    return this.output;
  }

  public PrintWriter getWriter() throws IOException {
    return this.writer;
  }

  public boolean isCommitted() {
    return false;
  }

  public void reset() {
    // TODO Auto-generated method stub

  }

  public void resetBuffer() {
    // TODO Auto-generated method stub

  }

  public void addCookie(Cookie arg0) {
    // TODO Auto-generated method stub

  }

  public void addDateHeader(String arg0, long arg1) {
    // TODO Auto-generated method stub

  }

  public void addHeader(String arg0, String arg1) {
    // TODO Auto-generated method stub

  }

  public void addIntHeader(String arg0, int arg1) {
    // TODO Auto-generated method stub

  }

  public boolean containsHeader(String arg0) {
    // TODO Auto-generated method stub
    return false;
  }

  public String encodeRedirectURL(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  public String encodeRedirectUrl(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  public String encodeURL(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  public String encodeUrl(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  public void sendError(int arg0) throws IOException {
    // TODO Auto-generated method stub

  }

  public void sendError(int arg0, String arg1) throws IOException {
    // TODO Auto-generated method stub

  }

  @Override
  public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException {
    // TODO Auto-generated method stub
  }

  public void setDateHeader(String arg0, long arg1) {
    // TODO Auto-generated method stub

  }

  public void setHeader(String arg0, String arg1) {
    // TODO Auto-generated method stub

  }

  public void setIntHeader(String arg0, int arg1) {
    // TODO Auto-generated method stub

  }

  public void setStatus(int arg0) {
    // TODO Auto-generated method stub

  }

  public void setStatus(int arg0, String arg1) {
    // TODO Auto-generated method stub

  }

  public String getCharacterEncoding() {
    // TODO Auto-generated method stub
    return null;
  }

  public Locale getLocale() {
    // TODO Auto-generated method stub
    return null;
  }

  public void setBufferSize(int arg0) {
    // TODO Auto-generated method stub

  }

  public void setContentLength(int arg0) {
    // TODO Auto-generated method stub

  }

  public void setContentType(String arg0) {
    // TODO Auto-generated method stub

  }

  public void setLocale(Locale arg0) {
    // TODO Auto-generated method stub

  }

  private static class ByteArrayServletOutputStream extends ServletOutputStream {
    ByteArrayOutputStream baos;

    public ByteArrayServletOutputStream(ByteArrayOutputStream baos) {
      this.baos = baos;
    }

    public void write(int i) throws IOException {
      baos.write(i);
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {

    }
  }

  public String getContentType() {
    // TODO Auto-generated method stub
    return null;
  }

  public void setCharacterEncoding(String arg0) {
    // TODO Auto-generated method stub
  }

  // servlet 3.0.1 API

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
