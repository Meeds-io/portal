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
package org.exoplatform.web.register;

import static org.exoplatform.web.register.ExternalRegisterHandler.ACTION_PARAM;
import static org.exoplatform.web.register.ExternalRegisterHandler.ALREADY_AUTHENTICATED_MESSAGE_PARAM;
import static org.exoplatform.web.register.ExternalRegisterHandler.CAPTCHA_PARAM;
import static org.exoplatform.web.register.ExternalRegisterHandler.EMAIL_PARAM;
import static org.exoplatform.web.register.ExternalRegisterHandler.EMAIL_VERIFICATION_SENT;
import static org.exoplatform.web.register.ExternalRegisterHandler.*;
import static org.exoplatform.web.register.ExternalRegisterHandler.EXPIRED_ACTION_NAME;
import static org.exoplatform.web.register.ExternalRegisterHandler.FIRSTNAME_PARAM;
import static org.exoplatform.web.register.ExternalRegisterHandler.LASTNAME_PARAM;
import static org.exoplatform.web.register.ExternalRegisterHandler.LOGIN;
import static org.exoplatform.web.register.ExternalRegisterHandler.NAME;
import static org.exoplatform.web.register.ExternalRegisterHandler.PASSWORD_CONFIRM_PARAM;
import static org.exoplatform.web.register.ExternalRegisterHandler.PASSWORD_PARAM;
import static org.exoplatform.web.register.ExternalRegisterHandler.REQUIRE_EMAIL_VALIDATION;
import static org.exoplatform.web.register.ExternalRegisterHandler.SAVE_EXTERNAL_ACTION;
import static org.exoplatform.web.register.ExternalRegisterHandler.SUCCESS_MESSAGE_PARAM;
import static org.exoplatform.web.register.ExternalRegisterHandler.TOKEN;
import static org.exoplatform.web.register.ExternalRegisterHandler.TOKEN_ID_PARAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.gatein.wci.security.Credentials;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.idm.UserImpl;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.resources.impl.LocaleConfigImpl;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.web.security.security.RemindPasswordTokenService;

import io.meeds.portal.security.service.SecuritySettingService;

