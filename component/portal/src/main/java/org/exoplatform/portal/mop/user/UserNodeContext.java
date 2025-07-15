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

import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;

public class UserNodeContext implements NodeModel<UserNode> {

    /** The related navigation. */
    final UserNavigation navigation;

    /** . */
    final UserNodeFilterConfig filterConfig;

    /** . */
    private UserNodeFilter filter;

    public UserNodeContext(UserNavigation navigation, UserNodeFilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        this.navigation = navigation;
    }

    public NodeContext<UserNode> getContext(UserNode node) {
        return node.context;
    }

    public UserNode create(NodeContext<UserNode> context) {
        return new UserNode(this, context);
    }

    void filter(UserNode userNode) {
        if (filterConfig != null) {
            if (filter == null) {
                filter = new UserNodeFilter(navigation.portal, filterConfig);
            }
            userNode.context.filter(filter);
        }
    }
}
