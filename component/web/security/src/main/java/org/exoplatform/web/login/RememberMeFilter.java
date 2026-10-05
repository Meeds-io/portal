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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.web.security.security.CookieTokenService;

/**
 * The remember me filter performs an authentication using the
 * {@link ServletContainer} when the current request is a GET request, the user
 * is not authenticated and there is a remember me token cookie in the request.
 *
 */
public class RememberMeFilter extends AbstractFilter {

  private static final Log LOG = ExoLogger.getLogger(RememberMeFilter.class);

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
            // The token was validated just above, so a failure here is not a
            // statement about it: a rejected JAAS login arrives as a WCI
            // AuthenticationException (a RuntimeException wrapping the bare
            // ServletException Tomcat throws once JAASRealm has swallowed the
            // LoginException) and is indistinguishable from an outage — except
            // for the one permanent, expected rejection the shipped
            // gatein-domain chain marks on the request: a disabled user
            // (FilterDisabledLoginModule sets DISABLED_USER_NAME before
            // throwing, as LoginHandler already relies on). Clear the cookie
            // for that case only, so the browser stops re-running the chain on
            // every request for the cookie's whole lifetime, and keep it
            // otherwise: this cookie is written at path "/" and shared with
            // every Spring WAR, so deleting it on a transient IDM or database
            // failure cost users their remember-me everywhere at once.
            if (request.getAttribute(FilterDisabledLoginModule.DISABLED_USER_NAME) != null) {
              LoginUtils.clearRememberMeCookie(request, response);
            }
            // Debug, not warn: Tomcat's JAASRealm has already logged the
            // underlying LoginException with its stack at warn
            // (tomcat-catalina 11.0.24 JAASRealm:441-442 — no module of this
            // chain throws a FailedLoginException, so nothing lands in its
            // debug branch), and this filter is mapped on every portal
            // request, so a second warn here would only duplicate it.
            LOG.debug("Cannot log user {} in with its rememberme token", username, e);
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
