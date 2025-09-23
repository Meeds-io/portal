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

import static org.exoplatform.web.security.security.CookieTokenService.FORGOT_PASSWORD_TOKEN;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import org.exoplatform.commons.utils.I18N;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.rest.UserFieldValidator;
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

import io.meeds.spring.web.localization.HttpRequestLocaleWrapper;

public class PasswordRecoveryHandler extends JspBasedWebHandler {

  public static final QualifiedName      TOKEN                      = QualifiedName.create("gtn", "token");

  public static final QualifiedName      LANG                       = QualifiedName.create("gtn", "lang");

  public static final QualifiedName      INIT_URL                   = QualifiedName.create("gtn", "initialURI");

  public static final String             NAME                       = "forgot-password";

  public static final String             FORM_URL_PARAM             = "formUrl";

  public static final String             ACTION_PARAM               = "action";

  public static final String             RESET_PASSWORD_ACTION_NAME = "resetPassword";

  public static final String             INITIAL_URI_PARAM          = INIT_URL.getName();

  public static final String             EXPIRED_ACTION_NAME        = "expired";

  public static final String             SEND_ACTION_NAME           = "send";

  public static final String             ERROR_MESSAGE_PARAM        = "error";

  public static final String             TOKEN_ID_PARAM             = "tokenId";

  public static final String             SUCCESS_MESSAGE_PARAM      = "success";

  public static final String             USERNAME_PARAM             = "username";

  public static final String             PASSWORD_PARAM             = "password";

  public static final String             PASSWORD_CONFIRM_PARAM     = "password2";

  public static final UserFieldValidator PASSWORD_VALIDATOR         =
                                                            new UserFieldValidator(PASSWORD_PARAM, false, false, 8, 255);

  public static final String             FORGOT_PASSWORD_JSP_PATH   = "/WEB-INF/jsp/forgotpassword/forgot_password.jsp"; // NOSONAR

  private ServletContext                 servletContext;

  private PasswordRecoveryService        passwordRecoveryService;

  private ResourceBundleService          resourceBundleService;

  private OrganizationService            organizationService;

