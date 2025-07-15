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
package org.exoplatform.web.login.recovery;

import static org.exoplatform.web.login.recovery.PasswordRecoveryHandler.ACTION_PARAM;
import static org.exoplatform.web.login.recovery.PasswordRecoveryHandler.ERROR_MESSAGE_PARAM;
import static org.exoplatform.web.login.recovery.PasswordRecoveryHandler.EXPIRED_ACTION_NAME;
import static org.exoplatform.web.login.recovery.PasswordRecoveryHandler.FORM_URL_PARAM;
import static org.exoplatform.web.login.recovery.PasswordRecoveryHandler.INITIAL_URI_PARAM;
import static org.exoplatform.web.login.recovery.PasswordRecoveryHandler.INIT_URL;
import static org.exoplatform.web.login.recovery.PasswordRecoveryHandler.NAME;
import static org.exoplatform.web.login.recovery.PasswordRecoveryHandler.PASSWORD_CONFIRM_PARAM;
import static org.exoplatform.web.login.recovery.PasswordRecoveryHandler.PASSWORD_PARAM;
import static org.exoplatform.web.login.recovery.PasswordRecoveryHandler.RESET_PASSWORD_ACTION_NAME;
import static org.exoplatform.web.login.recovery.PasswordRecoveryHandler.SEND_ACTION_NAME;
import static org.exoplatform.web.login.recovery.PasswordRecoveryHandler.SUCCESS_MESSAGE_PARAM;
import static org.exoplatform.web.login.recovery.PasswordRecoveryHandler.TOKEN;
import static org.exoplatform.web.login.recovery.PasswordRecoveryHandler.TOKEN_ID_PARAM;
import static org.exoplatform.web.login.recovery.PasswordRecoveryHandler.USERNAME_PARAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.utils.I18N;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.resources.impl.LocaleConfigImpl;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.security.security.CookieTokenService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PasswordRecoveryHandlerTest {

  private static final Locale     REQUEST_LOCALE = Locale.ENGLISH;

  private static final String     CONTEXT_PATH   = "/portal";

  private static final String     INITIAL_URI    = "initUri";

  private static final String     TOKEN_VALUE    = "tokenValue";

  @Mock
  private ServletContext          servletContext;

  @Mock
  private PortalContainer         container;

  @Mock
  private PasswordRecoveryService passwordRecoveryService;

  @Mock
  private ResourceBundleService   resourceBundleService;

  @Mock
  private ResourceBundle          resourceBundle;

  @Mock
  private OrganizationService     organizationService;

  @Mock
  private UserHandler             userHandler;

  @Mock
  private LocaleConfigService     localeConfigService;

  @Mock
  private BrandingService         brandingService;

  @Mock
  private JavascriptConfigService javascriptConfigService;

  @Mock
  private WebAppController        controller;

  @Mock
  private Router                  router;

  @Mock
  private HttpServletRequest      request;

  @Mock
  private HttpServletResponse     response;

  @Mock
  private RequestDispatcher       requestDispatcher;

  @Mock
  private SkinService             skinService;

  private ControllerContext       controllerContext;

  private PasswordRecoveryHandler passwordRecoveryHandler;

  private Map<String, Object>     applicationParameters;

  @Before
  public void setUp() throws Exception {
    this.applicationParameters = null;
    ExoContainerContext.setCurrentContainer(container);
    lenient().when(container.getComponentInstanceOfType(ResourceBundleService.class)).thenReturn(resourceBundleService);

    when(container.getPortalContext()).thenReturn(servletContext);
    when(request.getContextPath()).thenReturn(CONTEXT_PATH);
    when(servletContext.getContextPath()).thenReturn(CONTEXT_PATH);

    when(request.getLocale()).thenReturn(REQUEST_LOCALE);
    LocaleConfigImpl localeConfig = new LocaleConfigImpl();
    localeConfig.setLocale(REQUEST_LOCALE);
    when(localeConfigService.getLocaleConfig(REQUEST_LOCALE.getLanguage())).thenReturn(localeConfig);

    when(resourceBundleService.getSharedResourceBundleNames()).thenReturn(new String[0]);
    when(resourceBundleService.getResourceBundle(any(String[].class), eq(REQUEST_LOCALE))).thenReturn(resourceBundle);
    when(resourceBundle.getString(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

    when(javascriptConfigService.getJSConfig()).thenReturn(new JSONObject());

    when(servletContext.getRequestDispatcher(any())).thenReturn(requestDispatcher);
    when(passwordRecoveryService.getPasswordRecoverURL(null, I18N.toTagIdentifier(REQUEST_LOCALE))).thenReturn("/" + NAME);

    when(organizationService.getUserHandler()).thenReturn(userHandler);

    passwordRecoveryHandler = new PasswordRecoveryHandler(container,
                                                          passwordRecoveryService,
                                                          resourceBundleService,
                                                          organizationService,
                                                          localeConfigService,
                                                          brandingService,
                                                          javascriptConfigService,
                                                          skinService) {
      @Override
      protected void extendApplicationParameters(JSONObject applicationParameters,
                                                 Map<String, Object> additionalParameters) {
        PasswordRecoveryHandlerTest.this.applicationParameters = additionalParameters;
        super.extendApplicationParameters(applicationParameters, additionalParameters);
      }
    };
  }

  @After
  public void teardown() throws Exception {
    ExoContainerContext.setCurrentContainer(null);
  }

  @Test
  public void testDisplayForgotPasswordPage() throws Exception {
    prepareResetFormContext();
    passwordRecoveryHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(CONTEXT_PATH + "/" + NAME, applicationParameters.get(FORM_URL_PARAM));
    assertEquals(INITIAL_URI, applicationParameters.get(INITIAL_URI_PARAM));
    assertFalse(applicationParameters.containsKey(ERROR_MESSAGE_PARAM));
    assertFalse(applicationParameters.containsKey(SUCCESS_MESSAGE_PARAM));
    assertFalse(applicationParameters.containsKey(USERNAME_PARAM));

    verify(passwordRecoveryService, never()).changePass(any(), any(), any(), any());
    verify(passwordRecoveryService, never()).sendRecoverPasswordEmail(any(), any(), any());
  }

  @Test
  public void testSendResetPasswordUrlWhenEmptyUsername() throws Exception {
    prepareResetFormContext();
    when(request.getParameter(ACTION_PARAM)).thenReturn(SEND_ACTION_NAME);

    passwordRecoveryHandler.execute(controllerContext);
    assertNotNull(applicationParameters);
    assertEquals(CONTEXT_PATH + "/" + NAME, applicationParameters.get(FORM_URL_PARAM));
    assertEquals(INITIAL_URI, applicationParameters.get(INITIAL_URI_PARAM));
    assertEquals("gatein.forgotPassword.emptyUserOrEmail", applicationParameters.get(ERROR_MESSAGE_PARAM));

    verify(passwordRecoveryService, never()).changePass(any(), any(), any(), any());
    verify(passwordRecoveryService, never()).sendRecoverPasswordEmail(any(), any(), any());
  }

  @Test
  public void testSendResetPasswordUrlWhenUserNotFound() throws Exception {
    prepareResetFormContext();
    String username = "user";

    when(request.getParameter(ACTION_PARAM)).thenReturn(SEND_ACTION_NAME);
    when(request.getParameter(USERNAME_PARAM)).thenReturn(username);

    passwordRecoveryHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(CONTEXT_PATH + "/" + NAME, applicationParameters.get(FORM_URL_PARAM));
    assertEquals(INITIAL_URI, applicationParameters.get(INITIAL_URI_PARAM));
    assertFalse(applicationParameters.containsKey(ERROR_MESSAGE_PARAM));
    assertEquals("gatein.forgotPassword.emailSendSuccessful", applicationParameters.get(SUCCESS_MESSAGE_PARAM));
    assertEquals(username, applicationParameters.get(USERNAME_PARAM));

    verify(passwordRecoveryService, never()).changePass(any(), any(), any(), any());
    verify(passwordRecoveryService, never()).sendRecoverPasswordEmail(any(), any(), any());
  }

  @Test
  public void testSendResetPasswordUrlWhenUserNotEnabled() throws Exception {
    prepareResetFormContext();
    String username = "user";

    when(request.getParameter(ACTION_PARAM)).thenReturn(SEND_ACTION_NAME);
    when(request.getParameter(USERNAME_PARAM)).thenReturn(username);

    User user = mock(User.class);
    when(user.isEnabled()).thenReturn(false);
    when(userHandler.findUserByName(eq(username), any())).thenReturn(user);

    passwordRecoveryHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(CONTEXT_PATH + "/" + NAME, applicationParameters.get(FORM_URL_PARAM));
    assertEquals(INITIAL_URI, applicationParameters.get(INITIAL_URI_PARAM));
    assertFalse(applicationParameters.containsKey(ERROR_MESSAGE_PARAM));
    assertEquals("gatein.forgotPassword.emailSendSuccessful", applicationParameters.get(SUCCESS_MESSAGE_PARAM));
    assertEquals(username, applicationParameters.get(USERNAME_PARAM));

    verify(passwordRecoveryService, never()).changePass(any(), any(), any(), any());
    verify(passwordRecoveryService, never()).sendRecoverPasswordEmail(any(), any(), any());
  }

  @Test
  public void testSendResetPasswordUrlWhenUserEnabledAndSendFailure() throws Exception {
    prepareResetFormContext();
    String username = "user";

    when(request.getParameter(ACTION_PARAM)).thenReturn(SEND_ACTION_NAME);
    when(request.getParameter(USERNAME_PARAM)).thenReturn(username);

    User user = mock(User.class);
    when(user.isEnabled()).thenReturn(true);
    when(userHandler.findUserByName(eq(username), any())).thenReturn(user);

    passwordRecoveryHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(CONTEXT_PATH + "/" + NAME, applicationParameters.get(FORM_URL_PARAM));
    assertEquals(INITIAL_URI, applicationParameters.get(INITIAL_URI_PARAM));
    assertEquals(username, applicationParameters.get(USERNAME_PARAM));

    verify(passwordRecoveryService, never()).changePass(any(), any(), any(), any());
    verify(passwordRecoveryService, times(1)).sendRecoverPasswordEmail(eq(user), eq(REQUEST_LOCALE), any(HttpServletRequest.class));

    assertFalse(applicationParameters.containsKey(SUCCESS_MESSAGE_PARAM));
    assertEquals("gatein.forgotPassword.emailSendFailure", applicationParameters.get(ERROR_MESSAGE_PARAM));
  }

  @Test
  public void testGetRequiresLifeCycle() {
    assertTrue(passwordRecoveryHandler.getRequiresLifeCycle());
  }

  @Test
  public void testGetHandlerName() {
    assertEquals(NAME, passwordRecoveryHandler.getHandlerName());
  }

  @Test
  public void testSendResetPasswordUrlWhenUserEnabledAndSendSuccess() throws Exception {
    prepareResetFormContext();
    String username = "user";

    when(request.getParameter(ACTION_PARAM)).thenReturn(SEND_ACTION_NAME);
    when(request.getParameter(USERNAME_PARAM)).thenReturn(username);

    User user = mock(User.class);
    when(user.isEnabled()).thenReturn(true);
    when(userHandler.findUserByName(eq(username), any())).thenReturn(user);
    when(passwordRecoveryService.sendRecoverPasswordEmail(any(), any(), any())).thenReturn(true);

    passwordRecoveryHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(CONTEXT_PATH + "/" + NAME, applicationParameters.get(FORM_URL_PARAM));
    assertEquals(INITIAL_URI, applicationParameters.get(INITIAL_URI_PARAM));
    assertEquals(username, applicationParameters.get(USERNAME_PARAM));

    verify(passwordRecoveryService, never()).changePass(any(), any(), any(), any());
    verify(passwordRecoveryService, times(1)).sendRecoverPasswordEmail(eq(user), eq(REQUEST_LOCALE), any(HttpServletRequest.class));

    assertFalse(applicationParameters.containsKey(ERROR_MESSAGE_PARAM));
    assertEquals("gatein.forgotPassword.emailSendSuccessful", applicationParameters.get(SUCCESS_MESSAGE_PARAM));
  }

  @Test
  public void testDisplayTokenExpiredPage() throws Exception {
    prepareResetPasswordContext();

    passwordRecoveryHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertFalse(applicationParameters.containsKey(USERNAME_PARAM));
    assertFalse(applicationParameters.containsKey(ERROR_MESSAGE_PARAM));
    assertFalse(applicationParameters.containsKey(SUCCESS_MESSAGE_PARAM));
    assertEquals(EXPIRED_ACTION_NAME, applicationParameters.get(ACTION_PARAM));

    verify(passwordRecoveryService, never()).changePass(any(), any(), any(), any());
    verify(passwordRecoveryService, never()).sendRecoverPasswordEmail(any(), any(), any());
  }

  @Test
  public void testDisplayResetPassword() throws Exception {
    prepareResetPasswordContext();
    String username = "user";
    when(passwordRecoveryService.verifyToken(TOKEN_VALUE, CookieTokenService.FORGOT_PASSWORD_TOKEN)).thenReturn(username);

    passwordRecoveryHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(username, applicationParameters.get(USERNAME_PARAM));
    assertEquals(TOKEN_VALUE, applicationParameters.get(TOKEN_ID_PARAM));
    assertEquals(RESET_PASSWORD_ACTION_NAME, applicationParameters.get(ACTION_PARAM));
    assertFalse(applicationParameters.containsKey(ERROR_MESSAGE_PARAM));
    assertFalse(applicationParameters.containsKey(SUCCESS_MESSAGE_PARAM));

    verify(passwordRecoveryService, never()).changePass(any(), any(), any(), any());
    verify(passwordRecoveryService, never()).sendRecoverPasswordEmail(any(), any(), any());
  }

  @Test
  public void testDisplayResetPasswordUserNotMatch() throws Exception {
    prepareResetPasswordContext();

    String username = "user";
    String password = "pass1";
    String passwordConfirm = "pass2";
    when(passwordRecoveryService.verifyToken(TOKEN_VALUE, CookieTokenService.FORGOT_PASSWORD_TOKEN)).thenReturn(username);
    when(request.getParameter(ACTION_PARAM)).thenReturn(RESET_PASSWORD_ACTION_NAME);
    when(request.getParameter(USERNAME_PARAM)).thenReturn("user2");
    when(request.getParameter(PASSWORD_PARAM)).thenReturn(password);
    when(request.getParameter(PASSWORD_CONFIRM_PARAM)).thenReturn(passwordConfirm);

    passwordRecoveryHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(username, applicationParameters.get(USERNAME_PARAM));
    assertEquals(TOKEN_VALUE, applicationParameters.get(TOKEN_ID_PARAM));
    assertEquals(RESET_PASSWORD_ACTION_NAME, applicationParameters.get(ACTION_PARAM));
    assertEquals("gatein.forgotPassword.usernameChanged", applicationParameters.get(ERROR_MESSAGE_PARAM));
    assertFalse(applicationParameters.containsKey(SUCCESS_MESSAGE_PARAM));
    assertEquals(password, applicationParameters.get(PASSWORD_PARAM));
    assertEquals(passwordConfirm, applicationParameters.get(PASSWORD_CONFIRM_PARAM));

    verify(passwordRecoveryService, never()).changePass(any(), any(), any(), any());
    verify(passwordRecoveryService, never()).sendRecoverPasswordEmail(any(), any(), any());
  }

  @Test
  public void testDisplayResetPasswordWhenBothPasswordsNotMatch() throws Exception {
    prepareResetPasswordContext();

    String username = "user";
    String password = "pass1";
    String passwordConfirm = "pass2";

    when(passwordRecoveryService.verifyToken(TOKEN_VALUE, CookieTokenService.FORGOT_PASSWORD_TOKEN)).thenReturn(username);
    when(request.getParameter(ACTION_PARAM)).thenReturn(RESET_PASSWORD_ACTION_NAME);
    when(request.getParameter(USERNAME_PARAM)).thenReturn(username);
    when(request.getParameter(PASSWORD_PARAM)).thenReturn(password);
    when(request.getParameter(PASSWORD_CONFIRM_PARAM)).thenReturn(passwordConfirm);

    passwordRecoveryHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(username, applicationParameters.get(USERNAME_PARAM));
    assertEquals(TOKEN_VALUE, applicationParameters.get(TOKEN_ID_PARAM));
    assertEquals(RESET_PASSWORD_ACTION_NAME, applicationParameters.get(ACTION_PARAM));
    assertEquals("gatein.forgotPassword.confirmPasswordNotMatch", applicationParameters.get(ERROR_MESSAGE_PARAM));
    assertFalse(applicationParameters.containsKey(SUCCESS_MESSAGE_PARAM));
    assertEquals(password, applicationParameters.get(PASSWORD_PARAM));
    assertEquals(passwordConfirm, applicationParameters.get(PASSWORD_CONFIRM_PARAM));

    verify(passwordRecoveryService, never()).changePass(any(), any(), any(), any());
    verify(passwordRecoveryService, never()).sendRecoverPasswordEmail(any(), any(), any());
  }

  @Test
  public void testDisplayResetPasswordWhenPasswordNotValid() throws Exception {
    prepareResetPasswordContext();

    String username = "user";
    String password = "pass1";
    String passwordConfirm = password;

    when(passwordRecoveryService.verifyToken(TOKEN_VALUE, CookieTokenService.FORGOT_PASSWORD_TOKEN)).thenReturn(username);
    when(request.getParameter(ACTION_PARAM)).thenReturn(RESET_PASSWORD_ACTION_NAME);
    when(request.getParameter(USERNAME_PARAM)).thenReturn(username);
    when(request.getParameter(PASSWORD_PARAM)).thenReturn(password);
    when(request.getParameter(PASSWORD_CONFIRM_PARAM)).thenReturn(passwordConfirm);

    PropertyManager.setProperty("gatein.validators.passwordpolicy.length.max", "8");
    PropertyManager.setProperty("gatein.validators.passwordpolicy.length.min", "255");
    try {
      passwordRecoveryHandler.execute(controllerContext);
    } finally {
      PropertyManager.setProperty("gatein.validators.passwordpolicy.length.max", "");
      PropertyManager.setProperty("gatein.validators.passwordpolicy.length.min", "");
    }

    assertNotNull(applicationParameters);
    assertEquals(username, applicationParameters.get(USERNAME_PARAM));
    assertEquals(TOKEN_VALUE, applicationParameters.get(TOKEN_ID_PARAM));
    assertEquals(RESET_PASSWORD_ACTION_NAME, applicationParameters.get(ACTION_PARAM));
    assertEquals("onboarding.login.passwordCondition", applicationParameters.get(ERROR_MESSAGE_PARAM));
    assertFalse(applicationParameters.containsKey(SUCCESS_MESSAGE_PARAM));

    verify(passwordRecoveryService, never()).changePass(any(), any(), any(), any());
    verify(passwordRecoveryService, never()).sendRecoverPasswordEmail(any(), any(), any());
  }

  private void prepareResetFormContext() {
    controllerContext = new ControllerContext(controller,
                                              router,
                                              request,
                                              response,
                                              Collections.singletonMap(INIT_URL, INITIAL_URI));
  }

  private void prepareResetPasswordContext() {
    Map<QualifiedName, String> parameters = new HashMap<>();
    parameters.put(INIT_URL, INITIAL_URI);
    parameters.put(TOKEN, TOKEN_VALUE);
    controllerContext = new ControllerContext(controller,
                                              router,
                                              request,
                                              response,
                                              parameters);
  }

}
