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
package org.exoplatform.portal.mop.user;

import java.util.ResourceBundle;

import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.gatein.common.util.EmptyResourceBundle;

public class UserNavigation {

    /** . */
    final UserPortalImpl portal;

    /** . */
    final NavigationContext navigation;

    /** . */
    private final boolean modifiable;

    public UserNavigation(UserPortalImpl portal, NavigationContext navigation, boolean modifiable) {
        if (navigation == null) {
            throw new NullPointerException();
        }
        if (navigation.getState() == null) {
            throw new IllegalArgumentException("No state for navigation " + navigation.getKey());
        }

        //
        this.portal = portal;
        this.navigation = navigation;
        this.modifiable = modifiable;
    }

    public ResourceBundle getBundle() {
        ResourceBundle bundle = portal.getBundle(this);
        if (bundle == null) {
            bundle = EmptyResourceBundle.INSTANCE;
        }
        return bundle;
    }

    public SiteKey getKey() {
        return navigation.getKey();
    }

    public int getPriority() {
        Integer priority = navigation.getState().getPriority();
        return priority != null ? priority : PageNavigation.UNDEFINED_PRIORITY;
    }

    public boolean isModifiable() {
        return modifiable;
    }

    @Override
    public String toString() {
        return "UserNavigation[key=" + navigation.getKey() + "]";
    }
    
}
