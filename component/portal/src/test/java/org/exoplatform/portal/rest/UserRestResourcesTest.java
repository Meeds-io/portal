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
package org.exoplatform.portal.rest;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.exoplatform.portal.config.DefaultGroupVisibilityPlugin;
import org.exoplatform.web.login.recovery.ChangePasswordConnector;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;
import org.json.JSONObject;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.rest.model.UserRestEntity;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.rest.services.BaseRestServicesTestCase;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.organization.idm.UserImpl;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.rest.impl.*;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.test.mock.MockHttpServletRequest;

public class UserRestResourcesTest extends BaseRestServicesTestCase {

  private static final String USER_1 = "testuser1";

  private static final String USER_2 = "testuser2";

  private UserHandler         userHandler;

  private ChangePasswordConnector changePasswordConnector;

  private UserACL             userACL;

  private UserAccountService  userAccountService;

  protected Class<?> getComponentClass() {
    return UserRestResourcesV1.class;
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

    PasswordRecoveryService passwordRecoveryService = mock(PasswordRecoveryService.class);
    changePasswordConnector = mock(ChangePasswordConnector.class);
    Mockito.doNothing().when(changePasswordConnector).changePassword(any(), any());
    Mockito.when(passwordRecoveryService.allowChangePassword(any())).thenReturn(true);
    Mockito.when(passwordRecoveryService.getActiveChangePasswordConnector()).thenReturn(changePasswordConnector);
    getContainer().unregisterComponent(PasswordRecoveryService.class);
    getContainer().registerComponentInstance(PasswordRecoveryService.class, passwordRecoveryService);

    OrganizationService organizationService = mock(OrganizationService.class);
    userHandler = mock(UserHandler.class);
    userACL = mock(UserACL.class);

    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(userHandler.findUserByName(eq(USER_2), any(UserStatus.class))).thenReturn(null);

    UserImpl user = new UserImpl(USER_1);
    when(userHandler.findUserByName(eq(USER_1), any(UserStatus.class))).thenReturn(user);

    userAccountService = mock(UserAccountService.class);
    getContainer().unregisterComponent(UserAccountService.class);
    getContainer().registerComponentInstance(UserAccountService.class, userAccountService);

    getContainer().unregisterComponent(OrganizationService.class);
    getContainer().unregisterComponent(UserACL.class);

    getContainer().registerComponentInstance("org.exoplatform.services.organization.OrganizationService", organizationService);
    getContainer().registerComponentInstance("org.exoplatform.portal.config.UserACL", userACL);

    ResourceBundleService resourceBundleService = container.getComponentInstanceOfType(ResourceBundleService.class);
    if (resourceBundleService == null) {
      resourceBundleService = Mockito.mock(ResourceBundleService.class);
      container.registerComponentInstance(resourceBundleService);
    }
  }

  @Override
  public void tearDown() throws Exception {
    getContainer().unregisterComponent("org.exoplatform.services.organization.OrganizationService");
    getContainer().unregisterComponent("org.exoplatform.portal.config.UserACL");
    getContainer().unregisterComponent(UserAccountService.class);
    super.tearDown();
  }

  public void testUnauthorizedNotSameUser() throws Exception {
    // Given
    String path = getChangePasswordPath(USER_1);
    String currentPassword = "currentPassword";
    String newPassword = "newPassword";
    MockHttpServletRequest httpRequest = getChangePasswordRequest(path, currentPassword, newPassword);

    EnvironmentContext envctx = new EnvironmentContext();
    envctx.put(HttpServletRequest.class, httpRequest);

    startUserSession(USER_2);

    // When
    ContainerResponse resp = launcher.service("PATCH",
                                              path,
                                              "",
                                              getChangePasswordHeaders(),
                                              getChangePasswordData(currentPassword, newPassword),
                                              envctx);

    // Then
    assertEquals(String.valueOf(resp.getEntity()), 401, resp.getStatus());
  }

