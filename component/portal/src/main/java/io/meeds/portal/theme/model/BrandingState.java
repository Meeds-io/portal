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

import io.meeds.portal.theme.constant.ModesOffered;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The branding state as stored: a null field leaves the configured value
 * apply. The version stamps every write and is what a publication compares.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrandingState {

  private long         version;

  private boolean      legacyImported;

  private String       companyName;

  private Long         logoFileId;

  private Long         darkLogoFileId;

  private Long         faviconFileId;

  private Long         darkFaviconFileId;

  private Long         themeInUseId;

  private ModesOffered modesOffered;

  private Long         lastEffectiveThemeId;

  private ModesOffered lastEffectiveModes;

  private int          modesGeneration;

}
