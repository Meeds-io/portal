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
package org.exoplatform.web.login;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.gatein.wci.ServletContainer;
import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.security.Credentials;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.web.AbstractFilter;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.web.security.security.CookieTokenService;

/**
 * The remember me filter performs an authentication using the
 * {@link ServletContainer} when the current request is a GET request, the user
 * is not authenticated and there is a remember me token cookie in the request.
 *
 */
public class RememberMeFilter extends AbstractFilter {

  private List<String> ignoredPaths = null;

  @Override
  protected void afterInit(FilterConfig config) throws ServletException {
    String ignoredPathsParameter = config.getInitParameter("ignoredPaths");
    if (StringUtils.isBlank(ignoredPathsParameter)) {
      this.ignoredPaths = Collections.emptyList();
    } else {
      this.ignoredPaths = Arrays.asList(StringUtils.split(ignoredPathsParameter, ","));
    }
  }

  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
    doFilter((HttpServletRequest) req, (HttpServletResponse) resp, chain);
  }

  private void doFilter(HttpServletRequest request, // NOSONAR
                        HttpServletResponse response,
                        FilterChain chain) throws IOException, ServletException {
    ExoContainerContext.setCurrentContainer(getContainer());
    String servletPath = request.getServletPath();
    if (request.getRemoteUser() == null
        && this.ignoredPaths.stream().noneMatch(ignoredPath -> StringUtils.startsWith(servletPath, ignoredPath))) {
      String token = LoginUtils.getRememberMeTokenCookie(request);
      if (token != null) {
        ExoContainer container = getContainer();
        CookieTokenService tokenservice = container.getComponentInstanceOfType(CookieTokenService.class);
        String username = tokenservice.validateToken(token, false);
        if (username != null) {
          Credentials credentials = new Credentials(username,"");
          ServletContainer servletContainer = ServletContainerFactory.getServletContainer();
          try {
            servletContainer.login(request, response, credentials);
          } catch (Exception e) {
            // Clear token cookie if we did not authenticate
            if (request.getRemoteUser() == null) {
              Cookie cookie = new Cookie(LoginUtils.COOKIE_NAME, "");
              cookie.setPath("/");
              cookie.setMaxAge(0);
              cookie.setHttpOnly(true);
              cookie.setSecure(request.isSecure());
              response.addCookie(cookie);
            }
          }
        }
      }
    }

    // Continue
    chain.doFilter(request, response);
  }

  public void begin(OrganizationService orgService) {
    if (orgService instanceof ComponentRequestLifecycle componentRequestLifcycle) {
      RequestLifeCycle.begin(componentRequestLifcycle);
    }
  }

  public void end(OrganizationService orgService) {
    if (orgService instanceof ComponentRequestLifecycle) {
      RequestLifeCycle.end();
    }
  }
}
