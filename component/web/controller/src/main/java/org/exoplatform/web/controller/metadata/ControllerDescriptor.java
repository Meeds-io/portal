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
package org.exoplatform.web.controller.metadata;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.web.controller.router.RegexFactory;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.controller.router.RouterConfigException;

/**
 * Describe a controller.
 *
 */
public class ControllerDescriptor {

    /** . */
    private final List<RouteDescriptor> routes;

    /** . */
    private char separatorEscape;

    public ControllerDescriptor() {
        this.routes = new ArrayList<RouteDescriptor>();
        this.separatorEscape = '_';
    }

    public ControllerDescriptor add(RouteDescriptor... routes) {
        if (routes == null) {
            throw new NullPointerException();
        }

        //
        for (RouteDescriptor route : routes) {
            if (route == null) {
                throw new IllegalArgumentException();
            }

            //
            this.routes.add(route);
        }

        //
        return this;
    }

    public ControllerDescriptor separatorEscapedBy(char c) {
        this.separatorEscape = c;
        return this;
    }

    public char getSeparatorEscape() {
        return separatorEscape;
    }

    public void setSeparatorEscape(char separatorEscape) {
        this.separatorEscape = separatorEscape;
    }

    public List<RouteDescriptor> getRoutes() {
        return routes;
    }

    public Router build() throws RouterConfigException {
        return new Router(this);
    }

    public Router build(RegexFactory regexFactory) throws RouterConfigException {
        return new Router(this, regexFactory);
    }
}
