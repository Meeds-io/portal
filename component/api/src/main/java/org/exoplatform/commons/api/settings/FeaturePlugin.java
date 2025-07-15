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
package org.exoplatform.commons.api.settings;

import org.exoplatform.container.component.BaseComponentPlugin;

/**
 * {@link ExoFeatureService} plugin that allows to manage enablement of feature
 * by plugin instead of managing it in Service so that each addon can manage the
 * feature enablement by user
 */
public abstract class FeaturePlugin extends BaseComponentPlugin {

  /**
   * @return feature name
   */
  public String getFeatureName() {
    return getName();
  }

  /**
   * Determines whether the fefature is enabled for a user
   * 
   * @param featureName
   * @param username
   * @return true if enabled, else false
   */
  public abstract boolean isFeatureActiveForUser(String featureName, String username);

  /**
   * Initialization feature plugin
   */
  public void init() {}
}
