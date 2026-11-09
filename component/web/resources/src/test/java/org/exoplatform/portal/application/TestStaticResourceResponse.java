/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package org.exoplatform.portal.application;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.gatein.portal.controller.resource.ScriptContent;
import org.mockito.ArgumentCaptor;

import jakarta.servlet.http.HttpServletResponse;
import junit.framework.TestCase;

/**
 * Pins the cache-header decision {@link ResourceRequestFilter} delegates to.
 * The filter's own call is not covered: loading {@link ResourceRequestFilter}
 * outside a container initializes its static assets version to null for the
 * whole test JVM, which breaks the tests that read it afterwards.
 */
public class TestStaticResourceResponse extends TestCase {

  private static final ScriptContent SCRIPT  = new ScriptContent("a;".getBytes(StandardCharsets.UTF_8), -55414319);

  private static final long          MAX_AGE = 3600;

  public void testScriptIsPubliclyCacheableOnlyUnderItsHash() {
    assertTrue(StaticResourceResponse.isPubliclyCacheable(SCRIPT, "-55414319", false));
    // A page rendered before a reload, or by another node
    assertFalse(StaticResourceResponse.isPubliclyCacheable(SCRIPT, "1278397679", false));
    assertFalse(StaticResourceResponse.isPubliclyCacheable(SCRIPT, null, false));
    // Developer mode: every script URL carries hash=0
    assertFalse(StaticResourceResponse.isPubliclyCacheable(SCRIPT, "-55414319", true));
    assertFalse(StaticResourceResponse.isPubliclyCacheable(new ScriptContent(new byte[0], 0), "0", true));
  }

  public void testPubliclyCacheableContentIsCachedForMaxAge() throws Exception {
    HttpServletResponse response = response(new StringWriter());
    long before = System.currentTimeMillis();
    StaticResourceResponse.write(response, "a;".getBytes(StandardCharsets.UTF_8), "text/javascript", true, MAX_AGE);
    long after = System.currentTimeMillis();
    verify(response).setHeader("Cache-Control", "public, " + MAX_AGE);
    verify(response, never()).setHeader("Cache-Control", "no-cache");
    ArgumentCaptor<Long> expires = ArgumentCaptor.forClass(Long.class);
    verify(response).setDateHeader(eq("Expires"), expires.capture());
    assertTrue(expires.getValue() >= before + MAX_AGE * 1000);
    assertTrue(expires.getValue() <= after + MAX_AGE * 1000);
    verify(response).setDateHeader(eq("Last-Modified"), anyLong());
  }

  public void testOtherContentIsRevalidated() throws Exception {
    StringWriter body = new StringWriter();
    HttpServletResponse response = response(body);
    StaticResourceResponse.write(response, "a;".getBytes(StandardCharsets.UTF_8), "text/javascript", false, MAX_AGE);
    verify(response).setHeader("Cache-Control", "no-cache");
    verify(response, never()).setHeader("Cache-Control", "public, " + MAX_AGE);
    verify(response, never()).setDateHeader(anyString(), anyLong());
    verify(response).setContentType("text/javascript");
    verify(response).setContentLength(2);
    assertEquals("a;", body.toString());
  }

  private HttpServletResponse response(StringWriter body) throws Exception {
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getWriter()).thenReturn(new PrintWriter(body));
    return response;
  }

}
