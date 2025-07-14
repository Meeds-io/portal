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
package org.exoplatform.commons.file.resource;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;

import java.util.HashMap;
import java.util.Map;

/**
 * This Component Plugin allows you to dynamically define a resource provider.
 */
public class ResourceProviderPlugin extends BaseComponentPlugin {
    private Map<String, String> resourceProviderData = new HashMap<>();
    private static final String STORAGE_TYPE = "storageType";
    private static final String CLASS_NAME = "class";

    public ResourceProviderPlugin(InitParams initParams) {
        if (initParams != null) {
            ValueParam typeParam  = initParams.getValueParam(STORAGE_TYPE);
            ValueParam classParam  = initParams.getValueParam(CLASS_NAME);
            if (typeParam != null && classParam != null && !typeParam.getValue().isEmpty() && !classParam.getValue().isEmpty())
                resourceProviderData.put(typeParam.getValue(), classParam.getValue());
        }
    }

    public Map<String, String> getResourceProviderData() {
        return resourceProviderData;
    }

    public void setResourceProviderData(Map<String, String> resourceProviderData) {
        this.resourceProviderData = resourceProviderData;
    }
}