  public void testAdminAuthorizedToChangePassword() throws Exception {
    // Given
    String path = getChangePasswordPath(USER_1);
    String currentPassword = "currentPassword";
    String newPassword = "newPassword1";

    EnvironmentContext envctx = requestContext(getChangePasswordRequest(path, currentPassword, newPassword, USER_2, true));

    startUserSession(USER_2);
    when(userACL.isAdministrator(ConversationState.getCurrent().getIdentity())).thenReturn(true);

    // When
    ContainerResponse resp = launcher.service("PATCH",
                                              path,
                                              "",
                                              getChangePasswordHeaders(),
                                              getChangePasswordData(currentPassword, newPassword),
                                              envctx);

    // Then
    assertEquals(String.valueOf(resp.getEntity()), 204, resp.getStatus());
    assertNull(resp.getEntity());
  }

  public void testChangePasswordUserNotFoundError() throws Exception {
    // Given
    String path = getChangePasswordPath(USER_2);
    String currentPassword = "currentPassword";
    String newPassword = "newPassword";
    MockHttpServletRequest httpRequest = getChangePasswordRequest(path, currentPassword, newPassword);

    EnvironmentContext envctx = new EnvironmentContext();
    envctx.put(HttpServletRequest.class, httpRequest);

    startUserSession(USER_2);
    when(userACL.isAdministrator(ConversationState.getCurrent().getIdentity())).thenReturn(true);

    // When
    ContainerResponse resp = launcher.service("PATCH",
                                              path,
                                              "",
                                              getChangePasswordHeaders(),
                                              getChangePasswordData(currentPassword, newPassword),
                                              envctx);

    // Then
    assertEquals(String.valueOf(resp.getEntity()), 500, resp.getStatus());
    Object entity = resp.getEntity();
    assertNotNull(entity);
    assertTrue(entity instanceof String);
    String errorMessage = (String) entity;
    assertEquals(UserRestResourcesV1.USER_NOT_FOUND_ERROR_CODE, errorMessage);
  }

  public void testSameUserWrongPassword() throws Exception {
    // Given
    String path = getChangePasswordPath(USER_1);
    String currentPassword = "currentPassword";
    String newPassword = "newPassword";
    MockHttpServletRequest httpRequest = getChangePasswordRequest(path, currentPassword, newPassword);
    EnvironmentContext envctx = new EnvironmentContext();
    envctx.put(HttpServletRequest.class, httpRequest);

    startUserSession(USER_1);

    // When
    ContainerResponse resp = launcher.service("PATCH",
                                              path,
                                              "",
                                              getChangePasswordHeaders(),
                                              getChangePasswordData(currentPassword, newPassword),
                                              envctx);

    // Then
    assertEquals(String.valueOf(resp.getEntity()), 500, resp.getStatus());
    Object entity = resp.getEntity();
    assertNotNull(entity);
    assertTrue(entity instanceof String);
    String errorMessage = (String) entity;
    assertEquals(UserRestResourcesV1.WRONG_USER_PASSWORD_ERROR_CODE, errorMessage);
  }

  public void testSameUserAuthorizedToChangePassword() throws Exception {
    // Given
    String path = getChangePasswordPath(USER_1);
    String currentPassword = "currentPassword";
    String newPassword = "newPassword1";
    MockHttpServletRequest httpRequest = getChangePasswordRequest(path, currentPassword, newPassword);
    EnvironmentContext envctx = new EnvironmentContext();
    envctx.put(HttpServletRequest.class, httpRequest);

    startUserSession(USER_1);

    // When
    when(userHandler.authenticate(USER_1, currentPassword)).thenReturn(true);
    ContainerResponse resp = launcher.service("PATCH",
                                              path,
                                              "",
                                              getChangePasswordHeaders(),
                                              getChangePasswordData(currentPassword, newPassword),
                                              envctx);

    // Then
    assertEquals(String.valueOf(resp.getEntity()), 204, resp.getStatus());
  }

