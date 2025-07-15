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
package org.exoplatform.web.handler;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebRequestHandler;

import jakarta.servlet.http.HttpServletResponse;

public class DefaultRequestHandler extends WebRequestHandler {

  private final UserPortalConfigService portalConfigService;

  public DefaultRequestHandler(UserPortalConfigService portalConfigService) {
    this.portalConfigService = portalConfigService;
  }

  @Override
  public String getHandlerName() {
    return "default";
  }

  @Override
  public boolean execute(ControllerContext context) throws Exception {
    String currentUser = context.getRequest().getRemoteUser();
    String defaultUri = portalConfigService.getDefaultPath(currentUser);
    HttpServletResponse resp = context.getResponse();
    if (StringUtils.isBlank(defaultUri)) {
      if (StringUtils.isBlank(currentUser)) {
        String currentPortalContainerName = PortalContainer.getCurrentPortalContainerName();
        resp.sendRedirect("/" + currentPortalContainerName + "/login");
        return true;
      } else {
        resp.sendRedirect("/portal/" + portalConfigService.getMetaPortal() + "/page-not-found");
        return true;
      }
    } else {
      resp.sendRedirect(resp.encodeRedirectURL(defaultUri));
      return true;
    }
  }

  @Override
  protected boolean getRequiresLifeCycle() {
    return true;
  }
}
