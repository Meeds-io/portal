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
package org.exoplatform.upload;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

/**
 * This listener for the purpose of cleaning up temporary files that are uploaded to the server but not removed by specific
 * actions from user
 *
 * The listener is triggered when a session is destroyed
 *
 */
public class UploadSessionListener extends Listener<PortalContainer, HttpSessionEvent> {
    @Override
    public void onEvent(Event<PortalContainer, HttpSessionEvent> event) throws Exception {
        PortalContainer container = event.getSource();
        HttpSession session = event.getData().getSession();

        UploadService uploadService = container.getComponentInstanceOfType(UploadService.class);
        uploadService.cleanUp(session);
    }
}
