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

import org.exoplatform.commons.utils.ExceptionUtil;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.portletcontainer.PortletContainerException;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Apr 29, 2009
 */
public class LogPortletExceptionListener extends BaseComponentPlugin implements PortletExceptionListener {

    protected static Log log = ExoLogger.getLogger("portal:UIPortletLifecycle");

    public void handle(PortletContainerException e) {
        log.error("The portlet could not be loaded. Check if properly deployed.", ExceptionUtil.getRootCause(e));

    }
}
