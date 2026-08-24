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

import static io.meeds.portal.security.service.SecuritySettingService.ACCESS_TYPE_MODIFIED;
import static io.meeds.portal.security.service.SecuritySettingService.ACCOUNT_DEACTIVATION_ENABLED_PARAM;
import static io.meeds.portal.security.service.SecuritySettingService.ACCOUNT_DEACTIVATION_MODIFIED;
import static io.meeds.portal.security.service.SecuritySettingService.ACCOUNT_DELETION_ANONYMIZATION_ENABLED_PARAM;
import static io.meeds.portal.security.service.SecuritySettingService.ACCOUNT_DELETION_ANONYMIZATION_MODIFIED;
import static io.meeds.portal.security.service.SecuritySettingService.ACCOUNT_DELETION_ENABLED_PARAM;
import static io.meeds.portal.security.service.SecuritySettingService.ACCOUNT_DELETION_MODIFIED;
import static io.meeds.portal.security.service.SecuritySettingService.DEFAULT_ACCOUNT_DEACTIVATION;
import static io.meeds.portal.security.service.SecuritySettingService.DEFAULT_ACCOUNT_DELETION;
import static io.meeds.portal.security.service.SecuritySettingService.DEFAULT_ACCOUNT_DELETION_ANONYMIZATION;
import static io.meeds.portal.security.service.SecuritySettingService.DEFAULT_DELETED_USER_LABEL;
import static io.meeds.portal.security.service.SecuritySettingService.DEFAULT_REGISTRATION_EXTERNAL_USER;
import static io.meeds.portal.security.service.SecuritySettingService.DEFAULT_REGISTRATION_TYPE;
import static io.meeds.portal.security.service.SecuritySettingService.DELETED_USER_LABELS_MODIFIED;
import static io.meeds.portal.security.service.SecuritySettingService.DELETED_USER_LABEL_PARAM_PREFIX;
import static io.meeds.portal.security.service.SecuritySettingService.EXTERNAL_USER_REG_MODIFIED;
import static io.meeds.portal.security.service.SecuritySettingService.EXTRA_GROUPS_SEPARATOR;
import static io.meeds.portal.security.service.SecuritySettingService.REGISTRATION_EXTERNAL_USER_PARAM;
import static io.meeds.portal.security.service.SecuritySettingService.REGISTRATION_EXTRA_GROUPS_PARAM;
import static io.meeds.portal.security.service.SecuritySettingService.REGISTRATION_TYPE_PARAM;
import static io.meeds.portal.security.service.SecuritySettingService.SECURITY_CONTEXT;
import static io.meeds.portal.security.service.SecuritySettingService.SECURITY_SCOPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;

import io.meeds.portal.security.constant.UserRegistrationType;
import io.meeds.portal.security.model.RegistrationSetting;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({ "unchecked", "rawtypes" })
public class SecuritySettingServiceTest {

  @Mock
  private SettingService         settingService;

  @Mock
  private ListenerService        listenerService;

  @Mock
  private LocaleConfigService    localeConfigService;

  @Mock
  private LocaleConfig           enLocaleConfig;

  @Mock
  private LocaleConfig           frLocaleConfig;

  private SecuritySettingService securitySettingService;

  @Before
  public void setUp() {
    lenient().when(enLocaleConfig.getLocaleName()).thenReturn("en");
    lenient().when(frLocaleConfig.getLocaleName()).thenReturn("fr");
    lenient().when(localeConfigService.getLocalConfigs()).thenReturn(Arrays.asList(enLocaleConfig, frLocaleConfig));
    lenient().when(localeConfigService.getDefaultLocaleConfig()).thenReturn(enLocaleConfig);
    securitySettingService = new SecuritySettingService(settingService, listenerService, localeConfigService);
  }

