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
package org.exoplatform.portal.pc;

import org.gatein.pc.api.PortletStateType;

public class ExoPortletStateType extends PortletStateType<ExoPortletState> {

    /** . */
    private static final ExoPortletStateType instance = new ExoPortletStateType();

    public static ExoPortletStateType getInstance() {
        return instance;
    }

    public Class<ExoPortletState> getJavaType() {
        return ExoPortletState.class;
    }

    public boolean equals(ExoPortletState state1, ExoPortletState state2) {
        return state1.equals(state2);
    }

    public int hashCode(ExoPortletState state) {
        return state.hashCode();
    }

    public String toString(ExoPortletState state) {
        return state.toString();
    }
}