  public void testIsInternalUserAllowedToChangePassword() throws Exception {
    // The property exo.portal.allow.change.external.password isn't displayed (null)
    // Given
    startUserSession(USER_1);

    // When
    ContainerResponse resp = launcher.service("GET",
                                              "/v1/users/isSynchronizedUserAllowedToChangePassword",
                                              "",
                                              null,
                                              null,
                                              null);

    // Then
    assertEquals(String.valueOf(resp.getEntity()), 200, resp.getStatus());
    assertTrue(String.valueOf(resp.getEntity()).contains("true"));
    //Check the fail case
    assertNull(System.getProperty("exo.portal.allow.change.external.password"));

    // The property exo.portal.allow.change.external.password is true
    //When
    System.setProperty("exo.portal.allow.change.external.password", "true");
    resp = launcher.service("GET",
                            "/v1/users/isSynchronizedUserAllowedToChangePassword",
                            "",
                            null,
                            null,
                            null);

    // Then
    assertEquals(String.valueOf(resp.getEntity()), 200, resp.getStatus());
    assertTrue(String.valueOf(resp.getEntity()).contains("true"));

    // The property exo.portal.allow.change.external.password is false
    //When
    System.setProperty("exo.portal.allow.change.external.password", "false");
    resp = launcher.service("GET",
                            "/v1/users/isSynchronizedUserAllowedToChangePassword",
                            "",
                            null,
                            null,
                            null);

    // Then
    assertEquals(String.valueOf(resp.getEntity()), 200, resp.getStatus());
    assertTrue(String.valueOf(resp.getEntity()).contains("true"));
  }

  /**
   * The resource hands the body to the service and answers 201 with the
   * login the service returns, generated or given
   */
  public void testCreateUser() throws Exception {
    startUserSession(USER_1);

    ContainerResponse response = getResponse("POST", "/v1/users", "", confirmedContext(USER_1));
    assertEquals(400, response.getStatus());
    verify(userAccountService, never()).createUser(any(), any());

    when(userAccountService.createUser(any(), any())).thenReturn("ugenerated01");
    JSONObject data = new JSONObject();
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("email", USER_2 + "@example.com");
    data.put("language", "fr");
    data.put("role", "administrator");
    data.put("enabled", true);
    response = getResponse("POST", "/v1/users", data.toString(), confirmedContext(USER_1));
    assertEquals(String.valueOf(response.getEntity()), 201, response.getStatus());
    assertEquals(Collections.singletonMap("userName", "ugenerated01"), response.getEntity());

    ArgumentCaptor<UserAccount> account = ArgumentCaptor.forClass(UserAccount.class);
    verify(userAccountService).createUser(account.capture(), any());
    assertNull(account.getValue().getUserName());
    assertNull(account.getValue().getPassword());
    assertEquals(USER_2, account.getValue().getFirstName());
    assertEquals(USER_2, account.getValue().getLastName());
    assertEquals(USER_2 + "@example.com", account.getValue().getEmail());
    assertEquals("fr", account.getValue().getLanguage());
    assertEquals("administrator", account.getValue().getRole());
    assertTrue(account.getValue().isEnabled());

    data.put("userName", USER_2);
    data.put("password", "newPassword1");
    data.put("enabled", false);
    response = getResponse("POST", "/v1/users", data.toString(), confirmedContext(USER_1));
    assertEquals(201, response.getStatus());
    verify(userAccountService, times(2)).createUser(account.capture(), any());
    assertEquals(USER_2, account.getValue().getUserName());
    assertEquals("newPassword1", account.getValue().getPassword());
    assertFalse(account.getValue().isEnabled());
  }

