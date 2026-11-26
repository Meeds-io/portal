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
package io.meeds.portal.logging.constant;

import java.util.Locale;

import ch.qos.logback.classic.Level;

/**
 * The levels an administrator can give to a logger at runtime. {@code OFF}
 * silences the logger; there is no {@code ALL}, which {@code TRACE} already
 * covers for every message the platform writes.
 */
public enum LoggingLevel {
  TRACE(Level.TRACE),
  DEBUG(Level.DEBUG),
  INFO(Level.INFO),
  WARN(Level.WARN),
  ERROR(Level.ERROR),
  OFF(Level.OFF);

  private final Level level;

  LoggingLevel(Level level) {
    this.level = level;
  }

  public Level getLevel() {
    return level;
  }

  /**
   * @param  name a level name, in any case
   * @return      the matching level
   * @throws IllegalArgumentException with the message code
   *                                  {@code logging.level.invalid} when the
   *                                  name is not one of the levels
   */
  public static LoggingLevel fromName(String name) {
    if (name != null) {
      try {
        return valueOf(name.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException _) {
        // fall through to the message code
      }
    }
    throw new IllegalArgumentException("logging.level.invalid");
  }

  /**
   * @param  level a logback level, {@code null} for a logger that inherits
   * @return       the matching level, {@code null} when there is none
   */
  public static LoggingLevel fromLevel(Level level) {
    if (level != null) {
      for (LoggingLevel value : values()) {
        if (value.level.equals(level)) {
          return value;
        }
      }
    }
    return null;
  }
}
