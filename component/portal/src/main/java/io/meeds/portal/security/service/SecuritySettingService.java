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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;

import io.meeds.portal.security.constant.UserRegistrationType;
import io.meeds.portal.security.model.RegistrationSetting;

public class SecuritySettingService {

  public static final String                  ACCESS_TYPE_MODIFIED               = "meeds.settings.access.type.modified";

  public static final String                  EXTERNAL_USER_REG_MODIFIED         = "meeds.settings.access.externalUsers.modified";

  public static final String                  DEFAULT_GROUPS_MODIFIED            = "meeds.settings.access.defaultGroups.modified";

  public static final String                  ACCOUNT_DEACTIVATION_MODIFIED      = "meeds.settings.access.accountDeactivation.modified";

  public static final String                  ACCOUNT_DELETION_MODIFIED          = "meeds.settings.access.accountDeletion.modified";

  public static final String                  ACCOUNT_DELETION_ANONYMIZATION_MODIFIED =
                                                                                      "meeds.settings.access.accountDeletionAnonymization.modified";

  public static final String                  DELETED_USER_LABELS_MODIFIED       = "meeds.settings.access.deletedUserLabels.modified";

  protected static final Context              SECURITY_CONTEXT                   = Context.GLOBAL.id("SECURITY");

  protected static final Scope                SECURITY_SCOPE                     = Scope.APPLICATION.id("SECURITY");

  protected static final String               REGISTRATION_TYPE_PARAM            = "REGISTRATION_TYPE";

  protected static final String               REGISTRATION_EXTERNAL_USER_PARAM   = "REGISTRATION_EXTERNAL_USER";

  protected static final String               REGISTRATION_EXTRA_GROUPS_PARAM    = "REGISTRATION_EXTRA_GROUPS";

  protected static final String               ACCOUNT_DEACTIVATION_ENABLED_PARAM = "ACCOUNT_DEACTIVATION_ENABLED";

  protected static final String               ACCOUNT_DELETION_ENABLED_PARAM     = "ACCOUNT_DELETION_ENABLED";

  protected static final String               ACCOUNT_DELETION_ANONYMIZATION_ENABLED_PARAM =
                                                                                           "ACCOUNT_DELETION_ANONYMIZATION_ENABLED";

  protected static final String               DELETED_USER_LABEL_PARAM_PREFIX    = "DELETED_USER_LABEL_";

  protected static final String               EXTRA_GROUPS_SEPARATOR             = ",";

  protected static final UserRegistrationType DEFAULT_REGISTRATION_TYPE          =
                                                                        UserRegistrationType.valueOf(System.getProperty("meeds.settings.access.type.default",
                                                                                                                        OPEN.name())
                                                                                                           .toUpperCase());

  protected static final boolean              DEFAULT_REGISTRATION_EXTERNAL_USER =
                                                                                 Boolean.parseBoolean(System.getProperty("meeds.settings.access.externalUsers",
                                                                                                                         "false")
                                                                                                            .toLowerCase());

  protected static final boolean              DEFAULT_ACCOUNT_DEACTIVATION       =
                                                                           Boolean.parseBoolean(System.getProperty("meeds.settings.access.accountDeactivation",
                                                                                                                   "false")
                                                                                                      .toLowerCase());

  protected static final boolean              DEFAULT_ACCOUNT_DELETION           =
                                                                       Boolean.parseBoolean(System.getProperty("meeds.settings.access.accountDeletion",
                                                                                                               "false")
                                                                                                  .toLowerCase());

  protected static final boolean              DEFAULT_ACCOUNT_DELETION_ANONYMIZATION =
                                                                                     Boolean.parseBoolean(System.getProperty("meeds.settings.access.accountDeletionAnonymization",
                                                                                                                             "false")
                                                                                                                .toLowerCase());

  protected static final String               DEFAULT_DELETED_USER_LABEL         =
                                                                         System.getProperty("meeds.settings.access.deletedUserLabel",
                                                                                            "Deleted user");

  protected static final String               MACHINE_LOCALE_NAME                = "ma";

  private static final Log                    LOG                                =
                                                  ExoLogger.getLogger(SecuritySettingService.class);

  private RegistrationSetting                 registrationSetting;

  private SettingService                      settingService;

  private ListenerService                     listenerService;

  private LocaleConfigService                 localeConfigService;

