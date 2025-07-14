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
package org.exoplatform.portal.mop;

/**
 * Group various event types.
 *
 */
public final class EventType {

    private EventType() {
    }

    /** . */
    public static final String NAVIGATION_CREATED = "org.exoplatform.portal.mop.navigation.navigation_created";

    public static final String NAVIGATION_DESTROY = "org.exoplatform.portal.mop.navigation.navigation_destroy";

    /** . */
    public static final String NAVIGATION_DESTROYED = "org.exoplatform.portal.mop.navigation.navigation_destroyed";

    /** . */
    public static final String NAVIGATION_UPDATED = "org.exoplatform.portal.mop.navigation.navigation_updated";

    public static final String NAVIGATION_NODE_ADD = "org.exoplatform.portal.mop.navigation.node_add";

    public static final String NAVIGATION_NODE_CREATE = "org.exoplatform.portal.mop.navigation.node_create";

    public static final String NAVIGATION_NODE_REMOVE = "org.exoplatform.portal.mop.navigation.node_remove";

    public static final String NAVIGATION_NODE_DESTROY = "org.exoplatform.portal.mop.navigation.node_destroy";

    public static final String NAVIGATION_NODE_RENAME = "org.exoplatform.portal.mop.navigation.node_rename";

    public static final String NAVIGATION_NODE_UPDATE = "org.exoplatform.portal.mop.navigation.node_update";

    public static final String NAVIGATION_NODE_MOVE = "org.exoplatform.portal.mop.navigation.node_move";

    /** . */
    public static final String PAGE_CREATED = "org.exoplatform.portal.mop.page.page_created";

    /** . */
    public static final String PAGE_DESTROYED = "org.exoplatform.portal.mop.page.page_destroyed";

    /** . */
    public static final String PAGE_UPDATED = "org.exoplatform.portal.mop.page.page_updated";

}
