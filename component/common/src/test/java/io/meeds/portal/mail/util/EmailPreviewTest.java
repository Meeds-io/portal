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
package io.meeds.portal.mail.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Enumeration;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;

import org.junit.Test;

import io.meeds.portal.mail.model.EmailBrand;
import io.meeds.portal.mail.model.EmailPreviewReader;

public class EmailPreviewTest {

  private static final Map<String, String> LABELS = Map.ofEntries(Map.entry("onboarding.email.header", "Create your account and join "),
                                                                 Map.entry("onboarding.email.hello", "Hello"),
                                                                 Map.entry("onboarding.email.welcome", "You have been invited to join "),
                                                                 Map.entry("onboarding.email.accessLink", "To create your account, click the link"),
                                                                 Map.entry("onboarding.email.join", "Join"),
                                                                 Map.entry("onboarding.email.linkValidity", "This link will expire in"),
                                                                 Map.entry("onboarding.email.validity.days", "{0} days"),
                                                                 Map.entry("onboarding.email.bye", "See you soon."),
                                                                 Map.entry("gatein.forgotPassword.email.header", "Password Reset"),
                                                                 Map.entry("gatein.forgotPassword.email.hi", "Hi"),
                                                                 Map.entry("gatein.forgotPassword.email.message", "We received a request to reset your password."),
                                                                 Map.entry("gatein.forgotPassword.email.username", "Your login"),
                                                                 Map.entry("gatein.forgotPassword.email.linkDescription", "Click the link below."),
                                                                 Map.entry("gatein.forgotPassword.email.helpMessage", "If it was not you, ignore this email."));

  private static final ResourceBundle      BUNDLE = new ResourceBundle() {
                                                    @Override
                                                    protected Object handleGetObject(String key) {
                                                      return LABELS.get(key);
                                                    }

                                                    @Override
                                                    public Enumeration<String> getKeys() {
                                                      return new Vector<>(LABELS.keySet()).elements();
                                                    }
                                                  };

  private static final EmailBrand          BRAND  = new EmailBrand("Acme Corp", "#3f8487", "Inter", "12px");

  @Test
  public void testInvitationIsTheEmailOfTheLayoutWithTheTypedBranding() {
    String html = EmailPreview.render(EmailPreview.INVITATION, BRAND, BUNDLE, "en", "https://acme.example/portal",
                                      new EmailPreviewReader("Amal Haddad", "Amal", "ahaddad"));
    assertTrue(html, html.contains("<h1 "));
    assertTrue(html, html.contains("Create your account and join Acme Corp</h1>"));
    assertTrue(html, html.contains("Hello Amal Haddad,"));
    assertTrue("the lead ends with a colon", html.contains("To create your account, click the link:"));
    assertTrue(html, html.contains(">Join</a>"));
    assertTrue("the link points to the site", html.contains("href=\"https://acme.example/portal/on-boarding?lang=en&amp;token=sample-token\""));
    assertTrue("the validity is written", html.contains("This link will expire in 7 days"));
    assertTrue("the primary color, nudged to be readable", html.contains("background-color:" + BRAND.getFillColor()));
    assertTrue("the font of the branding", html.contains("font:15px/1.7 'Inter',Arial,Helvetica,sans-serif"));
    assertTrue("the radius", html.contains("border-radius:12px"));
  }

  @Test
  public void testGreetingIsTheOneOfTheReader() {
    String invitation = EmailPreview.render(EmailPreview.INVITATION, BRAND, BUNDLE, "en", null, new EmailPreviewReader("Amal <b>Haddad</b>", "Amal", "ahaddad"));
    assertTrue(invitation, invitation.contains("Hello Amal &lt;b&gt;Haddad&lt;/b&gt;,"));
    assertFalse(invitation, invitation.contains("Sam"));
    String reset = EmailPreview.render(EmailPreview.PASSWORD_RESET, BRAND, BUNDLE, "en", null, new EmailPreviewReader("Amal Haddad", "Amal", "ahaddad"));
    assertTrue(reset, reset.contains("Hi Amal,"));
    assertTrue(reset, reset.contains("ahaddad"));
    assertFalse(reset, reset.contains("srivera"));
  }

