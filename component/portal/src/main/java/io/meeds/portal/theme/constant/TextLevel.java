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
package io.meeds.portal.theme.constant;

import static io.meeds.portal.theme.util.ThemeColorCalculator.DARK_GREY;
import static io.meeds.portal.theme.util.ThemeColorCalculator.DARK_NEUTRAL;
import static io.meeds.portal.theme.util.ThemeColorCalculator.LIGHT_GREY;
import static io.meeds.portal.theme.util.ThemeColorCalculator.LIGHT_NEUTRAL;

/** A text level: its two default colors, light and dark. */
public enum TextLevel {
  NEUTRAL(LIGHT_NEUTRAL, DARK_NEUTRAL), GREY(LIGHT_GREY, DARK_GREY);

  private final String lightDefault;

  private final String darkDefault;

  TextLevel(String lightDefault, String darkDefault) {
    this.lightDefault = lightDefault;
    this.darkDefault = darkDefault;
  }

  public String getLightDefault() {
    return lightDefault;
  }

  public String getDarkDefault() {
    return darkDefault;
  }
}
