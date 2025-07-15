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
package org.exoplatform.web.application.javascript;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.gatein.portal.controller.resource.ResourceId;
import org.gatein.portal.controller.resource.script.FetchMode;

public class ScriptResourceDescriptor {

    /** . */
    final ResourceId id;

    /** . */
    final String group;

    /** . */
    final String alias;

    /** . */
    final List<Locale> supportedLocales;

    /** . */
    final List<Javascript> modules;

    /** . */
    final List<DependencyDescriptor> dependencies;

    /** . */
    FetchMode fetchMode;

    public ScriptResourceDescriptor(ResourceId id, FetchMode fetchMode) {
        this(id, fetchMode, null, null);
    }

    public ScriptResourceDescriptor(ResourceId id, FetchMode fetchMode, String alias, String group) {
        this.id = id;
        this.modules = new ArrayList<Javascript>();
        this.dependencies = new ArrayList<DependencyDescriptor>();
        this.supportedLocales = new ArrayList<Locale>();
        this.fetchMode = fetchMode;
        this.alias = alias;
        this.group = group;
    }

    public ResourceId getId() {
        return id;
    }

    public List<Locale> getSupportedLocales() {
        return supportedLocales;
    }

    public List<Javascript> getModules() {
        return modules;
    }

    public List<DependencyDescriptor> getDependencies() {
        return dependencies;
    }

    public String getAlias() {
        return alias;
    }

    public String getGroup() {
        return group;
    }
}
