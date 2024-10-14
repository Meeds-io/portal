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
package org.exoplatform.web.register;

import static org.exoplatform.web.security.security.CookieTokenService.EMAIL_VALIDATION_TOKEN;
import static org.exoplatform.web.security.security.CookieTokenService.EXTERNAL_REGISTRATION_TOKEN;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.rest.UserFieldValidator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Membership;
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
import org.exoplatform.web.security.security.RemindPasswordTokenService;

import io.meeds.portal.security.service.SecuritySettingService;
import io.meeds.spring.web.localization.HttpRequestLocaleWrapper;

import nl.captcha.Captcha;
import nl.captcha.servlet.CaptchaServletUtil;
import nl.captcha.text.producer.DefaultTextProducer;
import nl.captcha.text.renderer.DefaultWordRenderer;

public class ExternalRegisterHandler extends JspBasedWebHandler {

  public static final String             REQUIRE_EMAIL_VALIDATION            = "requireEmailValidation";

  private static final String            ADMINISTRATORS_GROUP                = "/platform/administrators";

  private static final Log               LOG                                 = ExoLogger.getLogger(ExternalRegisterHandler.class);

  private static final QualifiedName     SERVER_CAPTCHA                      = QualifiedName.create("gtn", "serveCaptcha");

  public static final String             USERNAME_PARAM                      = "username";

  public static final String             EMAIL_PARAM                         = "email";

  public static final String             LASTNAME_PARAM                      = "lastName";

  public static final String             FIRSTNAME_PARAM                     = "firstName";

  public static final String             PASSWORD_PARAM                      = "password";

  public static final String             PASSWORD_CONFIRM_PARAM              = "password2";

  public static final UserFieldValidator PASSWORD_VALIDATOR                  =
                                                            new UserFieldValidator(PASSWORD_PARAM, false, false, 8, 255);

  public static final UserFieldValidator LASTNAME_VALIDATOR                  =
                                                            new UserFieldValidator(LASTNAME_PARAM, false, true);

  public static final UserFieldValidator FIRSTNAME_VALIDATOR                 =
                                                             new UserFieldValidator(FIRSTNAME_PARAM, false, true);

  public static final UserFieldValidator EMAIL_VALIDATOR                     = new UserFieldValidator(EMAIL_PARAM, false, false);

  public static final String             NAME                                = "external-registration";

  public static final QualifiedName      TOKEN                               = QualifiedName.create("gtn", "token");

  public static final QualifiedName      LANG                                = QualifiedName.create("gtn", "lang");

  public static final QualifiedName      INIT_URL                            = QualifiedName.create("gtn", "initURL");

  public static final String             REQ_PARAM_ACTION                    = "action";

  public static final String             LOGIN                               = "/login";

  public static final String             USERS_GROUP                         = "/platform/users";

  public static final String             EXTERNAL_USERS_GROUP                = "/platform/externals";

  public static final String             CAPTCHA_PARAM                       = "captcha";

  public static final String             ACTION_PARAM                        = "action";

  public static final String             VALIDATE_EXTERNAL_EMAIL_ACTION      = "validateEmail";

  public static final String             SAVE_EXTERNAL_ACTION                = "saveExternal";

  public static final String             EXPIRED_ACTION_NAME                 = "expired";

  public static final String             SUCCESS_MESSAGE_PARAM               = "success";

  public static final String             ERROR_MESSAGE_PARAM                 = "error";

  public static final String             ERROR_FIELD_PARAM                   = "errorField";

  public static final String             ALREADY_AUTHENTICATED_MESSAGE_PARAM = "authenticated";

  public static final String             EMAIL_VERIFICATION_SENT             = "emailVerificationSent";

  public static final String             TOKEN_ID_PARAM                      = "tokenId";

  public static final int                CAPTCHA_WIDTH                       = 200;

  public static final int                CAPTCHA_HEIGHT                      = 50;

  public static final String             EXTERNAL_REGISTRATION_JSP_PATH      =
                                                                        "/WEB-INF/jsp/externalRegistration/init_account.jsp";     // NOSONAR

  private static final String            MEMBER                              = "member";

  public static final String             USERNAME_REQUEST_PARAM              = "username";

  public static final String             PASSWORD_REQUEST_PARAM              = "password";

  public static final String             INITIAL_URI_PARAM                   = "initialURI";

  private PortalContainer                container;

