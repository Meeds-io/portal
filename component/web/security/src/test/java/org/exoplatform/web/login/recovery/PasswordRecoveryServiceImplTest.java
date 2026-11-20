/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.utils.MailUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.mail.Message;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.idm.UserImpl;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.security.PortalToken;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.web.security.security.OnboardingTokenService;
import org.exoplatform.web.security.security.RemindPasswordTokenService;

@RunWith(MockitoJUnitRunner.class)
public class PasswordRecoveryServiceImplTest {

  private static final long           SEVEN_DAYS = TimeUnit.DAYS.toMillis(7);

  @Mock
  private OrganizationService         organizationService;

  @Mock
  private MailService                 mailService;

  @Mock
  private ResourceBundleService       bundleService;

  @Mock
  private RemindPasswordTokenService  remindPasswordTokenService;

  @Mock
  private OnboardingTokenService      onboardingTokenService;

  @Mock
  private CookieTokenService          cookieTokenService;

  @Mock
  private WebAppController            webAppController;

  @Mock
  private BrandingService             brandingService;

  private PasswordRecoveryServiceImpl passwordRecoveryService;

  private ResourceBundle              bundle;

  @Before
  public void setUp() {
    InitParams initParams = new InitParams();
    ValueParam connector = new ValueParam();
    connector.setName("changePasswordConnector");
    connector.setValue("DefaultChangePasswordConnector");
    initParams.addParameter(connector);
    passwordRecoveryService = new PasswordRecoveryServiceImpl(initParams,
                                                              organizationService,
                                                              mailService,
                                                              bundleService,
                                                              remindPasswordTokenService,
                                                              onboardingTokenService,
                                                              cookieTokenService,
                                                              webAppController,
                                                              brandingService);
    bundle = new ResourceBundle() {
      private final Map<String, String> labels = Map.of("onboarding.email.validity.days",
                                                        "{0} days",
                                                        "onboarding.email.validity.hours",
                                                        "{0} hours",
                                                        "onboarding.email.header",
                                                        "Join",
                                                        "onboarding.email.hello",
                                                        "Hello",
                                                        "gatein.forgotPassword.email.hi",
                                                        "Hi",
                                                        "gatein.forgotPassword.email.username",
                                                        "Your login",
                                                        "external.confirmation.account.email.reminder",
                                                        "Your username is:",
                                                        "external.confirmation.account.email.signIn",
                                                        "Sign In",
                                                        "email.button.fallback",
                                                        "Copy this address:",
                                                        "onboarding.email.accessLink",
                                                        "Click the link");

      @Override
      protected Object handleGetObject(String key) {
        return labels.get(key);
      }

      @Override
      public Enumeration<String> getKeys() {
        return new Vector<>(labels.keySet()).elements();
      }
    };
    lenient().when(bundleService.getSharedResourceBundleNames()).thenReturn(new String[0]);
    lenient().when(bundleService.getResourceBundle(any(String[].class), any(Locale.class))).thenReturn(bundle);
    lenient().when(brandingService.getCompanyName()).thenReturn("Acme");
    lenient().when(onboardingTokenService.getValidityMillis()).thenReturn(SEVEN_DAYS);
    lenient().when(onboardingTokenService.createToken(anyString(), eq(CookieTokenService.ONBOARD_TOKEN))).thenReturn("tok.en");
    lenient().when(remindPasswordTokenService.getValidityTime()).thenReturn(TimeUnit.HOURS.toSeconds(24));
  }

  /**
   * The message is the caller's sanitized HTML, inserted as given: its
   * markup stays, and a placeholder's form inside it stays text, never the
   * link
   */
  @Test
  public void testOnboardingMessageIsInsertedAsGivenWithPlaceholdersAsText() {
    String body = passwordRecoveryService.buildOnboardingEmailBody(user(),
                                                                   bundle,
                                                                   "https://acme.example/on-boarding?token=tok.en",
                                                                   "<p>Welcome ${RESET_PASSWORD_LINK} <b>Jane</b></p>",
                                                                   SEVEN_DAYS);
    assertTrue(body, body.contains("<p>Welcome ${RESET_PASSWORD_LINK} <b>Jane</b></p>"));
    assertTrue(body, body.contains("href=\"https://acme.example/on-boarding?token=tok.en\""));
    // the button (twice, for Outlook and for the other clients), the plain address (twice) and the Gmail action (twice)
    assertEquals(body, 6, countOf(body, "https://acme.example/on-boarding?token=tok.en"));
    // The only placeholder form left is the message's own text
    assertFalse(body, body.replace("Welcome ${RESET_PASSWORD_LINK}", "").contains("${"));
  }

