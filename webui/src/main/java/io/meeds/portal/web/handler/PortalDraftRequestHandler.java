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
package io.meeds.portal.web.handler;

import java.util.Locale;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestHandler;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.controller.QualifiedName;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class PortalDraftRequestHandler extends PortalRequestHandler {

  public static final String        HANDLER_NAME    = "portal-draft";

  protected static final Log        LOG             = ExoLogger.getLogger(PortalDraftRequestHandler.class);

  public static final QualifiedName REQUEST_PATH    = QualifiedName.create("gtn", "path");

  public static final QualifiedName REQUEST_SITE_ID = QualifiedName.create("gtn", "siteId");

  public static final QualifiedName LANG            = QualifiedName.create("gtn", "lang");

  private LayoutService             layoutService;

  private UserACL                   userAcl;

  @Override
  public String getHandlerName() {
    return HANDLER_NAME;
  }

  @Override
  public boolean execute(ControllerContext controllerContext) throws Exception {
    HttpServletRequest request = controllerContext.getRequest();
    HttpServletResponse response = controllerContext.getResponse();

    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Expires", "0");

    Locale requestLocale = getRequestLocale(controllerContext);
    String requestSiteId = controllerContext.getParameter(REQUEST_SITE_ID);
    String requestPath = controllerContext.getParameter(REQUEST_PATH);

    if (requestSiteId == null) {
      response.sendRedirect(request.getContextPath());
      return true;
    }

    PortalConfig portalConfig = getLayoutService().getPortalConfig(Long.parseLong(requestSiteId));
    if (portalConfig == null
        || (!PortalConfig.DRAFT.equals(portalConfig.getType()))
        || !getUserAcl().hasEditPermission(portalConfig, ConversationState.getCurrent().getIdentity())) {
      response.sendRedirect(request.getContextPath());
      return true;
    }

    return processRequest(controllerContext,
                          portalConfig.getType(),
                          portalConfig.getName(),
                          requestPath,
                          requestLocale);
  }

  public LayoutService getLayoutService() {
    if (layoutService == null) {
      layoutService = ExoContainerContext.getService(LayoutService.class);
    }
    return layoutService;
  }

  public UserACL getUserAcl() {
    if (userAcl == null) {
      userAcl = ExoContainerContext.getService(UserACL.class);
    }
    return userAcl;
  }
}
