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

import org.exoplatform.commons.utils.Safe;
import org.exoplatform.portal.mop.storage.NavigationStorage;

class NodeDataUpdateAdapter implements TreeUpdateAdapter<NodeData> {

    static NodeDataUpdateAdapter create(NavigationStorage persistence) {
        return new NodeDataUpdateAdapter(persistence);
    }

    /** . */
    private final NavigationStorage persistence;

    NodeDataUpdateAdapter(NavigationStorage persistence) {
        this.persistence = persistence;
    }

    public String getHandle(NodeData node) {
        return node.id;
    }

    public String[] getChildren(NodeData node) {
        return node.children;
    }

    public NodeData getDescendant(NodeData node, String handle) {
        NodeData data = persistence.loadNode(Safe.parseLong(handle));
        NodeData current = data;
        while (current != null) {
            if (node.id.equals(current.id)) {
                return data;
            } else {
                if (current.parentId != null) {
                    current = persistence.loadNode(Safe.parseLong(current.parentId));
                } else {
                    current = null;
                }
            }
        }
        return null;
    }

    public NodeData getData(NodeData node) {
        return node;
    }

    public NodeState getState(NodeData node) {
        return null;
    }

    public String getName(NodeData node) {
        return null;
    }
}
