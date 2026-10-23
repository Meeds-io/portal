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
package io.meeds.web.security.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.rest.UserAccount;
import org.exoplatform.portal.rest.UserAccountCreationException;
import org.exoplatform.portal.rest.UserAccountService;
import org.exoplatform.portal.rest.UserAccountUpdateException;
import org.exoplatform.portal.rest.UserFieldValidator;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.organization.idm.MembershipImpl;
import org.exoplatform.services.organization.idm.UserImpl;
import org.exoplatform.services.organization.idm.cache.CacheableMembershipHandlerImpl;
import org.exoplatform.services.organization.idm.cache.CacheableUserHandlerImpl;
import org.exoplatform.services.organization.idm.cache.CacheableUserProfileHandlerImpl;
import org.exoplatform.services.organization.impl.UserProfileImpl;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.web.login.recovery.ChangePasswordConnector;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;

@RunWith(MockitoJUnitRunner.class)
public class UserAccountServiceImplTest {

  private static final Pattern              GENERATED_LOGIN = Pattern.compile("^u[a-z0-9]{11}$");

  private static final String               EMAIL           = "jane.doe@example.com";

  @Mock
  private OtpService                        otpService;

  @Mock
  private OrganizationService               organizationService;

  @Mock
  private UserACL                           userACL;

  @Mock
  private LocaleConfigService               localeConfigService;

  @Mock
  private CacheableUserHandlerImpl          userHandler;

  @Mock
  private CacheableUserProfileHandlerImpl   userProfileHandler;

  @Mock
  private CacheableMembershipHandlerImpl    membershipHandler;

  @Mock
  private GroupHandler                      groupHandler;

  @Mock
  private MembershipTypeHandler             membershipTypeHandler;

  @Mock
  private ListAccess<User>                  usersByEmail;

  @Mock
  private PasswordRecoveryService           passwordRecoveryService;

  @Mock
  private ChangePasswordConnector           changePasswordConnector;

  @InjectMocks
  private RestartCountingUserAccountService userAccountService;

  /**
   * What the store holds after the creation, as the read-back finds it
   */
  private List<Membership>                  storedMemberships;

  private UserProfile                       storedProfile;

  private Set<String>                       existingLogins;

  private UserImpl                          createdUser;

  private int                               creationFailuresLeft;

  private String                            creationFailureMessage;

