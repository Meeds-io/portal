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

import static org.exoplatform.web.security.security.CookieTokenService.EMAIL_VALIDATION_TOKEN;
import static org.exoplatform.web.security.security.CookieTokenService.EXTERNAL_REGISTRATION_TOKEN;
import static org.exoplatform.web.security.security.CookieTokenService.FORGOT_PASSWORD_TOKEN;
import static org.exoplatform.web.security.security.CookieTokenService.ONBOARD_TOKEN;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.exoplatform.commons.utils.I18N;
import org.exoplatform.commons.utils.MailUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.Constants;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.mail.Message;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.login.onboarding.OnboardingHandler;
import org.exoplatform.web.register.ExternalRegisterHandler;
import org.exoplatform.web.security.Token;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.web.security.security.RemindPasswordTokenService;

public class PasswordRecoveryServiceImpl implements PasswordRecoveryService {

  private static final String                  USER_FIRST_NAME_PARAM            = "${FIRST_NAME}";

  private static final String                  LOGIN_LINK_PARAM                 = "${LOGIN_LINK}";

  private static final String                  USERNAME_PARAM                   = "${USERNAME}";

  private static final String                  DISPLAY_NAME_PARAM               = "${DISPLAY_NAME}";

  private static final String                  EXTERNAL_REGISTRATION_LINK_PARAM = "${EXTERNAL_REGISTRATION_LINK}";

  private static final String                  SPACE_DISPLAY_NAME_PARAM         = "${SPACE_DISPLAY_NAME}";

  private static final String                  SENDER_DISPLAY_NAME_PARAM        = "${SENDER_DISPLAY_NAME}";

  private static final String                  RESET_PASSWORD_LINK_PARAM        = "${RESET_PASSWORD_LINK}";

  private static final String                  USER_DISPLAY_NAME_PARAM          = "${USER_DISPLAY_NAME}";

  private static final String                  COMPANY_NAME_PARAM               = "${COMPANY_NAME}";

  public static final String                   AUTHENTICATION_ATTEMPTS          = "authenticationAttempts";

  protected static Log                         log                              =
                                                   ExoLogger.getLogger(PasswordRecoveryServiceImpl.class);

  private final OrganizationService            orgService;

  private final MailService                    mailService;

  private final ResourceBundleService          bundleService;

  private final RemindPasswordTokenService     remindPasswordTokenService;

  private final CookieTokenService             cookieTokenService;

  private final BrandingService                brandingService;

  private final WebAppController               webController;

  public static final String                   CONFIGURED_DOMAIN_URL_KEY        = "gatein.email.domain.url";

  private String                               changePasswordConnectorName;

  private Map<String, ChangePasswordConnector> changePasswordConnectorMap;

  public PasswordRecoveryServiceImpl(InitParams initParams,
                                     OrganizationService orgService,
                                     MailService mailService,
                                     ResourceBundleService bundleService,
                                     RemindPasswordTokenService remindPasswordTokenService,
                                     CookieTokenService cookieTokenService,
                                     WebAppController controller,
                                     BrandingService brandingService) {
    this.orgService = orgService;
    this.mailService = mailService;
    this.bundleService = bundleService;
    this.remindPasswordTokenService = remindPasswordTokenService;
    this.cookieTokenService = cookieTokenService;
    this.webController = controller;
    this.brandingService = brandingService;
    this.changePasswordConnectorMap = new HashMap<>();
    this.changePasswordConnectorName = initParams.getValueParam("changePasswordConnector").getValue();

  }

  @Override
  public void addConnector(ChangePasswordConnector connector) {
    if (!this.changePasswordConnectorMap.containsKey(connector.getName())) {
      changePasswordConnectorMap.put(connector.getName(), connector);
    }
  }

  @Override
  public String verifyToken(String tokenId, String type) {
    Token token = remindPasswordTokenService.getToken(tokenId, type);
    if (token == null || token.isExpired()) {
      return null;
    }
    return token.getUsername();
  }

  @Override
  public void deleteToken(String tokenId, String type) {
    remindPasswordTokenService.deleteToken(tokenId, type);
  }

  @Override
  public String verifyToken(String tokenId) {
    return verifyToken(tokenId, "");
  }

