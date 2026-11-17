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

import java.util.Date;

import io.meeds.portal.theme.constant.ThemeOrigin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A theme as stored: its own settings hold what an administrator set from the
 * UI only, and a theme bound to the configuration carries its configured
 * values apart.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Theme {

  private long          id;

  private String        name;

  private boolean       nameSetInUI;

  private ThemeOrigin   origin;

  private String        configKey;

  private ThemeSettings settings;

  /** The configured values of a bound theme, null for an unbound one. */
  private ThemeSettings configSettings;

  private Date          updatedDate;

  public boolean isDefaultTheme() {
    return origin == ThemeOrigin.DEFAULT;
  }

  public boolean isDeletable() {
    return origin == ThemeOrigin.UI;
  }

}
