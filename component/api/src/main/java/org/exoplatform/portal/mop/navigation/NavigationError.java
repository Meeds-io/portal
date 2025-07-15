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

public enum NavigationError {

    MOVE_CONCURRENTLY_REMOVED_SRC_NODE,

    MOVE_CONCURRENTLY_REMOVED_DST_NODE,

    MOVE_CONCURRENTLY_REMOVED_MOVED_NODE,

    MOVE_CONCURRENTLY_CHANGED_SRC_NODE,

    MOVE_CONCURRENTLY_REMOVED_PREVIOUS_NODE,

    MOVE_CONCURRENTLY_DUPLICATE_NAME,

    ADD_CONCURRENTLY_REMOVED_PARENT_NODE,

    ADD_CONCURRENTLY_ADDED_NODE,

    ADD_CONCURRENTLY_REMOVED_PREVIOUS_NODE,

    UPDATE_CONCURRENTLY_REMOVED_NODE,

    RENAME_CONCURRENTLY_REMOVED_NODE,

    RENAME_CONCURRENTLY_DUPLICATE_NAME,

    NAVIGATION_NO_SITE,

}
