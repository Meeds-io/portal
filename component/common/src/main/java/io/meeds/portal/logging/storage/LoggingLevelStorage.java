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
package io.meeds.portal.logging.storage;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;

import io.meeds.portal.logging.constant.LoggingLevel;
import io.meeds.portal.logging.model.StoredLevels;

import lombok.RequiredArgsConstructor;

/**
 * Keeps the levels the administrators set at runtime, so that they survive a
 * restart and reach every node: one setting holds a token that changes on every
 * write, then the levels, one line {@code logger=LEVEL} each. Each node
 * compares the token to the one it applied to know it has to apply the levels
 * again. They are one setting so that a reader never sees the token of one
 * write with the levels of another, and a write is stored whole or not at all.
 * A level is {@code NAME}, or {@code NAME@<milliseconds>} when the time of the
 * change is known, which is how a change is given back after a delay.
 */
@Component
@RequiredArgsConstructor
public class LoggingLevelStorage {

  private static final Scope  SCOPE          = Scope.APPLICATION.id("LoggingLevels");

  private static final String STATE_KEY      = "state";

  private static final String VERSION_PREFIX = "version=";

  private static final char   TIME_SEPARATOR = '@';

  private final SettingService settingService;

  /**
   * @return what was stored, {@code null} when nothing was ever stored. A line
   *         that is not a level is skipped, not fatal: the others still apply.
   */
  public StoredLevels read() {
    SettingValue<?> value = settingService.get(Context.GLOBAL, SCOPE, STATE_KEY);
    if (value == null || value.getValue() == null) {
      return null;
    }
    String[] lines = value.getValue().toString().split("\n");
    if (lines.length == 0 || !lines[0].startsWith(VERSION_PREFIX)) {
      return null;
    }
    Map<String, LoggingLevel> overrides = new LinkedHashMap<>();
    Map<String, Long> changedAt = new HashMap<>();
    for (int i = 1; i < lines.length; i++) {
      int separator = lines[i].indexOf('=');
      if (separator > 0) {
        String name = lines[i].substring(0, separator);
        String level = lines[i].substring(separator + 1);
        int at = level.indexOf(TIME_SEPARATOR);
        try {
          overrides.put(name, LoggingLevel.fromName(at < 0 ? level : level.substring(0, at)));
          Long time = at < 0 ? null : parseTime(level.substring(at + 1));
          if (time != null) {
            changedAt.put(name, time);
          }
        } catch (IllegalArgumentException _) {
          // skipped
        }
      }
    }
    return new StoredLevels(lines[0].substring(VERSION_PREFIX.length()), overrides, changedAt);
  }

  /**
   * @return the stored levels, by logger name, none when nothing was stored
   */
  public Map<String, LoggingLevel> getOverrides() {
    StoredLevels stored = read();
    return stored == null ? new LinkedHashMap<>() : stored.overrides();
  }

  /**
   * @return the token of the last write, {@code null} when nothing was ever
   *         stored
   */
  public String getVersion() {
    StoredLevels stored = read();
    return stored == null ? null : stored.version();
  }

  /**
   * Replaces the stored levels, with a new token, and no known time of change
   * (a change with no time is never given back after a delay).
   *
   * @param  overrides the levels, by logger name
   * @return           the new token
   */
  public String saveOverrides(Map<String, LoggingLevel> overrides) {
    return saveOverrides(overrides, Map.of());
  }

  /**
   * Replaces the stored levels, with a new token.
   *
   * @param  overrides the levels, by logger name
   * @param  changedAt when each level was changed, in milliseconds since the
   *                   epoch; a logger that is not there has no known time
   * @return           the new token
   */
  public String saveOverrides(Map<String, LoggingLevel> overrides, Map<String, Long> changedAt) {
    String version = UUID.randomUUID().toString();
    StringBuilder content = new StringBuilder(VERSION_PREFIX).append(version).append('\n');
    overrides.forEach((name, level) -> {
      content.append(name).append('=').append(level.name());
      Long time = changedAt.get(name);
      if (time != null && time > 0) {
        content.append(TIME_SEPARATOR).append(time);
      }
      content.append('\n');
    });
    settingService.set(Context.GLOBAL, SCOPE, STATE_KEY, SettingValue.create(content.toString()));
    return version;
  }

  private static Long parseTime(String value) {
    try {
      long time = Long.parseLong(value.trim());
      return time > 0 ? time : null;
    } catch (NumberFormatException _) {
      return null;
    }
  }
}