  @Before
  public void setUp() throws Exception {
    storedMemberships = new ArrayList<>();
    existingLogins = new HashSet<>();
    createdUser = null;
    creationFailuresLeft = 0;
    creationFailureMessage = null;

    when(organizationService.getUserHandler()).thenReturn(userHandler);
    lenient().when(organizationService.getUserProfileHandler()).thenReturn(userProfileHandler);
    lenient().when(organizationService.getMembershipHandler()).thenReturn(membershipHandler);
    lenient().when(organizationService.getGroupHandler()).thenReturn(groupHandler);
    lenient().when(organizationService.getMembershipTypeHandler()).thenReturn(membershipTypeHandler);
    lenient().when(userACL.getAdminMSType()).thenReturn("manager");

    LocaleConfig english = mock(LocaleConfig.class);
    lenient().when(english.getLocaleName()).thenReturn("en");
    LocaleConfig french = mock(LocaleConfig.class);
    lenient().when(french.getLocaleName()).thenReturn("fr");
    lenient().when(localeConfigService.getLocalConfigs()).thenReturn(List.of(english, french));

    lenient().when(userHandler.findUsersByQuery(any(), eq(UserStatus.ANY))).thenReturn(usersByEmail);
    lenient().when(usersByEmail.getSize()).thenReturn(0);
    lenient().when(userHandler.createUserInstance(anyString())).thenAnswer(invocation -> new UserImpl(invocation.getArgument(0)));
    // The pre-check and the read-back read the same store
    lenient().when(userHandler.findUserByName(anyString(), eq(UserStatus.ANY))).thenAnswer(invocation -> {
      String login = invocation.getArgument(0);
      if (createdUser != null && createdUser.getUserName().equals(login)) {
        return copy(createdUser);
      }
      return existingLogins.contains(login) ? new UserImpl(login) : null;
    });
    // The store stamps the creation date, grants the platform users
    // membership, and refuses the logins it already holds
    lenient().doAnswer(invocation -> {
      UserImpl user = invocation.getArgument(0);
      if (creationFailuresLeft > 0) {
        creationFailuresLeft--;
        throw new IllegalStateException("Identity operation error: ",
                                        new RuntimeException(creationFailureMessage));
      }
      if (existingLogins.contains(user.getUserName())) {
        throw new IllegalStateException("Identity operation error: ",
                                        new RuntimeException(UserAccountServiceImpl.IDENTITY_ALREADY_PRESENT_MESSAGE));
      }
      user.setCreatedDate(new Date());
      user.setEnabled(true);
      createdUser = user;
      storedMemberships.add(membership("member", user.getUserName(), UserAccountServiceImpl.PLATFORM_USERS_GROUP));
      return null;
    }).when(userHandler).createUser(any(), eq(true));
    lenient().doAnswer(invocation -> {
      if (createdUser != null && createdUser.getUserName().equals(invocation.getArgument(0))) {
        createdUser.setEnabled(invocation.getArgument(1));
      }
      return createdUser;
    }).when(userHandler).setEnabled(anyString(), any(Boolean.class), eq(true));
    lenient().when(membershipHandler.findMembershipsByUser(anyString())).thenAnswer(invocation -> new ArrayList<>(storedMemberships));
    lenient().when(membershipHandler.findMembershipByUserGroupAndType(anyString(), anyString(), anyString()))
             .thenAnswer(invocation -> storedMemberships.stream()
                                                        .filter(m -> m.getUserName().equals(invocation.getArgument(0))
                                                                     && m.getGroupId().equals(invocation.getArgument(1))
                                                                     && m.getMembershipType().equals(invocation.getArgument(2)))
                                                        .findFirst()
                                                        .orElse(null));
    lenient().doAnswer(invocation -> {
      storedMemberships.removeIf(m -> m.getId().equals(invocation.getArgument(0)));
      return null;
    }).when(membershipHandler).removeMembership(anyString(), eq(true));
    lenient().doAnswer(invocation -> {
      User user = invocation.getArgument(0);
      Group group = invocation.getArgument(1);
      MembershipType type = invocation.getArgument(2);
      storedMemberships.add(membership(type.getName(), user.getUserName(), group.getId()));
      return null;
    }).when(membershipHandler).linkMembership(any(), any(), any(), eq(true));
    lenient().when(groupHandler.findGroupById(anyString())).thenAnswer(invocation -> {
      Group group = mock(Group.class);
      lenient().when(group.getId()).thenReturn(invocation.getArgument(0));
      return group;
    });
    lenient().when(membershipTypeHandler.findMembershipType(anyString())).thenAnswer(invocation -> {
      MembershipType type = mock(MembershipType.class);
      lenient().when(type.getName()).thenReturn(invocation.getArgument(0));
      return type;
    });
    lenient().when(userProfileHandler.findUserProfileByName(anyString())).thenAnswer(invocation -> storedProfile);
    lenient().when(userProfileHandler.createUserProfileInstance(anyString()))
             .thenAnswer(invocation -> new UserProfileImpl(invocation.getArgument(0)));
    lenient().doAnswer(invocation -> {
      storedProfile = invocation.getArgument(0);
      return null;
    }).when(userProfileHandler).saveUserProfile(any(), eq(true));
  }

  @Test
  public void testConfirmSessionValidatesAndConsumesTheDedicatedMethodCode() throws Exception {
    userAccountService.confirmSession("admin", "12345");
    verify(otpService).validateAndConsumeOtp("admin", UserAccountService.OTP_METHOD, "12345");
  }

  @Test(expected = IllegalAccessException.class)
  public void testConfirmSessionRefusesAnInvalidCode() throws Exception {
    doThrow(new IllegalAccessException()).when(otpService)
                                         .validateAndConsumeOtp("admin", UserAccountService.OTP_METHOD, "00000");
    userAccountService.confirmSession("admin", "00000");
  }

  /**
   * A generated login is a letter and eleven lowercase letters or digits,
   * so that it never starts with a digit and passes the default username
   * policy; two draws differ
   */
  @Test
  public void testGenerateUsernameShape() {
    Set<String> drawn = new HashSet<>();
    for (int i = 0; i < 200; i++) {
      String login = userAccountService.generateUsername();
      assertTrue(login, GENERATED_LOGIN.matcher(login).matches());
      assertNull(login, new UserFieldValidator("userName", true, false).validate(Locale.ENGLISH, login));
      drawn.add(login);
    }
    assertEquals(200, drawn.size());
  }

