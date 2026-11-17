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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A theme's effective values per version, Auto and Automatic computed, with
 * the contrast ratio of every text color on its background and the texts that
 * read badly.
 */
@Data
@NoArgsConstructor
public class ResolvedTheme {

  private Map<String, String> light         = new HashMap<>();

  private Map<String, String> dark          = new HashMap<>();

  /** Text key to its ratio on its background, one decimal. */
  private Map<String, Double> lightRatios   = new HashMap<>();

  private Map<String, Double> darkRatios    = new HashMap<>();

  /** Text keys below 4.5:1. */
  private Set<String>         lightWarnings = new HashSet<>();

  private Set<String>         darkWarnings  = new HashSet<>();

}
