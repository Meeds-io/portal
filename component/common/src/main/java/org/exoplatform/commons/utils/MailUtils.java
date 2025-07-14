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
package org.exoplatform.commons.utils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.branding.BrandingService;

public class MailUtils {

  public static final String DEFAULT_FROM_EMAIL = "noreply@meeds.io";

  public static final String SENDER_NAME_PARAM  = "exo:notificationSenderName";

  public static final String SENDER_EMAIL_PARAM = "exo:notificationSenderEmail";

  private MailUtils() {
    // Util Class contianing static calls
  }

  public static String getSenderName() {
    SettingValue<?> name = getSettingService().get(Context.GLOBAL, Scope.GLOBAL.id(null), SENDER_NAME_PARAM);
    return name != null ? (String) name.getValue() : getBrandingCompanyName();
  }

  public static String getSenderEmail() {
    SettingValue<?> mail = getSettingService().get(Context.GLOBAL, Scope.GLOBAL.id(null), SENDER_EMAIL_PARAM);
    return mail != null ? (String) mail.getValue() : System.getProperty("gatein.email.smtp.from", DEFAULT_FROM_EMAIL);
  }

  private static SettingService getSettingService() {
    SettingService settingService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SettingService.class);
    if (settingService == null) {
      settingService = PortalContainer.getInstance().getComponentInstanceOfType(SettingService.class);
    }
    return settingService;
  }

  private static String getBrandingCompanyName() {
    BrandingService brandingService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(BrandingService.class);
    if (brandingService != null) {
      return brandingService.getCompanyName();
    }
    return null;
  }

}