  @Test
  public void testOnboardingMessageBlockIsAbsentWhenBlank() {
    String body = passwordRecoveryService.buildOnboardingEmailBody(user(), bundle, "https://acme.example/x", "  ", SEVEN_DAYS);
    assertFalse(body, body.contains("line-height:22px;\">"));
    assertFalse(body, body.contains("${"));
    assertTrue(body, body.contains("Jane Doe"));
    assertTrue(body, body.contains("Acme"));
  }

  /**
   * Whole days from one day on, whole hours under a day
   */
  @Test
  public void testValidityLabel() {
    assertEquals("7 days", PasswordRecoveryServiceImpl.buildValidityLabel(bundle, SEVEN_DAYS));
    assertEquals("24 hours", PasswordRecoveryServiceImpl.buildValidityLabel(bundle, TimeUnit.HOURS.toMillis(24)));
    assertEquals("25 hours", PasswordRecoveryServiceImpl.buildValidityLabel(bundle, TimeUnit.HOURS.toMillis(25)));
    assertEquals("2 days", PasswordRecoveryServiceImpl.buildValidityLabel(bundle, TimeUnit.HOURS.toMillis(48)));
    assertEquals("36 hours", PasswordRecoveryServiceImpl.buildValidityLabel(bundle, TimeUnit.HOURS.toMillis(36)));
    assertEquals("23 hours", PasswordRecoveryServiceImpl.buildValidityLabel(bundle, TimeUnit.HOURS.toMillis(23)));
    assertEquals("0 hours", PasswordRecoveryServiceImpl.buildValidityLabel(bundle, TimeUnit.MINUTES.toMillis(30)));
  }

  /**
   * The onboarding email is sent with the onboarding token's validity and
   * the message; the external registration email shares the template with
   * the password reminder's validity and no message
   */
  @Test
  public void testSendOnboardingEmailStatesTheOnboardingValidity() throws Exception {
    try (MockedStatic<MailUtils> mailUtils = Mockito.mockStatic(MailUtils.class)) {
      mailUtils.when(MailUtils::getSenderName).thenReturn("Acme");
      mailUtils.when(MailUtils::getSenderEmail).thenReturn("noreply@acme.example");
      assertTrue(passwordRecoveryService.sendOnboardingEmail(user(),
                                                             Locale.ENGLISH,
                                                             new StringBuilder("https://acme.example/portal"),
                                                             "Hello there"));
      ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
      verify(mailService).sendMessage(sent.capture());
      String body = sent.getValue().getBody();
      assertTrue(body, body.contains("7 days"));
      assertTrue(body, body.contains("Hello there"));
      assertTrue(body, body.contains("href=\"https://acme.example/portal/on-boarding?lang=en&amp;token=tok.en\""));
      assertEquals("jane@example.com", sent.getValue().getTo());
      verify(remindPasswordTokenService, never()).createToken(anyString(), anyString());
    }
  }

  @Test
  public void testSendOnboardingEmailWithoutMessageKeepsTheThreeArgumentContract() throws Exception {
    try (MockedStatic<MailUtils> mailUtils = Mockito.mockStatic(MailUtils.class)) {
      mailUtils.when(MailUtils::getSenderName).thenReturn("Acme");
      mailUtils.when(MailUtils::getSenderEmail).thenReturn("noreply@acme.example");
      assertTrue(passwordRecoveryService.sendOnboardingEmail(user(), Locale.FRENCH, new StringBuilder("https://acme.example/portal")));
      ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
      verify(mailService).sendMessage(sent.capture());
      assertFalse(sent.getValue().getBody(), sent.getValue().getBody().contains("line-height:22px;\">"));
      assertTrue(sent.getValue().getBody(), sent.getValue().getBody().contains("lang=fr"));
    }
  }