  @Override
  public boolean allowChangePassword(String username) throws Exception {
    User user = orgService.getUserHandler().findUserByName(username);// To be
                                                                     // changed
                                                                     // later by
                                                                     // checking
                                                                     // internal
                                                                     // store
                                                                     // information
                                                                     // from
                                                                     // social
                                                                     // user
                                                                     // profile
    return user != null && (user.isInternalStore()
        || this.changePasswordConnectorMap.get(this.changePasswordConnectorName).isAllowChangeExternalPassword());
  }

  @Override
  public boolean changePass(final String tokenId, final String tokenType, final String username, final String password) {
    try {
      this.changePasswordConnectorMap.get(this.changePasswordConnectorName).changePassword(username, password);
      try {
        remindPasswordTokenService.deleteToken(tokenId, tokenType);
        remindPasswordTokenService.deleteTokensByUsernameAndType(username, tokenType);

        // delete all token which have no type
        // this is rememberMe token
        // as user have change his password, these tokens are no more valid
        cookieTokenService.deleteTokensByUsernameAndType(username, "");

      } catch (Exception ex) {
        log.warn("Can not delete token: " + tokenId, ex);
      }

      User user = orgService.getUserHandler().findUserByName(username);
      if (user != null) {
        UserProfile profile = orgService.getUserProfileHandler().findUserProfileByName(username);
        if (profile != null && profile.getAttribute(AUTHENTICATION_ATTEMPTS)!=null && !profile.getAttribute(AUTHENTICATION_ATTEMPTS).equals("0")) {
          profile.setAttribute(AUTHENTICATION_ATTEMPTS, String.valueOf(0));
          orgService.getUserProfileHandler().saveUserProfile(profile, true);
        }
      }

      return true;
    } catch (Exception ex) {
      log.error("Can not change pass for user: " + username, ex);
      return false;
    }
  }

  @Override
  public boolean sendOnboardingEmail(User user, Locale locale, StringBuilder url) {
    if (user == null) {
      throw new IllegalArgumentException("User or Locale must not be null");
    }

    ResourceBundle bundle = bundleService.getResourceBundle(bundleService.getSharedResourceBundleNames(), locale);

    String tokenId = remindPasswordTokenService.createToken(user.getUserName(), ONBOARD_TOKEN);
    StringBuilder redirectUrl = new StringBuilder();
    redirectUrl.append(url);
    redirectUrl.append("/" + OnboardingHandler.NAME);
    redirectUrl.append("?lang=" + I18N.toTagIdentifier(locale));
    redirectUrl.append("&token=" + tokenId);
    String emailBody = buildOnboardingEmailBody(user, bundle, redirectUrl.toString());
    String emailSubject = bundle.getString("onboarding.email.header") + " " + brandingService.getCompanyName();
    String senderName = MailUtils.getSenderName();
    String from = MailUtils.getSenderEmail();
    if (senderName != null && !senderName.trim().isEmpty()) {
      from = senderName + " <" + from + ">";
    }

    Message message = new Message();
    message.setFrom(from);
    message.setTo(user.getEmail());
    message.setSubject(emailSubject);
    message.setBody(emailBody);
    message.setMimeType("text/html");

    try {
      mailService.sendMessage(message);
    } catch (Exception ex) {
      log.error("Failure to send onboarding email", ex);
      return false;
    }

    return true;
  }

  private String buildOnboardingEmailBody(User user, ResourceBundle bundle, String link) {
    String content;
    InputStream input = this.getClass().getClassLoader().getResourceAsStream("conf/onBoarding_email_template.html");
    if (input == null) {
      content = "";
    } else {
      content = resolveLanguage(input, bundle);
    }

    content = content.replace(USER_DISPLAY_NAME_PARAM, user == null || user.getDisplayName() == null ? "" : user.getDisplayName());
    content = content.replace(COMPANY_NAME_PARAM, brandingService.getCompanyName());
    content = content.replace(RESET_PASSWORD_LINK_PARAM, link);

    return content;
  }

  @Override
  public String sendExternalRegisterEmail(String sender,
                                          String email,
                                          Locale locale,
                                          String space,
                                          StringBuilder url) throws Exception {
    return sendExternalRegisterEmail(sender, email, locale, space, url, true);
  }

