/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package org.exoplatform.portal.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.picocontainer.Startable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.rest.annotations.RESTEndpoint;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.organization.idm.UserImpl;

@RunWith(MockitoJUnitRunner.Silent.class)
public class UserAccountConfirmationSettingsTest {

  private static final String STORED_LOGIN = "hr-sync";

  private static final String TYPED_LOGIN  = "HR-Sync";

  @Mock
  private OrganizationService organizationService;

  @Mock
  private UserHandler         userHandler;

  @Mock
  private UserACL             userACL;

  @Mock
  private Log                 log;

  @Before
  public void setUp() {
    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(userACL.getSuperUser()).thenReturn("root");
  }

  @Test
  public void testEnabledByDefault() {
    UserAccountConfirmationSettings settings = new UserAccountConfirmationSettings(null, organizationService, userACL);
    assertTrue(settings.isEnabled());
    assertTrue(settings.isConfiguredEnabled());
    assertFalse(settings.hasExemptedUsers());
  }

  @Test
  public void testConfiguredValuesAreRead() {
    InitParams params = new InitParams();
    params.addParameter(valueParam(UserAccountConfirmationSettings.ENABLED_PARAM, "false"));
    params.addParameter(valueParam(UserAccountConfirmationSettings.EXEMPTED_USERS_PARAM, " hr-sync , , deprovisioning "));
    UserAccountConfirmationSettings settings = new UserAccountConfirmationSettings(params, organizationService, userACL);
    assertFalse(settings.isEnabled());
    assertFalse(settings.isConfiguredEnabled());
    assertTrue(settings.hasExemptedUsers());
  }

  @Test
  public void testStartupWarnsOnlyWhenConfiguredOff() {
    newSettings(true, Collections.emptyList(), true).start();
    verify(log, never()).warn(anyString());

    newSettings(false, Collections.emptyList(), true).start();
    verify(log, times(1)).warn(anyString());
  }

  /**
   * The container creates at boot only the components it starts: without it,
   * the switch's MBean and the startup WARN would only exist after the first
   * guarded request on the node. Kills the mutant that drops Startable.
   */
  @Test
  public void testStartedWithTheContainer() {
    assertTrue(Startable.class.isAssignableFrom(UserAccountConfirmationSettings.class));
  }

  @Test
  public void testRuntimeSwitchIsLoggedAndNotPersisted() {
    UserAccountConfirmationSettings settings = newSettings(true, Collections.emptyList(), true);

    settings.setEnabled(false);
    assertFalse(settings.isEnabled());
    verify(log).warn(anyString(), eq(true), eq(false), eq(true));

    settings.setEnabled(true);
    assertTrue(settings.isEnabled());
    verify(log).warn(anyString(), eq(false), eq(true), eq(true));

    settings.setEnabled(false);
    // A restart builds the component again from the same configuration
    UserAccountConfirmationSettings restarted = newSettings(settings.isConfiguredEnabled(), Collections.emptyList(), true);
    assertTrue(restarted.isEnabled());
  }

  @Test
  public void testSwitchIsVolatile() throws Exception {
    Field field = UserAccountConfirmationSettings.class.getDeclaredField("enabled");
    assertTrue("The switch is written by the JMX thread and read by request threads",
               Modifier.isVolatile(field.getModifiers()));
  }

  /**
   * The component must never be served by the REST management provider: an
   * unattended administrator session could then switch the confirmation off.
   * The switch itself is a JMX attribute and operation.
   */
  @Test
  public void testExposedThroughJmxOnlyNeverThroughRest() throws Exception {
    assertFalse(UserAccountConfirmationSettings.class.isAnnotationPresent(RESTEndpoint.class));
    assertTrue(UserAccountConfirmationSettings.class.isAnnotationPresent(Managed.class));
    Method setter = UserAccountConfirmationSettings.class.getMethod("setEnabled", boolean.class);
    Method getter = UserAccountConfirmationSettings.class.getMethod("isEnabled");
    assertTrue(setter.isAnnotationPresent(Managed.class));
    assertTrue(getter.isAnnotationPresent(Managed.class));
  }

  @Test
  public void testEmptyDefaultExemptsNobodyWithoutLookup() {
    UserAccountConfirmationSettings settings = newSettings(true, Collections.emptyList(), true);
    assertFalse(settings.isExempted(STORED_LOGIN));
    verifyNoInteractions(userHandler);
  }

  @Test
  public void testListedAccountIsExempted() throws Exception {
    stubUser(STORED_LOGIN, STORED_LOGIN, true);
    UserAccountConfirmationSettings settings = newSettings(true, List.of(STORED_LOGIN), true);
    assertTrue(settings.isExempted(STORED_LOGIN));
  }

  @Test
  public void testUnlistedAccountIsNotExempted() throws Exception {
    stubUser("john", "john", true);
    UserAccountConfirmationSettings settings = newSettings(true, List.of(STORED_LOGIN), true);
    assertFalse(settings.isExempted("john"));
  }

  @Test
  public void testAccountCreatedAfterStartupIsExemptedWithoutRestart() throws Exception {
    UserAccountConfirmationSettings settings = newSettings(true, List.of(STORED_LOGIN), true);
    // Startup reads the list and resolves nothing
    verifyNoInteractions(userHandler);
    assertFalse(settings.isExempted(STORED_LOGIN));

    stubUser(STORED_LOGIN, STORED_LOGIN, true);
    assertTrue(settings.isExempted(STORED_LOGIN));
  }

