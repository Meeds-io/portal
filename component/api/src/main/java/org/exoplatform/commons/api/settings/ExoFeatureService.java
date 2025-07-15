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

public interface ExoFeatureService {

  /**
   * An event to trigger using ListenerService when a feature status changes
   * using API
   */
  public final String FEATURE_STATUS_CHANGED_EVENT = "feature.status.changed";

  /**
   * Check the status of a feature
   * 
   * @param featureName
   * @return true if the featureName is on, false if it's off
   */
  public boolean isActiveFeature(String featureName);

  /**
   * Switch feature featureName on or off
   * 
   * @param featureName
   * @param isActive new status of feature, true = on and false = off
   */
  public void saveActiveFeature(String featureName, boolean isActive);

  /**
   * Add a feature plugin that will manage feature enablement that will be
   * applied only on one feature identified by its name
   * 
   * @param featurePlugin of type {@link FeaturePlugin}
   */
  void addFeaturePlugin(FeaturePlugin featurePlugin);

  /**
   * Determines whether the feature is active for a user or not
   * 
   * @param featureName
   * @param username
   * @return true if active, else false
   */
  boolean isFeatureActiveForUser(String featureName, String username);
}