  private ServletContext                 servletContext;

  private RemindPasswordTokenService     remindPasswordTokenService;

  private PasswordRecoveryService        passwordRecoveryService;

  private ResourceBundleService          resourceBundleService;

  private OrganizationService            organizationService;

  private SecuritySettingService         securitySettingService;

  public ExternalRegisterHandler(PortalContainer container, // NOSONAR
                                 RemindPasswordTokenService remindPasswordTokenService,
                                 PasswordRecoveryService passwordRecoveryService,
                                 ResourceBundleService resourceBundleService,
                                 OrganizationService organizationService,
                                 LocaleConfigService localeConfigService,
                                 BrandingService brandingService,
                                 SecuritySettingService securitySettingService,
                                 JavascriptConfigService javascriptConfigService,
                                 SkinService skinService) {
    super(localeConfigService, brandingService, javascriptConfigService, skinService);
    this.container = container;
    this.servletContext = container.getPortalContext();
    this.remindPasswordTokenService = remindPasswordTokenService;
    this.passwordRecoveryService = passwordRecoveryService;
    this.resourceBundleService = resourceBundleService;
    this.organizationService = organizationService;
    this.securitySettingService = securitySettingService;
  }

  @Override
  public String getHandlerName() {
    return NAME;
  }

  @Override
  public boolean execute(ControllerContext controllerContext) throws Exception {// NOSONAR
    HttpServletRequest request = new HttpRequestLocaleWrapper(controllerContext.getRequest());
    HttpServletResponse response = controllerContext.getResponse();

    Map<String, Object> parameters = new HashMap<>();
    if (request.getRemoteUser() != null) {
      parameters.put(ALREADY_AUTHENTICATED_MESSAGE_PARAM, "true");
      return dispatch(controllerContext, request, response, parameters);
    }

    Locale locale = request.getLocale();
    ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(resourceBundleService.getSharedResourceBundleNames(),
                                                                            locale);

    String serveCaptcha = controllerContext.getParameter(SERVER_CAPTCHA);
    if ("true".equals(serveCaptcha)) {
      return serveCaptchaImage(request, response);
    }
    String requestAction = request.getParameter(REQ_PARAM_ACTION);
    String initialUri = request.getParameter(INITIAL_URI_PARAM);
    String token = controllerContext.getParameter(TOKEN);
    String username = getStoredCredentials(token, requestAction);
    if (username == null) {
      // Token expired
      return dispatch(controllerContext, request, response, Collections.singletonMap(ACTION_PARAM, EXPIRED_ACTION_NAME));
    }

    if (VALIDATE_EXTERNAL_EMAIL_ACTION.equalsIgnoreCase(requestAction)) {
      User user = generateUserFromCredential(username);
      username = createUser(user);
      passwordRecoveryService.sendAccountCreatedConfirmationEmail(username, locale, getBaseUrl(request));
      remindPasswordTokenService.deleteToken(token);
      wrapForAutomaticLogin(request, response, initialUri, user.getUserName(), user.getPassword());
    } else if (SAVE_EXTERNAL_ACTION.equalsIgnoreCase(requestAction)) {
      if (findUser(username) != null) {
        // User already exists
        redirectToLoginPage(request, response, username);
        return true;
      }
      String requestUsername = request.getParameter(USERNAME_PARAM);
      String requestEmail = request.getParameter(EMAIL_PARAM);
      String firstName = request.getParameter(FIRSTNAME_PARAM);
      String lastName = request.getParameter(LASTNAME_PARAM);
      String password = request.getParameter(PASSWORD_PARAM);
      String confirmPass = request.getParameter(PASSWORD_CONFIRM_PARAM);
      String captcha = request.getParameter(CAPTCHA_PARAM);

      HttpSession session = request.getSession();
      if (!isValidCaptch(session, captcha)) {
        parameters.put(ERROR_MESSAGE_PARAM, resourceBundle.getString("gatein.forgotPassword.captchaError"));
        parameters.put(ERROR_FIELD_PARAM, CAPTCHA_PARAM);
      } else if (isValidUserAndPassword(username,
                                        requestUsername,
                                        requestEmail,
                                        password,
                                        confirmPass,
                                        parameters,
                                        resourceBundle,
                                        locale)
          && isValidUserFullName(firstName, lastName, parameters, locale)) {
        try {
          if (session.getAttribute(REQUIRE_EMAIL_VALIDATION) == null) {
            username = createUser(requestUsername, requestEmail, firstName, lastName, password);
            passwordRecoveryService.sendAccountCreatedConfirmationEmail(username, locale, getBaseUrl(request));
            remindPasswordTokenService.deleteToken(token);
            wrapForAutomaticLogin(request, response, initialUri, username, password);
            return true;
          } else if (session.getAttribute(REQUIRE_EMAIL_VALIDATION).equals("true")) {
            User user = organizationService.getUserHandler().createUserInstance(requestUsername);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(requestEmail);
            user.setPassword(password);
            String data = generateUserDetailCredential(user);
            passwordRecoveryService.sendAccountVerificationEmail(data,
                                                                 requestUsername,
                                                                 firstName,
                                                                 lastName,
                                                                 requestEmail,
                                                                 locale,
                                                                 getBaseUrl(request));
            parameters.put(SUCCESS_MESSAGE_PARAM, EMAIL_VERIFICATION_SENT);
            session.setAttribute(REQUIRE_EMAIL_VALIDATION, "false");
          }
        } catch (Exception e) {
          LOG.warn("Error while registering external user", e);
          parameters.put(ERROR_MESSAGE_PARAM, resourceBundle.getString("external.registration.fail.create.user"));
        }
      }
      parameters.put(EMAIL_PARAM, requestEmail);
      parameters.put(FIRSTNAME_PARAM, firstName);
      parameters.put(LASTNAME_PARAM, lastName);
      parameters.put(PASSWORD_PARAM, password);
      parameters.put(PASSWORD_CONFIRM_PARAM, confirmPass);
    }

    // Token can be generated using email or username (Metamask for example)
    if (StringUtils.contains(username, "@")) {
      parameters.put(EMAIL_PARAM, escapeXssCharacters(username));
    } else {
      parameters.put(USERNAME_PARAM, escapeXssCharacters(username));
    }
    parameters.put(TOKEN_ID_PARAM, token);
    parameters.put(INITIAL_URI_PARAM, initialUri);

    return dispatch(controllerContext, request, response, parameters);
  }