  @Test
  public void testListedSuperUserIsNeverExempted() throws Exception {
    stubUser("root", "root", true);
    stubUser("ROOT", "root", true);
    UserAccountConfirmationSettings settings = newSettings(true, Arrays.asList("root", "ROOT", "Root"), true);
    assertFalse(settings.isExempted("root"));
    assertFalse(settings.isExempted("ROOT"));
  }

  @Test
  public void testListedLoginMatchesStoredNameUnderDefaultCaseRule() throws Exception {
    stubUser(TYPED_LOGIN, STORED_LOGIN, true);
    stubUser(STORED_LOGIN, STORED_LOGIN, true);
    UserAccountConfirmationSettings settings = newSettings(true, List.of(TYPED_LOGIN), true);
    assertTrue(settings.isExempted(TYPED_LOGIN));
    assertTrue(settings.isExempted(STORED_LOGIN));
  }

  @Test
  public void testListedLoginExemptsNobodyInExactCaseMode() throws Exception {
    // In exact mode the store finds no account under another case
    stubUser(STORED_LOGIN, STORED_LOGIN, true);
    UserAccountConfirmationSettings settings = newSettings(true, List.of(TYPED_LOGIN), false);
    assertFalse(settings.isExempted(STORED_LOGIN));
    assertFalse(settings.isExempted(TYPED_LOGIN));
  }

  /**
   * The list is matched against the account's stored name, never the login
   * as the client typed it
   */
  @Test
  public void testMatchIsOnTheStoredNameNotTheTypedLogin() throws Exception {
    stubUser(TYPED_LOGIN, STORED_LOGIN, true);
    stubUser(STORED_LOGIN, STORED_LOGIN, true);
    UserAccountConfirmationSettings settings = newSettings(true, List.of(TYPED_LOGIN), false);
    assertFalse(settings.isExempted(TYPED_LOGIN));
  }

  @Test
  public void testListedUnknownLoginExemptsNobody() {
    UserAccountConfirmationSettings settings = newSettings(true, List.of("ghost"), true);
    assertFalse(settings.isExempted("ghost"));
  }

  @Test
  public void testListedDisabledAccountIsNotExempted() throws Exception {
    stubUser(STORED_LOGIN, STORED_LOGIN, false);
    UserAccountConfirmationSettings settings = newSettings(true, List.of(STORED_LOGIN), true);
    assertFalse(settings.isExempted(STORED_LOGIN));
  }

  /**
   * The user cache keys an entry by the login as typed while every eviction
   * uses the stored name: an integration signed in as HR-Sync still finds its
   * stale entry once hr-sync is deactivated, so the account is read again by
   * its stored name before being exempted.
   */
  @Test
  public void testDeactivatedAccountIsRefusedOnNextRequestWhateverTheStaleEntry() throws Exception {
    stubUser(TYPED_LOGIN, STORED_LOGIN, true);
    stubUser(STORED_LOGIN, STORED_LOGIN, true);
    UserAccountConfirmationSettings settings = newSettings(true, List.of(STORED_LOGIN), true);
    assertTrue(settings.isExempted(TYPED_LOGIN));

    // hr-sync deactivated: the eviction hits the stored name only
    when(userHandler.findUserByName(STORED_LOGIN, UserStatus.ENABLED)).thenReturn(null);
    assertFalse(settings.isExempted(TYPED_LOGIN));
    verify(userHandler, times(2)).findUserByName(STORED_LOGIN, UserStatus.ENABLED);
  }

  @Test
  public void testFailedLookupExemptsNobodyAndWarns() throws Exception {
    when(userHandler.findUserByName(anyString(), any())).thenThrow(new IllegalStateException("directory down"));
    UserAccountConfirmationSettings settings = newSettings(true, List.of(STORED_LOGIN), true);
    assertFalse(settings.isExempted(STORED_LOGIN));
    verify(log).warn(anyString(), eq(STORED_LOGIN), any(IllegalStateException.class));
  }

  @Test
  public void testBlankUserIsNotExempted() {
    UserAccountConfirmationSettings settings = newSettings(true, List.of(STORED_LOGIN), true);
    assertFalse(settings.isExempted(null));
    assertFalse(settings.isExempted(" "));
    verifyNoInteractions(userHandler);
  }

  private UserAccountConfirmationSettings newSettings(boolean enabled, List<String> exemptedUsers, boolean caseInsensitive) {
    UserAccountConfirmationSettings settings = new UserAccountConfirmationSettings(enabled,
                                                                                   exemptedUsers,
                                                                                   caseInsensitive,
                                                                                   organizationService,
                                                                                   userACL,
                                                                                   log);
    assertNotNull(settings);
    return settings;
  }

  private void stubUser(String requestedName, String storedName, boolean enabled) throws Exception {
    UserImpl user = new UserImpl(storedName);
    user.setEnabled(enabled);
    // The store filters by status: a disabled account is not found as ENABLED
    when(userHandler.findUserByName(requestedName, UserStatus.ENABLED)).thenReturn(enabled ? user : null);
  }

  private ValueParam valueParam(String name, String value) {
    ValueParam param = new ValueParam();
    param.setName(name);
    param.setValue(value);
    return param;
  }

}
