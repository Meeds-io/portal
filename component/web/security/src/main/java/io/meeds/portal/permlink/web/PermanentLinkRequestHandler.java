/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.portal.permlink.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.security.sso.SSOHelper;

import io.meeds.portal.permlink.service.PermanentLinkService;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServletResponse;

public class PermanentLinkRequestHandler extends WebRequestHandler {

  protected static final Log        LOG          = ExoLogger.getLogger(PermanentLinkRequestHandler.class);

  public static final QualifiedName REQUEST_PATH = QualifiedName.create("path");

  private PermanentLinkService      permanentLinkService;

  private UserPortalConfigService   userPortalConfigService;

  @Override
  public String getHandlerName() {
    return "permanent-link";
  }

  @Override
  protected boolean getRequiresLifeCycle() {
    return true;
  }

  @Override
  public void onInit(WebAppController controller, ServletConfig sConfig) throws Exception {
    permanentLinkService = ExoContainerContext.getService(PermanentLinkService.class);
    userPortalConfigService = ExoContainerContext.getService(UserPortalConfigService.class);
  }

  @Override
  public boolean execute(ControllerContext context) throws Exception {
    String requestPath = context.getParameter(REQUEST_PATH);
    Identity currentIdentity = getCurrentIdentity();
    try {
      String directAccessUrl = permanentLinkService.getDirectAccessUrl(requestPath, currentIdentity);
      HttpServletResponse res = context.getResponse();
      // Use HTTP 302 response instead of 301 to not
      // allow to cache the redirect directive in routers
      // and user browsers to secure effective URL
      res.sendRedirect(directAccessUrl);
    } catch (IllegalAccessException e) {
      if (currentIdentity == null || StringUtils.equals(IdentityConstants.ANONIM, currentIdentity.getUserId())) {
        String loginPath = getAuthenticationUrl(context.getRequest().getRequestURI());
        context.getResponse().sendRedirect(loginPath);
      } else {
        LOG.warn("Error while handling permanent link '{}' redirecting to not found page", requestPath, e);
        context.getResponse()
               .sendRedirect(String.format("/portal/%s/page-not-found",
                                           userPortalConfigService.getMetaPortal()));
      }
    } catch (ObjectNotFoundException e) {
      LOG.warn("Error while handling permanent link '{}'", requestPath, e);
      context.getResponse()
             .sendRedirect(String.format("/portal/%s/page-not-found",
                                         userPortalConfigService.getMetaPortal()));
    }
    return true;
  }

  public String getAuthenticationUrl(String permanentLink) {
    StringBuilder loginPath = new StringBuilder();

    // . Check SSO Enable
    SSOHelper ssoHelper = ExoContainerContext.getService(SSOHelper.class);
    if (ssoHelper != null && ssoHelper.isSSOEnabled() && ssoHelper.skipJSPRedirection()) {
      loginPath.append("/portal").append(ssoHelper.getSSORedirectURLSuffix());
    } else {
      loginPath.append("/portal/login");
    }
    loginPath.append("?initialURI=").append(URLEncoder.encode(permanentLink, StandardCharsets.UTF_8));
    return loginPath.toString();
  }

  private Identity getCurrentIdentity() {
    ConversationState conversationState = ConversationState.getCurrent();
    return conversationState == null ? null : conversationState.getIdentity();
  }

}
