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
package org.exoplatform.web.login.onboarding;

import static org.exoplatform.web.login.onboarding.OnboardingHandler.ACTION_PARAM;
import static org.exoplatform.web.login.onboarding.OnboardingHandler.CAPTCHA_PARAM;
import static org.exoplatform.web.login.onboarding.OnboardingHandler.ERROR_MESSAGE_PARAM;
import static org.exoplatform.web.login.onboarding.OnboardingHandler.EXPIRED_ACTION_NAME;
import static org.exoplatform.web.login.onboarding.OnboardingHandler.NAME;
import static org.exoplatform.web.login.onboarding.OnboardingHandler.PASSWORD_CONFIRM_PARAM;
import static org.exoplatform.web.login.onboarding.OnboardingHandler.PASSWORD_PARAM;
import static org.exoplatform.web.login.onboarding.OnboardingHandler.RESET_PASSWORD_ACTION_NAME;
import static org.exoplatform.web.login.onboarding.OnboardingHandler.TOKEN;
import static org.exoplatform.web.login.onboarding.OnboardingHandler.TOKEN_ID_PARAM;
import static org.exoplatform.web.login.onboarding.OnboardingHandler.USERNAME_PARAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserHandler;
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

import nl.captcha.Captcha;

@RunWith(MockitoJUnitRunner.class)
public class OnboardingHandlerTest {

  private static final Locale     REQUEST_LOCALE = Locale.ENGLISH;

  private static final String     CONTEXT_PATH   = "/portal";

  private static final String     TOKEN_VALUE    = "tokenValue";

  private static final String     USERNAME       = "username";

  private static final String     CAPTCHA_VALUE  = "captchaValue";

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
  private HttpSession             session;

  @Mock
  private HttpServletRequest      request;

  @Mock
  private HttpServletResponse     response;

  @Mock
  private RequestDispatcher       requestDispatcher;

  @Mock
  private SkinService             skinService;

  @Mock
  private Captcha                 captcha;

  @Mock
  private Credentials             credentials;

  private ControllerContext       controllerContext;

  private OnboardingHandler       onboardingHandler;

  private Map<String, Object>     applicationParameters;