  /**
   * A refused field is a 400 whose body is the code; a creation the store
   * did not complete is a 500 whose body is the code, the login and the
   * missing parts included
   */
  public void testCreateUserAnswersTheServiceRefusals() throws Exception {
    startUserSession(USER_1);
    JSONObject data = new JSONObject();
    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("email", USER_2 + "@example.com");

    when(userAccountService.createUser(any(), any())).thenThrow(new IllegalArgumentException("USERNAME:ALREADY_EXISTS_AS_DELETED"));
    ContainerResponse response = getResponse("POST", "/v1/users", data.toString(), confirmedContext(USER_1));
    assertEquals(400, response.getStatus());
    assertEquals("USERNAME:ALREADY_EXISTS_AS_DELETED", response.getEntity());

    reset(userAccountService);
    when(userAccountService.createUser(any(), any())).thenThrow(new UserAccountCreationException("USER:CREATED_INCOMPLETE:" + USER_2
        + ":MEMBERSHIPS"));
    response = getResponse("POST", "/v1/users", data.toString(), confirmedContext(USER_1));
    assertEquals(500, response.getStatus());
    assertEquals("USER:CREATED_INCOMPLETE:" + USER_2 + ":MEMBERSHIPS", response.getEntity());
  }

  public void testGetOnboardingSettings() throws Exception {
    PasswordRecoveryService passwordRecoveryService = getContainer().getComponentInstanceOfType(PasswordRecoveryService.class);
    when(passwordRecoveryService.getOnboardingTokenValidity()).thenReturn(TimeUnit.DAYS.toMillis(7));
    startUserSession(USER_1);

    ContainerResponse response = getResponse("GET", "/v1/users/onboardingSettings", "");
    assertEquals(200, response.getStatus());
    assertEquals(Map.of("linkValidityDays", 7L, "linkValidityHours", 168L, "linkValidityWholeDays", true), response.getEntity());
    assertEquals("private, no-store", String.valueOf(response.getHttpHeaders().getFirst("Cache-Control")));

    when(passwordRecoveryService.getOnboardingTokenValidity()).thenReturn(TimeUnit.HOURS.toMillis(36));
    response = getResponse("GET", "/v1/users/onboardingSettings", "");
    assertEquals(Map.of("linkValidityDays", 1L, "linkValidityHours", 36L, "linkValidityWholeDays", false), response.getEntity());

    // Two whole days are the lower bound of the days wording
    when(passwordRecoveryService.getOnboardingTokenValidity()).thenReturn(TimeUnit.HOURS.toMillis(48));
    response = getResponse("GET", "/v1/users/onboardingSettings", "");
    assertEquals(Map.of("linkValidityDays", 2L, "linkValidityHours", 48L, "linkValidityWholeDays", true), response.getEntity());

    // One day reads as 24 hours, as the email states it
    when(passwordRecoveryService.getOnboardingTokenValidity()).thenReturn(TimeUnit.DAYS.toMillis(1));
    response = getResponse("GET", "/v1/users/onboardingSettings", "");
    assertEquals(Map.of("linkValidityDays", 1L, "linkValidityHours", 24L, "linkValidityWholeDays", false), response.getEntity());

    // Not a whole number of hours: the email states whole hours, so does the flag
    when(passwordRecoveryService.getOnboardingTokenValidity()).thenReturn(TimeUnit.MINUTES.toMillis(24 * 60 + 30));
    response = getResponse("GET", "/v1/users/onboardingSettings", "");
    assertEquals(Map.of("linkValidityDays", 1L, "linkValidityHours", 24L, "linkValidityWholeDays", false), response.getEntity());
  }

