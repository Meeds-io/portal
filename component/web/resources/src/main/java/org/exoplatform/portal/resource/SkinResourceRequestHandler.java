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
package org.exoplatform.portal.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Date;

import jakarta.servlet.http.HttpServletResponse;

import org.exoplatform.commons.utils.BinaryOutput;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.ResourceRequestFilter;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.gatein.portal.controller.resource.ResourceRequestHandler;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @deprecated Replaced by {@link ResourceRequestFilter} which will handle files
 *             using webapp context based URL instead of a centralized endpoint
 *             for all skins to define inside the monolith
 */
@Deprecated(forRemoval = true, since = "7.0")
public class SkinResourceRequestHandler extends WebRequestHandler {

    /** . */
    private final Log          log               = ExoLogger.getLogger(getClass());

    /** . */
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    /** . */
    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";

    /** . */
    public static final String LAST_MODIFIED = "Last-Modified";

    /** . */
    public static final String   EXPIRES           = "Expires";

    /** . */
    private final SkinService skinService;

    public SkinResourceRequestHandler(SkinService skinService) {
        this.skinService = skinService;
    }

    @Override
    public String getHandlerName() {
        return "skin";
    }

    @Override
    public boolean execute(final ControllerContext context) throws Exception {
        String compressParam = context.getParameter(ResourceRequestHandler.COMPRESS_QN);
        boolean compress = "min".equals(compressParam);

        //
        final HttpServletResponse response = context.getResponse();

        // Check if cached resource has not been modifed, return 304 code
        String ifModifiedSinceString = context.getRequest().getHeader(IF_MODIFIED_SINCE);
        long ifModifiedSince = ifModifiedSinceString == null ? 0 : new Date(ifModifiedSinceString).getTime();
        if (ifModifiedSince > 0) {
            response.setHeader("Cache-Control", null);
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return true;
        } else {
            //
            response.setContentType("text/css");

            final OutputStream out = response.getOutputStream();
            final BinaryOutput output = new BinaryOutput() {
                public Charset getCharset() {
                    return UTF_8;
                }

                public void write(byte b) throws IOException {
                    out.write(b);
                }

                public void write(byte[] bytes) throws IOException {
                    out.write(bytes);
                }

                public void write(byte[] bytes, int off, int len) throws IOException {
                    out.write(bytes, off, len);
                }
            };

            //
            final String resource = "/" + context.getParameter(ResourceRequestHandler.RESOURCE_QN) + ".css";
            try {
                ResourceRenderer renderer = new SkinResourceRenderer(response, context, output);
                if (skinService.renderCSS(context, renderer, compress)) {
                    // Ok we did the job
                    return true;
                } else {
                    log.warn("CSS " + resource + " not found");
                    return false;
                }
            } catch (Exception e) {
                if (e instanceof SocketException) {
                    // Should we print something/somewhere exception message
                } else {
                    // We want to ignore the ClientAbortException since this is caused by the users
                    // browser closing the connection and is not something we should be logging.
                    if(e.getClass().toString().contains("ClientAbortException")) {
                        return true;
                    }
                    log.error("Could not render css " + resource, e);
                }
                return false;
            }
        }
    }

    /**
     * If cached resource has not changed since date in http header (If_Modified_Since), return true otherwise return false.
     */
    private boolean isNotModified(long ifModifedSince, long lastModified) {
      return Math.abs(ifModifedSince - lastModified) < 1000;
    }

    @Override
    protected boolean getRequiresLifeCycle() {
        return false;
    }

    public static class SkinResourceRenderer implements ResourceRenderer {

      private final BinaryOutput      output;

      private final HttpServletResponse response;

      private final ControllerContext context;

      public SkinResourceRenderer(HttpServletResponse response, ControllerContext context, BinaryOutput output) {
        this.response = response;
        this.output = output;
        this.context = context;
      }

      public BinaryOutput getOutput() {
        return output;
      }

      public void setExpiration(long seconds) {
        if (seconds > 0) {
          response.setHeader("Cache-Control", "public, " + seconds);
          response.setDateHeader(EXPIRES, (System.currentTimeMillis() + seconds * 1000L));
          response.setHeader("Etag", "W/\"" + ResourceRequestHandler.VERSION.hashCode() + "\"");
        } else {
          response.setHeader("Cache-Control", "no-cache");
        }

        long lastModified = ExoContainerContext.getService(SkinService.class).getLastModified(context);
        response.setDateHeader(LAST_MODIFIED, lastModified);
      }
    }
}