  public PasswordRecoveryHandler(PortalContainer container, // NOSONAR
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
  public boolean execute(ControllerContext controllerContext) throws Exception {// NOSONAR
    HttpServletRequest request = new HttpRequestLocaleWrapper(controllerContext.getRequest());
    HttpServletResponse response = controllerContext.getResponse();

    Locale locale = request.getLocale();
    ResourceBundle bundle = resourceBundleService.getResourceBundle(resourceBundleService.getSharedResourceBundleNames(),
                                                                    locale);

    String token = controllerContext.getParameter(TOKEN);
    String initialURI = escapeXssCharacters(controllerContext.getParameter(INIT_URL));

    String requestAction = request.getParameter(ACTION_PARAM);
    Map<String, Object> parameters = new HashMap<>();
    String contextPath = servletContext.getContextPath();
    String forgotPasswordPath = passwordRecoveryService.getPasswordRecoverURL(token, I18N.toTagIdentifier(locale));
    request.setAttribute(FORM_URL_PARAM, contextPath + forgotPasswordPath);
    if (StringUtils.isNotBlank(token)) {
      // . Check tokenID is expired or not
      String username = passwordRecoveryService.verifyToken(token, FORGOT_PASSWORD_TOKEN);
      if (username == null) {
        request.setAttribute(ACTION_PARAM, EXPIRED_ACTION_NAME);
        return false;
      }

      if (RESET_PASSWORD_ACTION_NAME.equalsIgnoreCase(requestAction)) {
        String password = request.getParameter(PASSWORD_PARAM);
        String confirmPass = request.getParameter(PASSWORD_CONFIRM_PARAM);
        String requestedUsername = request.getParameter(USERNAME_PARAM);
        if (validateUserAndPassword(username, requestedUsername, password, confirmPass, parameters, bundle, locale)) {
          if (passwordRecoveryService.changePass(token, FORGOT_PASSWORD_TOKEN, username, password)) {
            redirectToLoginPage(request, response);
            return true;
          } else {
            returnSubmissionError(bundle.getString("gatein.forgotPassword.resetPasswordFailure"), response);
            return true;
          }
        } else {
          returnSubmissionError((String)parameters.get(ERROR_MESSAGE_PARAM), response);
          return true;
        }
      }
      request.setAttribute(USERNAME_PARAM, escapeXssCharacters(username));
      request.setAttribute(TOKEN_ID_PARAM, token);
      request.setAttribute(ACTION_PARAM, RESET_PASSWORD_ACTION_NAME);
    } else if (SEND_ACTION_NAME.equalsIgnoreCase(requestAction)) {
      String username = request.getParameter(USERNAME_PARAM);
      if (StringUtils.isBlank(username)) {
        returnSubmissionError(bundle.getString("gatein.forgotPassword.emptyUserOrEmail"), response);
        return true;
      } else {
        User user = findUser(username);
        if (user == null || !user.isEnabled() || passwordRecoveryService.sendRecoverPasswordEmail(user, locale, request)) {
          // Send a success message even when user is not found to not inform
          // anonymous users which usernames and emails exists
          returnSubmissionSuccess(bundle.getString("gatein.forgotPassword.emailSendSuccessful"), response);
          return true;
        } else {
          returnSubmissionError(bundle.getString("gatein.forgotPassword.emailSendFailure"), response);
          return true;
        }
      }
    }

    if (initialURI != null) {
      request.setAttribute(INITIAL_URI_PARAM, initialURI);
    }
    return false;

  }

  @Override
  public String getHandlerName() {
    return NAME;
  }

  @Override
  protected boolean getRequiresLifeCycle() {
    return true;
  }

  protected void extendApplicationParameters(JSONObject applicationParameters, Map<String, Object> additionalParameters) {
    additionalParameters.forEach(applicationParameters::put);
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

  private User findUser(String usernameOrEmail) throws Exception {
    User user = organizationService.getUserHandler().findUserByName(usernameOrEmail, UserStatus.ANY);
    if (user == null && usernameOrEmail.contains("@")) {
      Query query = new Query();
      //in passwordRecovery context, we do not allow search by email with wildcard
      usernameOrEmail=usernameOrEmail.replace("*","");
      query.setEmail(usernameOrEmail);
      ListAccess<User> list = organizationService.getUserHandler().findUsersByQuery(query, UserStatus.ANY);
      if (list != null && list.getSize() > 0) {
        user = list.load(0, 1)[0];
      }
    }
    return user;
  }

  private String escapeXssCharacters(String message) {
    message = (message == null) ? null
                                : message.replace("&", "&amp")
                                         .replace("<", "&lt;")
                                         .replace(">", "&gt;")
                                         .replace("\"", "&quot;")
                                         .replace("'", "&#x27;")
                                         .replace("/", "&#x2F;");
    return message;
  }

  private void redirectToLoginPage(HttpServletRequest request,
                                   HttpServletResponse response) throws IOException {
    // Invalidate the Captcha
    request.getSession().removeAttribute(NAME);

    String path = servletContext.getContextPath() + "/login";
    response.sendRedirect(path);
  }
  private static void returnSubmissionError(String message, HttpServletResponse response) throws
                                                                                                        IOException {
    response.setStatus(400);
    response.setContentType("application/json");
    response.getOutputStream().write(new JSONObject()
                                         .put(ERROR_MESSAGE_PARAM, message)
                                         .toString()
                                         .getBytes());
  }

  private static void returnSubmissionSuccess(String message, HttpServletResponse response) throws
                                                                                                        IOException {
    response.setStatus(200);
    response.setContentType("application/json");
    response.getOutputStream().write(new JSONObject()
                                         .put(SUCCESS_MESSAGE_PARAM, message)
                                         .toString()
                                         .getBytes());
  }

}
