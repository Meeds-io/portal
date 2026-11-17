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

import java.util.ArrayList;
import java.util.List;

import io.meeds.portal.theme.constant.ModesOffered;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What the Branding and Theme page sends on Apply: the version it loaded, and
 * every change made since. A null field changes nothing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrandingPublication {

  private long                        version;

  private BrandingIdentityPublication branding;

  private List<ThemePublication>      themes          = new ArrayList<>();

  private List<Long>                  deletedThemeIds = new ArrayList<>();

  private ThemeReference              themeInUse;

  private ModesOffered                modesOffered;

}
