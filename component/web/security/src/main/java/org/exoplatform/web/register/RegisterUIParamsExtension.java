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
package org.exoplatform.web.register;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.login.LoginHandler;
import org.exoplatform.web.login.UIParamsExtension;

import io.meeds.portal.security.constant.UserRegistrationType;
import io.meeds.portal.security.service.SecuritySettingService;

public class RegisterUIParamsExtension implements UIParamsExtension {

  public static final String        ONBOARDING_ENABLED_PARAM    = "enabled";

  public static final String        ONBOARDING_REGISTER_ENABLED = "onboardingRegisterEnabled";

  private static final List<String> EXTENSION_NAMES             = Arrays.asList(LoginHandler.REGISTER_EXTENSION_NAME,
                                                                                LoginHandler.LOGIN_EXTENSION_NAME);

  private SecuritySettingService    securitySettingService;

  public RegisterUIParamsExtension(SecuritySettingService securitySettingService) {
    this.securitySettingService = securitySettingService;
  }

  @Override
  public List<String> getExtensionNames() {
    return EXTENSION_NAMES;
  }

  @Override
  public Map<String, Object> extendParameters(ControllerContext controllerContext, String extensionName) {
    if (isRegisterEnabled()) {
      Map<String, Object> params = new HashMap<>();
      params.put("registerEnabled", true);
      params.put(ONBOARDING_REGISTER_ENABLED, true);
      return params;
    }
    return Collections.emptyMap();
  }

  public boolean isRegisterEnabled() {
    return this.securitySettingService.getRegistrationType() == UserRegistrationType.OPEN;
  }

}
