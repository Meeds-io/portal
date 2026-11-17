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

import static io.meeds.portal.theme.util.ThemeColorCalculator.DARK_APP_BACKGROUND;
import static io.meeds.portal.theme.util.ThemeColorCalculator.DARK_DRAWER_BACKGROUND;
import static io.meeds.portal.theme.util.ThemeColorCalculator.DARK_PAGE_BACKGROUND;
import static io.meeds.portal.theme.util.ThemeColorCalculator.LIGHT_APP_BACKGROUND;
import static io.meeds.portal.theme.util.ThemeColorCalculator.LIGHT_DRAWER_BACKGROUND;
import static io.meeds.portal.theme.util.ThemeColorCalculator.LIGHT_PAGE_BACKGROUND;

/** A background kind: its light default and its dark default. */
public enum BackgroundKind {
  PAGE(LIGHT_PAGE_BACKGROUND, DARK_PAGE_BACKGROUND),
  APP(LIGHT_APP_BACKGROUND, DARK_APP_BACKGROUND),
  DRAWER(LIGHT_DRAWER_BACKGROUND, DARK_DRAWER_BACKGROUND);

  private final String lightDefault;

  private final String darkDefault;

  BackgroundKind(String lightDefault, String darkDefault) {
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
