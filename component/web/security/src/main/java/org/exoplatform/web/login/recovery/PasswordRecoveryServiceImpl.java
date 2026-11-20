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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import io.meeds.portal.mail.model.EmailBrand;
import io.meeds.portal.mail.model.EmailModel;
import io.meeds.portal.mail.model.EmailModelBuilder;
import io.meeds.portal.mail.util.EmailLayout;

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
import org.exoplatform.web.security.Token;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.web.security.security.OnboardingTokenService;
import org.exoplatform.web.security.security.RemindPasswordTokenService;

public class PasswordRecoveryServiceImpl implements PasswordRecoveryService {

  private static final String                  EXTERNAL_REGISTRATION_NAME       = "external-registration";

  public static final String                   AUTHENTICATION_ATTEMPTS          = "authenticationAttempts";

  public static final QualifiedName            TOKEN                            = QualifiedName.create("gtn", "token");

  public static final QualifiedName            LANG                             = QualifiedName.create("gtn", "lang");

  public static final String                   NAME                             = "forgot-password";

  protected static Log                         log                              =
                                                   ExoLogger.getLogger(PasswordRecoveryServiceImpl.class);

  private final OrganizationService            orgService;

  private final MailService                    mailService;

  private final ResourceBundleService          bundleService;

  private final RemindPasswordTokenService     remindPasswordTokenService;