  public SecuritySettingService(SettingService settingService,
                                ListenerService listenerService,
                                LocaleConfigService localeConfigService) {
    this.settingService = settingService;
    this.listenerService = listenerService;
    this.localeConfigService = localeConfigService;
  }

  public RegistrationSetting getRegistrationSetting() {
    if (registrationSetting == null) {
      registrationSetting = new RegistrationSetting(getRegistrationType(),
                                                    isRegistrationExternalUser(),
                                                    getRegistrationGroupIds(),
                                                    isAccountDeactivationEnabled(),
                                                    isAccountDeletionEnabled(),
                                                    isAccountDeletionAnonymizationEnabled(),
                                                    getDeletedUserLabels());
    }
    return registrationSetting;
  }

  public void saveRegistrationSetting(RegistrationSetting registrationSetting) {
    saveRegistrationType(registrationSetting.getType());
    saveRegistrationExternalUser(registrationSetting.isExternalUser());
    saveRegistrationGroupIds(registrationSetting.getExtraGroupIds());
    saveAccountDeactivationEnabled(registrationSetting.isAccountDeactivationEnabled());
    saveAccountDeletionEnabled(registrationSetting.isAccountDeletionEnabled());
    saveAccountDeletionAnonymizationEnabled(registrationSetting.isAccountDeletionAnonymizationEnabled());
    saveDeletedUserLabels(registrationSetting.getDeletedUserLabels());
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

  public boolean isAccountDeactivationEnabled() {
    SettingValue<?> settingValue = settingService.get(SECURITY_CONTEXT, SECURITY_SCOPE, ACCOUNT_DEACTIVATION_ENABLED_PARAM);
    if (settingValue == null || settingValue.getValue() == null) {
      return DEFAULT_ACCOUNT_DEACTIVATION;
    } else {
      return Boolean.parseBoolean(settingValue.getValue().toString());
    }
  }

  public void saveAccountDeactivationEnabled(boolean accountDeactivationEnabled) {
    if (accountDeactivationEnabled != isAccountDeactivationEnabled()) {
      try {
        settingService.set(SECURITY_CONTEXT,
                           SECURITY_SCOPE,
                           ACCOUNT_DEACTIVATION_ENABLED_PARAM,
                           SettingValue.create(String.valueOf(accountDeactivationEnabled)));
        broadcastEvent(ACCOUNT_DEACTIVATION_MODIFIED, null, accountDeactivationEnabled);
      } finally {
        registrationSetting = null;
      }
      if (!accountDeactivationEnabled) {
        saveAccountDeletionEnabled(false);
      }
    }
  }

  public boolean isAccountDeletionEnabled() {
    SettingValue<?> settingValue = settingService.get(SECURITY_CONTEXT, SECURITY_SCOPE, ACCOUNT_DELETION_ENABLED_PARAM);
    if (settingValue == null || settingValue.getValue() == null) {
      return DEFAULT_ACCOUNT_DELETION;
    } else {
      return Boolean.parseBoolean(settingValue.getValue().toString());
    }
  }

  public void saveAccountDeletionEnabled(boolean accountDeletionEnabled) {
    if (accountDeletionEnabled && !isAccountDeactivationEnabled()) {
      LOG.debug("Account deletion can't be enabled while account deactivation is disabled, keeping it disabled");
      accountDeletionEnabled = false;
    }
    if (accountDeletionEnabled != isAccountDeletionEnabled()) {
      try {
        settingService.set(SECURITY_CONTEXT,
                           SECURITY_SCOPE,
                           ACCOUNT_DELETION_ENABLED_PARAM,
                           SettingValue.create(String.valueOf(accountDeletionEnabled)));
        broadcastEvent(ACCOUNT_DELETION_MODIFIED, null, accountDeletionEnabled);
      } finally {
        registrationSetting = null;
      }
      if (!accountDeletionEnabled) {
        saveAccountDeletionAnonymizationEnabled(false);
      }
    }
  }

  public boolean isAccountDeletionAnonymizationEnabled() {
    SettingValue<?> settingValue = settingService.get(SECURITY_CONTEXT,
                                                      SECURITY_SCOPE,
                                                      ACCOUNT_DELETION_ANONYMIZATION_ENABLED_PARAM);
    if (settingValue == null || settingValue.getValue() == null) {
      return DEFAULT_ACCOUNT_DELETION_ANONYMIZATION;
    } else {
      return Boolean.parseBoolean(settingValue.getValue().toString());
    }
  }

  public void saveAccountDeletionAnonymizationEnabled(boolean accountDeletionAnonymizationEnabled) {
    if (accountDeletionAnonymizationEnabled && !isAccountDeletionEnabled()) {
      LOG.debug("Deleted accounts anonymization can't be enabled while account deletion is disabled, keeping it disabled");
      accountDeletionAnonymizationEnabled = false;
    }
    if (accountDeletionAnonymizationEnabled != isAccountDeletionAnonymizationEnabled()) {
      try {
        settingService.set(SECURITY_CONTEXT,
                           SECURITY_SCOPE,
                           ACCOUNT_DELETION_ANONYMIZATION_ENABLED_PARAM,
                           SettingValue.create(String.valueOf(accountDeletionAnonymizationEnabled)));
        broadcastEvent(ACCOUNT_DELETION_ANONYMIZATION_MODIFIED, null, accountDeletionAnonymizationEnabled);
      } finally {
        registrationSetting = null;
      }
    }
  }

  public Map<String, String> getDeletedUserLabels() {
    return readDeletedUserLabels();
  }

  public String getDeletedUserLabel(Locale locale) {
    Map<String, String> deletedUserLabels = readDeletedUserLabels();
    String label = null;
    if (locale != null) {
      label = deletedUserLabels.get(locale.toString());
      if (StringUtils.isBlank(label)) {
        label = deletedUserLabels.get(locale.getLanguage());
      }
    }
    if (StringUtils.isBlank(label)) {
      label = deletedUserLabels.get(getDefaultLanguage());
    }
    return StringUtils.isBlank(label) ? DEFAULT_DELETED_USER_LABEL : label;
  }

  public void saveDeletedUserLabels(Map<String, String> deletedUserLabels) {
    Map<String, String> labels = deletedUserLabels == null ? Collections.emptyMap() :
                                                           deletedUserLabels.entrySet()
                                                                            .stream()
                                                                            .filter(label -> StringUtils.isNotBlank(label.getValue()))
                                                                            .collect(HashMap::new,
                                                                                     (map, label) -> map.put(label.getKey(),
                                                                                                             label.getValue()),
                                                                                     HashMap::putAll);
    if (!Objects.equals(labels, readDeletedUserLabels())) {
      try {
        for (String language : getSupportedLanguages()) {
          if (labels.containsKey(language)) {
            settingService.set(SECURITY_CONTEXT,
                               SECURITY_SCOPE,
                               DELETED_USER_LABEL_PARAM_PREFIX + language,
                               SettingValue.create(labels.get(language)));
          } else {
            settingService.remove(SECURITY_CONTEXT, SECURITY_SCOPE, DELETED_USER_LABEL_PARAM_PREFIX + language);
          }
        }
        broadcastEvent(DELETED_USER_LABELS_MODIFIED, null, labels);
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

  private Map<String, String> readDeletedUserLabels() {
    Map<String, String> deletedUserLabels = new HashMap<>();
    for (String language : getSupportedLanguages()) {
      SettingValue<?> settingValue = settingService.get(SECURITY_CONTEXT,
                                                        SECURITY_SCOPE,
                                                        DELETED_USER_LABEL_PARAM_PREFIX + language);
      if (settingValue != null && settingValue.getValue() != null && StringUtils.isNotBlank(settingValue.getValue().toString())) {
        deletedUserLabels.put(language, settingValue.getValue().toString());
      }
    }
    return deletedUserLabels;
  }

  private List<String> getSupportedLanguages() {
    Collection<LocaleConfig> localeConfigs = localeConfigService.getLocalConfigs();
    if (localeConfigs == null || localeConfigs.isEmpty()) {
      return Collections.singletonList(getDefaultLanguage());
    } else {
      return localeConfigs.stream()
                          .map(LocaleConfig::getLocaleName)
                          .filter(language -> !StringUtils.equals(language, MACHINE_LOCALE_NAME))
                          .toList();
    }
  }

  private String getDefaultLanguage() {
    LocaleConfig defaultLocaleConfig = localeConfigService.getDefaultLocaleConfig();
    return defaultLocaleConfig == null ? Locale.ENGLISH.getLanguage() : defaultLocaleConfig.getLocaleName();
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
