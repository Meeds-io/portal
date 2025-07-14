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

import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;

import jakarta.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.exoplatform.portal.config.DefaultGroupVisibilityPlugin;
import org.exoplatform.web.login.recovery.ChangePasswordConnector;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;
import org.json.JSONObject;
import org.mockito.Mock;
import org.mockito.Mockito;

import org.exoplatform.commons.utils.ListAccess;
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

  public void testCreateUser() throws Exception {
    when(userHandler.findUserByName(eq(USER_2), any())).thenReturn(null);
    @SuppressWarnings("unchecked")
    ListAccess<User> listAccess = mock(ListAccess.class);
    when(userHandler.findUsersByQuery(any(), any())).thenReturn(listAccess);
    when(listAccess.getSize()).thenReturn(0);
    UserImpl user = new UserImpl(USER_2);
    when(userHandler.createUserInstance(eq(USER_2))).thenReturn(user);

    startUserSession(USER_1);

    JSONObject data = new JSONObject();

    ContainerResponse response = getResponse("POST", "/v1/users", data.toString());
    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertEquals(response.getEntity().toString(), 400, response.getStatus());

    data.put("userName", "");
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "password");
    data.put("email", USER_2 + "@example.com");
    response = getResponse("POST", "/v1/users", data.toString());
    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertEquals(response.getEntity().toString(), 400, response.getStatus());

    data.put("userName", USER_2);
    data.put("lastName", "");
    data.put("firstName", USER_2);
    data.put("password", "password");
    data.put("email", USER_2 + "@example.com");
    response = getResponse("POST", "/v1/users", data.toString());
    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertEquals(response.getEntity().toString(), 400, response.getStatus());

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", "");
    data.put("password", "password");
    data.put("email", USER_2 + "@example.com");
    response = getResponse("POST", "/v1/users", data.toString());
    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertEquals(response.getEntity().toString(), 400, response.getStatus());

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "");
    data.put("email", USER_2 + "@example.com");
    response = getResponse("POST", "/v1/users", data.toString());
    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertEquals(response.getEntity().toString(), 400, response.getStatus());

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "password");
    data.put("email", "");
    response = getResponse("POST", "/v1/users", data.toString());
    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertEquals(response.getEntity().toString(), 400, response.getStatus());

    verify(userHandler, atMost(0)).createUser(any(User.class), anyBoolean());

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "newPassword1");
    data.put("email", USER_2 + "@example.com");
    response = getResponse("POST", "/v1/users", data.toString());
    assertNotNull(response);
    assertNull(response.getEntity());
    assertEquals(204, response.getStatus());

    verify(userHandler, atLeast(1)).createUser(eq(user), eq(true));
  }

  public void testUpdateUser() throws Exception {
    when(userHandler.findUserByName(eq(USER_2), any())).thenReturn(null);
    @SuppressWarnings("unchecked")
    ListAccess<User> listAccess = mock(ListAccess.class);
    when(userHandler.findUsersByQuery(any(), any())).thenReturn(listAccess);
    when(listAccess.getSize()).thenReturn(0);

    String email2 = USER_2 + "@example.com";
    UserImpl user2 = new UserImpl(USER_2);
    user2.setEmail(email2);
    user2.setFirstName(USER_2);
    user2.setLastName(USER_2);
    user2.setEnabled(false);

    String email1 = USER_1 + "@example.com";
    UserImpl user1 = new UserImpl(USER_1);
    user1.setEmail(email1);
    user1.setFirstName(USER_1);
    user1.setLastName(USER_1);
    user1.setEnabled(true);

    startUserSession(USER_1);
    when(userACL.isAdministrator(ConversationState.getCurrent().getIdentity())).thenReturn(true);

    JSONObject data = new JSONObject();

    ContainerResponse response = getResponse("PUT", "/v1/users", data.toString());
    assertNotNull(response);
    assertEquals(404, response.getStatus());

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "");
    data.put("email", email2);
    response = getResponse("PUT", "/v1/users", data.toString());
    assertNotNull(response);
    assertEquals(404, response.getStatus());

    when(userHandler.findUserByName(eq(USER_2), any())).thenReturn(user2);
    data.put("userName", USER_2);
    data.put("lastName", "");
    data.put("firstName", USER_2);
    data.put("password", "password");
    data.put("email", email2);
    response = getResponse("PUT", "/v1/users", data.toString());
    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertEquals(response.getEntity().toString(), 400, response.getStatus());

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", "");
    data.put("password", "password");
    data.put("email", email2);
    response = getResponse("PUT", "/v1/users", data.toString());
    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertEquals(response.getEntity().toString(), 400, response.getStatus());

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "");
    data.put("email", email2);
    response = getResponse("PUT", "/v1/users", data.toString());
    assertNotNull(response);
    assertNull(response.getEntity());
    assertEquals(204, response.getStatus());

    when(userHandler.findUserByName(eq(USER_1), any())).thenReturn(user1);
    data.put("userName", USER_1);
    data.put("lastName", USER_1);
    data.put("firstName", USER_1);
    data.put("password", "");
    data.put("enabled", false);
    data.put("email", email1);
    response = getResponse("PUT", "/v1/users", data.toString());
    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertEquals(400, response.getStatus());
    assertNotNull(response.getEntity());
    assertEquals("SelfDisable", response.getEntity().toString());

    verify(userHandler, atMost(0)).saveUser(any(User.class), anyBoolean());
    verify(userHandler, atMost(0)).setEnabled(anyString(), anyBoolean(), anyBoolean());

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "newPassword1");
    data.put("email", email2);
    response = getResponse("PUT", "/v1/users", data.toString());
    assertNotNull(response);
    assertNull(response.getEntity());
    assertEquals(204, response.getStatus());

    verify(userHandler, times(0)).saveUser(user2, true);
    verify(changePasswordConnector, times(1)).changePassword(USER_2, "newPassword1");
    verify(userHandler, atMost(0)).setEnabled(anyString(), anyBoolean(), anyBoolean());
    
    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "");
    data.put("email", email2);
    data.put("enabled", true);
    response = getResponse("PUT", "/v1/users", data.toString());
    assertNotNull(response);
    assertNull(response.getEntity());
    assertEquals(204, response.getStatus());

    verify(userHandler, atMost(1)).saveUser(eq(user2), eq(true));
    verify(userHandler, atLeast(1)).setEnabled(anyString(), anyBoolean(), anyBoolean());

    user2.setEnabled(true);
    when(userACL.getSuperUser()).thenReturn(USER_2);

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "");
    data.put("enabled", false);
    data.put("email", email2);
    response = getResponse("PUT", "/v1/users", data.toString());
    assertNotNull(response);
    assertEquals(400, response.getStatus());
    assertNotNull(response.getEntity());
    assertEquals("DisableSuperUser", response.getEntity().toString());

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "");
    data.put("enabled", true);
    data.put("email", email2);
    response = getResponse("PUT", "/v1/users", data.toString());
    assertNotNull(response);
    assertNull(response.getEntity());
    assertEquals(204, response.getStatus());
    user2.setDisplayName(USER_2+" "+USER_2);
    verify(userHandler, atMost(1)).saveUser(eq(user2), eq(true));

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

    ContainerResponse response = launcher.service("DELETE", "/v1/users/NOT_FOUND", "", null, null, null);
    assertNotNull(response);
    assertEquals(404, response.getStatus());

    verify(userHandler, atMost(0)).removeUser(anyString(), anyBoolean());

    response = launcher.service("DELETE", "/v1/users/" + USER_1, "", null, null, null);
    assertNotNull(response);
    assertEquals(400, response.getStatus());
    assertEquals("SelfDelete", response.getEntity());

    verify(userHandler, atMost(0)).removeUser(anyString(), anyBoolean());

    response = launcher.service("DELETE", "/v1/users/" + USER_2, "", null, null, null);
    assertNotNull(response);
    assertEquals(204, response.getStatus());

    verify(userHandler, atLeastOnce()).removeUser(anyString(), anyBoolean());

    when(userACL.getSuperUser()).thenReturn(USER_2);
    response = launcher.service("DELETE", "/v1/users/" + USER_2, "", null, null, null);
    assertNotNull(response);
    assertEquals(400, response.getStatus());
    assertEquals("DeleteSuperUser", response.getEntity());
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
