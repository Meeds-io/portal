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
package org.exoplatform.services.resources.impl;

import java.util.List;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValuesParam;

/**
 * This class is used to define new resource bundles
 */
public class BaseResourceBundlePlugin extends BaseComponentPlugin {

    private final InitParams params;

    public BaseResourceBundlePlugin(InitParams params) {
        this.params = params;
    }

    /**
     * @return the list of enclosed "classpath" resource bundles
     */
    @SuppressWarnings("unchecked")
    public List<String> getClasspathResources() {
        ValuesParam vParam = params.getValuesParam("classpath.resources");
        return vParam == null ? null : vParam.getValues();
    }

    /**
     * @return the list of enclosed "portal" resource bundles
     */
    @SuppressWarnings("unchecked")
    public List<String> getPortalResources() {
        ValuesParam vParam = params.getValuesParam("portal.resource.names");
        return vParam == null ? null : vParam.getValues();
    }

    /**
     * @return the list of enclosed "init" resource bundles
     */
    @SuppressWarnings("unchecked")
    public List<String> getInitResources() {
        ValuesParam vParam = params.getValuesParam("init.resources");
        return vParam == null ? null : vParam.getValues();
    }
}