  /**
   * The update is the account service's: the resource answers 404 for an
   * unknown login, hands the edited fields and the administrator over, and
   * maps a refused field to 400 and an unproven update to 500, the code as
   * body
   */
  public void testUpdateUser() throws Exception {
    when(userHandler.findUserByName(eq(USER_2), any())).thenReturn(null);
    startUserSession(USER_1);
    when(userACL.isAdministrator(ConversationState.getCurrent().getIdentity())).thenReturn(true);

    JSONObject data = new JSONObject();
    data.put("userName", USER_2);
    data.put("lastName", "Doe");
    data.put("firstName", "Jane");
    data.put("email", USER_2 + "@example.com");
    data.put("password", "newPassword1");
    data.put("language", "fr");
    data.put("enabled", false);
    ContainerResponse response = getResponse("PUT", "/v1/users", data.toString(), confirmedContext(USER_1));
    assertEquals(404, response.getStatus());
    verify(userAccountService, never()).updateUser(any(), any(), any());

    when(userHandler.findUserByName(eq(USER_2), any())).thenReturn(new UserImpl(USER_2));
    response = getResponse("PUT", "/v1/users", data.toString(), confirmedContext(USER_1));
    assertEquals(204, response.getStatus());
    ArgumentCaptor<UserAccount> account = ArgumentCaptor.forClass(UserAccount.class);
    verify(userAccountService).updateUser(account.capture(), any(), eq(USER_1));
    assertEquals(USER_2, account.getValue().getUserName());
    assertEquals("Jane", account.getValue().getFirstName());
    assertEquals("Doe", account.getValue().getLastName());
    assertEquals(USER_2 + "@example.com", account.getValue().getEmail());
    assertEquals("newPassword1", account.getValue().getPassword());
    assertEquals("fr", account.getValue().getLanguage());
    assertFalse(account.getValue().isEnabled());

    doThrow(new IllegalArgumentException(UserAccountService.SELF_DISABLE)).when(userAccountService).updateUser(any(), any(), any());
    response = getResponse("PUT", "/v1/users", data.toString(), confirmedContext(USER_1));
    assertEquals(400, response.getStatus());
    assertEquals(UserAccountService.SELF_DISABLE, response.getEntity());

    doThrow(new UserAccountUpdateException(UserAccountService.USER_UPDATE_INCOMPLETE)).when(userAccountService)
                                                                                     .updateUser(any(), any(), any());
    response = getResponse("PUT", "/v1/users", data.toString(), confirmedContext(USER_1));
    assertEquals(500, response.getStatus());
    assertEquals(UserAccountService.USER_UPDATE_INCOMPLETE, response.getEntity());
    verify(userHandler, never()).saveUser(any(), anyBoolean());
    verify(changePasswordConnector, never()).changePassword(any(), any());
  }


  /**
   * The account read returns the stored language, null when it cannot be
   * read, and whether the account is in the internal store
   */
  public void testGetUserLanguage() throws Exception {
    UserImpl user2 = new UserImpl(USER_2);
    user2.setEnabled(true);
    when(userHandler.findUserByName(eq(USER_2), any())).thenReturn(user2);
    startUserSession(USER_1);
    when(userACL.isAdministrator(any())).thenReturn(true);
    OrganizationService organizationService = getContainer().getComponentInstanceOfType(OrganizationService.class);
    MembershipHandler membershipHandler = mock(MembershipHandler.class);
    when(organizationService.getMembershipHandler()).thenReturn(membershipHandler);

    when(userAccountService.getLanguage(USER_2)).thenReturn("fr");
    ContainerResponse response = getResponse("GET", "/v1/users/" + USER_2, "", confirmedContext(USER_1));
    assertEquals(200, response.getStatus());
    UserRestEntity account = (UserRestEntity) response.getEntity();
    assertEquals("fr", account.getLanguage());
    assertEquals(Boolean.TRUE, account.getIsInternal());

    when(userAccountService.getLanguage(USER_2)).thenThrow(new IllegalStateException("unreadable"));
    response = getResponse("GET", "/v1/users/" + USER_2, "", confirmedContext(USER_1));
    assertEquals(200, response.getStatus());
    assertNull(((UserRestEntity) response.getEntity()).getLanguage());
  }


