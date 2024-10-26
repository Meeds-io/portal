/*
 * Copyright (C) 2011 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.mop.navigation;

import org.exoplatform.commons.utils.Safe;
import org.exoplatform.portal.mop.storage.NavigationStorage;

/**
* @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
*/
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
