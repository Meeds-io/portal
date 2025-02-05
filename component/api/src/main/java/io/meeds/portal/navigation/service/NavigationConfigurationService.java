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
package io.meeds.portal.navigation.service;

import java.util.List;
import java.util.Locale;

import org.exoplatform.commons.addons.AddOnService;

import io.meeds.portal.navigation.constant.SidebarMode;
import io.meeds.portal.navigation.model.NavigationConfiguration;
import io.meeds.portal.navigation.model.SidebarConfiguration;
import io.meeds.portal.navigation.model.TopbarApplication;
import io.meeds.portal.navigation.model.TopbarConfiguration;

public interface NavigationConfigurationService {

  /**
   * @return {@link NavigationConfiguration} with the complete configuration of
   *         Navigation
   */
  NavigationConfiguration getConfiguration();

  /**
   * @param username User name
   * @param locale {@link Locale} to compute Menu item names
   * @param resolve either resolve name and icon of elements or not
   * @return {@link NavigationConfiguration} with the complete configuration of
   *         Navigation
   */
  NavigationConfiguration getConfiguration(String username, Locale locale, boolean resolve);

  /**
   * @param username User name
   * @param locale {@link Locale} to compute Menu item names
   * @return {@link TopbarConfiguration} switch user role and customized
   *         settings
   */
  TopbarConfiguration getTopbarConfiguration(String username, Locale locale);

  /**
   * @param username User name
   * @param locale {@link Locale} to compute Menu item names
   * @return {@link SidebarConfiguration} switch user role and customized
   *         settings
   */
  SidebarConfiguration getSidebarConfiguration(String username, Locale locale);

  /**
   * Retrieves the preferred mode of sidebar by a user
   * 
   * @param username User name as identifier
   * @return preferred {@link SidebarMode} or default if not set by user yet
   */
  SidebarMode getSidebarUserMode(String username);

  /**
   * Updates the preferred mode of sidebar by the user
   * 
   * @param username User name as identifier
   * @param mode Preferred {@link SidebarMode} by the user
   */
  void updateSidebarUserMode(String username, SidebarMode mode);

  /**
   * Updates the Navigation configuration
   * 
   * @param navigationConfiguration
   */
  void updateConfiguration(NavigationConfiguration navigationConfiguration);

  /**
   * @return Default Topbar Applications as configured in {@link AddOnService}
   */
  List<TopbarApplication> getDefaultTopbarApplications();

  /**
   * @return true if Users can have personal home URL, else false
   */
  boolean isAllowUserHome();

}