  @Test
  public void testNoReaderLeavesTheGreetingWithoutAName() {
    String reset = EmailPreview.render(EmailPreview.PASSWORD_RESET, BRAND, BUNDLE, "en", null, new EmailPreviewReader(" ", null, ""));
    assertTrue(reset, reset.contains("Hi,"));
    assertFalse("no login is made up", reset.contains("Your login"));
    assertTrue(EmailPreview.render(EmailPreview.INVITATION, BRAND, BUNDLE, "en", null, null).contains("Hello,"));
    String onlyFullName = EmailPreview.render(EmailPreview.PASSWORD_RESET, BRAND, BUNDLE, "en", null, new EmailPreviewReader("Amal", null, null));
    assertTrue("a reader known by one name is greeted by it", onlyFullName.contains("Hi Amal,"));
  }

  @Test
  public void testPasswordResetHasTheLoginApartAndTheResetButton() {
    String html = EmailPreview.render(EmailPreview.PASSWORD_RESET, BRAND, BUNDLE, "fr", "https://acme.example",
                                      new EmailPreviewReader("Amal Haddad", "Amal", "ahaddad"));
    assertTrue(html, html.contains("<html lang=\"fr\""));
    assertTrue(html, html.contains("Password Reset</h1>"));
    assertTrue(html, html.contains("Hi Amal,"));
    assertTrue(html, html.contains("ahaddad"));
    assertTrue("the English label stands in for a missing translation", html.contains(">Reset password</a>"));
    assertTrue(html, html.contains("href=\"https://acme.example/forgot-password?token=sample-token&amp;lang=fr\""));
  }

  @Test
  public void testRightToLeftLanguageIsMirrored() {
    String html = EmailPreview.render(EmailPreview.INVITATION, BRAND, BUNDLE, "ar", "https://acme.example");
    assertTrue(html, html.contains("<html lang=\"ar\" dir=\"rtl\""));
  }

  @Test
  public void testCompanyNameHoldingMarkupIsEscaped() {
    String html = EmailPreview.render(EmailPreview.INVITATION, new EmailBrand("<script>alert(1)</script>", "#476a9c"), BUNDLE, "en", null);
    assertFalse(html, html.contains("<script>alert(1)"));
    assertTrue(html, html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
  }

  @Test
  public void testSiteAddressThatIsNotHttpIsReplacedByAnExample() {
    for (String bad : new String[] { "javascript:alert(1)", "ftp://a.example", "https://a.example/\" onclick=\"x", "", null }) {
      String html = EmailPreview.render(EmailPreview.PASSWORD_RESET, BRAND, BUNDLE, "en", bad);
      assertTrue(String.valueOf(bad), html.contains("href=\"https://example.com/forgot-password?token=sample-token&amp;lang=en\""));
    }
    assertTrue(EmailPreview.render(EmailPreview.PASSWORD_RESET, BRAND, BUNDLE, "en", "http://localhost:8080/portal/")
                           .contains("href=\"http://localhost:8080/portal/forgot-password?token=sample-token&amp;lang=en\""));
  }

  @Test
  public void testMissingBundleShowsTheKeysInsteadOfFailing() {
    String html = EmailPreview.render(EmailPreview.INVITATION, BRAND, null, "en", null);
    assertTrue(html, html.contains("onboarding.email.header"));
  }

  @Test
  public void testUnknownTemplateIsRefused() {
    assertThrows(IllegalArgumentException.class, () -> EmailPreview.render("nope", BRAND, BUNDLE, "en", null));
    assertThrows(IllegalArgumentException.class, () -> EmailPreview.render(null, BRAND, BUNDLE, "en", null));
    assertTrue(EmailPreview.isTemplate("invitation"));
    assertTrue(EmailPreview.isTemplate("reset"));
    assertFalse(EmailPreview.isTemplate("OTP"));
    assertFalse("a missing template is not one", EmailPreview.isTemplate(null));
    assertEquals(2, EmailPreview.TEMPLATES.size());
  }

}
