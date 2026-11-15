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
package org.exoplatform.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.web.AbstractFilter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;

public class CacheUserProfileFilter extends AbstractFilter {

    /**
     * "subject".
     */
    public static final String USER_PROFILE = "UserProfile";

    /**
     * Logger.
     */
    private static Log log = ExoLogger.getLogger(CacheUserProfileFilter.class);

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException {
        ConversationState state = ConversationState.getCurrent();
        try {
            OrganizationService orgService = getContainer().getComponentInstanceOfType(OrganizationService.class);
            ConversationRegistry conversationRegistry = getContainer().getComponentInstanceOfType(ConversationRegistry.class);
            if (UserSessionGuard.endSessionOfAGoneUser((HttpServletRequest) request,
                                                       (HttpServletResponse) response,
                                                       state,
                                                       orgService,
                                                       conversationRegistry)) {
                return;
            }
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.warn("An error occured while cache user profile", e);
        }

    }

    public void destroy() {
    }

    public void begin(OrganizationService orgService) {
        if (orgService instanceof ComponentRequestLifecycle) {
            RequestLifeCycle.begin((ComponentRequestLifecycle) orgService);
        }
    }

    public void end(OrganizationService orgService) {
        if (orgService instanceof ComponentRequestLifecycle) {
            RequestLifeCycle.end();
        }
    }
}
