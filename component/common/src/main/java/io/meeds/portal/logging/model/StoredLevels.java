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

import java.util.Map;

import io.meeds.portal.logging.constant.LoggingLevel;

/**
 * What an administrator changed, as stored: the levels and the token of the
 * write that stored them, read together.
 *
 * @param version   the token of the write, changed by every write
 * @param overrides the levels, by logger name, in the order they were set
 * @param changedAt when each level was last changed, in milliseconds since
 *                  the epoch; a logger that is not there has no known time
 */
public record StoredLevels(String version, Map<String, LoggingLevel> overrides, Map<String, Long> changedAt) {

  public StoredLevels(String version, Map<String, LoggingLevel> overrides) {
    this(version, overrides, Map.of());
  }

}