  private String getStoredCredentials(String token, String requestAction) {
    if (StringUtils.isBlank(token)) {
      return null;
    }
    if (StringUtils.equals(VALIDATE_EXTERNAL_EMAIL_ACTION, requestAction)) {
      return passwordRecoveryService.verifyToken(token, EMAIL_VALIDATION_TOKEN);
    } else {
      return passwordRecoveryService.verifyToken(token, EXTERNAL_REGISTRATION_TOKEN);
    }
  }

  private boolean isValidUserFullName(String firstName, String lastName, Map<String, Object> parameters, Locale locale) {
    String errorMessage = FIRSTNAME_VALIDATOR.validate(locale, firstName);
    if (StringUtils.isBlank(errorMessage)) {
      errorMessage = LASTNAME_VALIDATOR.validate(locale, lastName);
      if (StringUtils.isNotBlank(errorMessage)) {
        parameters.put(ERROR_MESSAGE_PARAM, errorMessage);
        parameters.put(ERROR_FIELD_PARAM, LASTNAME_PARAM);
      }
    } else {
      parameters.put(ERROR_MESSAGE_PARAM, errorMessage);
      parameters.put(ERROR_FIELD_PARAM, FIRSTNAME_PARAM);
    }
    return StringUtils.isBlank(errorMessage);
  }

