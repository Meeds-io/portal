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

public enum Visibility {

    /**
     * The object is displayed.
     */
    DISPLAYED,

    /**
     * The object is hidden.
     */
    HIDDEN,

    /**
     * The object visibility is defined by the validity in a related time range.
     */
    TEMPORAL,

    /**
     * The object visibility is system.
     */
    SYSTEM,

    /**
     * The object visibility is draft.
     */
    DRAFT;

    // Exclude Draft since it shouldn't be visible
    public static final Visibility[] DEFAULT_VISIBILITIES = new Visibility[] { // NOSONAR
      DISPLAYED,
      HIDDEN,
      TEMPORAL,
      SYSTEM
    };

}
