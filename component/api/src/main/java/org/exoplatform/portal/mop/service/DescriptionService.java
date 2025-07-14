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
package org.exoplatform.portal.mop.service;

import org.exoplatform.portal.mop.State;

import java.util.Locale;
import java.util.Map;

public interface DescriptionService {

  /**
   * Returns a map containing all the descriptions of an object or null if the
   * object is not internationalized.
   *
   * @param id the object id
   * @return the map the description map
   */
  Map<Locale, State> getDescriptions(String id);

  /**
   * Updates the description of the specified object or remove the
   * internationalized characteristic of the object if the description map is
   * null.
   *
   * @param id the object id
   * @param descriptions the new descriptions
   */
  void setDescriptions(String id, Map<Locale, org.exoplatform.portal.mop.State> descriptions);

}