  private boolean isValidUserAndPassword(String tokenUsernameOrEmail, // NOSONAR
                                         String username,
                                         String email,
                                         String password,
                                         String confirmPass,
                                         Map<String, Object> parameters,
                                         ResourceBundle bundle,
                                         Locale locale) {
    boolean isEmailToken = StringUtils.contains(tokenUsernameOrEmail, "@");
    boolean notSameUser =
                        StringUtils.isBlank(tokenUsernameOrEmail) || (StringUtils.isBlank(username) && StringUtils.isBlank(email))
                            || (isEmailToken && !StringUtils.equals(tokenUsernameOrEmail, email))
                            || (!isEmailToken && !StringUtils.equals(tokenUsernameOrEmail, username));
    if (notSameUser) {
      String errorMessage = bundle.getString("gatein.forgotPassword.usernameChanged");
      errorMessage = errorMessage.replace("{0}", tokenUsernameOrEmail);
      parameters.put(ERROR_MESSAGE_PARAM, errorMessage);
    } else {
      String errorMessage = PASSWORD_VALIDATOR.validate(locale, password);
      if (StringUtils.isNotBlank(errorMessage)) {
        parameters.put(ERROR_MESSAGE_PARAM, errorMessage);
        parameters.put(ERROR_FIELD_PARAM, PASSWORD_PARAM);
      } else if (!isEmailToken) {
        // User added an email in registration form
        // which wasn't provided at first place in token
        // generation
        errorMessage = EMAIL_VALIDATOR.validate(locale, email);
        if (StringUtils.isNotBlank(errorMessage)) {
          parameters.put(ERROR_MESSAGE_PARAM, errorMessage);
          parameters.put(ERROR_FIELD_PARAM, EMAIL_PARAM);
        }
      } else if (!StringUtils.equals(password, confirmPass)) {
        parameters.put(ERROR_MESSAGE_PARAM, bundle.getString("gatein.forgotPassword.confirmPasswordNotMatch"));
        parameters.put(ERROR_FIELD_PARAM, PASSWORD_CONFIRM_PARAM);
      }
    }
    return !parameters.containsKey(ERROR_MESSAGE_PARAM);
  }

  private String createUser(String username, String email, String firstName, String lastName, String password) throws Exception {
    User user = organizationService.getUserHandler().createUserInstance(username);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setPassword(password);
    user.setEmail(email);

    return createUser(user);
  }

  @SuppressWarnings("deprecation")
  private String createUser(User user) throws Exception {
    String login = user.getUserName();
    if (StringUtils.isBlank(login)) {
      login = generateUsername(user.getFirstName(), user.getLastName());
      user.setUserName(login);
    }
    organizationService.getUserHandler().createUser(user, true);

    Collection<Membership> memberships = organizationService.getMembershipHandler()
                                                            .findMembershipsByUserAndGroup(login, ADMINISTRATORS_GROUP);

    boolean isAdministrator = CollectionUtils.isNotEmpty(memberships);
    if (!isAdministrator && securitySettingService.isRegistrationExternalUser()) {
      // Avoid incoherence by indicating an admin user As external
      deleteFromInternalUsersGroup(login);
      addToExternalUsersGroup(login);
    }
    return login;
  }

  private void deleteFromInternalUsersGroup(String username) throws Exception {
    Collection<Membership> usersMemberhips = organizationService.getMembershipHandler()
        .findMembershipsByUserAndGroup(username, USERS_GROUP);
    if (CollectionUtils.isNotEmpty(usersMemberhips)) {
      for (Membership usersMemberhip : usersMemberhips) {
        organizationService.getMembershipHandler().removeMembership(usersMemberhip.getId(), true);
      }
    }
  }

  private void addToExternalUsersGroup(String username) throws Exception {
    Collection<Membership> externalsUsersMemberhips = organizationService.getMembershipHandler()
                                                                         .findMembershipsByUserAndGroup(username,
                                                                                                        EXTERNAL_USERS_GROUP);
    if (CollectionUtils.isNotEmpty(externalsUsersMemberhips)) {
      for (Membership usersMemberhip : externalsUsersMemberhips) {
        organizationService.getMembershipHandler().removeMembership(usersMemberhip.getId(), true);
      }
    }
    
    organizationService.getMembershipHandler()
                       .linkMembership(organizationService.getUserHandler().findUserByName(username),
                                       organizationService.getGroupHandler().findGroupById(EXTERNAL_USERS_GROUP),
                                       organizationService.getMembershipTypeHandler().findMembershipType(MEMBER),
                                       true);
  }

  private StringBuilder getBaseUrl(HttpServletRequest request) {
    StringBuilder url = new StringBuilder();
    url.append(request.getScheme()).append("://").append(request.getServerName());
    if (request.getServerPort() != 80 && request.getServerPort() != 443) {
      url.append(':').append(request.getServerPort());
    }
    url.append(servletContext.getContextPath());
    return url;
  }