  @Test
  public void testCreateUserGeneratesTheLoginAndStampsTheUiSource() throws Exception {
    String login = userAccountService.createUser(account(null, null, null, null), Locale.ENGLISH);

    assertTrue(login, GENERATED_LOGIN.matcher(login).matches());
    assertEquals(login, createdUser.getUserName());
    assertEquals(UserAccountService.CREATION_SOURCE_UI, createdUser.getCreationSource());
    assertNull(createdUser.getPassword());
    assertEquals("Jane", createdUser.getFirstName());
    assertEquals("Doe", createdUser.getLastName());
    assertEquals(EMAIL, createdUser.getEmail());
    assertTrue(createdUser.isEnabled());
    // One read-back, in a new transaction, after the caches were evicted
    assertEquals(1, userAccountService.restarts);
    verify(userHandler).clearCache(login);
    verify(userProfileHandler).clearCache(login);
    verify(membershipHandler).clearMembershipCache(login);
    verify(userHandler, never()).setEnabled(anyString(), any(Boolean.class), any(Boolean.class));
    verify(userProfileHandler, never()).saveUserProfile(any(), any(Boolean.class));
  }

  @Test
  public void testCreateUserWithAGivenLoginKeepsIt() throws Exception {
    String login = userAccountService.createUser(account("jdoe", null, null, null), Locale.ENGLISH);
    assertEquals("jdoe", login);
    assertEquals("jdoe", createdUser.getUserName());
  }