  public void testDeleteUser() throws Exception {
    String email2 = USER_2 + "@example.com";
    UserImpl user2 = new UserImpl(USER_2);
    user2.setEmail(email2);
    user2.setFirstName(USER_2);
    user2.setLastName(USER_2);
    user2.setEnabled(false);
    when(userHandler.findUserByName(eq(USER_2), any())).thenReturn(user2);

    String email1 = USER_1 + "@example.com";
    UserImpl user1 = new UserImpl(USER_1);
    user1.setEmail(email1);
    user1.setFirstName(USER_1);
    user1.setLastName(USER_1);
    user1.setEnabled(true);
    when(userHandler.findUserByName(eq(USER_1), any())).thenReturn(user1);

    startUserSession(USER_1);

    ContainerResponse response = launcher.service("DELETE", "/v1/users/NOT_FOUND", "", null, null, confirmedContext(USER_1));
    assertNotNull(response);
    assertEquals(404, response.getStatus());

    verify(userHandler, atMost(0)).removeUser(anyString(), anyBoolean());

    response = launcher.service("DELETE", "/v1/users/" + USER_1, "", null, null, confirmedContext(USER_1));
    assertNotNull(response);
    assertEquals(400, response.getStatus());
    assertEquals("SelfDelete", response.getEntity());

    verify(userHandler, atMost(0)).removeUser(anyString(), anyBoolean());

    response = launcher.service("DELETE", "/v1/users/" + USER_2, "", null, null, confirmedContext(USER_1));
    assertNotNull(response);
    assertEquals(204, response.getStatus());

    verify(userHandler, atLeastOnce()).removeUser(anyString(), anyBoolean());

    when(userACL.getSuperUser()).thenReturn(USER_2);
    response = launcher.service("DELETE", "/v1/users/" + USER_2, "", null, null, confirmedContext(USER_1));
    assertNotNull(response);
    assertEquals(400, response.getStatus());
    assertEquals("DeleteSuperUser", response.getEntity());
  }

  public void testCreateUserRequiresConfirmedSession() throws Exception {
    startUserSession(USER_1);

    // Refused before the body is read: an empty body answers the same
    ContainerResponse response = getResponse("POST", "/v1/users", "{}", unconfirmedContext(USER_1));
    assertEquals(403, response.getStatus());
    assertEquals(UserAccountConfirmation.OTP_REQUIRED, response.getEntity());

    JSONObject data = new JSONObject();
    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "newPassword1");
    data.put("email", USER_2 + "@example.com");
    response = getResponse("POST", "/v1/users", data.toString(), unconfirmedContext(USER_1));
    assertEquals(403, response.getStatus());
    assertEquals(UserAccountConfirmation.OTP_REQUIRED, response.getEntity());

    // No request bound: refused as well
    response = getResponse("POST", "/v1/users", data.toString());
    assertEquals(403, response.getStatus());

    // The confirmation of another user does not count
    HttpServletRequest request = unconfirmedRequest(USER_1);
    when(request.getSession(false).getAttribute(UserAccountConfirmation.SESSION_ATTRIBUTE)).thenReturn(USER_2);
    response = getResponse("POST", "/v1/users", data.toString(), requestContext(request));
    assertEquals(403, response.getStatus());

    // Credentials carried by the request change nothing
    request = unconfirmedRequest(USER_1);
    when(request.getHeader("Authorization")).thenReturn("Basic dGVzdHVzZXIxOnBhc3N3b3Jk");
    when(request.getAuthType()).thenReturn(HttpServletRequest.BASIC_AUTH);
    response = getResponse("POST", "/v1/users", data.toString(), requestContext(request));
    assertEquals(403, response.getStatus());
    assertEquals(UserAccountConfirmation.OTP_REQUIRED, response.getEntity());

