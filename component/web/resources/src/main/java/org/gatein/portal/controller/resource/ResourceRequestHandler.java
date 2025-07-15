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
package org.gatein.portal.controller.resource;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.WebAppEvent;
import org.gatein.wci.WebAppListener;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.ResourceRequestFilter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.controller.QualifiedName;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @deprecated Replaced by {@link ResourceRequestFilter} which will handle files
 *             using webapp context based URL instead of a centralized endpoint
 *             for all skins to define inside the monolith
 */
@Deprecated
public class ResourceRequestHandler extends WebRequestHandler implements WebAppListener {

  private static final Log          LOG               = ExoLogger.getLogger(ResourceRequestHandler.class);

  public static final String        HANDLER_NAME      = "script";

  public static final String        IF_MODIFIED_SINCE = "If-Modified-Since";

  public static final String        LAST_MODIFIED     = "Last-Modified";

  /** . */
  public static final QualifiedName VERSION_QN        = QualifiedName.create("gtn", "version");

  /** . */
  public static final QualifiedName RESOURCE_QN       = QualifiedName.create("gtn", "resource");

  /** . */
  public static final QualifiedName SCOPE_QN          = QualifiedName.create("gtn", "scope");

  /** . */
  public static final QualifiedName COMPRESS_QN       = QualifiedName.create("gtn", "compress");

  /** . */
  public static final QualifiedName ORIENTATION_QN    = QualifiedName.create("gtn", "orientation");

  /** . */
  public static final QualifiedName LANG_QN           = QualifiedName.create("gtn", "lang");

  public static long                MAX_AGE           = ResourceRequestFilter.maxAge;  // NOSONAR can't be final

  public static String              VERSION           = ResourceRequestFilter.version; // NOSONAR can't be final

  @Override
  public String getHandlerName() {
    return HANDLER_NAME;
  }

  @Override
  public boolean execute(ControllerContext context) throws Exception {
    HttpServletResponse response = context.getResponse();
    String resourceParam = context.getParameter(RESOURCE_QN);
    String scopeParam = context.getParameter(SCOPE_QN);

    if (scopeParam != null && resourceParam != null) {
      String compressParam = context.getParameter(COMPRESS_QN);
      ScriptContent script = ExoContainerContext.getService(JavascriptConfigService.class)
                                                .getScriptContent(ResourceScope.valueOf(scopeParam),
                                                                  resourceParam,
                                                                  "true".equals(compressParam));

      if (script == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
      } else {
        byte[] bytes = script.getContentAsBytes();
        response.setContentType("text/javascript");
        response.setContentLength(bytes.length);
        response.setHeader("Cache-Control", "public, " + MAX_AGE);
        response.setDateHeader(ResourceRequestFilter.LAST_MODIFIED, System.currentTimeMillis());
        response.setDateHeader(ResourceRequestFilter.EXPIRES,
                               System.currentTimeMillis() + ResourceRequestHandler.MAX_AGE * 1000);
        PrintWriter writer = response.getWriter();
        IOUtils.write(bytes, writer, StandardCharsets.UTF_8);
        writer.close();
      }
    } else {
      String msg = "Missing scope or resource param";
      LOG.error(msg);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
    }
    return true;
  }

  @Override
  protected boolean getRequiresLifeCycle() {
    return false;
  }

  @Override
  public void onInit(WebAppController controller, ServletConfig sConfig) throws Exception {
    super.onInit(controller, sConfig);
    LOG.debug("Registering ResourceRequestHandler for servlet container events");
    ServletContainerFactory.getServletContainer().addWebAppListener(this);
  }

  @Override
  public void onDestroy(WebAppController controller) {
    super.onDestroy(controller);
    LOG.debug("Unregistering ResourceRequestHandler for servlet container events");
    ServletContainerFactory.getServletContainer().removeWebAppListener(this);
  }

  @Override
  public void onEvent(WebAppEvent event) {
    // Nothing to do
  }

}
