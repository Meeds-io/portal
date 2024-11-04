/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
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

import static org.exoplatform.web.security.security.CookieTokenService.ONBOARD_TOKEN;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.rest.UserFieldValidator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.application.JspBasedWebHandler;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;

import nl.captcha.Captcha;
import nl.captcha.servlet.CaptchaServletUtil;
import nl.captcha.text.producer.DefaultTextProducer;
import nl.captcha.text.renderer.DefaultWordRenderer;

public class OnboardingHandler extends JspBasedWebHandler {

  private static final QualifiedName     SERVER_CAPTCHA             = QualifiedName.create("gtn", "serveCaptcha");

  protected static Log                   log                        = ExoLogger.getLogger(OnboardingHandler.class);

  public static final String             USERNAME_PARAM             = "username";

  public static final String             PASSWORD_PARAM             = "password";

  public static final String             PASSWORD_CONFIRM_PARAM     = "password2";

  public static final UserFieldValidator PASSWORD_VALIDATOR         =
                                                            new UserFieldValidator(PASSWORD_PARAM, false, false, 8, 255);

  public static final String             NAME                       = "on-boarding";

  public static final QualifiedName      TOKEN                      = QualifiedName.create("gtn", "token");

  public static final QualifiedName      LANG                       = QualifiedName.create("gtn", "lang");

  public static final String             CAPTCHA_PARAM              = "captcha";

  public static final String             ACTION_PARAM               = "action";

  public static final String             RESET_PASSWORD_ACTION_NAME = "resetPassword";

  public static final String             EXPIRED_ACTION_NAME        = "expired";

  public static final String             ERROR_MESSAGE_PARAM        = "error";

  public static final String             TOKEN_ID_PARAM             = "tokenId";

  public static final int                CAPTCHA_WIDTH              = 200;

  public static final int                CAPTCHA_HEIGHT             = 50;

  public static final String             ONBOARDING_JSP_PATH        = "/WEB-INF/jsp/onboarding/reset_password.jsp";      // NOSONAR

  private ServletContext                 servletContext;

  private PasswordRecoveryService        passwordRecoveryService;

  private ResourceBundleService          resourceBundleService;

  private OrganizationService            organizationService;

  public OnboardingHandler(PortalContainer container, // NOSONAR
                           PasswordRecoveryService passwordRecoveryService,
                           ResourceBundleService resourceBundleService,
                           OrganizationService organizationService,
                           LocaleConfigService localeConfigService,
                           BrandingService brandingService,
                           JavascriptConfigService javascriptConfigService,
                           SkinService skinService) {
    super(localeConfigService, brandingService, javascriptConfigService, skinService);
    this.servletContext = container.getPortalContext();
    this.passwordRecoveryService = passwordRecoveryService;
    this.resourceBundleService = resourceBundleService;
    this.organizationService = organizationService;
  }

  @Override
  public String getHandlerName() {
    return NAME;
  }

  @Override
  public boolean execute(ControllerContext controllerContext) throws Exception { // NOSONAR
    HttpServletRequest request = controllerContext.getRequest();
    HttpServletResponse response = controllerContext.getResponse();

    Locale locale = request.getLocale();
    ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(resourceBundleService.getSharedResourceBundleNames(),
                                                                            locale);

    String serveCaptcha = controllerContext.getParameter(SERVER_CAPTCHA);
    if ("true".equals(serveCaptcha)) {
      return serveCaptchaImage(request, response);
    }

    String token = controllerContext.getParameter(TOKEN);
    Map<String, Object> parameters = new HashMap<>();
    String username = StringUtils.isBlank(token) ? null : passwordRecoveryService.verifyToken(token, ONBOARD_TOKEN);
    if (username == null) {
      parameters.put(ACTION_PARAM, EXPIRED_ACTION_NAME);
      // . TokenId is expired
      return dispatch(controllerContext, request, response, parameters);
    }

    String requestAction = request.getParameter(ACTION_PARAM);
    if (RESET_PASSWORD_ACTION_NAME.equalsIgnoreCase(requestAction)) {
      String password = request.getParameter(PASSWORD_PARAM);
      String confirmPass = request.getParameter(PASSWORD_CONFIRM_PARAM);
      String requestedUsername = request.getParameter(USERNAME_PARAM);
      String captcha = request.getParameter(CAPTCHA_PARAM);
      if (!isValidCaptch(request.getSession(), captcha)) {
        parameters.put(ERROR_MESSAGE_PARAM, resourceBundle.getString("gatein.forgotPassword.captchaError"));
      } else if (validateUserAndPassword(username,
                                         requestedUsername,
                                         password,
                                         confirmPass,
                                         parameters,
                                         resourceBundle,
                                         locale)) {
        if (passwordRecoveryService.changePass(token, ONBOARD_TOKEN, username, password)) {
          String loginPath = servletContext.getContextPath() + "/login";
          User user = findUser(username);
          if (user != null) {
            loginPath += "?email=" + user.getEmail();
          }
          response.sendRedirect(loginPath);
          return true;
        } else {
          parameters.put(ERROR_MESSAGE_PARAM, resourceBundle.getString("gatein.forgotPassword.resetPasswordFailure"));
        }
      }
      parameters.put(PASSWORD_PARAM, password);
      parameters.put(PASSWORD_CONFIRM_PARAM, confirmPass);
    }
    parameters.put(USERNAME_PARAM, escapeXssCharacters(username));
    parameters.put(TOKEN_ID_PARAM, token);
    parameters.put(ACTION_PARAM, RESET_PASSWORD_ACTION_NAME);

    return dispatch(controllerContext, request, response, parameters);
  }

