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
package org.exoplatform.web.portal;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.web.application.RequestContext;

/**
 * This listener attempts to find the {@link UserPortal} associated with the current request and invalidate it when the
 * navigation service emits an event for a navigation modification.
 *
 */
public class RefreshCurrentUserPortal extends Listener<NavigationService, SiteKey> {

    @Override
    public void onEvent(Event<NavigationService, SiteKey> event) throws Exception {
        RequestContext ctx = RequestContext.getCurrentInstance();
        if (ctx != null) {
            UserPortal userPortal = ctx.getUserPortal();
            if (userPortal != null) {
                userPortal.refresh();
            }
        }
    }
}
