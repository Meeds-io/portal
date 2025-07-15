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

/**
 * Filter should be called to detect invalid login attempt to portal.
 *
 */
public class ErrorLoginFilter extends AbstractFilter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Get informations about user
        String clientIPAddress = request.getRemoteAddr();
        String sessionID = httpRequest.getSession().getId();
        String username = httpRequest.getParameter("j_username");

        // Call InvalidLoginService, which can perform some actions (aka send mail to portal administrator)
        InvalidLoginAttemptsService invalidLoginService = (InvalidLoginAttemptsService) getContainer()
                .getComponentInstanceOfType(InvalidLoginAttemptsService.class);
        invalidLoginService.badLoginAttempt(sessionID, username, clientIPAddress);

        // Continue with request
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

}