  @Before
  public void setUp() {
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

    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(passwordRecoveryService.verifyToken(TOKEN_VALUE, CookieTokenService.ONBOARD_TOKEN)).thenReturn(USERNAME);

    onboardingHandler = new OnboardingHandler(container,
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
        OnboardingHandlerTest.this.applicationParameters = additionalParameters;
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
    assertTrue(onboardingHandler.getRequiresLifeCycle());
  }

  @Test
  public void testGetHandlerName() {
    assertEquals(NAME, onboardingHandler.getHandlerName());
  }

  @Test
  public void testDisplayTokenExpiredPage() throws Exception {
    prepareResetPasswordContext();
    reset(passwordRecoveryService);

    onboardingHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertFalse(applicationParameters.containsKey(ERROR_MESSAGE_PARAM));
    assertEquals(EXPIRED_ACTION_NAME, applicationParameters.get(ACTION_PARAM));

    verify(passwordRecoveryService, never()).changePass(any(), any(), any(), any());
  }

  @Test
  public void testDisplayOnboardingPage() throws Exception {
    prepareResetPasswordContext();

    onboardingHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(USERNAME, applicationParameters.get(USERNAME_PARAM));
    assertEquals(TOKEN_VALUE, applicationParameters.get(TOKEN_ID_PARAM));
    assertEquals(RESET_PASSWORD_ACTION_NAME, applicationParameters.get(ACTION_PARAM));
    assertFalse(applicationParameters.containsKey(ERROR_MESSAGE_PARAM));

    verify(passwordRecoveryService, never()).changePass(any(), any(), any(), any());
  }

  @Test
  public void testDisplayOnboardingUserNotMatch() throws Exception {
    prepareResetPasswordContext();

    String password = "pass1";
    String passwordConfirm = "pass2";

    when(request.getParameter(ACTION_PARAM)).thenReturn(RESET_PASSWORD_ACTION_NAME);
    when(request.getParameter(USERNAME_PARAM)).thenReturn("user2");
    when(request.getParameter(PASSWORD_PARAM)).thenReturn(password);
    when(request.getParameter(PASSWORD_CONFIRM_PARAM)).thenReturn(passwordConfirm);

    onboardingHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(USERNAME, applicationParameters.get(USERNAME_PARAM));
    assertEquals(TOKEN_VALUE, applicationParameters.get(TOKEN_ID_PARAM));
    assertEquals(RESET_PASSWORD_ACTION_NAME, applicationParameters.get(ACTION_PARAM));
    assertEquals("gatein.forgotPassword.usernameChanged", applicationParameters.get(ERROR_MESSAGE_PARAM));
    assertEquals(password, applicationParameters.get(PASSWORD_PARAM));
    assertEquals(passwordConfirm, applicationParameters.get(PASSWORD_CONFIRM_PARAM));

    verify(passwordRecoveryService, never()).changePass(any(), any(), any(), any());
  }

  @Test
  public void testDisplayOnboardingWhenBothPasswordsNotMatch() throws Exception {
    prepareResetPasswordContext();

    String password = "pass1";
    String passwordConfirm = "pass2";

    when(request.getParameter(ACTION_PARAM)).thenReturn(RESET_PASSWORD_ACTION_NAME);
    when(request.getParameter(USERNAME_PARAM)).thenReturn(USERNAME);
    when(request.getParameter(PASSWORD_PARAM)).thenReturn(password);
    when(request.getParameter(PASSWORD_CONFIRM_PARAM)).thenReturn(passwordConfirm);

    onboardingHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(USERNAME, applicationParameters.get(USERNAME_PARAM));
    assertEquals(TOKEN_VALUE, applicationParameters.get(TOKEN_ID_PARAM));
    assertEquals(RESET_PASSWORD_ACTION_NAME, applicationParameters.get(ACTION_PARAM));
    assertEquals("gatein.forgotPassword.confirmPasswordNotMatch", applicationParameters.get(ERROR_MESSAGE_PARAM));
    assertEquals(password, applicationParameters.get(PASSWORD_PARAM));
    assertEquals(passwordConfirm, applicationParameters.get(PASSWORD_CONFIRM_PARAM));

    verify(passwordRecoveryService, never()).changePass(any(), any(), any(), any());
  }

  @Test
  public void testDisplayOnboardingWhenPasswordNotValid() throws Exception {
    prepareResetPasswordContext();

    String password = "pass1";
    String passwordConfirm = password;

    when(request.getParameter(ACTION_PARAM)).thenReturn(RESET_PASSWORD_ACTION_NAME);
    when(request.getParameter(USERNAME_PARAM)).thenReturn(USERNAME);
    when(request.getParameter(PASSWORD_PARAM)).thenReturn(password);
    when(request.getParameter(PASSWORD_CONFIRM_PARAM)).thenReturn(passwordConfirm);

    PropertyManager.setProperty("gatein.validators.passwordpolicy.length.max", "8");
    PropertyManager.setProperty("gatein.validators.passwordpolicy.length.min", "255");
    try {
      onboardingHandler.execute(controllerContext);
    } finally {
      PropertyManager.setProperty("gatein.validators.passwordpolicy.length.max", "");
      PropertyManager.setProperty("gatein.validators.passwordpolicy.length.min", "");
    }

    assertNotNull(applicationParameters);
    assertEquals(USERNAME, applicationParameters.get(USERNAME_PARAM));
    assertEquals(TOKEN_VALUE, applicationParameters.get(TOKEN_ID_PARAM));
    assertEquals(RESET_PASSWORD_ACTION_NAME, applicationParameters.get(ACTION_PARAM));
    assertEquals("onboarding.login.passwordCondition", applicationParameters.get(ERROR_MESSAGE_PARAM));

    verify(passwordRecoveryService, never()).changePass(any(), any(), any(), any());
  }

  @Test
  public void testRedirectToLoginWhenValid() throws Exception {
    prepareResetPasswordContext();

    String password = "pass1234";
    String passwordConfirm = password;

    when(request.getParameter(ACTION_PARAM)).thenReturn(RESET_PASSWORD_ACTION_NAME);
    when(request.getParameter(USERNAME_PARAM)).thenReturn(USERNAME);
    when(request.getParameter(PASSWORD_PARAM)).thenReturn(password);
    when(request.getParameter(PASSWORD_CONFIRM_PARAM)).thenReturn(passwordConfirm);
    when(passwordRecoveryService.changePass(TOKEN_VALUE, CookieTokenService.ONBOARD_TOKEN, USERNAME, password)).thenReturn(true);

    onboardingHandler.execute(controllerContext);

    verify(passwordRecoveryService, times(1)).changePass(any(), any(), any(), any());
    verify(response, times(1)).sendRedirect(servletContext.getContextPath() + "/login");
  }

  private void prepareResetPasswordContext() {
    Map<QualifiedName, String> parameters = new HashMap<>();
    parameters.put(TOKEN, TOKEN_VALUE);
    controllerContext = new ControllerContext(controller,
                                              router,
                                              request,
                                              response,
                                              parameters);
  }

}
