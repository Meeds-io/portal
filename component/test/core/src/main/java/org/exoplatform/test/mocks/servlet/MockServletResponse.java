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
import java.util.Locale;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

public class MockServletResponse extends HttpServletResponseWrapper {

  private PrintWriter                  tmpWriter;

  private ByteArrayOutputStream        output;

  private ByteArrayServletOutputStream servletOutput;

  private Locale                       locale_;

  public MockServletResponse(HttpServletResponse httpServletResponse) {
    super(httpServletResponse);
    output = new ByteArrayOutputStream();
    tmpWriter = new PrintWriter(output);
    servletOutput = new ByteArrayServletOutputStream(output);
  }

  public void finalize() throws Throwable {
    super.finalize();
    servletOutput.close();
    output.close();
    tmpWriter.close();
  }

  public String getPortletContent() {
    String s = output.toString();
    reset();
    return s;
  }

  /*
   * public PrintWriter getWriter() throws IOException { //return
   * servletResponse.getWriter(); return tmpWriter; } public ServletOutputStream
   * getOutputStream() throws IOException { return servletOutput; }
   */

  public byte[] toByteArray() {
    return output.toByteArray();
  }

  public String getOutputContent() {
    return new String(output.toByteArray());
  }

  public void flushBuffer() throws IOException {
    tmpWriter.flush();
    servletOutput.flush();
  }

  public void reset() {
    output.reset();
  }

  public void close() throws IOException {
    tmpWriter.close();
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

  public Locale getLocale() {
    return locale_;
  }

  public void setLocale(java.util.Locale loc) {
    locale_ = loc;
  }

}