  protected boolean isValidCaptch(HttpSession session, String captchaValue) {
    Captcha captcha = (Captcha) session.getAttribute(NAME);
    return captcha != null && StringUtils.isNotBlank(captchaValue) && captcha.isCorrect(captchaValue);
  }

  private boolean validateUserAndPassword(String tokenUsername,
                                          String requestedUsername,
                                          String password,
                                          String confirmPass,
                                          Map<String, Object> parameters,
                                          ResourceBundle bundle,
                                          Locale locale) {
    if (requestedUsername == null || !requestedUsername.equals(tokenUsername)) {
      String errorMessage = bundle.getString("gatein.forgotPassword.usernameChanged");
      errorMessage = errorMessage.replace("{0}", tokenUsername);
      parameters.put(ERROR_MESSAGE_PARAM, errorMessage);
      return false;
    } else if (!StringUtils.equals(password, confirmPass)) {
      parameters.put(ERROR_MESSAGE_PARAM, bundle.getString("gatein.forgotPassword.confirmPasswordNotMatch"));
      return false;
    } else {
      String errorMessage = PASSWORD_VALIDATOR.validate(locale, password);
      if (StringUtils.isNotBlank(errorMessage)) {
        parameters.put(ERROR_MESSAGE_PARAM, errorMessage);
        return false;
      }
    }
    return true;
  }

  @Override
  protected boolean getRequiresLifeCycle() {
    return true;
  }

  protected void extendApplicationParameters(JSONObject applicationParameters, Map<String, Object> additionalParameters) {
    additionalParameters.forEach(applicationParameters::put);
  }

  private boolean dispatch(ControllerContext controllerContext,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           Map<String, Object> parameters) throws Exception {
    // Invalidate the Captcha
    request.getSession().removeAttribute(NAME);

    super.prepareDispatch(controllerContext,
                          "PORTLET/social/InternalOnboarding",
                          Collections.emptyList(),
                          Collections.singletonList("portal/login"),
                          params -> extendApplicationParameters(params, parameters));
    servletContext.getRequestDispatcher(ONBOARDING_JSP_PATH).include(request, response);
    return true;
  }

  private User findUser(String usernameOrEmail) throws Exception {
    User user = organizationService.getUserHandler().findUserByName(usernameOrEmail, UserStatus.ANY);
    if (user == null && usernameOrEmail.contains("@")) {
      Query query = new Query();
      query.setEmail(usernameOrEmail);
      ListAccess<User> list = organizationService.getUserHandler().findUsersByQuery(query, UserStatus.ANY);
      if (list != null && list.getSize() > 0) {
        user = list.load(0, 1)[0];
      }
    }
    return user;
  }

  public String escapeXssCharacters(String message) {
    message = (message == null) ? null
                                : message.replace("&", "&amp")
                                         .replace("<", "&lt;")
                                         .replace(">", "&gt;")
                                         .replace("\"", "&quot;")
                                         .replace("'", "&#x27;")
                                         .replace("/", "&#x2F;");
    return message;
  }

  private boolean serveCaptchaImage(HttpServletRequest req, HttpServletResponse resp) {
    HttpSession session = req.getSession();
    Captcha captcha;
    if (session.getAttribute(NAME) == null) {
      List<java.awt.Font> textFonts = Arrays.asList(
                                                    new Font("Arial", Font.BOLD, 40),
                                                    new Font("Courier", Font.BOLD, 40));
      captcha = new Captcha.Builder(CAPTCHA_WIDTH, CAPTCHA_HEIGHT)
                                                                  .addText(new DefaultTextProducer(5),
                                                                           new DefaultWordRenderer(Color.WHITE, textFonts))
                                                                  .gimp()
                                                                  .addNoise()
                                                                  .addBackground()
                                                                  .build();

      session.setAttribute(NAME, captcha);
      writeImage(resp, captcha.getImage());

    }

    captcha = (Captcha) session.getAttribute(NAME);
    writeImage(resp, captcha.getImage());

    return true;
  }

  private void writeImage(HttpServletResponse response, BufferedImage bi) {
    response.setHeader("Cache-Control", "private,no-cache,no-store");
    response.setContentType("image/png"); // PNGs allow for transparency. JPGs
                                          // do not.
    try {
      CaptchaServletUtil.writeImage(response.getOutputStream(), bi);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
  }
}
