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
package io.meeds.spring.web.security;

import java.io.IOException;

import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.security.Credentials;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.web.AbstractFilter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.web.HttpSessionStateKey;
import org.exoplatform.web.login.LoginUtils;
import org.exoplatform.web.security.security.CookieTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * A Web filter to authenticate user Identity using 'rememberme' cookie if
 * present.<br>
 * Note: added to be included in class packages scan for Spring
 */
public class PortalRememberMeFilter extends AbstractFilter {

  private static final Log            LOG = ExoLogger.getLogger(PortalRememberMeFilter.class);

  private static ConversationRegistry conversationRegistry;

  private static IdentityRegistry     identityRegistry;

  private static Authenticator        authenticator;

  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) resp;
    if (request.getRemoteUser() == null) {
      login(request, response);
    }
    chain.doFilter(req, resp);
  }

  private void login(HttpServletRequest request, HttpServletResponse response) {
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainerIfPresent();
    ExoContainer container = getContainer();
    ExoContainerContext.setCurrentContainer(container);
    try {
      String username = getRememberMeTokenUser(request);
      if (username != null) {
        try {
          login(request, response, new Credentials(username, ""));
          if (request.getRemoteUser() != null) {
            Identity identity = getIdentity(container, username);
            if (identity != null) {
              ConversationState state = new ConversationState(identity);
              getConversationRegistry(container).register(new HttpSessionStateKey(request.getSession()),
                                                          state);
              ConversationState.setCurrent(state);
            }
          }
        } catch (Exception e) {
          clearInvalidToken(request, response);
          LOG.warn("Error while logging in user {} using rememberme token, invalidate token", username, e);
        }
      }
    } finally {
      ExoContainerContext.setCurrentContainer(currentContainer);
    }
  }

  private void login(HttpServletRequest request, HttpServletResponse response, Credentials credentials) throws ServletException,
                                                                                                        IOException {
    HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
      @Override
      public String getContextPath() {
        return "/portal";
      }

      @Override
      public String getRequestURI() {
        return "/portal/login";
      }
    };
    HttpServletResponse wrappedResponse = new HttpServletResponseWrapper(response) {
      @Override
      public void sendRedirect(String location) throws IOException {
        // Nothing
      }

      @Override
      public void setStatus(int sc) {
        // Nothing
      }
    };
    ServletContainerFactory.getServletContainer()
                           .login(wrappedRequest, wrappedResponse, credentials);
  }

  private String getRememberMeTokenUser(HttpServletRequest request) {
    String token = LoginUtils.getRememberMeTokenCookie(request);
    if (token != null) {
      ExoContainer container = getContainer();
      CookieTokenService tokenservice = container.getComponentInstanceOfType(CookieTokenService.class);
      return tokenservice.validateToken(token, false);
    }
    return null;
  }

  private void clearInvalidToken(HttpServletRequest request, HttpServletResponse response) {
    if (request.getRemoteUser() == null) {
      Cookie cookie = new Cookie(LoginUtils.COOKIE_NAME, "");
      cookie.setPath("/");
      cookie.setMaxAge(0);
      cookie.setHttpOnly(true);
      cookie.setSecure(request.isSecure());
      response.addCookie(cookie);
    }
  }

  private Identity getIdentity(ExoContainer container, String userId) {
    Identity identity = getIdentityRegistry(container).getIdentity(userId);
    if (identity == null) {
      try {
        identity = getAuthenticator(container).createIdentity(userId);
        identityRegistry.register(identity);
      } catch (Exception e) {
        LOG.warn("Unable restore identity of user {}", userId, e);
      }
    }
    return identity;
  }

  private static IdentityRegistry getIdentityRegistry(ExoContainer container) {
    if (identityRegistry == null) {
      identityRegistry = container.getComponentInstanceOfType(IdentityRegistry.class);
    }
    return identityRegistry;
  }

  private static ConversationRegistry getConversationRegistry(ExoContainer container) {
    if (conversationRegistry == null) {
      conversationRegistry = container.getComponentInstanceOfType(ConversationRegistry.class);
    }
    return conversationRegistry;
  }

  private static Authenticator getAuthenticator(ExoContainer container) {
    if (authenticator == null) {
      authenticator = container.getComponentInstanceOfType(Authenticator.class);
    }
    return authenticator;
  }

}