  @Test
  public void testExternalRegistrationEmailStatesTheReminderValidity() throws Exception {
    try (MockedStatic<MailUtils> mailUtils = Mockito.mockStatic(MailUtils.class)) {
      mailUtils.when(MailUtils::getSenderName).thenReturn("Acme");
      mailUtils.when(MailUtils::getSenderEmail).thenReturn("noreply@acme.example");
      when(remindPasswordTokenService.createToken("jane@example.com", CookieTokenService.EXTERNAL_REGISTRATION_TOKEN)).thenReturn("ext.tok");

      String token = passwordRecoveryService.sendExternalRegisterEmail("admin",
                                                                       "jane@example.com",
                                                                       Locale.ENGLISH,
                                                                       null,
                                                                       new StringBuilder("https://acme.example/portal"),
                                                                       false,
                                                                       null);
      assertEquals("ext.tok", token);
      ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
      verify(mailService).sendMessage(sent.capture());
      assertTrue(sent.getValue().getBody(), sent.getValue().getBody().contains("24 hours"));
      assertFalse(sent.getValue().getBody(), sent.getValue().getBody().contains("${"));
      verify(onboardingTokenService, never()).createToken(anyString(), anyString());
    }
  }

  @Test
  public void testOnboardingTokenValidity() {
    assertEquals(SEVEN_DAYS, passwordRecoveryService.getOnboardingTokenValidity());
  }

  /**
   * Onboard tokens are verified and deleted through their own issuer, the
   * other types through the password reminder's
   */
  @Test
  public void testTokensAreRoutedByType() {
    PortalToken onboard = mock(PortalToken.class);
    when(onboard.isExpired()).thenReturn(false);
    when(onboard.getUsername()).thenReturn("jdoe");
    when(onboardingTokenService.getToken("tok.en", CookieTokenService.ONBOARD_TOKEN)).thenReturn(onboard);
    assertEquals("jdoe", passwordRecoveryService.verifyToken("tok.en", CookieTokenService.ONBOARD_TOKEN));
    verify(remindPasswordTokenService, never()).getToken(anyString(), anyString());

    assertNull(passwordRecoveryService.verifyToken("tok.en", CookieTokenService.FORGOT_PASSWORD_TOKEN));
    verify(remindPasswordTokenService).getToken("tok.en", CookieTokenService.FORGOT_PASSWORD_TOKEN);

    passwordRecoveryService.deleteToken("tok.en", CookieTokenService.ONBOARD_TOKEN);
    verify(onboardingTokenService).deleteToken("tok.en", CookieTokenService.ONBOARD_TOKEN);
    passwordRecoveryService.deleteToken("tok.en", CookieTokenService.FORGOT_PASSWORD_TOKEN);
    verify(remindPasswordTokenService).deleteToken("tok.en", CookieTokenService.FORGOT_PASSWORD_TOKEN);
  }

  @Test
  public void testRecoverEmailSetsTheLoginApartAndOffersAResetButton() {
    String body = passwordRecoveryService.buildRecoverEmailBody(user(), bundle, "https://acme.example/reset?token=t&lang=en");
    assertTrue(body, body.contains("Hi Jane,"));
    assertTrue(body, body.contains("Your login"));
    assertTrue(body, body.contains(">jdoe</strong>"));
    // no translation of the button yet: the English label stands in
    assertTrue(body, body.contains(">Reset password</a>"));
    assertTrue(body, body.contains("href=\"https://acme.example/reset?token=t&amp;lang=en\""));
    assertTrue(body, body.contains("Copy this address:"));
    assertFalse(body, body.contains("${"));
  }

  @Test
  public void testAccountCreatedEmailsCarryTheUsernameAndTheSignInLink() {
    String body = passwordRecoveryService.buildExternalConfirmationAccountEmailBody("Jane Doe",
                                                                                    "jdoe",
                                                                                    "https://acme.example/login",
                                                                                    bundle);
    assertTrue(body, body.contains("Your username is:"));
    assertTrue(body, body.contains(">jdoe</strong>"));
    assertTrue(body, body.contains(">Sign In</a>"));
    assertTrue(body, body.contains("Acme!"));
  }

  @Test
  public void testNamesAreEscapedInEveryEmail() {
    String evil = "<script>alert(1)</script>";
    User attacker = user();
    ((UserImpl) attacker).setDisplayName(evil);
    ((UserImpl) attacker).setFirstName(evil);
    String[] bodies = { passwordRecoveryService.buildOnboardingEmailBody(attacker, bundle, "https://acme.example/x", null, SEVEN_DAYS),
        passwordRecoveryService.buildRecoverEmailBody(attacker, bundle, "https://acme.example/x"),
        passwordRecoveryService.buildExternalEmailBody(evil, evil, "https://acme.example/x", bundle),
        passwordRecoveryService.buildExternalConfirmationAccountEmailBody(evil, evil, "https://acme.example/x", bundle) };
    for (String body : bodies) {
      assertFalse(body, body.contains("<script>"));
      assertTrue(body, body.contains("&lt;script&gt;"));
    }
  }

