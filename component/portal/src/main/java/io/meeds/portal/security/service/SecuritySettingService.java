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
package io.meeds.portal.security.service;

import static io.meeds.portal.security.constant.UserRegistrationType.OPEN;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.portal.security.constant.UserRegistrationType;
import io.meeds.portal.security.model.RegistrationSetting;

public class SecuritySettingService {

  public static final String                  ACCESS_TYPE_MODIFIED               = "meeds.settings.access.type.modified";

  public static final String                  EXTERNAL_USER_REG_MODIFIED         = "meeds.settings.access.externalUsers.modified";

  public static final String                  DEFAULT_GROUPS_MODIFIED            = "meeds.settings.access.defaultGroups.modified";

  protected static final Context              SECURITY_CONTEXT                   = Context.GLOBAL.id("SECURITY");

  protected static final Scope                SECURITY_SCOPE                     = Scope.APPLICATION.id("SECURITY");

  protected static final String               REGISTRATION_TYPE_PARAM            = "REGISTRATION_TYPE";

  protected static final String               REGISTRATION_EXTERNAL_USER_PARAM   = "REGISTRATION_EXTERNAL_USER";

  protected static final String               REGISTRATION_EXTRA_GROUPS_PARAM    = "REGISTRATION_EXTRA_GROUPS";

  protected static final String               EXTRA_GROUPS_SEPARATOR             = ",";

  protected static final UserRegistrationType DEFAULT_REGISTRATION_TYPE          =
                                                                        UserRegistrationType.valueOf(System.getProperty("meeds.settings.access.type.default",
                                                                                                                        OPEN.name())
                                                                                                           .toUpperCase());

  protected static final boolean              DEFAULT_REGISTRATION_EXTERNAL_USER =
                                                                                 Boolean.parseBoolean(System.getProperty("meeds.settings.access.externalUsers",
                                                                                                                         "false")
                                                                                                            .toLowerCase());

  private static final Log                    LOG                                =
                                                  ExoLogger.getLogger(SecuritySettingService.class);

  private RegistrationSetting                 registrationSetting;

  private SettingService                      settingService;

  private ListenerService                     listenerService;

  public SecuritySettingService(SettingService settingService,
                                ListenerService listenerService) {
    this.settingService = settingService;
    this.listenerService = listenerService;
  }

  public RegistrationSetting getRegistrationSetting() {
    if (registrationSetting == null) {
      registrationSetting = new RegistrationSetting(getRegistrationType(),
                                                    isRegistrationExternalUser(),
                                                    getRegistrationGroupIds());
    }
    return registrationSetting;
  }

  public void saveRegistrationSetting(RegistrationSetting registrationSetting) {
    saveRegistrationType(registrationSetting.getType());
    saveRegistrationExternalUser(registrationSetting.isExternalUser());
    saveRegistrationGroupIds(registrationSetting.getExtraGroupIds());
  }

  public String[] getRegistrationGroupIds() {
    SettingValue<?> settingValue = settingService.get(SECURITY_CONTEXT, SECURITY_SCOPE, REGISTRATION_EXTRA_GROUPS_PARAM);
    if (settingValue == null || settingValue.getValue() == null) {
      return new String[0];
    } else {
      return Arrays.stream(settingValue.getValue().toString().split(EXTRA_GROUPS_SEPARATOR))
                   .filter(StringUtils::isNotBlank)
                   .distinct()
                   .toArray(String[]::new);
    }
  }

  public UserRegistrationType getRegistrationType() {
    SettingValue<?> settingValue = settingService.get(SECURITY_CONTEXT, SECURITY_SCOPE, REGISTRATION_TYPE_PARAM);
    if (settingValue == null || settingValue.getValue() == null) {
      return DEFAULT_REGISTRATION_TYPE;
    } else {
      return UserRegistrationType.valueOf(settingValue.getValue().toString());
    }
  }

  public void saveRegistrationType(UserRegistrationType registrationType) {
    if (registrationType == null) {
      registrationType = DEFAULT_REGISTRATION_TYPE;
    }
    UserRegistrationType storedRegistrationType = getRegistrationType();
    boolean modified = registrationType != storedRegistrationType;
    if (modified) {
      try {
        settingService.set(SECURITY_CONTEXT,
                           SECURITY_SCOPE,
                           REGISTRATION_TYPE_PARAM,
                           SettingValue.create(registrationType.toString()));
        broadcastEvent(ACCESS_TYPE_MODIFIED, null, registrationType);
      } finally {
        registrationSetting = null;
      }
    }
  }

  public boolean isRegistrationExternalUser() {
    SettingValue<?> settingValue = settingService.get(SECURITY_CONTEXT, SECURITY_SCOPE, REGISTRATION_EXTERNAL_USER_PARAM);
    if (settingValue == null || settingValue.getValue() == null) {
      return DEFAULT_REGISTRATION_EXTERNAL_USER;
    } else {
      return Boolean.parseBoolean(settingValue.getValue().toString());
    }
  }

  public void saveRegistrationExternalUser(boolean externalUser) {
    if (externalUser != isRegistrationExternalUser()) {
      try {
        settingService.set(SECURITY_CONTEXT,
                           SECURITY_SCOPE,
                           REGISTRATION_EXTERNAL_USER_PARAM,
                           SettingValue.create(String.valueOf(externalUser)));
        broadcastEvent(EXTERNAL_USER_REG_MODIFIED, null, externalUser);
      } finally {
        registrationSetting = null;
      }
    }
  }

  public void saveRegistrationGroupIds(String[] groupIds) {
    try {
      if (groupIds == null) {
        groupIds = new String[0];
      }
      settingService.set(SECURITY_CONTEXT,
                         SECURITY_SCOPE,
                         REGISTRATION_EXTRA_GROUPS_PARAM,
                         SettingValue.create(StringUtils.join(groupIds, EXTRA_GROUPS_SEPARATOR)));
      broadcastEvent(DEFAULT_GROUPS_MODIFIED, null, groupIds);
    } finally {
      registrationSetting = null;
    }
  }

  private void broadcastEvent(String eventName, Object source, Object data) {
    try {
      listenerService.broadcast(eventName, source, data);
    } catch (Exception e) {
      LOG.warn("Error broacasting event {} with source {} and data {}",
               eventName,
               source,
               data,
               e);
    }
  }

}