  private boolean isValidCaptch(HttpSession session, String captchaValue) {
    Captcha captcha = (Captcha) session.getAttribute(NAME);
    return ((captcha != null) && (captcha.isCorrect(captchaValue)));
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
                          "PORTLET/social/ExternalOnboarding",
                          Collections.emptyList(),
                          Collections.singletonList("portal/login"),
                          params -> extendApplicationParameters(params, parameters));
    servletContext.getRequestDispatcher(EXTERNAL_REGISTRATION_JSP_PATH).include(request, response);
    return true;
  }

  private void redirectToLoginPage(HttpServletRequest request,
                                   HttpServletResponse response,
                                   String initialUri) throws IOException {
    // Invalidate the Captcha
    request.getSession().removeAttribute(NAME);

    String path = !StringUtils.startsWith(initialUri, "/") ? servletContext.getContextPath() + LOGIN
                                                           : response.encodeRedirectURL(initialUri);
    response.sendRedirect(path);
  }

  @Override
  protected boolean getRequiresLifeCycle() {
    return true;
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

  private boolean serveCaptchaImage(HttpServletRequest req, HttpServletResponse resp) {
    HttpSession session = req.getSession();
    Captcha captcha;
    if (session.getAttribute(NAME) == null) {
      List<Font> textFonts = Arrays.asList(new Font("Arial", Font.BOLD, 40), new Font("Courier", Font.BOLD, 40));
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
      LOG.error(e.getMessage(), e);
    }
  }

  private String generateUsername(String firstname, String lastname) throws Exception {
    String userNameBase = new StringBuffer(firstname.replaceAll("\\s", "")).append(".")
                                                                           .append(lastname.replaceAll("\\s", ""))
                                                                           .toString()
                                                                           .toLowerCase();
    userNameBase = unAccent(userNameBase);
    String username = userNameBase;
    Random rand = new Random();// NOSONAR
    // Check if user name already existed (with identity manager, need to
    // move the handler to social)
    while (findUser(username) != null) {
      int num = rand.nextInt(89) + 10;// range between 10 and 99.
      username = userNameBase + String.valueOf(num);
    }
    return username;
  }

  private String unAccent(String src) {
    return Normalizer.normalize(src, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "").replace("'", "").replace("`", "");
  }

  private void wrapForAutomaticLogin(HttpServletRequest request,
                                     HttpServletResponse response,
                                     String initialUri,
                                     String username,
                                     String password) throws ServletException, IOException {
    restartTransaction();
    HttpServletRequestWrapper wrappedRequestForLogin = wrapRequestForLogin(request, username, password, initialUri);
    request.getRequestDispatcher(servletContext.getContextPath() + LOGIN).include(wrappedRequestForLogin, response);
    redirectToLoginPage(request, response, initialUri);
  }

  private HttpServletRequestWrapper wrapRequestForLogin(HttpServletRequest request,
                                                        String username,
                                                        String password,
                                                        String initialUri) {

    return new HttpServletRequestWrapper(request) {
      @Override
      public String getParameter(String name) {
        if (StringUtils.equals(name, USERNAME_REQUEST_PARAM)) {
          return username;
        } else if (StringUtils.equals(name, PASSWORD_REQUEST_PARAM)) {
          return password;
        } else if (StringUtils.equals(name, INITIAL_URI_PARAM)) {
          return initialUri;
        } else {
          return super.getParameter(name);
        }
      }

      @Override
      public String getRequestURI() {
        return servletContext.getContextPath() + LOGIN;
      }
    };
  }

  private void restartTransaction() {
    int i = 0;
    // Close transactions until no encapsulated transaction
    boolean success = true;
    do {
      try {
        RequestLifeCycle.end();
        i++;
      } catch (IllegalStateException e) {
        success = false;
      }
    } while (success);

    // Restart transactions with the same number of encapsulations
    for (int j = 0; j < i; j++) {
      RequestLifeCycle.begin(container);
    }
  }

  private String generateUserDetailCredential(User user) {
    return user.getUserName() + "::" + user.getFirstName() + "::" + user.getLastName() + "::" + user.getEmail() + "::"
        + user.getPassword();
  }

  private User generateUserFromCredential(String data) {
    String[] dataParts = StringUtils.split(data, "::");
    User user = organizationService.getUserHandler().createUserInstance(dataParts[0]);
    user.setFirstName(dataParts[1]);
    user.setLastName(dataParts[2]);
    user.setEmail(dataParts[3]);

    // password could contains '::'
    // to extract it, we must not simply get last part, as we could be a not
    // complete password.
    String firstPart = dataParts[0] + "::" + dataParts[1] + "::" + dataParts[2] + "::" + dataParts[3] + "::";
    String password = data.substring(firstPart.length());
    user.setPassword(password);
    return user;
  }

}
