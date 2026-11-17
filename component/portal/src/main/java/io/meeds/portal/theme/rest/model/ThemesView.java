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
package io.meeds.portal.theme.rest.model;

import java.util.List;

import io.meeds.portal.theme.constant.ModesOffered;
import io.meeds.portal.theme.model.Theme;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What the Branding and Theme page loads: every theme as stored, a configured
 * theme's configured values beside its own, the published state and the
 * version a publication must send back.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThemesView {

  private long         version;

  /** The company name an administrator set, null when the configured one applies. */
  private String       companyName;

  /** The company name shown, whatever set it. */
  private String       effectiveCompanyName;

  private Long         logoFileId;

  private Long         darkLogoFileId;

  private Long         faviconFileId;

  private Long         darkFaviconFileId;

  /** The theme an administrator selected, null when the configured one applies. */
  private Long         storedThemeInUseId;

  /** The theme in use, whatever chose it. */
  private long         themeInUseId;

  /** The modes an administrator chose, null when the configured ones apply. */
  private ModesOffered storedModesOffered;

  /** The modes offered, whatever chose them. */
  private ModesOffered modesOffered;

  private List<Theme>  themes;

}
