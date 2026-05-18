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

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.web.AbstractFilter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.StateKey;
import org.exoplatform.services.security.web.HttpSessionStateKey;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * A Web filter to retrieve currently authenticated user Identity to current Web
 * context session.<br>
 * Note: added to be included in class packages scan for Spring
 */
public class PortalIdentityFilter extends AbstractFilter {

  private static final Log            LOG = ExoLogger.getLogger(PortalIdentityFilter.class);

  private static ConversationRegistry conversationRegistry;

  private static IdentityRegistry     identityRegistry;

  private static Authenticator        authenticator;

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                            ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    ExoContainer container = getContainer();

    ExoContainerContext.setCurrentContainer(container);
    try {
      ConversationState.setCurrent(getCurrentState(container, httpRequest));
      chain.doFilter(request, response);
    } finally {
      ConversationState.setCurrent(null);
      ExoContainerContext.setCurrentContainer(null);
    }
  }

  private ConversationState getCurrentState(ExoContainer container, // NOSONAR
                                            HttpServletRequest httpRequest) {
    String userId = httpRequest.getRemoteUser();
    if (StringUtils.isBlank(userId)) {
      return new ConversationState(new Identity(IdentityConstants.ANONIM));
    } else {
      ConversationState state = null;
      HttpSession httpSession = httpRequest.getSession(false);
      if (httpSession != null) {
        StateKey stateKey = new HttpSessionStateKey(httpSession);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Looking for Conversation State " + httpSession.getId());
        }
        state = getConversationRegistry(container).getState(stateKey);
        if (state != null && !userId.equals(state.getIdentity().getUserId())) {
          state = null;
          conversationRegistry.unregister(stateKey, false);
          LOG.warn("The current conversation state with the session ID {} does not belong to the user {}. Identity registries has been cleared.",
                   httpSession.getId(),
                   userId);
        }
      }

      if (state == null) {
        Identity identity = getIdentity(container, userId);
        if (identity != null) {
          state = new ConversationState(identity);
          getConversationRegistry(container).register(new HttpSessionStateKey(httpRequest.getSession()),
                                                      state);
        }
      }
      return state;
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
