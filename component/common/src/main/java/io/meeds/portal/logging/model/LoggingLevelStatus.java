/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.portal.logging.model;

/**
 * What the page may do, as a system administrator configured it.
 *
 * @param enabled  whether the levels are listed and changed at all
 * @param readOnly whether the levels are listed but not changed
 * @param revertAfterMinutes the minutes after which a change is given back to
 *                           the level of {@code logback.xml}, {@code 0} for no
 *                           limit
 */
public record LoggingLevelStatus(boolean enabled, boolean readOnly, int revertAfterMinutes) {

  public LoggingLevelStatus(boolean enabled, boolean readOnly) {
    this(enabled, readOnly, 0);
  }

}
