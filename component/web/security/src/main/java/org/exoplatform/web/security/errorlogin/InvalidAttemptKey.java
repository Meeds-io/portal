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

import org.exoplatform.commons.utils.Safe;

class InvalidAttemptKey {
    private final String sessionId;
    private final String username;
    private final String hostname;

    private InvalidAttemptKey(String sessionId, String username, String hostname) {
        this.sessionId = sessionId;
        this.username = username;
        this.hostname = hostname;
    }

    public static InvalidAttemptKey createKey(InvalidLoginPolicy policy, String sessionId, String username, String hostname) {
        switch (policy) {
            case SESSION:
                return new InvalidAttemptKey(sessionId, null, null);

            case SESSION_AND_USER:
                return new InvalidAttemptKey(sessionId, username, null);

            case SERVER:
                return new InvalidAttemptKey(null, null, hostname);

            default:
                throw new IllegalArgumentException("Non-expected value of InvalidLoginPolicy.");
        }
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof InvalidAttemptKey) {
            InvalidAttemptKey that = (InvalidAttemptKey) obj;
            return Safe.equals(sessionId, that.sessionId) && Safe.equals(username, that.username)
                    && Safe.equals(hostname, that.hostname);
        }
        return false;
    }

    public int hashCode() {
        int result = 1234567;
        if (sessionId != null) {
            result = sessionId.hashCode();
        }
        if (username != null) {
            result = result ^ username.hashCode();
        }
        if (hostname != null) {
            result = result ^ hostname.hashCode();
        }
        return result;
    }

}