  @Test
  public void testGetRegistrationSetting() {
    RegistrationSetting registrationSetting = securitySettingService.getRegistrationSetting();
    assertNotNull(registrationSetting); // NOSONAR
    assertEquals(DEFAULT_REGISTRATION_TYPE, registrationSetting.getType());
    assertEquals(DEFAULT_REGISTRATION_EXTERNAL_USER, registrationSetting.isExternalUser());
    assertNotNull(registrationSetting.getExtraGroupIds());
    assertEquals(0, registrationSetting.getExtraGroupIds().length);
    assertEquals(DEFAULT_ACCOUNT_DEACTIVATION, registrationSetting.isAccountDeactivationEnabled());
    assertEquals(DEFAULT_ACCOUNT_DELETION, registrationSetting.isAccountDeletionEnabled());
    assertEquals(DEFAULT_ACCOUNT_DELETION_ANONYMIZATION, registrationSetting.isAccountDeletionAnonymizationEnabled());
    assertNotNull(registrationSetting.getDeletedUserLabels());
    assertTrue(registrationSetting.getDeletedUserLabels().isEmpty());
  }

  @Test
  public void testSaveRegistrationSetting() {
    securitySettingService.saveRegistrationSetting(new RegistrationSetting());
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT), eq(SECURITY_SCOPE), anyString(), any());
  }

  @Test
  public void testSaveRegistrationSettingWithNoDefault() {
    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            REGISTRATION_TYPE_PARAM)).thenReturn((SettingValue) SettingValue.create(UserRegistrationType.RESTRICTED.name()));
    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            REGISTRATION_EXTERNAL_USER_PARAM)).thenReturn((SettingValue) SettingValue.create(true));
    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            ACCOUNT_DEACTIVATION_ENABLED_PARAM)).thenReturn((SettingValue) SettingValue.create(true));
    securitySettingService.saveRegistrationSetting(new RegistrationSetting());
    verify(settingService, times(4)).set(eq(SECURITY_CONTEXT), eq(SECURITY_SCOPE), anyString(), any());
  }

  @Test
  public void testGetRegistrationType() {
    UserRegistrationType registrationType = securitySettingService.getRegistrationType();
    assertNotNull(registrationType);
    assertEquals(DEFAULT_REGISTRATION_TYPE, registrationType);

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            REGISTRATION_TYPE_PARAM)).thenReturn((SettingValue) SettingValue.create(UserRegistrationType.OPEN.name()));

    registrationType = securitySettingService.getRegistrationType();
    assertNotNull(registrationType);
    assertEquals(UserRegistrationType.OPEN, registrationType);

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            REGISTRATION_TYPE_PARAM)).thenReturn((SettingValue) SettingValue.create(UserRegistrationType.RESTRICTED.name()));

    registrationType = securitySettingService.getRegistrationType();
    assertNotNull(registrationType);
    assertEquals(UserRegistrationType.RESTRICTED, registrationType);
  }

  @Test
  public void testSaveRegistrationType() throws Exception {
    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            REGISTRATION_TYPE_PARAM)).thenReturn((SettingValue) SettingValue.create(UserRegistrationType.RESTRICTED.name()));
    securitySettingService.saveRegistrationType(UserRegistrationType.OPEN);
    verify(settingService,
           times(1)).set(eq(SECURITY_CONTEXT),
                            eq(SECURITY_SCOPE),
                               eq(REGISTRATION_TYPE_PARAM),
                         argThat(args -> StringUtils.equals(args.getValue().toString(), UserRegistrationType.OPEN.name())));
    verify(listenerService, times(1)).broadcast(ACCESS_TYPE_MODIFIED, null, UserRegistrationType.OPEN);

    securitySettingService.saveRegistrationType(UserRegistrationType.RESTRICTED);
    verify(settingService,
           never()).set(eq(SECURITY_CONTEXT),
                        eq(SECURITY_SCOPE),
                        eq(REGISTRATION_TYPE_PARAM),
                        argThat(args -> StringUtils.equals(args.getValue().toString(), UserRegistrationType.RESTRICTED.name())));

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            REGISTRATION_TYPE_PARAM)).thenReturn((SettingValue) SettingValue.create(UserRegistrationType.OPEN.name()));
    securitySettingService.saveRegistrationType(UserRegistrationType.RESTRICTED);
    verify(settingService,
           times(1)).set(eq(SECURITY_CONTEXT),
                         eq(SECURITY_SCOPE),
                         eq(REGISTRATION_TYPE_PARAM),
                         argThat(args -> StringUtils.equals(args.getValue().toString(), UserRegistrationType.RESTRICTED.name())));
    verify(listenerService, times(1)).broadcast(ACCESS_TYPE_MODIFIED, null, UserRegistrationType.RESTRICTED);
  }

  @Test
  public void testIsRegistrationExternalUser() {
    assertEquals(DEFAULT_REGISTRATION_EXTERNAL_USER, securitySettingService.isRegistrationExternalUser());

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            REGISTRATION_EXTERNAL_USER_PARAM)).thenReturn((SettingValue) SettingValue.create(true));
    assertTrue(securitySettingService.isRegistrationExternalUser());

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            REGISTRATION_EXTERNAL_USER_PARAM)).thenReturn((SettingValue) SettingValue.create(false));
    assertFalse(securitySettingService.isRegistrationExternalUser());
  }

  @Test
  public void testSaveRegistrationExternalUser() throws Exception {
    securitySettingService.saveRegistrationExternalUser(true);
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(REGISTRATION_EXTERNAL_USER_PARAM),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(), "true")));
    verify(listenerService, times(1)).broadcast(EXTERNAL_USER_REG_MODIFIED, null, true);

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            REGISTRATION_EXTERNAL_USER_PARAM)).thenReturn((SettingValue) SettingValue.create("true"));
    securitySettingService.saveRegistrationExternalUser(false);
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(REGISTRATION_EXTERNAL_USER_PARAM),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(), "false")));
    verify(listenerService, times(1)).broadcast(EXTERNAL_USER_REG_MODIFIED, null, false);
  }

  @Test
  public void testIsAccountDeactivationEnabled() {
    assertEquals(DEFAULT_ACCOUNT_DEACTIVATION, securitySettingService.isAccountDeactivationEnabled());

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            ACCOUNT_DEACTIVATION_ENABLED_PARAM)).thenReturn((SettingValue) SettingValue.create(true));
    assertTrue(securitySettingService.isAccountDeactivationEnabled());

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            ACCOUNT_DEACTIVATION_ENABLED_PARAM)).thenReturn((SettingValue) SettingValue.create(false));
    assertFalse(securitySettingService.isAccountDeactivationEnabled());
  }

  @Test
  public void testSaveAccountDeactivationEnabled() throws Exception {
    securitySettingService.saveAccountDeactivationEnabled(true);
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(ACCOUNT_DEACTIVATION_ENABLED_PARAM),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(), "true")));
    verify(listenerService, times(1)).broadcast(ACCOUNT_DEACTIVATION_MODIFIED, null, true);

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            ACCOUNT_DEACTIVATION_ENABLED_PARAM)).thenReturn((SettingValue) SettingValue.create("true"));
    securitySettingService.saveAccountDeactivationEnabled(false);
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(ACCOUNT_DEACTIVATION_ENABLED_PARAM),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(), "false")));
    verify(listenerService, times(1)).broadcast(ACCOUNT_DEACTIVATION_MODIFIED, null, false);
  }

  @Test
  public void testGetRegistrationGroupIds() {
    assertEquals(0, securitySettingService.getRegistrationGroupIds().length);

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            REGISTRATION_EXTRA_GROUPS_PARAM)).thenReturn((SettingValue) SettingValue.create(""));
    assertEquals(0, securitySettingService.getRegistrationGroupIds().length);

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            REGISTRATION_EXTRA_GROUPS_PARAM)).thenReturn((SettingValue) SettingValue.create("group1,group2"));
    String[] registrationGroupIds = securitySettingService.getRegistrationGroupIds();
    assertNotNull(registrationGroupIds);
    assertEquals(2, registrationGroupIds.length);
    assertEquals("group1", registrationGroupIds[0]);
    assertEquals("group2", registrationGroupIds[1]);
  }

  @Test
  public void testSaveRegistrationGroupIds() {
    securitySettingService.saveRegistrationGroupIds(new String[0]);
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(REGISTRATION_EXTRA_GROUPS_PARAM),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(), "")));
    securitySettingService.saveRegistrationGroupIds(new String[] { "/group1", "/group2" });
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(REGISTRATION_EXTRA_GROUPS_PARAM),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(),
                                                                            "/group1" + EXTRA_GROUPS_SEPARATOR + "/group2")));
  }

  @Test
  public void testIsAccountDeletionEnabled() {
    assertEquals(DEFAULT_ACCOUNT_DELETION, securitySettingService.isAccountDeletionEnabled());

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            ACCOUNT_DELETION_ENABLED_PARAM)).thenReturn((SettingValue) SettingValue.create(true));
    assertTrue(securitySettingService.isAccountDeletionEnabled());

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            ACCOUNT_DELETION_ENABLED_PARAM)).thenReturn((SettingValue) SettingValue.create(false));
    assertFalse(securitySettingService.isAccountDeletionEnabled());
  }

  @Test
  public void testSaveAccountDeletionEnabled() throws Exception {
    securitySettingService.saveAccountDeletionEnabled(true);
    verify(settingService, never()).set(eq(SECURITY_CONTEXT),
                                        eq(SECURITY_SCOPE),
                                        eq(ACCOUNT_DELETION_ENABLED_PARAM),
                                        any());

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            ACCOUNT_DEACTIVATION_ENABLED_PARAM)).thenReturn((SettingValue) SettingValue.create(true));
    securitySettingService.saveAccountDeletionEnabled(true);
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(ACCOUNT_DELETION_ENABLED_PARAM),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(), "true")));
    verify(listenerService, times(1)).broadcast(ACCOUNT_DELETION_MODIFIED, null, true);

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            ACCOUNT_DELETION_ENABLED_PARAM)).thenReturn((SettingValue) SettingValue.create("true"));
    securitySettingService.saveAccountDeletionEnabled(false);
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(ACCOUNT_DELETION_ENABLED_PARAM),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(), "false")));
    verify(listenerService, times(1)).broadcast(ACCOUNT_DELETION_MODIFIED, null, false);
  }

  @Test
  public void testSaveAccountDeactivationEnabledCascadesDeletionOff() throws Exception {
    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            ACCOUNT_DEACTIVATION_ENABLED_PARAM)).thenReturn((SettingValue) SettingValue.create(true));
    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            ACCOUNT_DELETION_ENABLED_PARAM)).thenReturn((SettingValue) SettingValue.create(true));
    securitySettingService.saveAccountDeactivationEnabled(false);
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(ACCOUNT_DEACTIVATION_ENABLED_PARAM),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(), "false")));
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(ACCOUNT_DELETION_ENABLED_PARAM),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(), "false")));
    verify(listenerService, times(1)).broadcast(ACCOUNT_DEACTIVATION_MODIFIED, null, false);
    verify(listenerService, times(1)).broadcast(ACCOUNT_DELETION_MODIFIED, null, false);
  }

  @Test
  public void testIsAccountDeletionAnonymizationEnabled() {
    assertEquals(DEFAULT_ACCOUNT_DELETION_ANONYMIZATION, securitySettingService.isAccountDeletionAnonymizationEnabled());

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            ACCOUNT_DELETION_ANONYMIZATION_ENABLED_PARAM)).thenReturn((SettingValue) SettingValue.create(true));
    assertTrue(securitySettingService.isAccountDeletionAnonymizationEnabled());

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            ACCOUNT_DELETION_ANONYMIZATION_ENABLED_PARAM)).thenReturn((SettingValue) SettingValue.create(false));
    assertFalse(securitySettingService.isAccountDeletionAnonymizationEnabled());
  }

  @Test
  public void testSaveAccountDeletionAnonymizationEnabled() throws Exception {
    securitySettingService.saveAccountDeletionAnonymizationEnabled(true);
    verify(settingService, never()).set(eq(SECURITY_CONTEXT),
                                        eq(SECURITY_SCOPE),
                                        eq(ACCOUNT_DELETION_ANONYMIZATION_ENABLED_PARAM),
                                        any());

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            ACCOUNT_DELETION_ENABLED_PARAM)).thenReturn((SettingValue) SettingValue.create(true));
    securitySettingService.saveAccountDeletionAnonymizationEnabled(true);
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(ACCOUNT_DELETION_ANONYMIZATION_ENABLED_PARAM),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(), "true")));
    verify(listenerService, times(1)).broadcast(ACCOUNT_DELETION_ANONYMIZATION_MODIFIED, null, true);

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            ACCOUNT_DELETION_ANONYMIZATION_ENABLED_PARAM)).thenReturn((SettingValue) SettingValue.create("true"));
    securitySettingService.saveAccountDeletionAnonymizationEnabled(false);
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(ACCOUNT_DELETION_ANONYMIZATION_ENABLED_PARAM),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(), "false")));
    verify(listenerService, times(1)).broadcast(ACCOUNT_DELETION_ANONYMIZATION_MODIFIED, null, false);
  }

  @Test
  public void testSaveAccountDeletionEnabledCascadesAnonymizationOff() throws Exception {
    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            ACCOUNT_DELETION_ENABLED_PARAM)).thenReturn((SettingValue) SettingValue.create(true));
    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            ACCOUNT_DELETION_ANONYMIZATION_ENABLED_PARAM)).thenReturn((SettingValue) SettingValue.create(true));
    securitySettingService.saveAccountDeletionEnabled(false);
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(ACCOUNT_DELETION_ENABLED_PARAM),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(), "false")));
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(ACCOUNT_DELETION_ANONYMIZATION_ENABLED_PARAM),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(), "false")));
    verify(listenerService, times(1)).broadcast(ACCOUNT_DELETION_ANONYMIZATION_MODIFIED, null, false);
  }

  @Test
  public void testGetDeletedUserLabels() {
    assertNotNull(securitySettingService.getDeletedUserLabels());
    assertTrue(securitySettingService.getDeletedUserLabels().isEmpty());

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            DELETED_USER_LABEL_PARAM_PREFIX + "en")).thenReturn((SettingValue) SettingValue.create("Gone"));
    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            DELETED_USER_LABEL_PARAM_PREFIX + "fr")).thenReturn((SettingValue) SettingValue.create("Parti"));
    Map<String, String> deletedUserLabels = securitySettingService.getDeletedUserLabels();
    assertEquals(2, deletedUserLabels.size());
    assertEquals("Gone", deletedUserLabels.get("en"));
    assertEquals("Parti", deletedUserLabels.get("fr"));
  }

  @Test
  public void testGetDeletedUserLabelByLocale() {
    assertEquals(DEFAULT_DELETED_USER_LABEL, securitySettingService.getDeletedUserLabel(Locale.FRENCH));

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            DELETED_USER_LABEL_PARAM_PREFIX + "en")).thenReturn((SettingValue) SettingValue.create("Gone"));
    assertEquals("Gone", securitySettingService.getDeletedUserLabel(Locale.FRENCH));
    assertEquals("Gone", securitySettingService.getDeletedUserLabel(null));

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            DELETED_USER_LABEL_PARAM_PREFIX + "fr")).thenReturn((SettingValue) SettingValue.create("Parti"));
    assertEquals("Parti", securitySettingService.getDeletedUserLabel(Locale.FRENCH));
    assertEquals("Gone", securitySettingService.getDeletedUserLabel(Locale.ENGLISH));
  }

  @Test
  public void testSaveDeletedUserLabels() throws Exception {
    securitySettingService.saveDeletedUserLabels(Collections.emptyMap());
    verify(settingService, never()).set(eq(SECURITY_CONTEXT),
                                        eq(SECURITY_SCOPE),
                                        eq(DELETED_USER_LABEL_PARAM_PREFIX + "en"),
                                        any());
    verify(settingService, never()).remove(eq(SECURITY_CONTEXT),
                                           eq(SECURITY_SCOPE),
                                           anyString());

    securitySettingService.saveDeletedUserLabels(Map.of("en", "Gone", "fr", " "));
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(DELETED_USER_LABEL_PARAM_PREFIX + "en"),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(), "Gone")));
    verify(settingService, times(1)).remove(SECURITY_CONTEXT,
                                            SECURITY_SCOPE,
                                            DELETED_USER_LABEL_PARAM_PREFIX + "fr");
    verify(listenerService, times(1)).broadcast(DELETED_USER_LABELS_MODIFIED, null, Map.of("en", "Gone"));

    when(settingService.get(SECURITY_CONTEXT,
                            SECURITY_SCOPE,
                            DELETED_USER_LABEL_PARAM_PREFIX + "en")).thenReturn((SettingValue) SettingValue.create("Gone"));
    securitySettingService.saveDeletedUserLabels(Map.of("en", "Gone"));
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(DELETED_USER_LABEL_PARAM_PREFIX + "en"),
                                         any());
  }

}
