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
package org.exoplatform.account.setup.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.idm.UserImpl;

@RunWith(MockitoJUnitRunner.class)
public class AccountSetupServiceTest {

  @Mock
  private SettingService      settingService;

  @Mock
  private OrganizationService organizationService;

  @Test
  public void shouldNotSkipAccountSetupWhenNothingSet() {
    // Given
    PropertyManager.setProperty(AccountSetupService.ACCOUNT_SETUP_SKIP_PROPERTY, "");
    PropertyManager.setProperty(PropertyManager.DEVELOPING, "");
    when(settingService.get(any(Context.class), any(Scope.class), eq(AccountSetupService.ACCOUNT_SETUP_NODE))).thenReturn(null);

    AccountSetupService accountSetupService = new AccountSetupService(settingService, organizationService);

    // When
    boolean mustSkip = accountSetupService.mustSkipAccountSetup();

    // Then
    assertFalse(mustSkip);
  }

  @Test
  public void shouldSkipAccountSetupWhenPropertyIsSet() {
    // Given
    PropertyManager.setProperty(AccountSetupService.ACCOUNT_SETUP_SKIP_PROPERTY, "true");

    AccountSetupService accountSetupService = new AccountSetupService(settingService, organizationService);

    // When
    boolean mustSkip = accountSetupService.mustSkipAccountSetup();

    // Then
    assertTrue(mustSkip);

    PropertyManager.setProperty(AccountSetupService.ACCOUNT_SETUP_SKIP_PROPERTY, "");
  }

  @Test
  public void shouldSkipAccountSetupWhenAlreadyDoneOrSkipped() {
    // Given
    when(settingService.get(any(Context.class),
                            any(Scope.class),
                            eq(AccountSetupService.ACCOUNT_SETUP_NODE))).thenReturn(new SettingValue("true"));

    AccountSetupService accountSetupService = new AccountSetupService(settingService, organizationService);

    // When
    boolean mustSkip = accountSetupService.mustSkipAccountSetup();

    // Then
    assertTrue(mustSkip);
  }

  @Test
  public void shouldSkipAccountSetupWhenDevelopingModeIsSet() {
    // Given
    PropertyManager.setProperty(PropertyManager.DEVELOPING, "true");

    AccountSetupService accountSetupService = new AccountSetupService(settingService, organizationService);

    // When
    boolean mustSkip = accountSetupService.mustSkipAccountSetup();

    // Then
    assertTrue(mustSkip);

    PropertyManager.setProperty(PropertyManager.DEVELOPING, "");
  }

  @Test
  public void shouldSkipAccountSetupWhenSkipSetup() {
    // Given
    AccountSetupService accountSetupService = new AccountSetupService(settingService, organizationService);

    // When
    boolean mustSkipBefore = accountSetupService.mustSkipAccountSetup();
    accountSetupService.setSkipSetup(true);
    boolean mustSkipAfter = accountSetupService.mustSkipAccountSetup();

    // Then
    assertFalse(mustSkipBefore);
    assertTrue(mustSkipAfter);
  }

  /**
   * The first-start setup creates the first administrator through the
   * organization service directly, outside every REST endpoint guarded by
   * the one-time-password confirmation: a new platform always gets an
   * administrator with a real mailbox, without any code.
   */
  @Test
  public void shouldCreateFirstAdministratorOutsideTheConfirmationGuard() throws Exception {
    OrganizationService orgService = mock(OrganizationService.class,
                                          withSettings().extraInterfaces(ComponentRequestLifecycle.class));
    UserHandler userHandler = mock(UserHandler.class);
    GroupHandler groupHandler = mock(GroupHandler.class);
    MembershipHandler membershipHandler = mock(MembershipHandler.class);
    MembershipTypeHandler membershipTypeHandler = mock(MembershipTypeHandler.class);
    when(orgService.getUserHandler()).thenReturn(userHandler);
    when(orgService.getGroupHandler()).thenReturn(groupHandler);
    when(orgService.getMembershipHandler()).thenReturn(membershipHandler);
    when(orgService.getMembershipTypeHandler()).thenReturn(membershipTypeHandler);
    when(userHandler.findUserByName("root")).thenReturn(new UserImpl("root"));
    UserImpl admin = new UserImpl("admin");
    when(userHandler.createUserInstance("admin")).thenReturn(admin);
    Group administrators = mock(Group.class);
    when(groupHandler.findGroupById("/platform/administrators")).thenReturn(administrators);
    MembershipType anyType = mock(MembershipType.class);
    when(membershipTypeHandler.findMembershipType("*")).thenReturn(anyType);

    AccountSetupService accountSetupService = new AccountSetupService(settingService, orgService);
    accountSetupService.createAccount("admin", "Admin", "Admin", "admin@example.com", "password1", "rootPassword");

    verify(userHandler).createUser(admin, true);
    verify(membershipHandler).linkMembership(admin, administrators, anyType, true);
    assertEquals("admin@example.com", ((User) admin).getEmail());
    assertTrue(accountSetupService.mustSkipAccountSetup());
  }
}
