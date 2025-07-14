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

/**
 * A filter for nodes, filtering occurs after node retrieval and is applied once nodes are loaded.
 *
 */
public interface NodeFilter {

    /**
     * Returns a value indicating whether or not the node should be visible or not in the user interface.
     *
     * @param depth the node depth
     * @param id the node id
     * @param name the node name
     * @param state the node state
     * @return the accept value
     */
    boolean accept(int depth, String id, String name, NodeState state);

    /**
     * @return true if the node should be hidden only when not having access to
     *         page and not having children, else false
     */
    boolean isHideOnlyPage();

}
