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
package org.exoplatform.portal.mop.navigation;

import org.exoplatform.portal.mop.SiteKey;

public class NavigationContext {

    /** . */
    final SiteKey key;

    /** . */
    NavigationState state;

    /** . */
    NavigationData data;

    public NavigationContext(NavigationData data) {
        if (data == null) {
            throw new NullPointerException();
        }

        //
        this.key = data.key;
        this.data = data;
    }

    public NavigationContext(SiteKey key, NavigationState state) {
        if (key == null) {
            throw new NullPointerException();
        }
        if (state == null) {
            throw new NullPointerException();
        }

        //
        this.key = key;
        this.state = state;
    }

    /**
     * Returns the navigation key.
     *
     * @return the navigation key
     */
    public SiteKey getKey() {
      return data == null ? this.key : data.key;
    }

    /**
     * Returns the navigation state.
     *
     * @return the navigation state
     */
    public NavigationState getState() {
      return state != null || data == null ? this.state : data.state;
    }

    public NavigationData getData() {
        return this.data;
    }

    public void setData(NavigationData data) {
      this.data = data;
    }

    /**
     * Updates the navigation state the behavior is not the same wether or not the navigation is persistent:
     * When the navigation is persistent, any state is allowed:
     * <ul>
     * <li>A non null state overrides the current persistent state.</li>
     * <li>The null state means to reset the state to the persistent state.</li>
     * <li>When the navigation is transient, only a non null state is allowed as it will be used for creation purpose.</li>
     * </ul>
     *
     * @param state the new state
     * @throws IllegalStateException when the state is cleared and the navigation is not persistent
     */
    public void setState(NavigationState state) throws IllegalStateException {
        this.state = state;
    }
}
