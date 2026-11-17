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
package io.meeds.portal.theme.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import io.meeds.portal.theme.entity.BrandingStateEntity;
import io.meeds.portal.theme.entity.ThemeEntity;
import io.meeds.portal.theme.model.BrandingState;
import io.meeds.portal.theme.model.Theme;
import io.meeds.portal.theme.model.ThemeSettings;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Maps the theme rows to the service DTOs: a settings column is a JSON map
 * variable to value, read null-free.
 */
public final class ThemeMapper {

  private static final JsonMapper                              JSON     = JsonMapper.builder().build();

  private static final TypeReference<Map<String, String>>      MAP_TYPE = new TypeReference<>() {
                                                                         };

  private static final TypeReference<ThemeSettings>            SETTINGS = new TypeReference<>() {
                                                                         };

  private ThemeMapper() {
    // utility
  }

  public static Theme toTheme(ThemeEntity entity) {
    return toTheme(entity, false);
  }

  /**
   * @param entity a theme row
   * @param withConfiguredName true to keep the configured name in the
   *          configured settings, for the writers that compare it; false for
   *          what a theme exposes
   * @return the theme
   */
  public static Theme toTheme(ThemeEntity entity, boolean withConfiguredName) {
    if (entity == null) {
      return null;
    }
    return new Theme(entity.getId(),
                     entity.getName(),
                     entity.isNameSetInUI(),
                     entity.getOrigin(),
                     entity.getConfigKey(),
                     new ThemeSettings(readMap(entity.getSharedSettings()),
                                       readMap(entity.getLightSettings()),
                                       readMap(entity.getDarkSettings())),
                     withConfiguredName ? readSettings(entity.getConfigSettings()) :
                                        withoutConfiguredName(readSettings(entity.getConfigSettings())),
                     entity.getUpdatedDate());
  }

  public static BrandingState toState(BrandingStateEntity entity) {
    return new BrandingState(entity.getVersion(),
                             entity.isLegacyImported(),
                             entity.getCompanyName(),
                             entity.getLogoFileId(),
                             entity.getDarkLogoFileId(),
                             entity.getFaviconFileId(),
                             entity.getDarkFaviconFileId(),
                             entity.getThemeInUseId(),
                             entity.getModesOffered(),
                             entity.getLastEffectiveThemeId(),
                             entity.getLastEffectiveModes(),
                             entity.getModesGeneration());
  }

  /**
   * @param json a stored JSON map, possibly null or blank
   * @return a mutable map without null keys or values
   */
  public static Map<String, String> readMap(String json) {
    Map<String, String> map = new HashMap<>();
    if (StringUtils.isNotBlank(json)) {
      Map<String, String> read = JSON.readValue(json, MAP_TYPE);
      if (read != null) {
        read.forEach((key, value) -> {
          if (key != null && value != null) {
            map.put(key, value);
          }
        });
      }
    }
    return map;
  }

  public static String writeMap(Map<String, String> map) {
    Map<String, String> nullFree = new HashMap<>();
    if (map != null) {
      map.forEach((key, value) -> {
        if (key != null && value != null) {
          nullFree.put(key, value);
        }
      });
    }
    return JSON.writeValueAsString(nullFree);
  }

  /**
   * The configured name is kept in the snapshot to tell a rename in the UI,
   * never a theme setting: it is left out of what a theme exposes.
   */
  private static ThemeSettings withoutConfiguredName(ThemeSettings settings) {
    if (settings != null) {
      settings.getShared().remove("name");
    }
    return settings;
  }

  public static ThemeSettings readSettings(String json) {
    if (StringUtils.isBlank(json)) {
      return null;
    }
    ThemeSettings settings = JSON.readValue(json, SETTINGS);
    return new ThemeSettings(nullFree(settings.getShared()), nullFree(settings.getLight()), nullFree(settings.getDark()));
  }

  public static String writeSettings(ThemeSettings settings) {
    return settings == null ? null :
                            JSON.writeValueAsString(new ThemeSettings(nullFree(settings.getShared()),
                                                                      nullFree(settings.getLight()),
                                                                      nullFree(settings.getDark())));
  }

  /**
   * @return a deep copy, so a cached value is never changed by its reader
   */
  public static ThemeSettings copy(ThemeSettings settings) {
    return settings == null ? null :
                            new ThemeSettings(nullFree(settings.getShared()),
                                              nullFree(settings.getLight()),
                                              nullFree(settings.getDark()));
  }

  private static Map<String, String> nullFree(Map<String, String> map) {
    Map<String, String> copy = new HashMap<>();
    if (map != null) {
      map.forEach((key, value) -> {
        if (key != null && value != null) {
          copy.put(key, value);
        }
      });
    }
    return copy;
  }

}
