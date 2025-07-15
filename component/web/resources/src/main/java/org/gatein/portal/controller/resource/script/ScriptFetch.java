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
package org.gatein.portal.controller.resource.script;

import java.util.LinkedHashSet;


class ScriptFetch {

    /** . */
    final ScriptResource resource;

    /** . */
    FetchMode mode;

    /** . */
    final LinkedHashSet<ScriptFetch> dependencies;

    /** . */
    final LinkedHashSet<ScriptFetch> dependsOnMe;

    ScriptFetch(ScriptResource resource, FetchMode mode) {
        this.resource = resource;
        this.mode = mode;
        this.dependencies = new LinkedHashSet<ScriptFetch>();
        this.dependsOnMe = new LinkedHashSet<ScriptFetch>();
    }

    void upgrade(FetchMode mode) {
        this.mode = mode;

        //
        for (ScriptFetch dependency : dependencies) {
            dependency.upgrade(mode);
        }
    }
}
