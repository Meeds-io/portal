/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package org.gatein.portal.controller.resource;

import java.io.Serializable;
import java.util.Locale;

import lombok.Data;
import lombok.EqualsAndHashCode.Exclude;
import lombok.Getter;

@Data
public class ScriptKey implements Serializable {

  private static final long  serialVersionUID   = 8421109092217952533L;

  public static final String SCOPE_QUERY_PARAM  = "scope";

  public static final String MINIFY_QUERY_PARAM = "minify";

  public static final String HASH_QUERY_PARAM   = "hash";

  @Getter
  final ResourceId           id;

  @Getter
  final boolean              minified;

  @Exclude
  final Locale               locale;

  public ScriptKey(ResourceId id, boolean minified, Locale locale) {
    this.id = id;
    this.minified = minified;
    this.locale = locale;
  }

}
