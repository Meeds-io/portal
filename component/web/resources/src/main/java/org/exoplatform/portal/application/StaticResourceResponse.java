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

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.gatein.portal.controller.resource.ScriptContent;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes a static resource with the cache headers its content allows. It holds
 * no state, unlike {@link ResourceRequestFilter}, whose static fields read the
 * portal container's properties when that class is first used.
 */
final class StaticResourceResponse {

  private static final String CACHE_CONTROL_HEADER_NAME = "Cache-Control";

  private StaticResourceResponse() {
  }

  /**
   * @param script the served script
   * @param requestHash the hash the request's URL names
   * @param developing whether the server runs in developer mode
   * @return true when the request names the served content's hash, outside
   *         developer mode: a URL whose hash is not the content's, such as a
   *         page rendered before a reload, never gets a cached copy of another
   *         content
   */
  static boolean isPubliclyCacheable(ScriptContent script, String requestHash, boolean developing) {
    return !developing && String.valueOf(script.getHash()).equals(requestHash);
  }

  /**
   * @param httpResponse the response to write
   * @param bytes the content
   * @param mimeType the content's type
   * @param publiclyCacheable whether the content may be cached for maxAge;
   *          else any cache asks the server again before reusing it
   * @param maxAge how long, in seconds, a publicly cacheable content may be
   *          cached
   * @throws IOException when the response can't be written
   */
  static void write(HttpServletResponse httpResponse,
                    byte[] bytes,
                    String mimeType,
                    boolean publiclyCacheable,
                    long maxAge) throws IOException {
    httpResponse.setContentType(mimeType);
    httpResponse.setContentLength(bytes.length);
    if (publiclyCacheable) {
      httpResponse.setHeader(CACHE_CONTROL_HEADER_NAME, "public, max-age=" + maxAge);
      httpResponse.setDateHeader(ResourceRequestFilter.LAST_MODIFIED, System.currentTimeMillis());
      httpResponse.setDateHeader(ResourceRequestFilter.EXPIRES, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(maxAge));
    } else {
      httpResponse.setHeader(CACHE_CONTROL_HEADER_NAME, "no-cache");
    }
    PrintWriter writer = httpResponse.getWriter();
    IOUtils.write(bytes, writer, StandardCharsets.UTF_8);
    writer.close();
  }

}
