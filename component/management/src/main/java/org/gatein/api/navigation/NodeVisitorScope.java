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
package org.gatein.api.navigation;

import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.navigation.VisitMode;
import org.gatein.api.Util;
import org.gatein.api.internal.Parameters;
import org.gatein.api.page.PageId;

public class NodeVisitorScope implements Scope {
    private final NodeVisitor visitor;

    public NodeVisitorScope(NodeVisitor visitor) {
        this.visitor = Parameters.requireNonNull(visitor, "visitor");
    }

    @Override
    public Visitor get() {
        return new NodeVisitorAdapter(visitor);
    }

    private static class NodeVisitorAdapter implements Visitor {
        private final NodeVisitor visitor;

        private NodePath nodePath;

        public NodeVisitorAdapter(NodeVisitor visitor) {
            this.visitor = visitor;
        }

        @Override
        public VisitMode enter(int depth, String id, String name, NodeState state) {
            nodePath = depth == 0 ? NodePath.root() : nodePath.append(name);
            String nodeName = depth == 0 ? null : name;
            NodeDetails details = depth == 0 ? null : new NodeDetails(state, nodePath);
            return visitor.visit(depth, nodeName, details) ? VisitMode.ALL_CHILDREN : VisitMode.NO_CHILDREN;
        }

        @Override
        public void leave(int depth, String id, String name, NodeState state) {
            nodePath = nodePath.parent();
        }
    }

    private static class NodeDetails implements NodeVisitor.NodeDetails {
        private NodeState nodeState;
        private NodePath nodePath;

        public NodeDetails(NodeState nodeState, NodePath nodePath) {
            this.nodeState = nodeState;
            this.nodePath = nodePath;
        }

        @Override
        public Visibility getVisibility() {
            return ObjectFactory.createVisibility(nodeState);
        }

        @Override
        public String getIconName() {
            return nodeState.getIcon();
        }

        @Override
        public PageId getPageId() {
            return Util.from(nodeState.getPageRef());
        }

        @Override
        public NodePath getNodePath() {
            return nodePath;
        }
    }
}
