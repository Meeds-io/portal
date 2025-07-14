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

import org.exoplatform.portal.mop.navigation.NodeChangeListener;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeState;

/**
 * This listener takes care of invalidating the cached state of the UserNode listener when it is updated against the navigation
 * service.
 *
 */
class UserNodeListener implements NodeChangeListener<NodeContext<UserNode>> {

    /** . */
    private NodeChangeListener<UserNode> next;

    UserNodeListener(NodeChangeListener<UserNode> next) {
        if (next != null) {
            this.next = next;
        }
    }

    public void onAdd(NodeContext<UserNode> target, NodeContext<UserNode> parent, NodeContext<UserNode> previous) {
        if (next != null) {
            next.onAdd(unwrap(target), unwrap(parent), unwrap(previous));
        }
    }

    public void onCreate(NodeContext<UserNode> target, NodeContext<UserNode> parent, NodeContext<UserNode> previous, String name) {
        if (next != null) {
            next.onCreate(unwrap(target), unwrap(parent), unwrap(previous), name);
        }
    }

    public void onRemove(NodeContext<UserNode> target, NodeContext<UserNode> parent) {
        if (next != null) {
            next.onRemove(unwrap(target), unwrap(parent));
        }
    }

    public void onDestroy(NodeContext<UserNode> target, NodeContext<UserNode> parent) {
        if (next != null) {
            next.onDestroy(unwrap(target), unwrap(parent));
        }
    }

    public void onRename(NodeContext<UserNode> target, NodeContext<UserNode> parent, String name) {
        UserNode unwrappedTarget = unwrap(target);
        unwrappedTarget.uri = null;
        if (next != null) {
            next.onRename(unwrappedTarget, unwrap(parent), name);
        }
    }

    public void onUpdate(NodeContext<UserNode> target, NodeState state) {
        UserNode unwrappedTarget = unwrap(target);
        if (next != null) {
            next.onUpdate(unwrappedTarget, state);
        }
    }

    public void onMove(NodeContext<UserNode> target, NodeContext<UserNode> from, NodeContext<UserNode> to,
            NodeContext<UserNode> previous) {
        UserNode unwrappedTarget = unwrap(target);
        unwrappedTarget.uri = null;
        if (next != null) {
            next.onMove(unwrappedTarget, unwrap(from), unwrap(to), unwrap(previous));
        }
    }

    private UserNode unwrap(NodeContext<UserNode> context) {
        return context != null ? context.getNode() : null;
    }
}
