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

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A theme the page touched: an existing one by id, or a new one by client id,
 * possibly started from another theme. Its maps are the stored maps the page
 * holds, never resolved values; a null value removes the stored one.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThemePublication {

  private Long                id;

  private String              clientId;

  private String              name;

  /** Null leaves an existing theme's shared settings unchanged. */
  private Map<String, String> shared;

  /** Null leaves an existing theme's light values unchanged. */
  private Map<String, String> light;

  /** Null leaves an existing theme's dark values unchanged. */
  private Map<String, String> dark;

  private ThemeReference      startFrom;

}