  private final OnboardingTokenService         onboardingTokenService;

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
                                     OnboardingTokenService onboardingTokenService,
                                     CookieTokenService cookieTokenService,
                                     WebAppController controller,
                                     BrandingService brandingService) {
    this.orgService = orgService;
    this.mailService = mailService;
    this.bundleService = bundleService;
    this.remindPasswordTokenService = remindPasswordTokenService;
    this.onboardingTokenService = onboardingTokenService;
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
    Token token = tokenService(type).getToken(tokenId, type);
    if (token == null || token.isExpired()) {
      return null;
    }
    return token.getUsername();
  }

  @Override
  public void deleteToken(String tokenId, String type) {
    tokenService(type).deleteToken(tokenId, type);
  }

  @Override
  public long getOnboardingTokenValidity() {
    return onboardingTokenService.getValidityMillis();
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
        tokenService(tokenType).deleteToken(tokenId, tokenType);
        tokenService(tokenType).deleteTokensByUsernameAndType(username, tokenType);

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
    return sendOnboardingEmail(user, locale, url, null);
  }

  @Override
  public boolean sendOnboardingEmail(User user, Locale locale, StringBuilder url, String message) {
    if (user == null) {
      throw new IllegalArgumentException("User or Locale must not be null");
    }

    ResourceBundle bundle = bundleService.getResourceBundle(bundleService.getSharedResourceBundleNames(), locale);

    String tokenId = onboardingTokenService.createToken(user.getUserName(), ONBOARD_TOKEN);
    StringBuilder redirectUrl = new StringBuilder();
    redirectUrl.append(url);
    redirectUrl.append("/on-boarding");
    redirectUrl.append("?lang=" + I18N.toTagIdentifier(locale));
    redirectUrl.append("&token=" + tokenId);
    String emailBody = buildOnboardingEmailBody(user,
                                                bundle,
                                                redirectUrl.toString(),
                                                message,
                                                onboardingTokenService.getValidityMillis());
    String emailSubject = bundle.getString("onboarding.email.header") + " " + brandingService.getCompanyName();
    String senderName = MailUtils.getSenderName();
    String from = MailUtils.getSenderEmail();
    if (senderName != null && !senderName.trim().isEmpty()) {
      from = senderName + " <" + from + ">";
    }

    Message mail = new Message();
    mail.setFrom(from);
    mail.setTo(user.getEmail());
    mail.setSubject(emailSubject);
    mail.setBody(emailBody);
    mail.setMimeType("text/html");

    try {
      mailService.sendMessage(mail);
    } catch (Exception ex) {
      log.error("Failure to send onboarding email", ex);
      return false;
    }

    return true;
  }

  /**
   * Builds the onboarding email: the administrator's message, HTML its caller
   * has sanitized, is inserted as is above the link, and the link's
   * validity is written in days when it is a whole number of days from two
   * up, in whole hours otherwise
   */
  protected String buildOnboardingEmailBody(User user,
                                            ResourceBundle bundle,
                                            String link,
                                            String message,
                                            long validityMillis) {
    String name = user == null || user.getDisplayName() == null ? "" : user.getDisplayName();
    String company = brandingService.getCompanyName();
    EmailModelBuilder email = emailOf(bundle).title(text(bundle, "onboarding.email.header").trim() + " " + company)
                                              .paragraph(EmailLayout.greeting(text(bundle, "onboarding.email.hello"), name))
                                              .paragraph(text(bundle, "onboarding.email.welcome").trim() + " " + company)
                                              .lead(EmailLayout.leadIn(text(bundle, "onboarding.email.accessLink")))
                                              .button(text(bundle, "onboarding.email.join"), link)
                                              .note(text(bundle, "onboarding.email.linkValidity") + " "
                                                  + buildValidityLabel(bundle, validityMillis))
                                              .signoff(text(bundle, "onboarding.email.bye"));
    if (StringUtils.isNotBlank(message)) {
      email.trustedHtml(buildOnboardingMessageBlock(message));
    }
    return render(email);
  }

  /**
   * The message block of the onboarding email: the administrator's message
   * as its caller hands it, HTML the caller has sanitized (the editor's
   * markup, scripts and attributes removed), nothing when it is blank. The
   * block is inserted by the one-pass fill, so a placeholder form inside
   * the message stays text.
   */
  protected static String buildOnboardingMessageBlock(String message) {
    if (StringUtils.isBlank(message)) {
      return "";
    }
    return "<div style=\"margin: 0 0 10px 0;line-height:22px;\">" + message.trim() + "</div>";
  }

  /**
   * The validity of a link as the email states it: in days when it is a
   * whole number of days from two up, in whole hours otherwise (a day reads
   * "24 hours")
   */
  protected static String buildValidityLabel(ResourceBundle bundle, long validityMillis) {
    if (isWholeDays(validityMillis)) {
      long days = TimeUnit.MILLISECONDS.toDays(validityMillis);
      return label(bundle, "onboarding.email.validity.days", days);
    }
    return label(bundle, "onboarding.email.validity.hours", TimeUnit.MILLISECONDS.toHours(validityMillis));
  }

  /**
   * @param validityMillis a validity in milliseconds
   * @return true when the validity is stated in days: a whole number of days,
   *         from two up
   */
  protected static boolean isWholeDays(long validityMillis) {
    long days = TimeUnit.MILLISECONDS.toDays(validityMillis);
    return days >= 2 && validityMillis == TimeUnit.DAYS.toMillis(days);
  }

  private static String label(ResourceBundle bundle, String key, long value) {
    String pattern;
    try {
      pattern = bundle.getString(key);
    } catch (MissingResourceException e) {
      pattern = "{0}";
    }
    return pattern.replace("{0}", String.valueOf(value));
  }

  private CookieTokenService tokenService(String type) {
    return ONBOARD_TOKEN.equals(type) ? onboardingTokenService : remindPasswordTokenService;
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

    return sendExternalRegisterEmail(sender, email, locale, space, url, spaceInvitation, null);
  }

  @Override
  public String sendExternalRegisterEmail(String sender,
                                          String email,
                                          Locale locale,
                                          String space,
                                          StringBuilder url,
                                          boolean spaceInvitation,
                                          String initialUri) throws Exception {

    ResourceBundle bundle = bundleService.getResourceBundle(bundleService.getSharedResourceBundleNames(), locale);

    String token = createToken(email);

    StringBuilder redirectUrl = new StringBuilder();
    redirectUrl.append(url);
    redirectUrl.append("/").append(EXTERNAL_REGISTRATION_NAME);
    redirectUrl.append("?lang=").append(I18N.toTagIdentifier(locale));
    redirectUrl.append("&token=").append(token);

    if (initialUri != null && !initialUri.isBlank() && initialUri.startsWith("/")) {
      redirectUrl.append("&initialURI=")
                 .append(URLEncoder.encode(initialUri, StandardCharsets.UTF_8));
    }

    String emailBody;
    String emailSubject;

    if (spaceInvitation) {
      UserHandler uHandler = orgService.getUserHandler();
      String senderFullName = uHandler.findUserByName(sender).getDisplayName();
      emailBody = buildExternalEmailBody(senderFullName, space, redirectUrl.toString(), bundle);
      emailSubject = bundle.getString("external.email.subject") + " " + (space != null ? space : "")
          + " " + bundle.getString("external.email.on") + " " + brandingService.getCompanyName();
    } else {
      emailBody = buildOnboardingEmailBody(null,
                                           bundle,
                                           redirectUrl.toString(),
                                           null,
                                           TimeUnit.SECONDS.toMillis(remindPasswordTokenService.getValidityTime()));
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

  protected String buildExternalEmailBody(String sender, String space, String link, ResourceBundle bundle) {
    String company = brandingService.getCompanyName();
    return render(emailOf(bundle).title(sender + " " + text(bundle, "external.email.invitedBy") + " " + space + " "
        + text(bundle, "external.email.on") + " " + company)
                                 .paragraph(EmailLayout.greeting(text(bundle, "external.email.hello"), ""))
                                 .paragraph(text(bundle, "external.email.descriptionPart1"))
                                 .paragraph(text(bundle, "external.email.descriptionPart2"))
                                 .lead(text(bundle, "external.email.accessLink"))
                                 .button(text(bundle, "external.email.create"), link)
                                 .note(text(bundle, "external.email.note"))
                                 .signoff(text(bundle, "external.email.bye")));
  }

  @Override
  public boolean sendAccountVerificationEmail(String data, String username, String firstName, String lastName, String email, Locale locale, StringBuilder url) {
    try {
      ResourceBundle bundle = bundleService.getResourceBundle(bundleService.getSharedResourceBundleNames(), locale);

      String tokenId = remindPasswordTokenService.createToken(data, EMAIL_VALIDATION_TOKEN);

      StringBuilder redirectUrl = new StringBuilder();
      redirectUrl.append(url);
      redirectUrl.append("/").append(EXTERNAL_REGISTRATION_NAME);
      redirectUrl.append("?action=validateEmail");
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
      redirectUrl.append("/login");

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

  protected String buildExternalConfirmationAccountEmailBody(String displayName,
                                                           String username,
                                                           String link,
                                                           ResourceBundle bundle) {
    return buildAccountCreatedEmailBody("external.confirmation.account.email.", displayName, username, link, bundle);
  }

  private String buildExternalVerificationAccountEmailBody(String displayName,
                                                           String username,
                                                           String link,
                                                           ResourceBundle bundle) {
    return buildAccountCreatedEmailBody("external.verification.account.email.", displayName, username, link, bundle);
  }

  protected String buildAccountCreatedEmailBody(String prefix,
                                              String displayName,
                                              String username,
                                              String link,
                                              ResourceBundle bundle) {
    return render(emailOf(bundle).title(text(bundle, prefix + "header") + " " + brandingService.getCompanyName() + "!")
                                 .paragraph(EmailLayout.greeting(text(bundle, prefix + "hi"), displayName))
                                 .paragraph(text(bundle, prefix + "thanks") + ".")
                                 .fact(text(bundle, prefix + "reminder"), username)
                                 .lead(text(bundle, prefix + "necessary"))
                                 .button(text(bundle, prefix + "signIn"), link)
                                 .signoff(text(bundle, prefix + "bye")));
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

    StringBuilder url = new StringBuilder();
    url.append(req.getScheme()).append("://").append(req.getServerName());
    if (req.getServerPort() != 80 && req.getServerPort() != 443) {
      url.append(':').append(req.getServerPort());
    }
    url.append(container.getPortalContext().getContextPath());
    url.append(getPasswordRecoverURL(tokenId, I18N.toTagIdentifier(locale)));

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

  protected String buildRecoverEmailBody(User user, ResourceBundle bundle, String link) {
    return render(emailOf(bundle).title(text(bundle, "gatein.forgotPassword.email.header"))
                                 .paragraph(EmailLayout.greeting(text(bundle, "gatein.forgotPassword.email.hi"), user.getFirstName()))
                                 .paragraph(text(bundle, "gatein.forgotPassword.email.message"))
                                 .fact(text(bundle, "gatein.forgotPassword.email.username"), user.getUserName())
                                 .lead(text(bundle, "gatein.forgotPassword.email.linkDescription"))
                                 .button(text(bundle, "gatein.forgotPassword.email.button", "Reset password"), link)
                                 .aside(text(bundle, "gatein.forgotPassword.email.helpMessage")));
  }

  // These method will be overwrite on Platform project
  protected String getEmailSubject(User user, ResourceBundle bundle) {
    return bundle.getString("gatein.forgotPassword.email.subject");
  }

  @Override
  public String getOnboardingURL(String tokenId, String lang) {
    String passwordRecoveryPath = "/portal/on-boarding";
    if (tokenId != null) {
      passwordRecoveryPath = "%s%stoken=%s".formatted(passwordRecoveryPath, passwordRecoveryPath.contains("?") ? "&" : "?", tokenId);
    }
    if (lang != null) {
      passwordRecoveryPath = "%s%slang=%s".formatted(passwordRecoveryPath, passwordRecoveryPath.contains("?") ? "&" : "?", lang);

    }
    return passwordRecoveryPath;
  }

  @Override
  public String getExternalRegistrationURL(String tokenId, String lang) {
    String passwordRecoveryPath = "/portal/external-registration";
    if (tokenId != null) {
      passwordRecoveryPath = "%s%stoken=%s".formatted(passwordRecoveryPath, passwordRecoveryPath.contains("?") ? "&" : "?", tokenId);
    }
    if (lang != null) {
      passwordRecoveryPath = "%s%slang=%s".formatted(passwordRecoveryPath, passwordRecoveryPath.contains("?") ? "&" : "?", lang);

    }
    return passwordRecoveryPath;
  }

  @Override
  public String getPasswordRecoverURL(String tokenId, String lang) {
    String passwordRecoveryPath = "/forgot-password";
    if (tokenId != null) {
      passwordRecoveryPath = "%s%stoken=%s".formatted(passwordRecoveryPath, passwordRecoveryPath.contains("?") ? "&" : "?", tokenId);
    }
    if (lang != null) {
      passwordRecoveryPath = "%s%slang=%s".formatted(passwordRecoveryPath, passwordRecoveryPath.contains("?") ? "&" : "?", lang);

    }
    return passwordRecoveryPath;
  }

  @Override
  public ChangePasswordConnector getActiveChangePasswordConnector() {
    return this.changePasswordConnectorMap.get(this.changePasswordConnectorName);
  }

  private EmailModelBuilder emailOf(ResourceBundle bundle) {
    // a bundle that does not tell its locale, or tells the root one, gives an English document, never "und"
    Locale locale = bundle.getLocale();
    return EmailModel.builder()
                     .lang(locale == null || locale.getLanguage().isEmpty() ? null : locale.toLanguageTag())
                     .fallbackLabel(text(bundle, "email.button.fallback", "If the button does not work, copy this address into your browser:"));
  }

  private String render(EmailModelBuilder email) {
    return EmailLayout.render(email.build(), EmailBrand.from(brandingService));
  }

  private static String text(ResourceBundle bundle, String key) {
    return text(bundle, key, key);
  }

  private static String text(ResourceBundle bundle, String key, String defaultValue) {
    try {
      return bundle.getString(key);
    } catch (MissingResourceException _) {
      return defaultValue;
    }
  }

}
