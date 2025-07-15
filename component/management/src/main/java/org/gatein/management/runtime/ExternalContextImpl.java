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
package org.gatein.management.runtime;

import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.gatein.management.api.ExternalContext;

public class ExternalContextImpl implements ExternalContext {

    @Override
    public String getRemoteUser() {
        Identity identity = getIdentity();
        if (identity != null) {
            String user = identity.getUserId();

            // Returning null implies it's an anonymous user
            if (IdentityConstants.ANONIM.equals(user)) {
                return null;
            }
            return user;
        }

        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        if (role == null) return false;

        Identity identity = getIdentity();
        if (identity != null) {
            for (String r : identity.getRoles()) {
                if (role.equals(r)) return true;
            }
            return false;
        } else {
            // In order for export/import gadget to work (conversation/identity is not set) we must return true here
            return true;
        }
    }

    private static Identity getIdentity() {
        ConversationState conversation = ConversationState.getCurrent();
        if (conversation == null) return null;

        return conversation.getIdentity();
    }
}
