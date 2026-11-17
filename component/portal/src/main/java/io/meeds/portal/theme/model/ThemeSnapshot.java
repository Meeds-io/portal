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
package io.meeds.portal.theme.model;

import java.util.List;

/**
 * The published branding state and every theme, read together: the one value
 * a node caches. The state and the themes come from one load, so the version a
 * client reads with the themes is the version of those themes.
 */
public class ThemeSnapshot {

  private final BrandingState state;

  private final List<Theme>   themes;

  public ThemeSnapshot(BrandingState state, List<Theme> themes) {
    this.state = state;
    this.themes = List.copyOf(themes);
  }

  public BrandingState getState() {
    return state;
  }

  /**
   * @return every theme, sorted by id
   */
  public List<Theme> getThemes() {
    return themes;
  }

  /**
   * @param id a theme id
   * @return the theme, null when this snapshot holds none with this id
   */
  public Theme getTheme(long id) {
    for (Theme theme : themes) {
      if (theme.getId() == id) {
        return theme;
      }
    }
    return null;
  }

}