  @Test
  public void testGreetingHasNoDanglingSpaceWhenTheReaderIsUnknown() {
    String body = passwordRecoveryService.buildOnboardingEmailBody(null, bundle, "https://acme.example/x", null, SEVEN_DAYS);
    assertTrue(body, body.contains("Hello,"));
    assertFalse(body, body.contains("Hello ,"));
  }

  @Test
  public void testLanguageOfTheReaderIsOnTheDocument() throws Exception {
    try (MockedStatic<MailUtils> mailUtils = Mockito.mockStatic(MailUtils.class)) {
      mailUtils.when(MailUtils::getSenderName).thenReturn("Acme");
      mailUtils.when(MailUtils::getSenderEmail).thenReturn("noreply@acme.example");
      assertTrue(passwordRecoveryService.sendOnboardingEmail(user(), Locale.FRENCH, new StringBuilder("https://acme.example/portal")));
      ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
      verify(mailService).sendMessage(sent.capture());
      // the test bundle has no locale, so the document falls back to English
      assertTrue(sent.getValue().getBody(), sent.getValue().getBody().startsWith("<!doctype html><html lang=\"en\""));
    }
  }

  @Test
  public void testDocumentFollowsTheLocaleOfTheBundle() {
    ResourceBundle arabic = new ResourceBundle() {
      @Override
      protected Object handleGetObject(String key) {
        return bundle.containsKey(key) ? bundle.getObject(key) : null;
      }

      @Override
      public java.util.Enumeration<String> getKeys() {
        return bundle.getKeys();
      }

      @Override
      public Locale getLocale() {
        return Locale.forLanguageTag("ar");
      }
    };
    String body = passwordRecoveryService.buildRecoverEmailBody(user(), arabic, "https://acme.example/x");
    assertTrue(body, body.startsWith("<!doctype html><html lang=\"ar\" dir=\"rtl\""));
    ResourceBundle root = new ResourceBundle() {
      @Override
      protected Object handleGetObject(String key) {
        return bundle.containsKey(key) ? bundle.getObject(key) : null;
      }

      @Override
      public java.util.Enumeration<String> getKeys() {
        return bundle.getKeys();
      }

      @Override
      public Locale getLocale() {
        return Locale.ROOT;
      }
    };
    assertTrue(passwordRecoveryService.buildRecoverEmailBody(user(), root, "https://acme.example/x").startsWith("<!doctype html><html lang=\"en\""));
  }

  @Test
  public void testLeadInEndsWithAColonUnlessTheTranslationAlreadyHasPunctuation() {
    assertTrue(passwordRecoveryService.buildOnboardingEmailBody(user(), bundle, "https://acme.example/x", null, SEVEN_DAYS)
                                      .contains("Click the link:"));
    for (String done : new String[] { "Click the link.", "Click the link:", "Cliquez sur le lien\u00a0:", "\u0627\u0646\u0642\u0631 \u0647\u0646\u0627\u060c" }) {
      ResourceBundle custom = new ResourceBundle() {
        @Override
        protected Object handleGetObject(String key) {
          return "onboarding.email.accessLink".equals(key) ? done : null;
        }

        @Override
        public Enumeration<String> getKeys() {
          return new Vector<String>().elements();
        }
      };
      String body = passwordRecoveryService.buildOnboardingEmailBody(user(), custom, "https://acme.example/x", null, SEVEN_DAYS);
      assertTrue(body, body.contains(">" + done + "<"));
    }
  }

  private static User user() {
    UserImpl user = new UserImpl("jdoe");
    user.setFirstName("Jane");
    user.setLastName("Doe");
    user.setDisplayName("Jane Doe");
    user.setEmail("jane@example.com");
    user.setCreatedDate(new Date());
    return user;
  }

  private static int countOf(String text, String part) {
    int count = 0;
    int index = text.indexOf(part);
    while (index >= 0) {
      count++;
      index = text.indexOf(part, index + part.length());
    }
    return count;
  }

}
