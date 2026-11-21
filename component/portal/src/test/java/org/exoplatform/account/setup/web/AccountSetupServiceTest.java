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
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    when(settingService.getOrThrow(any(Context.class), any(Scope.class), eq(AccountSetupService.ACCOUNT_SETUP_NODE))).thenReturn(null);

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
    when(settingService.getOrThrow(any(Context.class),
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
    assertTrue(accountSetupService.createAccount("admin", "Admin", "Admin", "admin@example.com", "password1", "rootPassword"));

    verify(userHandler).createUser(admin, true);
    verify(membershipHandler).linkMembership(admin, administrators, anyType, true);
    assertEquals("admin@example.com", ((User) admin).getEmail());
    assertTrue(accountSetupService.mustSkipAccountSetup());
  }

  @Test
  public void shouldNotSkipAccountSetupOnInitialInstall() {
    PropertyManager.setProperty(AccountSetupService.ACCOUNT_SETUP_SKIP_PROPERTY, "");
    PropertyManager.setProperty(PropertyManager.DEVELOPING, "");
    when(settingService.getOrThrow(any(Context.class), any(Scope.class), eq(AccountSetupService.ACCOUNT_SETUP_NODE))).thenReturn(null);
    AccountSetupService accountSetupService = new AccountSetupService(settingService, organizationService);

    assertFalse(accountSetupService.mustSkipAccountSetup());
    assertTrue(accountSetupService.isAccountSetupAllowed());
  }

  @Test
  public void shouldSkipAccountSetupWhenTheStateCanNotBeRead() {
    PropertyManager.setProperty(AccountSetupService.ACCOUNT_SETUP_SKIP_PROPERTY, "");
    PropertyManager.setProperty(PropertyManager.DEVELOPING, "");
    when(settingService.getOrThrow(any(Context.class),
                                   any(Scope.class),
                                   eq(AccountSetupService.ACCOUNT_SETUP_NODE))).thenThrow(new IllegalStateException("read failed"));
    when(settingService.getFromStore(any(Context.class),
                                     any(Scope.class),
                                     eq(AccountSetupService.ACCOUNT_SETUP_NODE))).thenThrow(new IllegalStateException("read failed"));
    AccountSetupService accountSetupService = new AccountSetupService(settingService, organizationService);

    assertTrue(accountSetupService.mustSkipAccountSetup());
    assertFalse(accountSetupService.isAccountSetupAllowed());
  }

  @Test
  public void shouldLetALaterSuccessfulReadDecideAfterAFailedOne() {
    PropertyManager.setProperty(AccountSetupService.ACCOUNT_SETUP_SKIP_PROPERTY, "");
    PropertyManager.setProperty(PropertyManager.DEVELOPING, "");
    when(settingService.getOrThrow(any(Context.class),
                                   any(Scope.class),
                                   eq(AccountSetupService.ACCOUNT_SETUP_NODE))).thenThrow(new IllegalStateException("read failed"))
                                                                                 .thenReturn(null)
                                                                                 .thenReturn(new SettingValue("true"));
    AccountSetupService accountSetupService = new AccountSetupService(settingService, organizationService);

    assertTrue(accountSetupService.mustSkipAccountSetup());
    assertFalse(accountSetupService.mustSkipAccountSetup());
    assertTrue(accountSetupService.mustSkipAccountSetup());
    // Once done, the answer is kept
    assertTrue(accountSetupService.mustSkipAccountSetup());
    verify(settingService, times(3)).getOrThrow(any(Context.class),
                                                any(Scope.class),
                                                eq(AccountSetupService.ACCOUNT_SETUP_NODE));
  }

  @Test
  public void shouldRefuseTheAccountCreationOnceTheSetupIsDone() {
    PropertyManager.setProperty(AccountSetupService.ACCOUNT_SETUP_SKIP_PROPERTY, "");
    PropertyManager.setProperty(PropertyManager.DEVELOPING, "");
    when(settingService.getFromStore(any(Context.class),
                                   any(Scope.class),
                                   eq(AccountSetupService.ACCOUNT_SETUP_NODE))).thenReturn(new SettingValue("true"));

    AccountSetupService accountSetupService = new AccountSetupService(settingService, organizationService);
    boolean created = accountSetupService.createAccount("admin", "Admin", "Admin", "admin@example.com", "password1", "rootPassword");

    assertFalse(created);
    verify(organizationService, never()).getUserHandler();
    verify(settingService, never()).set(any(Context.class), any(Scope.class), any(String.class), any(SettingValue.class));
  }

  @Test
  public void shouldRefuseTheAccountCreationWhenTheStateCanNotBeRead() {
    PropertyManager.setProperty(AccountSetupService.ACCOUNT_SETUP_SKIP_PROPERTY, "");
    PropertyManager.setProperty(PropertyManager.DEVELOPING, "");
    when(settingService.getFromStore(any(Context.class),
                                   any(Scope.class),
                                   eq(AccountSetupService.ACCOUNT_SETUP_NODE))).thenThrow(new IllegalStateException("read failed"));

    AccountSetupService accountSetupService = new AccountSetupService(settingService, organizationService);
    boolean created = accountSetupService.createAccount("admin", "Admin", "Admin", "admin@example.com", "password1", "rootPassword");

    assertFalse(created);
    verify(organizationService, never()).getUserHandler();
    verify(settingService, never()).set(any(Context.class), any(Scope.class), any(String.class), any(SettingValue.class));
  }

  @Test
  public void shouldRefuseTheAccountCreationWhenOnlyTheCacheSaysNotDone() {
    PropertyManager.setProperty(AccountSetupService.ACCOUNT_SETUP_SKIP_PROPERTY, "");
    PropertyManager.setProperty(PropertyManager.DEVELOPING, "");
    when(settingService.getOrThrow(any(Context.class),
                                   any(Scope.class),
                                   eq(AccountSetupService.ACCOUNT_SETUP_NODE))).thenReturn(null);
    when(settingService.getFromStore(any(Context.class),
                                     any(Scope.class),
                                     eq(AccountSetupService.ACCOUNT_SETUP_NODE))).thenReturn(new SettingValue("true"));

    AccountSetupService accountSetupService = new AccountSetupService(settingService, organizationService);

    assertFalse(accountSetupService.mustSkipAccountSetup());
    assertFalse(accountSetupService.createAccount("admin", "Admin", "Admin", "admin@example.com", "password1", "rootPassword"));
    verify(organizationService, never()).getUserHandler();
  }

  @Test
  public void shouldCreateTheFirstAdministratorOnlyOnceWhenSubmittedTwiceAtTheSameTime() throws Exception {
    PropertyManager.setProperty(AccountSetupService.ACCOUNT_SETUP_SKIP_PROPERTY, "");
    PropertyManager.setProperty(PropertyManager.DEVELOPING, "");
    AtomicBoolean stored = new AtomicBoolean(false);
    when(settingService.getFromStore(any(Context.class),
                                     any(Scope.class),
                                     eq(AccountSetupService.ACCOUNT_SETUP_NODE))).thenAnswer(i -> stored.get() ? new SettingValue("true") : null);
    doAnswer(i -> {
      stored.set(true);
      return null;
    }).when(settingService).set(any(Context.class), any(Scope.class), any(String.class), any(SettingValue.class));

    OrganizationService orgService = mock(OrganizationService.class,
                                          withSettings().extraInterfaces(ComponentRequestLifecycle.class));
    UserHandler userHandler = mock(UserHandler.class);
    when(orgService.getUserHandler()).thenReturn(userHandler);
    when(orgService.getGroupHandler()).thenReturn(mock(GroupHandler.class));
    when(orgService.getMembershipHandler()).thenReturn(mock(MembershipHandler.class));
    when(orgService.getMembershipTypeHandler()).thenReturn(mock(MembershipTypeHandler.class));
    when(userHandler.findUserByName("root")).thenReturn(new UserImpl("root"));
    when(userHandler.createUserInstance(any(String.class))).thenAnswer(i -> new UserImpl(i.getArgument(0)));
    CountDownLatch release = new CountDownLatch(1);
    CountDownLatch reached = new CountDownLatch(1);
    AtomicInteger created = new AtomicInteger();
    doAnswer(i -> {
      created.incrementAndGet();
      reached.countDown();
      assertTrue(release.await(10, TimeUnit.SECONDS));
      return null;
    }).when(userHandler).createUser(any(User.class), eq(true));

    AccountSetupService accountSetupService = new AccountSetupService(settingService, orgService);
    AtomicInteger accepted = new AtomicInteger();
    Runnable submit = () -> {
      if (accountSetupService.createAccount("admin", "Admin", "Admin", "admin@example.com", "password1", "rootPassword")) {
        accepted.incrementAndGet();
      }
    };
    Thread first = new Thread(submit);
    first.start();
    assertTrue(reached.await(10, TimeUnit.SECONDS));
    Thread second = new Thread(submit);
    second.start();
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    while (second.getState() == Thread.State.NEW || second.getState() == Thread.State.RUNNABLE) {
      if (System.nanoTime() > deadline) {
        release.countDown();
        fail("The second submission neither blocked nor finished");
      }
      Thread.sleep(5);
    }
    Thread.sleep(100);
    release.countDown();
    first.join(10000);
    second.join(10000);

    assertFalse(first.isAlive() || second.isAlive());
    assertEquals(1, accepted.get());
    assertEquals(1, created.get());
  }
}