  @Override
  public String sendExternalRegisterEmail(String sender,
                                          String email,
                                          Locale locale,
                                          String space,
                                          StringBuilder url,
                                          boolean spaceInvitation) throws Exception {

    ResourceBundle bundle = bundleService.getResourceBundle(bundleService.getSharedResourceBundleNames(), locale);

    String token = createToken(email);

    StringBuilder redirectUrl = new StringBuilder();
    redirectUrl.append(url);
    redirectUrl.append("/" + ExternalRegisterHandler.NAME);
    redirectUrl.append("?lang=" + I18N.toTagIdentifier(locale));
    redirectUrl.append("&token=" + token);

    String emailBody;
    String emailSubject;
    if (spaceInvitation) {
      UserHandler uHandler = orgService.getUserHandler();
      String senderFullName = uHandler.findUserByName(sender).getDisplayName();
      emailBody = buildExternalEmailBody(senderFullName, space, redirectUrl.toString(), bundle);
      emailSubject = bundle.getString("external.email.subject") + " " + (space != null ? space : "")
          + " " + bundle.getString("external.email.on") + " " + brandingService.getCompanyName() ;
    } else {
      emailBody = buildOnboardingEmailBody(null, bundle, redirectUrl.toString());
      emailSubject = bundle.getString("onboarding.email.header") + " " + brandingService.getCompanyName();
    }

    String senderName = MailUtils.getSenderName();
    String from = MailUtils.getSenderEmail();
    if (senderName != null && !senderName.trim().isEmpty()) {
      from = senderName + " <" + from + ">";
    }

    Message message = new Message();
    message.setFrom(from);
    message.setTo(email);
    message.setSubject(emailSubject);
    message.setBody(emailBody);
    message.setMimeType("text/html");
    mailService.sendMessage(message);
    return token;
  }

  private String createToken(String email) {
    return remindPasswordTokenService.createToken(email, EXTERNAL_REGISTRATION_TOKEN);
  }

  private String buildExternalEmailBody(String sender, String space, String link, ResourceBundle bundle) {
    String content;
    InputStream input = this.getClass().getClassLoader().getResourceAsStream("conf/external_email_template.html");
    if (input == null) {
      content = "";
    } else {
      content = resolveLanguage(input, bundle);
    }

    content = content.replace(SENDER_DISPLAY_NAME_PARAM, sender);
    content = content.replace(COMPANY_NAME_PARAM, brandingService.getCompanyName());
    content = content.replace(SPACE_DISPLAY_NAME_PARAM, space);
    content = content.replace(EXTERNAL_REGISTRATION_LINK_PARAM, link);

    return content;
  }

  @Override
  public boolean sendAccountVerificationEmail(String data, String username, String firstName, String lastName, String email, Locale locale, StringBuilder url) {
    try {
      ResourceBundle bundle = bundleService.getResourceBundle(bundleService.getSharedResourceBundleNames(), locale);

      String tokenId = remindPasswordTokenService.createToken(data, EMAIL_VALIDATION_TOKEN);

      StringBuilder redirectUrl = new StringBuilder();
      redirectUrl.append(url);
      redirectUrl.append("/").append(ExternalRegisterHandler.NAME);
      redirectUrl.append("?action=" + ExternalRegisterHandler.VALIDATE_EXTERNAL_EMAIL_ACTION);
      redirectUrl.append("&token=" + tokenId);

      String emailBody = buildExternalVerificationAccountEmailBody(firstName + " " + lastName,
                                                                   username,
                                                                   redirectUrl.toString(),
                                                                   bundle);
      String emailSubject = bundle.getString("external.verification.account.email.subject") + " "
          + brandingService.getCompanyName() + "!";

      String senderName = MailUtils.getSenderName();
      String from = MailUtils.getSenderEmail();
      if (senderName != null && !senderName.trim().isEmpty()) {
        from = senderName + " <" + from + ">";
      }

      Message message = new Message();
      message.setFrom(from);
      message.setTo(email);
      message.setSubject(emailSubject);
      message.setBody(emailBody);
      message.setMimeType("text/html");

      mailService.sendMessage(message);
    } catch (Exception ex) {
      log.error("Failure to send external confirmation account email", ex);
      return false;
    }

    return true;
  }

