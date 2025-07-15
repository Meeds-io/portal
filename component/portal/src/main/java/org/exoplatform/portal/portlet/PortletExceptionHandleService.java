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
package org.exoplatform.portal.portlet;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.portletcontainer.PortletContainerException;

public class PortletExceptionHandleService {

    private List<PortletExceptionListener> listeners;

    public void initListener(ComponentPlugin listener) {
        if (listener instanceof PortletExceptionListener) {
            if (listeners == null)
                listeners = new ArrayList<PortletExceptionListener>();
            listeners.add((PortletExceptionListener) listener);
        }

    }

    public void handle(PortletContainerException ex) {
        for (PortletExceptionListener listener : listeners) {
            listener.handle(ex);
        }
    }
}
