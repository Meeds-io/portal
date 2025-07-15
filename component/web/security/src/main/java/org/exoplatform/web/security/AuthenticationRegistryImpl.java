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
package org.exoplatform.web.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.servlet.http.HttpServletRequest;

import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.gatein.wci.security.Credentials;

/**
 * Temporary registry for hold credentials (and potentially other attributes) during login process to avoid store them in
 * session. Registry is used only during authentication process and attributes of target client are cleared usually after successful
 * authentication,
 *
 */
public class AuthenticationRegistryImpl implements AuthenticationRegistry {
  private static final Log                                 log      = ExoLogger.getLogger(AuthenticationRegistryImpl.class);

    // Key is ID of HTTP Session. Value is map with various attributes of single client (session),
    // which will be used during authentication process.
    private final ConcurrentMap<String, Map<String, Object>> registry = new ConcurrentHashMap<String, Map<String, Object>>();

    @Override
    public Credentials getCredentials(HttpServletRequest request) {
        return (Credentials) getAttributeOfClient(request, Credentials.CREDENTIALS);
    }

    @Override
    public void setCredentials(HttpServletRequest request, Credentials credentials) {
        setAttributeOfClient(request, Credentials.CREDENTIALS, credentials);
    }

    @Override
    public Credentials removeCredentials(HttpServletRequest request) {
        return (Credentials)removeAttributeOfClient(request, Credentials.CREDENTIALS);
    }

    @Override
    public void removeClient(String sessionId) {
        registry.remove(sessionId);

        if (log.isTraceEnabled()) {
            log.trace("Entry cleared for session " + sessionId);
        }
    }

    @Override
    public void setAttributeOfClient(HttpServletRequest request, String attributeName, Object attributeValue) {
        String sessionId = getSessionId(request);

        Map<String, Object> attributesOfClient = getAllAttributesOfClient(sessionId);
        attributesOfClient.put(attributeName, attributeValue);
    }

    @Override
    public Object getAttributeOfClient(HttpServletRequest request, String attributeName) {
        String sessionId = getSessionId(request);
        Map<String, Object> attributesOfClient = registry.get(sessionId);

        if (attributesOfClient == null) {
            return null;
        }

        return attributesOfClient.get(attributeName);
    }

    @Override
    public Object removeAttributeOfClient(HttpServletRequest request, String attributeName) {
        String sessionId = getSessionId(request);

        Map<String, Object> attributesOfClient = getAllAttributesOfClient(sessionId);

        Object removedAttribute = attributesOfClient.remove(attributeName);

        // Clear map if no more attributes are here.
        if (attributesOfClient.size() == 0) {
            removeClient(sessionId);
        }

        return removedAttribute;
    }

    private Map<String, Object> getAllAttributesOfClient(String sessionId) {
        Map<String, Object> attributes = registry.get(sessionId);

        if (attributes == null) {
            attributes = new ConcurrentHashMap<String, Object>();
            registry.putIfAbsent(sessionId, attributes);

            if (log.isTraceEnabled()) {
                log.trace("New entry created in AuthenticationRegistry for session " + sessionId);
            }
        }

        return registry.get(sessionId);
    }

    private String getSessionId(HttpServletRequest req) {
        return req.getSession().getId();
    }
}