  @Override
  public boolean sendAccountCreatedConfirmationEmail(String username, Locale locale, StringBuilder url) {

    try {
      User user = orgService.getUserHandler().findUserByName(username);

      ResourceBundle bundle = bundleService.getResourceBundle(bundleService.getSharedResourceBundleNames(), locale);

      StringBuilder redirectUrl = new StringBuilder();
      redirectUrl.append(url);
      redirectUrl.append(ExternalRegisterHandler.LOGIN);

      String emailBody = buildExternalConfirmationAccountEmailBody(user.getDisplayName(),
                                                                   user.getUserName(),
                                                                   redirectUrl.toString(),
                                                                   bundle);
      String emailSubject = bundle.getString("external.confirmation.account.email.subject") + " "
          + brandingService.getCompanyName() + "!";

      String senderName = MailUtils.getSenderName();
      String from = MailUtils.getSenderEmail();
      if (senderName != null && !senderName.trim().isEmpty()) {
        from = senderName + " <" + from + ">";
      }

      Message message = new Message();
      message.setFrom(from);
      message.setTo(user.getEmail());
      message.setSubject(emailSubject);
      message.setBody(emailBody);
      message.setMimeType("text/html");

      mailService.sendMessage(message);
    } catch (Exception ex) {
      log.error("Failure to send external confirmation account email", ex);
      return false;
    }

    return true;
  }

  private String buildExternalConfirmationAccountEmailBody(String dispalyName,
                                                           String username,
                                                           String link,
                                                           ResourceBundle bundle) {
    String content;
    InputStream input = this.getClass()
                            .getClassLoader()
                            .getResourceAsStream("conf/external_confirmation_account_email_template.html");
    if (input == null) {
      content = "";
    } else {
      content = resolveLanguage(input, bundle);
    }

    content = content.replace(DISPLAY_NAME_PARAM, dispalyName);
    content = content.replace(COMPANY_NAME_PARAM, brandingService.getCompanyName());
    content = content.replace(USERNAME_PARAM, username);
    content = content.replace(LOGIN_LINK_PARAM, link);

    return content;
  }

  private String buildExternalVerificationAccountEmailBody(String displayName,
                                                           String username,
                                                           String link,
                                                           ResourceBundle bundle) {
    String content;
    InputStream input = this.getClass()
        .getClassLoader()
        .getResourceAsStream("conf/external_verification_account_email_template.html");
    if (input == null) {
      content = "";
    } else {
      content = resolveLanguage(input, bundle);
    }

    content = content.replace(DISPLAY_NAME_PARAM, displayName);
    content = content.replace(COMPANY_NAME_PARAM, brandingService.getCompanyName());
    content = content.replace(USERNAME_PARAM, username);
    content = content.replace(LOGIN_LINK_PARAM, link);

    return content;
  }

  @Override
  public boolean sendRecoverPasswordEmail(User user, Locale defaultLocale, HttpServletRequest req) {
    if (user == null) {
      throw new IllegalArgumentException("User or Locale must not be null");
    }

    Locale locale = getLocaleOfUser(user.getUserName(), defaultLocale);

    PortalContainer container = PortalContainer.getCurrentInstance(req.getServletContext());

    ResourceBundle bundle = bundleService.getResourceBundle(bundleService.getSharedResourceBundleNames(), locale);

    String tokenId = remindPasswordTokenService.createToken(user.getUserName(), FORGOT_PASSWORD_TOKEN);

    Router router = webController.getRouter();
    Map<QualifiedName, String> params = new HashMap<>();
    params.put(WebAppController.HANDLER_PARAM, PasswordRecoveryHandler.NAME);
    params.put(PasswordRecoveryHandler.TOKEN, tokenId);
    params.put(PasswordRecoveryHandler.LANG, I18N.toTagIdentifier(locale));

    StringBuilder url = new StringBuilder();
    url.append(req.getScheme()).append("://").append(req.getServerName());
    if (req.getServerPort() != 80 && req.getServerPort() != 443) {
      url.append(':').append(req.getServerPort());
    }
    url.append(container.getPortalContext().getContextPath());
    url.append(router.render(params));

    String emailBody = buildRecoverEmailBody(user, bundle, url.toString());
    String emailSubject = getEmailSubject(user, bundle);

    String senderName = MailUtils.getSenderName();
    String from = MailUtils.getSenderEmail();
    if (senderName != null && !senderName.trim().isEmpty()) {
      from = senderName + " <" + from + ">";
    }

    Message message = new Message();
    message.setFrom(from);
    message.setTo(user.getEmail());
    message.setSubject(emailSubject);
    message.setBody(emailBody);
    message.setMimeType("text/html");

    try {
      mailService.sendMessage(message);
    } catch (Exception ex) {
      log.error("Failure to send recover password email", ex);
      return false;
    }

    return true;
  }

