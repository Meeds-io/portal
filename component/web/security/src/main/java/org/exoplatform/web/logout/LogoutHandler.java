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
package org.exoplatform.web.logout;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.web.login.LoginUtils;
import org.exoplatform.web.login.LogoutControl;
import org.exoplatform.web.security.PortalToken;
import org.exoplatform.web.security.security.AbstractTokenService;
import org.exoplatform.web.security.security.CookieTokenService;
import org.gatein.wci.ServletContainerFactory;

public class LogoutHandler extends WebRequestHandler {

  @Override
  public String getHandlerName() {
    return "logout";
  }

  @Override
  protected boolean getRequiresLifeCycle() {
    return true;
  }

  @Override
  public void onInit(WebAppController controller, ServletConfig servletConfig) {

  }

  @Override
  public boolean execute(ControllerContext context) throws Exception { // NOSONAR
    HttpServletRequest request = context.getRequest();
    HttpServletResponse response = context.getResponse();

    // Delete the token from store
    String token = getTokenCookie(request);
    if (token != null) {
      AbstractTokenService<PortalToken, String> tokenService = AbstractTokenService.getInstance(CookieTokenService.class);
      tokenService.deleteToken(token);
    }

    LogoutControl.wantLogout();

    ServletContainerFactory.getServletContainer().logout(request, response);



    Cookie cookie = new Cookie(LoginUtils.COOKIE_NAME, "");
    cookie.setPath(request.getContextPath());
    cookie.setMaxAge(0);
    response.addCookie(cookie);

    response.sendRedirect("/");



    return true;
  }

private String getTokenCookie(HttpServletRequest req) {
  Cookie[] cookies = req.getCookies();
  if (cookies != null) {
    for (Cookie cookie : cookies) {
      if (LoginUtils.COOKIE_NAME.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
  }
  return null;
}


}
