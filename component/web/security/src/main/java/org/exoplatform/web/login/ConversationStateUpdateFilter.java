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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.exoplatform.container.web.AbstractFilter;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.web.security.AuthenticationRegistry;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.gatein.wci.security.Credentials;

/**
 * Filter is used to update {@link ConversationState} with necessary attributes after login of user. It needs to be configured
 * in filter chain after {@link org.exoplatform.services.security.web.SetCurrentIdentityFilter} !!!
 *
 */
public class ConversationStateUpdateFilter extends AbstractFilter {
  private static final Log log = ExoLogger.getLogger(ConversationStateUpdateFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest hreq = (HttpServletRequest) request;
        AuthenticationRegistry authRegistry = (AuthenticationRegistry) getContainer().getComponentInstanceOfType(
                AuthenticationRegistry.class);

        // This should happen during first request of authenticated user. We need to bind credentials to ConversationState
        // and unregister them from authenticationRegistry
        if (hreq.getRemoteUser() != null && authRegistry.getCredentials(hreq) != null) {
            Credentials credentials = authRegistry.removeCredentials(hreq);
            bindCredentialsToConversationState(credentials);
        }

        // Continue with filter chain
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    /**
     * Add credentials to {@link ConversationState}.
     *
     * @param credentials
     */
    protected void bindCredentialsToConversationState(Credentials credentials) {
        ConversationState currentConversationState = ConversationState.getCurrent();
        if (currentConversationState != null && credentials != null) {
            log.debug("Binding credentials to conversationState for user " + credentials.getUsername());
            currentConversationState.setAttribute(Credentials.CREDENTIALS, credentials);
        }
    }
}