  private Locale getLocaleOfUser(String username, Locale defLocale) {
    try {
      UserProfile profile = orgService.getUserProfileHandler().findUserProfileByName(username);
      String lang = profile == null ? null : profile.getUserInfoMap().get(Constants.USER_LANGUAGE);
      return (lang != null) ? LocaleContextInfo.getLocale(lang) : defLocale;
    } catch (Exception ex) { // NOSONAR
      log.debug("Can not load user profile language", ex);
      return defLocale;
    }
  }

  private String buildRecoverEmailBody(User user, ResourceBundle bundle, String link) {
    String content;
    InputStream input = this.getClass().getClassLoader().getResourceAsStream("conf/forgot_password_email_template.html");
    if (input == null) {
      content = "";
    } else {
      content = resolveLanguage(input, bundle);
    }

    content = content.replace(USER_FIRST_NAME_PARAM, user.getFirstName());
    content = content.replace(COMPANY_NAME_PARAM, brandingService.getCompanyName());
    content = content.replace(USERNAME_PARAM, user.getUserName());
    content = content.replace(RESET_PASSWORD_LINK_PARAM, link);

    return content;
  }

  private String resolveLanguage(InputStream input, ResourceBundle bundle) {
    // Read from input string
    StringBuffer content = new StringBuffer();
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(input));
      String line;
      while ((line = reader.readLine()) != null) {
        if (content.length() > 0) {
          content.append("\n");
        }
        resolveLanguage(content, line, bundle);
      }
    } catch (IOException ex) {
      log.error(ex);
    }
    return content.toString();
  }

  private static final Pattern PATTERN = Pattern.compile("&\\{([a-zA-Z0-9\\.]+)\\}");

  private void resolveLanguage(StringBuffer sb, String input, ResourceBundle bundle) {
    Matcher matcher = PATTERN.matcher(input);
    while (matcher.find()) {
      String key = matcher.group(1);
      String resource;
      try {
        resource = bundle.getString(key);
      } catch (MissingResourceException ex) {
        resource = key;
      }
      matcher.appendReplacement(sb, resource);
    }
    matcher.appendTail(sb);
  }

  // These method will be overwrite on Platform project
  protected String getEmailSubject(User user, ResourceBundle bundle) {
    return bundle.getString("gatein.forgotPassword.email.subject");
  }

  @Override
  public String getOnboardingURL(String tokenId, String lang) {
    Router router = webController.getRouter();
    Map<QualifiedName, String> params = new HashMap<>();
    params.put(WebAppController.HANDLER_PARAM, OnboardingHandler.NAME);
    if (tokenId != null) {
      params.put(OnboardingHandler.TOKEN, tokenId);
    }
    if (lang != null) {
      params.put(OnboardingHandler.LANG, lang);
    }
    return router.render(params);
  }

  @Override
  public String getExternalRegistrationURL(String tokenId, String lang) {
    Router router = webController.getRouter();
    Map<QualifiedName, String> params = new HashMap<>();
    params.put(WebAppController.HANDLER_PARAM, ExternalRegisterHandler.NAME);
    if (tokenId != null) {
      params.put(ExternalRegisterHandler.TOKEN, tokenId);
    }
    if (lang != null) {
      params.put(ExternalRegisterHandler.LANG, lang);
    }
    return router.render(params);
  }

  @Override
  public String getPasswordRecoverURL(String tokenId, String lang) {
    Router router = webController.getRouter();
    Map<QualifiedName, String> params = new HashMap<>();
    params.put(WebAppController.HANDLER_PARAM, PasswordRecoveryHandler.NAME);
    if (tokenId != null) {
      params.put(PasswordRecoveryHandler.TOKEN, tokenId);
    }
    if (lang != null) {
      params.put(PasswordRecoveryHandler.LANG, lang);
    }
    return router.render(params);
  }

  @Override
  public ChangePasswordConnector getActiveChangePasswordConnector() {
    return this.changePasswordConnectorMap.get(this.changePasswordConnectorName);
  }

}
