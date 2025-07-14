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
package org.exoplatform.web.security.errorlogin;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.exoplatform.container.web.AbstractFilter;
import org.exoplatform.services.security.ConversationState;

/**
 * Filter should be called to detect successful login of user into portal and call InvalidLoginAttemptsService.
 *
 */
public class LoginDetectorFilter extends AbstractFilter {
    private static final String ATTR_LOGIN_DETECTED = "LoginDetectorFilter.loginDetected";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        ConversationState state = ConversationState.getCurrent();

        if (state != null) {
            if (state.getAttribute(ATTR_LOGIN_DETECTED) == null) {
                String clientIPAddress = request.getRemoteAddr();
                String sessionID = httpRequest.getSession().getId();
                String username = httpRequest.getRemoteUser();
                state.setAttribute(ATTR_LOGIN_DETECTED, true);

                InvalidLoginAttemptsService invalidLoginService = (InvalidLoginAttemptsService) getContainer()
                        .getComponentInstanceOfType(InvalidLoginAttemptsService.class);
                invalidLoginService.successfulLoginAttempt(sessionID, username, clientIPAddress);
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

}
