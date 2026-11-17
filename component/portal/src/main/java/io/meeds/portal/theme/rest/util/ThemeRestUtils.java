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
package io.meeds.portal.theme.rest.util;

import org.apache.commons.lang3.StringUtils;

import io.meeds.portal.theme.constant.DisplayMode;
import io.meeds.portal.theme.model.BrandingPublication;
import io.meeds.portal.theme.model.ThemePublication;
import io.meeds.portal.theme.service.ThemeService;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The JSON of the theme endpoints, read and written with Jackson so that a
 * JSON null in a theme map survives: it means "remove the stored value".
 */
public final class ThemeRestUtils {

  public static final String     INVALID_BODY_MESSAGE = "branding.publication.invalidBody";

  public static final String     INVALID_CUSTOM_CSS_MESSAGE = "branding.customCss.invalidBody";

  public static final String     INVALID_DISPLAY_MODE_MESSAGE = "branding.displayMode.invalid";

  private static final JsonMapper JSON                 = JsonMapper.builder()
                                                                   .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                                                   .build();

  private ThemeRestUtils() {
    // utility
  }

  /**
   * @param body the request body
   * @return the publication, its maps keeping their null values
   * @throws IllegalArgumentException {@link #INVALID_BODY_MESSAGE} when the
   *           body is not a publication
   */
  public static BrandingPublication readPublication(String body) {
    if (StringUtils.isBlank(body)) {
      throw new IllegalArgumentException(INVALID_BODY_MESSAGE);
    }
    try {
      BrandingPublication publication = JSON.readValue(body, BrandingPublication.class);
      if (publication == null) {
        throw new IllegalArgumentException(INVALID_BODY_MESSAGE);
      }
      return publication;
    } catch (JacksonException e) {
      throw new IllegalArgumentException(INVALID_BODY_MESSAGE, e);
    }
  }

  /**
   * @param body a theme as the page holds it
   * @return the theme, its maps keeping their null values
   * @throws IllegalArgumentException {@link #INVALID_BODY_MESSAGE}
   */
  public static ThemePublication readTheme(String body) {
    try {
      ThemePublication theme = StringUtils.isBlank(body) ? null : JSON.readValue(body, ThemePublication.class);
      if (theme == null) {
        throw new IllegalArgumentException(INVALID_BODY_MESSAGE);
      }
      return theme;
    } catch (JacksonException e) {
      throw new IllegalArgumentException(INVALID_BODY_MESSAGE, e);
    }
  }

  /**
   * @param body {"mode": "AUTOMATIC|LIGHT|DARK"}
   * @return the display mode
   * @throws IllegalArgumentException {@link #INVALID_DISPLAY_MODE_MESSAGE}
   */
  public static DisplayMode readDisplayMode(String body) {
    try {
      JsonNode mode = StringUtils.isBlank(body) ? null : JSON.readTree(body).get("mode");
      if (mode == null || !mode.isString()) {
        throw new IllegalArgumentException(INVALID_DISPLAY_MODE_MESSAGE);
      }
      return DisplayMode.valueOf(mode.asString());
    } catch (JacksonException | IllegalArgumentException e) {
      throw new IllegalArgumentException(INVALID_DISPLAY_MODE_MESSAGE, e);
    }
  }

  /**
   * @param body {"customCss": "the CSS, blank to remove it"}
   * @return the custom CSS, blank to remove it
   * @throws IllegalArgumentException {@link #INVALID_CUSTOM_CSS_MESSAGE}
   */
  public static String readCustomCss(String body) {
    try {
      JsonNode css = StringUtils.isBlank(body) ? null : JSON.readTree(body).get("customCss");
      if (css == null || !(css.isString() || css.isNull())) {
        throw new IllegalArgumentException(INVALID_CUSTOM_CSS_MESSAGE);
      }
      return css.isNull() ? "" : css.asString();
    } catch (JacksonException e) {
      throw new IllegalArgumentException(INVALID_CUSTOM_CSS_MESSAGE, e);
    }
  }

  public static String toJson(Object value) {
    return JSON.writeValueAsString(value);
  }

  /**
   * @param e a refusal of a publication
   * @return the HTTP status it answers: 409 for a stale version, 500 for an
   *         engine failure, 400 for every other refusal
   */
  public static int statusOf(RuntimeException e) {
    if (e instanceof IllegalStateException) {
      return ThemeService.PUBLISH_CONFLICT_MESSAGE.equals(e.getMessage()) ? 409 : 500;
    }
    return e instanceof IllegalArgumentException ? 400 : 500;
  }

}
