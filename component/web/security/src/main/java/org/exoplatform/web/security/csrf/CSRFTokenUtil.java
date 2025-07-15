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
package org.exoplatform.web.security.csrf;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.exoplatform.services.security.ConversationState;
import org.gatein.common.util.UUIDGenerator;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Util class to manage CSRF token
 *
 */
public class CSRFTokenUtil {
    public static final String CSRF_TOKEN = "portal:csrf";

    protected static Log log = ExoLogger.getExoLogger(CSRFTokenUtil.class);

    protected static final UUIDGenerator generator = new UUIDGenerator();


    public static boolean check(HttpServletRequest request) {
        if (request != null) {
            String sessionToken = getToken(request);
            String reqToken = request.getParameter(CSRF_TOKEN);

            return reqToken != null && reqToken.equals(sessionToken);
        } else {
            log.warn("No HttpServletRequest found, can't check CSRF");
            return false;
        }
    }

    public static String getToken(HttpServletRequest request) {

        if (request != null) {
            if (request.getRemoteUser() == null) {
                HttpSession session = request.getSession();
                String token = (String) session.getAttribute(CSRF_TOKEN);
                if (token == null) {
                    token = generator.generateKey();
                    session.setAttribute(CSRF_TOKEN, token);
                }
                return token;
            } else {
                ConversationState conversationState = ConversationState.getCurrent();
                if (conversationState != null && conversationState.getIdentity() != null) {
                    String token = (String) conversationState.getAttribute(CSRF_TOKEN);
                    if (token == null) {
                        token = generator.generateKey();
                        conversationState.setAttribute(CSRF_TOKEN, token);
                    }
                    return token;
                } else {
                    return null;
                }
            }

        } else {
            log.warn("No HttpServletRequest found, can't generate CSRF token");
            return null;
        }

    }

}