  @Test
  public void testCreateUserRefusesAGivenLoginThePolicyRefuses() throws Exception {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                              () -> userAccountService.createUser(account("Jdoe", null, null, null),
                                                                                  Locale.ENGLISH));
    assertTrue(e.getMessage(), e.getMessage().startsWith("USERNAME:"));
    verify(userHandler, never()).createUser(any(), any(Boolean.class));
  }

  /**
   * A policy whose length or pattern refuses the generated form refuses every
   * candidate: the administrator is told to type a login, no candidate is
   * tried against the store
   */
  @Test
  public void testCreateUserRefusesGenerationWhenThePolicyRefusesItsForm() throws Exception {
    userAccountService.setUsernameValidator(new UserFieldValidator("userName", true, false, 1, 5));

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                              () -> userAccountService.createUser(account(null, null, null, null),
                                                                                  Locale.ENGLISH));
    assertEquals(UserAccountService.USERNAME_GENERATION_NOT_ALLOWED, e.getMessage());
    verify(userHandler, never()).createUser(any(), any(Boolean.class));
    verify(userHandler, never()).findUserByName(anyString(), any());
  }

  /**
   * A collision seen by the pre-check and a login the store refuses, as the
   * veto of a deleted account, each cost one attempt and another candidate
   * is drawn
   */
  @Test
  public void testCreateUserRetriesAGeneratedLoginPastACollisionAndADeletedLogin() throws Exception {
    UserAccountServiceImpl spied = new RestartCountingUserAccountService() {
      private int draws;

      @Override
      public String generateUsername() {
        draws++;
        return draws == 1 ? "utakenbyalive" : super.generateUsername();
      }
    };
    inject(spied);
    existingLogins.add("utakenbyalive");
    creationFailuresLeft = 1;
    doAnswer(invocation -> {
      throw new IllegalStateException("Error executing preSave on listener",
                                      new ObjectAlreadyExistsException(invocation.getArgument(0),
                                                                       "Unable to create a previously deleted user"));
    }).doAnswer(invocation -> {
      UserImpl user = invocation.getArgument(0);
      user.setCreatedDate(new Date());
      user.setEnabled(true);
      createdUser = user;
      storedMemberships.add(membership("member", user.getUserName(), UserAccountServiceImpl.PLATFORM_USERS_GROUP));
      return null;
    }).when(userHandler).createUser(any(), eq(true));

    String login = spied.createUser(account(null, null, null, null), Locale.ENGLISH);

    assertTrue(login, GENERATED_LOGIN.matcher(login).matches());
    assertTrue(!"utakenbyalive".equals(login));
    verify(userHandler, times(2)).createUser(any(), eq(true));
  }

  @Test
  public void testCreateUserStopsAfterFiveCollisions() throws Exception {
    when(userHandler.findUserByName(anyString(), eq(UserStatus.ANY))).thenAnswer(invocation -> new UserImpl(invocation.getArgument(0)));

    UserAccountCreationException e = assertThrows(UserAccountCreationException.class,
                                                  () -> userAccountService.createUser(account(null, null, null, null),
                                                                                      Locale.ENGLISH));
    assertEquals(UserAccountService.USER_USERNAME_GENERATION_EXHAUSTED, e.getMessage());
    verify(userHandler, times(UserAccountServiceImpl.GENERATION_ATTEMPTS)).findUserByName(anyString(), eq(UserStatus.ANY));
    verify(userHandler, never()).createUser(any(), any(Boolean.class));
  }

  @Test
  public void testCreateUserRefusesAGivenLoginAlreadyHeld() throws Exception {
    existingLogins.add("jdoe");
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                              () -> userAccountService.createUser(account("jdoe", null, null, null),
                                                                                  Locale.ENGLISH));
    assertEquals(UserAccountService.USERNAME_ALREADY_EXISTS, e.getMessage());
    verify(userHandler, never()).createUser(any(), any(Boolean.class));
  }

  /**
   * A login the pre-check did not see but the store refuses, inserted by a
   * concurrent creation: answered as already existing, and that other
   * account is left as it is
   */
  @Test
  public void testCreateUserRefusedInsertTouchesNoAccount() throws Exception {
    creationFailuresLeft = 1;
    creationFailureMessage = UserAccountServiceImpl.IDENTITY_ALREADY_PRESENT_MESSAGE + " jdoe";

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                              () -> userAccountService.createUser(account("jdoe", null, null, null),
                                                                                  Locale.ENGLISH));
    assertEquals(UserAccountService.USERNAME_ALREADY_EXISTS, e.getMessage());
    verify(userHandler, never()).setEnabled(anyString(), any(Boolean.class), any(Boolean.class));
    verify(membershipHandler, never()).linkMembership(any(), any(), any(), any(Boolean.class));
    assertEquals(0, userAccountService.restarts);
  }

  @Test
  public void testCreateUserRefusesTheLoginOfADeletedAccount() throws Exception {
    doThrow(new IllegalStateException("Error executing preSave on listener",
                                      new ObjectAlreadyExistsException(new UserImpl("jdoe"),
                                                                       "Unable to create a previously deleted user")))
                                                                                                                       .when(userHandler)
                                                                                                                       .createUser(any(),
                                                                                                                                   eq(true));
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                              () -> userAccountService.createUser(account("jdoe", null, null, null),
                                                                                  Locale.ENGLISH));
    assertEquals(UserAccountService.USERNAME_ALREADY_EXISTS_AS_DELETED, e.getMessage());
    verify(userHandler, never()).setEnabled(anyString(), any(Boolean.class), any(Boolean.class));
  }

  @Test
  public void testCreateUserAnotherStoreFailureCreatesNothing() throws Exception {
    creationFailuresLeft = 1;
    creationFailureMessage = "Connection lost";

    UserAccountCreationException e = assertThrows(UserAccountCreationException.class,
                                                  () -> userAccountService.createUser(account("jdoe", null, null, null),
                                                                                      Locale.ENGLISH));
    assertEquals(UserAccountService.USER_CREATION_FAILED, e.getMessage());
    verify(userHandler, never()).setEnabled(anyString(), any(Boolean.class), any(Boolean.class));
  }

  @Test
  public void testCreateUserValidatesTheRole() throws Exception {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                              () -> userAccountService.createUser(account("jdoe", null, null, "owner"),
                                                                                  Locale.ENGLISH));
    assertEquals(UserAccountService.ROLE_INVALID, e.getMessage());
    verify(userHandler, never()).createUser(any(), any(Boolean.class));
  }

  @Test
  public void testCreateUserValidatesTheLanguage() throws Exception {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                              () -> userAccountService.createUser(account("jdoe", null, "xx", null),
                                                                                  Locale.ENGLISH));
    assertEquals(UserAccountService.LANGUAGE_INVALID, e.getMessage());
    verify(userHandler, never()).createUser(any(), any(Boolean.class));
  }

  @Test
  public void testCreateUserStoresTheLanguage() throws Exception {
    userAccountService.createUser(account("jdoe", null, "fr", null), Locale.ENGLISH);
    assertEquals("fr", storedProfile.getAttribute("user.language"));
    assertEquals("jdoe", storedProfile.getUserName());
  }

  /**
   * The edit stores a supported language, refuses an unknown one without
   * writing, leaves the stored one on a blank value, and accepts the stored
   * one even when it is no longer a supported locale
   */
  @Test
  public void testUpdateLanguage() throws Exception {
    assertNull(userAccountService.getLanguage("jdoe"));
    assertNull(userAccountService.checkLanguage("jdoe", " "));
    assertEquals("fr", userAccountService.checkLanguage("jdoe", "fr"));
    assertNull(storedProfile);

    userAccountService.updateLanguage("jdoe", "fr");
    assertNull(userAccountService.checkLanguage("jdoe", "fr"));
    assertEquals("fr", storedProfile.getAttribute("user.language"));
    assertEquals("fr", userAccountService.getLanguage("jdoe"));

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                              () -> userAccountService.updateLanguage("jdoe", "xx"));
    assertEquals(UserAccountService.LANGUAGE_INVALID, e.getMessage());
    assertEquals("fr", storedProfile.getAttribute("user.language"));

    userAccountService.updateLanguage("jdoe", " ");
    userAccountService.updateLanguage("jdoe", null);
    assertEquals("fr", storedProfile.getAttribute("user.language"));

    storedProfile.setAttribute("user.language", "de");
    userAccountService.updateLanguage("jdoe", "de");
    assertEquals("de", storedProfile.getAttribute("user.language"));
    userAccountService.updateLanguage("jdoe", "en");
    assertEquals("en", storedProfile.getAttribute("user.language"));

    // A stored language that cannot be read: the one asked is checked as new
    when(userProfileHandler.findUserProfileByName("jdoe")).thenThrow(new RuntimeException("unreadable"));
    assertEquals("fr", userAccountService.checkLanguage("jdoe", "fr"));
    IllegalArgumentException unreadable = assertThrows(IllegalArgumentException.class,
                                                       () -> userAccountService.checkLanguage("jdoe", "xx"));
    assertEquals(UserAccountService.LANGUAGE_INVALID, unreadable.getMessage());
  }

  @Test
  public void testCreateUserValidatesAGivenPassword() throws Exception {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                              () -> userAccountService.createUser(account("jdoe", "short", null, null),
                                                                                  Locale.ENGLISH));
    assertTrue(e.getMessage(), e.getMessage().startsWith("PASSWORD:"));
    verify(userHandler, never()).createUser(any(), any(Boolean.class));

    userAccountService.createUser(account("jdoe", "LongEnough1", null, null), Locale.ENGLISH);
    assertEquals("LongEnough1", createdUser.getPassword());
  }

  /**
   * The password is stored as typed: a padded one keeps its spaces, since
   * no sign-in path trims, and one made of spaces is refused rather than
   * read as "no password"
   */
  @Test
  public void testCreateUserKeepsThePasswordAsTyped() throws Exception {
    userAccountService.createUser(account("jdoe", " LongEnough1 ", null, null), Locale.ENGLISH);
    assertEquals(" LongEnough1 ", createdUser.getPassword());

    createdUser = null;
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                              () -> userAccountService.createUser(account("jdoe2", "         ", null, null),
                                                                                  Locale.ENGLISH));
    assertTrue(e.getMessage(), e.getMessage().startsWith("PASSWORD:"));
    assertNull(createdUser);
  }

  @Test
  public void testCreateUserRefusesAUsedEmail() throws Exception {
    when(usersByEmail.getSize()).thenReturn(1);
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                              () -> userAccountService.createUser(account("jdoe", null, null, null),
                                                                                  Locale.ENGLISH));
    assertEquals(UserAccountService.EMAIL_ALREADY_EXISTS, e.getMessage());
    verify(userHandler, never()).createUser(any(), any(Boolean.class));
  }

  @Test
  public void testCreateExternalUserMovesTheMembership() throws Exception {
    userAccountService.createUser(account("jdoe", null, null, "external"), Locale.ENGLISH);

    assertEquals(1, storedMemberships.size());
    assertEquals(UserAccountServiceImpl.PLATFORM_EXTERNALS_GROUP, storedMemberships.get(0).getGroupId());
    assertEquals("member", storedMemberships.get(0).getMembershipType());
    assertTrue(createdUser.isEnabled());
  }

  @Test
  public void testCreateAdministratorAddsTheAdministrationMembership() throws Exception {
    userAccountService.createUser(account("jdoe", null, null, "ADMINISTRATOR"), Locale.ENGLISH);

    assertEquals(2, storedMemberships.size());
    assertEquals(UserAccountServiceImpl.PLATFORM_USERS_GROUP, storedMemberships.get(0).getGroupId());
    assertEquals(UserAccountServiceImpl.PLATFORM_ADMINISTRATORS_GROUP, storedMemberships.get(1).getGroupId());
    assertEquals("manager", storedMemberships.get(1).getMembershipType());
  }

  @Test
  public void testCreateUserDeactivatedWhenRequested() throws Exception {
    UserAccount account = account("jdoe", null, null, null);
    account.setEnabled(false);
    userAccountService.createUser(account, Locale.ENGLISH);
    verify(userHandler).setEnabled("jdoe", false, true);
    assertTrue(!createdUser.isEnabled());
  }

  /**
   * The read-back finds a part missing: the account is deactivated, read
   * back again, and reported with the missing part
   */
  @Test
  public void testCreateUserDeactivatesAnIncompleteAccount() throws Exception {
    doThrow(new IllegalStateException("store down")).when(membershipHandler).linkMembership(any(), any(), any(), eq(true));

    UserAccountCreationException e = assertThrows(UserAccountCreationException.class,
                                                  () -> userAccountService.createUser(account("jdoe", null, null, "administrator"),
                                                                                      Locale.ENGLISH));
    assertEquals(UserAccountService.USER_CREATED_INCOMPLETE + ":jdoe:MEMBERSHIPS", e.getMessage());
    verify(userHandler).setEnabled("jdoe", false, true);
    assertTrue(!createdUser.isEnabled());
    assertEquals(2, userAccountService.restarts);
    verify(userHandler, times(2)).clearCache("jdoe");
  }

  @Test
  public void testCreateUserReportsEveryMissingPartInOrder() throws Exception {
    doThrow(new IllegalStateException("store down")).when(membershipHandler).linkMembership(any(), any(), any(), eq(true));
    doThrow(new IllegalStateException("store down")).when(userProfileHandler).saveUserProfile(any(), eq(true));
    doThrow(new IllegalStateException("store down")).when(userHandler).setEnabled(anyString(), eq(false), eq(true));
    UserAccount account = account("jdoe", null, "fr", "administrator");
    account.setEnabled(false);

    UserAccountCreationException e = assertThrows(UserAccountCreationException.class,
                                                  () -> userAccountService.createUser(account, Locale.ENGLISH));
    assertEquals(UserAccountService.USER_CREATED_INCOMPLETE_ENABLED + ":jdoe:MEMBERSHIPS,LANGUAGE,ENABLED", e.getMessage());
  }

  @Test
  public void testCreateUserReportsAnIncompleteAccountThatStaysEnabled() throws Exception {
    doThrow(new IllegalStateException("store down")).when(membershipHandler).linkMembership(any(), any(), any(), eq(true));
    doThrow(new IllegalStateException("store down")).when(userHandler).setEnabled(anyString(), eq(false), eq(true));

    UserAccountCreationException e = assertThrows(UserAccountCreationException.class,
                                                  () -> userAccountService.createUser(account("jdoe", null, null, "external"),
                                                                                      Locale.ENGLISH));
    assertEquals(UserAccountService.USER_CREATED_INCOMPLETE_ENABLED + ":jdoe:MEMBERSHIPS", e.getMessage());
    assertTrue(createdUser.isEnabled());
  }

  /**
   * The read-back finds an account that is not this request's (another
   * creation date): nothing is deactivated
   */
  @Test
  public void testCreateUserDisablesNothingWhenTheReadBackIsAnotherAccount() throws Exception {
    doAnswer(invocation -> {
      UserImpl user = invocation.getArgument(0);
      user.setCreatedDate(new Date(1000));
      user.setEnabled(true);
      UserImpl other = new UserImpl(user.getUserName());
      other.setCreatedDate(new Date(2000));
      other.setEnabled(true);
      createdUser = other;
      return null;
    }).when(userHandler).createUser(any(), eq(true));

    UserAccountCreationException e = assertThrows(UserAccountCreationException.class,
                                                  () -> userAccountService.createUser(account("jdoe", null, null, null),
                                                                                      Locale.ENGLISH));
    assertEquals(UserAccountService.USER_CREATION_FAILED, e.getMessage());
    verify(userHandler, never()).setEnabled(anyString(), any(Boolean.class), any(Boolean.class));
  }

  private UserAccount account(String login, String password, String language, String role) {
    return new UserAccount(login, "Jane", "Doe", EMAIL, password, language, role, true);
  }

  private static Membership membership(String type, String username, String groupId) {
    return new MembershipImpl(type, username, groupId, false);
  }

  /**
   * The edit writes the names, the email and the language, then reads them
   * back in a new transaction
   */
  @Test
  public void testUpdateUserWritesThenReadsBack() throws Exception {
    storedAccount(true);
    userAccountService.updateUser(edit("Janet", "Smith", "janet.smith@example.com", "fr", true, null), Locale.ENGLISH, "admin");

    assertEquals("Janet", createdUser.getFirstName());
    assertEquals("Smith", createdUser.getLastName());
    assertEquals("janet.smith@example.com", createdUser.getEmail());
    assertEquals("fr", storedProfile.getAttribute("user.language"));
    verify(userHandler, never()).setEnabled(anyString(), any(Boolean.class), any(Boolean.class));
    verify(changePasswordConnector, never()).changePassword(any(), any());
    assertEquals(1, userAccountService.restarts);
  }

  /**
   * Every refusal comes before the first write: a deactivation of oneself or
   * of the super user, an email held by another account, an unknown language
   */
  @Test
  public void testUpdateUserRefusesBeforeAnyWrite() throws Exception {
    storedAccount(true);
    assertRefused(UserAccountService.SELF_DISABLE, edit("Jane", "Doe", EMAIL, null, false, "LongEnough1"), "jdoe");
    lenient().when(userACL.getSuperUser()).thenReturn("jdoe");
    assertRefused(UserAccountService.DISABLE_SUPER_USER, edit("Jane", "Doe", EMAIL, null, false, "LongEnough1"), "admin");
    when(usersByEmail.getSize()).thenReturn(1);
    assertRefused(UserAccountService.EMAIL_ALREADY_EXISTS, edit("Jane", "Doe", "taken@example.com", null, true, null), "admin");
    when(usersByEmail.getSize()).thenReturn(0);
    assertRefused(UserAccountService.LANGUAGE_INVALID, edit("Jane", "Doe", EMAIL, "xx", true, "LongEnough1"), "admin");
    // The super user's email stays editable while its account stays active
    userAccountService.updateUser(edit("Jane", "Doe", "root@example.com", null, true, null), Locale.ENGLISH, "admin");
    assertEquals("root@example.com", createdUser.getEmail());
  }

  /**
   * The password is changed first and proven to authenticate; when it does
   * not, nothing else is written
   */
  @Test
  public void testUpdateUserChangesThePasswordFirstAndProvesIt() throws Exception {
    storedAccount(true);
    when(userHandler.authenticate("jdoe", "LongEnough1")).thenReturn(false);
    UserAccountUpdateException e = assertThrows(UserAccountUpdateException.class,
                                                () -> userAccountService.updateUser(edit("Janet", "Doe", EMAIL, null, true, "LongEnough1"),
                                                                                    Locale.ENGLISH,
                                                                                    "admin"));
    assertEquals(UserAccountService.USER_PASSWORD_NOT_STORED, e.getMessage());
    verify(changePasswordConnector).changePassword("jdoe", "LongEnough1");
    verify(userHandler, never()).saveUser(any(), any(Boolean.class));
    assertEquals("Jane", createdUser.getFirstName());

    when(userHandler.authenticate("jdoe", "LongEnough1")).thenReturn(true);
    userAccountService.updateUser(edit("Janet", "Doe", EMAIL, null, true, "LongEnough1"), Locale.ENGLISH, "admin");
    org.mockito.InOrder order = org.mockito.Mockito.inOrder(changePasswordConnector, userHandler);
    order.verify(changePasswordConnector).changePassword("jdoe", "LongEnough1");
    order.verify(userHandler).saveUser(any(), eq(true));
    assertEquals("Janet", createdUser.getFirstName());
  }

  /**
   * A connector that refuses the password stops the edit before any other
   * write
   */
  @Test
  public void testUpdateUserStopsOnAPasswordTheConnectorRefuses() throws Exception {
    storedAccount(true);
    doThrow(new IllegalStateException("refused")).when(changePasswordConnector).changePassword("jdoe", "LongEnough1");
    assertThrows(IllegalStateException.class,
                 () -> userAccountService.updateUser(edit("Janet", "Doe", EMAIL, "fr", true, "LongEnough1"), Locale.ENGLISH, "admin"));
    verify(userHandler, never()).saveUser(any(), any(Boolean.class));
    assertNull(storedProfile);
  }

  /**
   * A deactivated account cannot sign in: its new password is checked
   * against the store directly, and a handler that cannot do it is reported
   */
  @Test
  public void testUpdateUserProvesTheNewPasswordOfADeactivatedAccount() throws Exception {
    storedAccount(false);
    when(userHandler.authenticateDB(any(), eq("LongEnough1"))).thenReturn(true);
    userAccountService.updateUser(edit("Jane", "Doe", EMAIL, null, false, "LongEnough1"), Locale.ENGLISH, "admin");
    verify(userHandler, never()).authenticate(anyString(), anyString());

    when(userHandler.authenticateDB(any(), eq("LongEnough1"))).thenReturn(false);
    UserAccountUpdateException e = assertThrows(UserAccountUpdateException.class,
                                                () -> userAccountService.updateUser(edit("Jane", "Doe", EMAIL, null, false, "LongEnough1"),
                                                                                    Locale.ENGLISH,
                                                                                    "admin"));
    assertEquals(UserAccountService.USER_PASSWORD_NOT_STORED, e.getMessage());

    UserHandler otherHandler = mock(UserHandler.class);
    when(organizationService.getUserHandler()).thenReturn(otherHandler);
    when(otherHandler.findUserByName(eq("jdoe"), eq(UserStatus.ANY))).thenAnswer(invocation -> copy(createdUser));
    e = assertThrows(UserAccountUpdateException.class,
                     () -> userAccountService.updateUser(edit("Jane", "Doe", EMAIL, null, false, "LongEnough1"), Locale.ENGLISH, "admin"));
    assertEquals(UserAccountService.USER_PASSWORD_NOT_VERIFIABLE, e.getMessage());
    verify(otherHandler, never()).saveUser(any(), any(Boolean.class));
  }

  /**
   * A write the store swallowed is reported: the names, the status (a
   * deactivation the store ignores, as with disableUserActived false) and
   * the language each read back other than requested
   */
  @Test
  public void testUpdateUserReportsAWriteTheStoreSwallowed() throws Exception {
    storedAccount(true);
    doNothing().when(userHandler).saveUser(any(), eq(true));
    assertIncomplete(edit("Janet", "Doe", EMAIL, null, true, null));

    storedAccount(true);
    doReturn(null).when(userHandler).setEnabled(anyString(), any(Boolean.class), eq(true));
    assertIncomplete(edit("Jane", "Doe", EMAIL, null, false, null));

    storedAccount(true);
    doNothing().when(userProfileHandler).saveUserProfile(any(), eq(true));
    assertIncomplete(edit("Jane", "Doe", EMAIL, "fr", true, null));
  }

  private void assertIncomplete(UserAccount account) {
    UserAccountUpdateException e = assertThrows(UserAccountUpdateException.class,
                                                () -> userAccountService.updateUser(account, Locale.ENGLISH, "admin"));
    assertEquals(UserAccountService.USER_UPDATE_INCOMPLETE, e.getMessage());
  }

  private void assertRefused(String code, UserAccount account, String modifier) throws Exception {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                              () -> userAccountService.updateUser(account, Locale.ENGLISH, modifier));
    assertEquals(code, e.getMessage());
    verify(changePasswordConnector, never()).changePassword(any(), any());
    verify(userHandler, never()).saveUser(any(), any(Boolean.class));
    verify(userHandler, never()).setEnabled(anyString(), any(Boolean.class), any(Boolean.class));
    assertNull(storedProfile);
  }

  /**
   * The account the edit tests change: the store keeps what saveUser writes
   */
  private void storedAccount(boolean enabled) throws Exception {
    createdUser = new UserImpl("jdoe");
    createdUser.setCreatedDate(new java.util.Date());
    createdUser.setFirstName("Jane");
    createdUser.setLastName("Doe");
    createdUser.setEmail(EMAIL);
    createdUser.setEnabled(enabled);
    storedProfile = null;
    lenient().when(passwordRecoveryService.getActiveChangePasswordConnector()).thenReturn(changePasswordConnector);
    lenient().doAnswer(invocation -> {
      User saved = invocation.getArgument(0);
      createdUser.setFirstName(saved.getFirstName());
      createdUser.setLastName(saved.getLastName());
      createdUser.setEmail(saved.getEmail());
      return null;
    }).when(userHandler).saveUser(any(), eq(true));
  }

  private static UserAccount edit(String firstName, String lastName, String email, String language, boolean enabled, String password) {
    UserAccount account = new UserAccount();
    account.setUserName("jdoe");
    account.setFirstName(firstName);
    account.setLastName(lastName);
    account.setEmail(email);
    account.setLanguage(language);
    account.setEnabled(enabled);
    account.setPassword(password);
    return account;
  }

  private static UserImpl copy(UserImpl user) {
    UserImpl copy = new UserImpl(user.getUserName());
    copy.setCreatedDate(user.getCreatedDate());
    copy.setEnabled(user.isEnabled());
    copy.setEmail(user.getEmail());
    copy.setFirstName(user.getFirstName());
    copy.setLastName(user.getLastName());
    return copy;
  }

  private void inject(UserAccountServiceImpl service) throws Exception {
    for (java.lang.reflect.Field field : UserAccountServiceImpl.class.getDeclaredFields()) {
      if (field.getType() == OrganizationService.class) {
        set(service, field, organizationService);
      } else if (field.getType() == UserACL.class) {
        set(service, field, userACL);
      } else if (field.getType() == LocaleConfigService.class) {
        set(service, field, localeConfigService);
      } else if (field.getType() == OtpService.class) {
        set(service, field, otpService);
      }
    }
  }

  private static void set(Object target, java.lang.reflect.Field field, Object value) throws Exception {
    field.setAccessible(true);
    field.set(target, value);
  }

  /**
   * The seam of the real transaction restart, which needs a container:
   * counted instead
   */
  private static class RestartCountingUserAccountService extends UserAccountServiceImpl {
    int restarts;

    @Override
    protected void restartTransaction() {
      restarts++;
    }
  }

}
