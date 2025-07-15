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
package org.exoplatform.web.login;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.exoplatform.web.ControllerContext;

/**
 * An API for services willing to extend UI (basically based on JSP) with
 * additional parameters. To extend the list of params, you can simply implement
 * this interface and declare it as a Kernel Component.
 */
public interface UIParamsExtension {

  /**
   * @return {@link List} of extension names that will be used to fill
   *         parameters
   */
  List<String> getExtensionNames();

  /**
   * Returns the list of parameters to retrieve in Login UI for Login components
   * 
   * @param controllerContext {@link ControllerContext} that provides the
   *          current {@link HttpServletRequest} and {@link HttpServletResponse}
   * @param extensionName extension name that needs to extend the parameters
   * @return List of Key/Value to add to Login JSON object params
   */
  Map<String, Object> extendParameters(ControllerContext controllerContext, String extensionName);

}