    verify(userHandler, never()).createUser(any(User.class), anyBoolean());
    verify(userHandler, never()).findUsersByQuery(any(), any());
  }

  public void testUpdateUserRequiresConfirmedSession() throws Exception {
    UserImpl user2 = new UserImpl(USER_2);
    user2.setEmail(USER_2 + "@example.com");
    user2.setEnabled(true);
    when(userHandler.findUserByName(eq(USER_2), any())).thenReturn(user2);

    startUserSession(USER_1);
    JSONObject data = new JSONObject();
    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("email", "new-" + USER_2 + "@example.com");
    data.put("enabled", false);

    // A non-administrator keeps today's answer, without the confirmation code
    ContainerResponse response = getResponse("PUT", "/v1/users", data.toString(), unconfirmedContext(USER_1));
    assertEquals(403, response.getStatus());
    assertNotEquals(UserAccountConfirmation.OTP_REQUIRED, response.getEntity());

    when(userACL.isAdministrator(ConversationState.getCurrent().getIdentity())).thenReturn(true);
    response = getResponse("PUT", "/v1/users", data.toString(), unconfirmedContext(USER_1));
    assertEquals(403, response.getStatus());
    assertEquals(UserAccountConfirmation.OTP_REQUIRED, response.getEntity());

    verify(userHandler, never()).findUserByName(eq(USER_2), any());
    verify(userHandler, never()).saveUser(any(User.class), anyBoolean());
    verify(userHandler, never()).setEnabled(anyString(), anyBoolean(), anyBoolean());
  }

  public void testAdministratorChangesSuperUserEmailFromConfirmedSession() throws Exception {
    UserImpl root = new UserImpl("root");
    root.setEnabled(true);
    when(userHandler.findUserByName(eq("root"), any())).thenReturn(root);

    startUserSession(USER_1);
    when(userACL.isAdministrator(ConversationState.getCurrent().getIdentity())).thenReturn(true);

    JSONObject data = new JSONObject();
    data.put("userName", "root");
    data.put("lastName", "Root");
    data.put("firstName", "Root");
    data.put("email", "admin@example.com");
    data.put("enabled", true);
    ContainerResponse response = getResponse("PUT", "/v1/users", data.toString(), confirmedContext(USER_1));
    assertEquals(204, response.getStatus());
    verify(userAccountService).updateUser(argThat(account -> "admin@example.com".equals(account.getEmail())), any(), eq(USER_1));
  }


  public void testDeleteUserRequiresConfirmedSession() throws Exception {
    UserImpl user2 = new UserImpl(USER_2);
    when(userHandler.findUserByName(eq(USER_2), any())).thenReturn(user2);
    startUserSession(USER_1);

    ContainerResponse response = launcher.service("DELETE", "/v1/users/" + USER_2, "", null, null, unconfirmedContext(USER_1));
    assertEquals(403, response.getStatus());
    assertEquals(UserAccountConfirmation.OTP_REQUIRED, response.getEntity());

    // Refused before the existence check: an unknown user answers the same
    response = launcher.service("DELETE", "/v1/users/NOT_FOUND", "", null, null, unconfirmedContext(USER_1));
    assertEquals(403, response.getStatus());
    assertEquals(UserAccountConfirmation.OTP_REQUIRED, response.getEntity());

    verify(userHandler, never()).findUserByName(anyString(), any());
    verify(userHandler, never()).removeUser(anyString(), anyBoolean());
  }

  public void testAdminChangePasswordOfAnotherUserRequiresConfirmedSession() throws Exception {
    String path = getChangePasswordPath(USER_1);
    startUserSession(USER_2);
    when(userACL.isAdministrator(ConversationState.getCurrent().getIdentity())).thenReturn(true);

    ContainerResponse resp = launcher.service("PATCH",
                                              path,
                                              "",
                                              getChangePasswordHeaders(),
                                              getChangePasswordData("currentPassword", "newPassword1"),
                                              requestContext(getChangePasswordRequest(path,
                                                                                      "currentPassword",
                                                                                      "newPassword1",
                                                                                      USER_2,
                                                                                      false)));
    assertEquals(403, resp.getStatus());
    assertEquals(UserAccountConfirmation.OTP_REQUIRED, resp.getEntity());
    verify(userHandler, never()).findUserByName(eq(USER_1), any());
    verify(changePasswordConnector, never()).changePassword(any(), any());
  }

  public void testSuperUserChangesOwnPasswordWithoutConfirmedSession() throws Exception {
    UserImpl root = new UserImpl("root");
    when(userHandler.findUserByName(eq("root"), any())).thenReturn(root);
    when(userHandler.authenticate("root", "currentPassword")).thenReturn(true);
    startUserSession("root");

    ContainerResponse resp = launcher.service("PATCH",
                                              getChangePasswordPath("root"),
                                              "",
                                              getChangePasswordHeaders(),
                                              getChangePasswordData("currentPassword", "newPassword1"),
                                              requestContext(getChangePasswordRequest(getChangePasswordPath("root"),
                                                                                      "currentPassword",
                                                                                      "newPassword1",
                                                                                      "root",
                                                                                      false)));
    assertEquals(String.valueOf(resp.getEntity()), 204, resp.getStatus());
    verify(changePasswordConnector).changePassword("root", "newPassword1");
  }

  public void testConfirmSession() throws Exception {
    startUserSession(USER_1);
    doThrow(new IllegalAccessException()).when(userAccountService).confirmSession(USER_1, "00000");

    HttpServletRequest request = unconfirmedRequest(USER_1);
    ContainerResponse resp = launcher.service("POST",
                                              "/v1/users/confirmation",
                                              "",
                                              getChangePasswordHeaders(),
                                              "otpCode=00000".getBytes(),
                                              requestContext(request));
    assertEquals(401, resp.getStatus());
    assertEquals(UserRestResourcesV1.OTP_CODE_INVALID, resp.getEntity());
    verify(request.getSession(true), never()).setAttribute(anyString(), any());

    resp = launcher.service("POST",
                            "/v1/users/confirmation",
                            "",
                            getChangePasswordHeaders(),
                            "otpCode=12345".getBytes(),
                            requestContext(request));
    assertEquals(204, resp.getStatus());
    verify(userAccountService).confirmSession(USER_1, "12345");
    verify(request.getSession(true)).setAttribute(UserAccountConfirmation.SESSION_ATTRIBUTE, USER_1);
  }

  private MockHttpServletRequest getChangePasswordRequest(String path, String currentPassword, String newPassword) {
    byte[] formData = getChangePasswordData(currentPassword, newPassword);
    ByteArrayInputStream dataInputStream = new ByteArrayInputStream(formData);
    return new MockHttpServletRequest(path,
                                      dataInputStream,
                                      dataInputStream.available(),
                                      "PATCH",
                                      getChangePasswordHeaders());
  }

  private MockHttpServletRequest getChangePasswordRequest(String path,
                                                          String currentPassword,
                                                          String newPassword,
                                                          String remoteUser,
                                                          boolean confirmed) {
    byte[] formData = getChangePasswordData(currentPassword, newPassword);
    ByteArrayInputStream dataInputStream = new ByteArrayInputStream(formData);
    MockHttpServletRequest request = new MockHttpServletRequest(path,
                                                                dataInputStream,
                                                                dataInputStream.available(),
                                                                "PATCH",
                                                                getChangePasswordHeaders()) {
      @Override
      public String getRemoteUser() {
        return remoteUser;
      }
    };
    if (confirmed) {
      request.getSession().setAttribute(UserAccountConfirmation.SESSION_ATTRIBUTE, remoteUser);
    }
    return request;
  }

  private byte[] getChangePasswordData(String currentPassword, String newPassword) {
    return ("currentPassword=" + currentPassword
        + "&newPassword="
        + newPassword).getBytes();
  }

  private MultivaluedMap<String, String> getChangePasswordHeaders() {
    MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
    headers.putSingle("Content-Type", "application/x-www-form-urlencoded");
    return headers;
  }

  private String getChangePasswordPath(String username) {
    return "/v1/users/" + username + "/changePassword";
  }
}