import nl.captcha.Captcha;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ExternalRegisterHandlerTest {

  private static final Locale        REQUEST_LOCALE = Locale.ENGLISH;

  private static final String        CONTEXT_PATH   = "/portal";       // NOSONAR

  private static final String        TOKEN_VALUE    = "tokenValue";

  private static final String        EMAIL          = "email@test.com";

  private static final String        LASTNAME       = "lastName";

  private static final String        FIRSTNAME      = "firstName";

  private static final String        CAPTCHA_VALUE  = "captchaValue";

  @Mock
  private ServletContext             servletContext;

  @Mock
  private PortalContainer            container;

  @Mock
  private RemindPasswordTokenService remindPasswordTokenService;

  @Mock
  private PasswordRecoveryService    passwordRecoveryService;

  @Mock
  private ResourceBundleService      resourceBundleService;

  @Mock
  private ResourceBundle             resourceBundle;

  @Mock
  private OrganizationService        organizationService;

  @Mock
  private UserHandler                userHandler;

  @Mock
  private GroupHandler               groupHandler;

  @Mock
  private MembershipTypeHandler      membershipTypeHandler;

  @Mock
  private MembershipHandler          membershipHandler;

  @Mock
  private LocaleConfigService        localeConfigService;

  @Mock
  private BrandingService            brandingService;

  @Mock
  private JavascriptConfigService    javascriptConfigService;

  @Mock
  private WebAppController           controller;

  @Mock
  private SecuritySettingService     securitySettingService;

  @Mock
  private Router                     router;

  @Mock
  private InitParams                 params;

  @Mock
  private HttpSession                session;

  @Mock
  private HttpServletRequest         request;

  @Mock
  private HttpServletResponse        response;

  @Mock
  private RequestDispatcher          requestDispatcher;

  @Mock
  private SkinService                skinService;

  @Mock
  private Captcha                    captcha;

  @Mock
  private Credentials                credentials;

  private ControllerContext          controllerContext;

  private ExternalRegisterHandler    externalRegisterHandler;

  private Map<String, Object>        applicationParameters;

  @Before
  public void setUp() throws Exception { // NOSONAR
    this.applicationParameters = null;
    ExoContainerContext.setCurrentContainer(container);
    lenient().when(container.getComponentInstanceOfType(ResourceBundleService.class)).thenReturn(resourceBundleService);

    when(container.getPortalContext()).thenReturn(servletContext);
    when(request.getContextPath()).thenReturn(CONTEXT_PATH);
    when(servletContext.getContextPath()).thenReturn(CONTEXT_PATH);
    when(request.getSession()).thenReturn(session);
    when(session.getAttribute(NAME)).thenReturn(captcha);
    when(captcha.isCorrect(CAPTCHA_VALUE)).thenReturn(true);
    when(request.getParameter(CAPTCHA_PARAM)).thenReturn(CAPTCHA_VALUE);

    when(request.getLocale()).thenReturn(REQUEST_LOCALE);
    LocaleConfigImpl localeConfig = new LocaleConfigImpl();
    localeConfig.setLocale(REQUEST_LOCALE);
    when(localeConfigService.getLocaleConfig(REQUEST_LOCALE.getLanguage())).thenReturn(localeConfig);

    when(resourceBundleService.getSharedResourceBundleNames()).thenReturn(new String[0]);
    when(resourceBundleService.getResourceBundle(any(String[].class), eq(REQUEST_LOCALE))).thenReturn(resourceBundle);
    when(resourceBundle.getString(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

    when(javascriptConfigService.getJSConfig()).thenReturn(new JSONObject());

    when(servletContext.getRequestDispatcher(any())).thenReturn(requestDispatcher);
    when(request.getRequestDispatcher(any())).thenReturn(requestDispatcher);

    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(organizationService.getGroupHandler()).thenReturn(groupHandler);
    when(organizationService.getMembershipTypeHandler()).thenReturn(membershipTypeHandler);
    when(organizationService.getMembershipHandler()).thenReturn(membershipHandler);
    when(securitySettingService.getRegistrationGroupIds()).thenReturn(new String[] { "/platform/external" });
    when(passwordRecoveryService.verifyToken(TOKEN_VALUE, CookieTokenService.EXTERNAL_REGISTRATION_TOKEN)).thenReturn(EMAIL);

    externalRegisterHandler = new ExternalRegisterHandler(container,
                                                          remindPasswordTokenService,
                                                          passwordRecoveryService,
                                                          resourceBundleService,
                                                          organizationService,
                                                          localeConfigService,
                                                          brandingService,
                                                          securitySettingService,
                                                          javascriptConfigService,
                                                          skinService) {
      @Override
      protected void extendApplicationParameters(JSONObject applicationParameters, Map<String, Object> additionalParameters) {
        ExternalRegisterHandlerTest.this.applicationParameters = additionalParameters;
        super.extendApplicationParameters(applicationParameters, additionalParameters);
      }
    };
  }

  @After
  public void teardown() {
    ExoContainerContext.setCurrentContainer(null);
  }

  @Test
  public void testGetRequiresLifeCycle() {
    assertTrue(externalRegisterHandler.getRequiresLifeCycle());
  }

  @Test
  public void testGetHandlerName() {
    assertEquals(NAME, externalRegisterHandler.getHandlerName());
  }

  @Test
  public void testDisplayTokenExpiredPage() throws Exception {
    prepareResetPasswordContext();
    reset(passwordRecoveryService);

    externalRegisterHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertFalse(applicationParameters.containsKey(ERROR_MESSAGE_PARAM));
    assertEquals(EXPIRED_ACTION_NAME, applicationParameters.get(ACTION_PARAM));

    verify(passwordRecoveryService, never()).sendAccountCreatedConfirmationEmail(any(), any(), any());
  }

  @Test
  public void testDisplayExternalRegistrationPage() throws Exception {
    prepareResetPasswordContext();

    externalRegisterHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(EMAIL, applicationParameters.get(EMAIL_PARAM));
    assertEquals(TOKEN_VALUE, applicationParameters.get(TOKEN_ID_PARAM));
    assertFalse(applicationParameters.containsKey(ERROR_MESSAGE_PARAM));

    verify(passwordRecoveryService, never()).sendAccountCreatedConfirmationEmail(any(), any(), any());
  }

  @Test
  public void testDisplayExternalRegistrationUserNotMatch() throws Exception {
    prepareResetPasswordContext();

    String password = "pass1";
    String passwordConfirm = "pass2";

    when(request.getParameter(ACTION_PARAM)).thenReturn(SAVE_EXTERNAL_ACTION);
    when(request.getParameter(EMAIL_PARAM)).thenReturn("email2");
    when(request.getParameter(PASSWORD_PARAM)).thenReturn(password);
    when(request.getParameter(PASSWORD_CONFIRM_PARAM)).thenReturn(passwordConfirm);

    externalRegisterHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(EMAIL, applicationParameters.get(EMAIL_PARAM));
    assertEquals(TOKEN_VALUE, applicationParameters.get(TOKEN_ID_PARAM));
    assertEquals("gatein.forgotPassword.usernameChanged", applicationParameters.get(ERROR_MESSAGE_PARAM));
    assertNull(applicationParameters.get(ERROR_FIELD_PARAM));
    assertEquals(password, applicationParameters.get(PASSWORD_PARAM));
    assertEquals(passwordConfirm, applicationParameters.get(PASSWORD_CONFIRM_PARAM));

    verify(passwordRecoveryService, never()).sendAccountCreatedConfirmationEmail(any(), any(), any());
  }

  @Test
  public void testDisplayExternalRegistrationWhenBothPasswordsNotMatch() throws Exception {
    prepareResetPasswordContext();

    String password = "pass1234";
    String passwordConfirm = "pass123";

    when(request.getParameter(ACTION_PARAM)).thenReturn(SAVE_EXTERNAL_ACTION);
    when(request.getParameter(EMAIL_PARAM)).thenReturn(EMAIL);
    when(request.getParameter(PASSWORD_PARAM)).thenReturn(password);
    when(request.getParameter(PASSWORD_CONFIRM_PARAM)).thenReturn(passwordConfirm);

    externalRegisterHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(EMAIL, applicationParameters.get(EMAIL_PARAM));
    assertEquals(TOKEN_VALUE, applicationParameters.get(TOKEN_ID_PARAM));
    assertEquals("gatein.forgotPassword.confirmPasswordNotMatch", applicationParameters.get(ERROR_MESSAGE_PARAM));
    assertEquals(PASSWORD_CONFIRM_PARAM, applicationParameters.get(ERROR_FIELD_PARAM));
    assertEquals(password, applicationParameters.get(PASSWORD_PARAM));
    assertEquals(passwordConfirm, applicationParameters.get(PASSWORD_CONFIRM_PARAM));

    verify(passwordRecoveryService, never()).sendAccountCreatedConfirmationEmail(any(), any(), any());
  }

  @Test
  public void testDisplayExternalRegistrationWhenPasswordNotValid() throws Exception {
    prepareResetPasswordContext();

    String password = "pass1";
    String passwordConfirm = password;

    when(request.getParameter(ACTION_PARAM)).thenReturn(SAVE_EXTERNAL_ACTION);
    when(request.getParameter(EMAIL_PARAM)).thenReturn(EMAIL);
    when(request.getParameter(PASSWORD_PARAM)).thenReturn(password);
    when(request.getParameter(PASSWORD_CONFIRM_PARAM)).thenReturn(passwordConfirm);

    PropertyManager.setProperty("gatein.validators.passwordpolicy.length.max", "8");
    PropertyManager.setProperty("gatein.validators.passwordpolicy.length.min", "255");
    try {
      externalRegisterHandler.execute(controllerContext);
    } finally {
      PropertyManager.setProperty("gatein.validators.passwordpolicy.length.max", "");
      PropertyManager.setProperty("gatein.validators.passwordpolicy.length.min", "");
    }

    assertNotNull(applicationParameters);
    assertEquals(EMAIL, applicationParameters.get(EMAIL_PARAM));
    assertEquals(TOKEN_VALUE, applicationParameters.get(TOKEN_ID_PARAM));
    assertEquals("onboarding.login.passwordCondition", applicationParameters.get(ERROR_MESSAGE_PARAM));
    assertEquals(PASSWORD_PARAM, applicationParameters.get(ERROR_FIELD_PARAM));

    verify(passwordRecoveryService, never()).sendAccountCreatedConfirmationEmail(any(), any(), any());
  }

  @Test
  public void testDisplayExternalRegistrationWhenFirstNameNotValid() throws Exception {
    prepareResetPasswordContext();

    String password = "pass1234";
    String passwordConfirm = password;

    when(request.getParameter(ACTION_PARAM)).thenReturn(SAVE_EXTERNAL_ACTION);
    when(request.getParameter(EMAIL_PARAM)).thenReturn(EMAIL);
    when(request.getParameter(LASTNAME_PARAM)).thenReturn(LASTNAME);
    when(request.getParameter(PASSWORD_PARAM)).thenReturn(password);
    when(request.getParameter(PASSWORD_CONFIRM_PARAM)).thenReturn(passwordConfirm);

    externalRegisterHandler.execute(controllerContext);

    assertEquals("EmptyFieldValidator.msg.empty-input", applicationParameters.get(ERROR_MESSAGE_PARAM));
    assertEquals(FIRSTNAME_PARAM, applicationParameters.get(ERROR_FIELD_PARAM));

    verify(passwordRecoveryService, never()).sendAccountCreatedConfirmationEmail(any(), any(), any());
  }

  @Test
  public void testDisplayExternalRegistrationWhenLastNameNotValid() throws Exception {
    prepareResetPasswordContext();

    String password = "pass1234";
    String passwordConfirm = password;

    when(request.getParameter(ACTION_PARAM)).thenReturn(SAVE_EXTERNAL_ACTION);
    when(request.getParameter(EMAIL_PARAM)).thenReturn(EMAIL);
    when(request.getParameter(FIRSTNAME_PARAM)).thenReturn(FIRSTNAME);
    when(request.getParameter(PASSWORD_PARAM)).thenReturn(password);
    when(request.getParameter(PASSWORD_CONFIRM_PARAM)).thenReturn(passwordConfirm);

    externalRegisterHandler.execute(controllerContext);

    assertEquals("EmptyFieldValidator.msg.empty-input", applicationParameters.get(ERROR_MESSAGE_PARAM));
    assertEquals(LASTNAME_PARAM, applicationParameters.get(ERROR_FIELD_PARAM));

    verify(passwordRecoveryService, never()).sendAccountCreatedConfirmationEmail(any(), any(), any());
  }

  @Test
  public void testDisplayExternalRegistrationWhenErrorCreatingUser() throws Exception {
    prepareResetPasswordContext();

    String password = "pass1234";
    String passwordConfirm = password;

    when(request.getParameter(ACTION_PARAM)).thenReturn(SAVE_EXTERNAL_ACTION);
    when(request.getParameter(EMAIL_PARAM)).thenReturn(EMAIL);
    when(request.getParameter(FIRSTNAME_PARAM)).thenReturn(FIRSTNAME);
    when(request.getParameter(LASTNAME_PARAM)).thenReturn(LASTNAME);
    when(request.getParameter(PASSWORD_PARAM)).thenReturn(password);
    when(request.getParameter(PASSWORD_CONFIRM_PARAM)).thenReturn(passwordConfirm);
    when(userHandler.createUserInstance(any())).thenThrow(IllegalStateException.class);

    externalRegisterHandler.execute(controllerContext);

    assertEquals("external.registration.fail.create.user", applicationParameters.get(ERROR_MESSAGE_PARAM));
    assertNull(applicationParameters.get(ERROR_FIELD_PARAM));

    verify(passwordRecoveryService, never()).sendAccountCreatedConfirmationEmail(any(), any(), any());
  }

  @Test
  public void testDisplayExternalRegistrationWithInfoWhenAlreadyLoggedIn() throws Exception {
    prepareResetPasswordContext();

    when(request.getRemoteUser()).thenReturn("username");

    externalRegisterHandler.execute(controllerContext);

    assertEquals("true", applicationParameters.get(ALREADY_AUTHENTICATED_MESSAGE_PARAM));
  }

  @Test
  public void testRedirectToLoginWhenValid() throws Exception {
    prepareResetPasswordContext();

    String password = "pass1234";
    String passwordConfirm = password;

    when(request.getParameter(ACTION_PARAM)).thenReturn(SAVE_EXTERNAL_ACTION);
    when(request.getParameter(EMAIL_PARAM)).thenReturn(EMAIL);
    when(request.getParameter(FIRSTNAME_PARAM)).thenReturn(FIRSTNAME);
    when(request.getParameter(LASTNAME_PARAM)).thenReturn(LASTNAME);
    when(request.getParameter(PASSWORD_PARAM)).thenReturn(password);
    when(request.getParameter(PASSWORD_CONFIRM_PARAM)).thenReturn(passwordConfirm);

    when(userHandler.createUserInstance(any())).thenAnswer(invocation -> new UserImpl(invocation.getArgument(0)));

    externalRegisterHandler.execute(controllerContext);

    assertNull(applicationParameters);

    verify(passwordRecoveryService, times(1)).sendAccountCreatedConfirmationEmail(any(), any(), any());
    verify(response, times(1)).sendRedirect(servletContext.getContextPath() + LOGIN);
  }
  
  @Test
  public void testRegisterInternalWhenValid() throws Exception {
    prepareResetPasswordContext();

    String password = "pass1234";
    String passwordConfirm = password;

    when(request.getParameter(ACTION_PARAM)).thenReturn(SAVE_EXTERNAL_ACTION);
    when(request.getParameter(EMAIL_PARAM)).thenReturn(EMAIL);
    when(request.getParameter(FIRSTNAME_PARAM)).thenReturn(FIRSTNAME);
    when(request.getParameter(LASTNAME_PARAM)).thenReturn(LASTNAME);
    when(request.getParameter(PASSWORD_PARAM)).thenReturn(password);
    when(request.getParameter(PASSWORD_CONFIRM_PARAM)).thenReturn(passwordConfirm);

    when(userHandler.createUserInstance(any())).thenAnswer(invocation -> new UserImpl(invocation.getArgument(0)));

    externalRegisterHandler.execute(controllerContext);
    verify(organizationService.getUserHandler(), times(1)).createUser(any(User.class), anyBoolean());
    verify(organizationService.getMembershipHandler(), never()).removeMembership(anyString(), anyBoolean());
  }

  @Test
  public void testRegisterExternalWhenValid() throws Exception {
    prepareResetPasswordContext();

    String password = "pass1234";
    String passwordConfirm = password;

    when(request.getParameter(ACTION_PARAM)).thenReturn(SAVE_EXTERNAL_ACTION);
    when(request.getParameter(EMAIL_PARAM)).thenReturn(EMAIL);
    when(request.getParameter(FIRSTNAME_PARAM)).thenReturn(FIRSTNAME);
    when(request.getParameter(LASTNAME_PARAM)).thenReturn(LASTNAME);
    when(request.getParameter(PASSWORD_PARAM)).thenReturn(password);
    when(request.getParameter(PASSWORD_CONFIRM_PARAM)).thenReturn(passwordConfirm);

    when(userHandler.createUserInstance(any())).thenAnswer(invocation -> new UserImpl(invocation.getArgument(0)));
    when(securitySettingService.isRegistrationExternalUser()).thenReturn(true);

    externalRegisterHandler.execute(controllerContext);
    verify(organizationService.getUserHandler(), times(1)).createUser(any(User.class), anyBoolean());
    verify(organizationService.getMembershipHandler(), times(1)).linkMembership(any(), any(), any(), anyBoolean());
  }

  @Test
  public void testRedirectToRegistrationWhenValid() throws Exception {
    prepareResetPasswordContext();

    String password = "pass1234";
    String passwordConfirm = password;

    when(request.getParameter(ACTION_PARAM)).thenReturn(SAVE_EXTERNAL_ACTION);
    when(request.getParameter(EMAIL_PARAM)).thenReturn(EMAIL);
    when(request.getParameter(FIRSTNAME_PARAM)).thenReturn(FIRSTNAME);
    when(request.getParameter(LASTNAME_PARAM)).thenReturn(LASTNAME);
    when(request.getParameter(PASSWORD_PARAM)).thenReturn(password);
    when(request.getParameter(PASSWORD_CONFIRM_PARAM)).thenReturn(passwordConfirm);
    when(session.getAttribute(REQUIRE_EMAIL_VALIDATION)).thenReturn("true");

    when(userHandler.createUserInstance(any())).thenAnswer(invocation -> new UserImpl(invocation.getArgument(0)));

    externalRegisterHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(EMAIL_VERIFICATION_SENT, applicationParameters.get(SUCCESS_MESSAGE_PARAM));

    verify(passwordRecoveryService, times(1)).sendAccountVerificationEmail(any(), any(), any(), any(), any(), any(), any());
  }

  private void prepareResetPasswordContext() {
    Map<QualifiedName, String> parameters = new HashMap<>();
    parameters.put(TOKEN, TOKEN_VALUE);
    controllerContext = new ControllerContext(controller, router, request, response, parameters);
  }

}
