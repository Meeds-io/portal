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
package org.exoplatform.portal.resource.compressor;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;

@Managed
@ManagedDescription("A resource compressor plugin")
@NameTemplate({ @Property(key = "service", value = "resource"), @Property(key = "compressor", value = "{Name}") })
public abstract class BaseResourceCompressorPlugin extends BaseComponentPlugin implements ResourceCompressorPlugin {

    private int priority;

    protected final Log log = ExoLogger.getLogger(getClass());

    public BaseResourceCompressorPlugin(InitParams params) {
        ValueParam priorityParam = params.getValueParam("plugin.priority");
        try {
            this.priority = Integer.parseInt(priorityParam.getValue());
        } catch (NumberFormatException NBFEx) {
            this.priority = -1;
        }
    }

    @Managed
    @ManagedDescription("The plugin priority")
    public int getPriority() {
        return priority;
    }

    @Managed
    @ManagedDescription("The plugin type")
    public String getType() {
        return getResourceType().name();
    }
}
