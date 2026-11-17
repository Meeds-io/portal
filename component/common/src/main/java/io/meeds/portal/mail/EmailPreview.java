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
package io.meeds.portal.mail;

import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * A sample of an email of the platform, with the saved branding,
 * for the preview of the branding page. The two
 * samples are the account emails that use the shared layout for real, with the
 * labels of the same bundle and the same structure as the email a user
 * receives, so that what the administrator sees is what is sent.
 */
public final class EmailPreview {

  /** The invitation to join the platform */
  public static final String       INVITATION       = "invitation";

  /** The email that lets a user choose a new password */
  public static final String       PASSWORD_RESET   = "reset";

  public static final List<String> TEMPLATES        = List.of(INVITATION, PASSWORD_RESET);

  private static final String      SAMPLE_VALIDITY  = "7";

  private static final String      SAMPLE_TOKEN     = "sample-token";

  private static final Pattern     SITE_URL         = Pattern.compile("https?://[^\\s\"'<>]+");

  private EmailPreview() {
  }

  public static boolean isTemplate(String template) {
    return template != null && TEMPLATES.contains(template);
  }

  /**
   * @param template one of {@link #TEMPLATES}
   * @param brand the branding to show, the saved one
   * @param bundle the labels of the language of the preview
   * @param lang the language tag of the document
   * @param siteUrl the address of the site, which the sample link points to;
   *          anything that is not an http or https address is replaced by an
   *          example address
   * @return the html of the sample email
   * @throws IllegalArgumentException when the template is not known
   */
  public static String render(String template, EmailBrand brand, ResourceBundle bundle, String lang, String siteUrl) {
    return render(template, brand, bundle, lang, siteUrl, null);
  }

  /**
   * Same as
   * {@link #render(String, EmailBrand, ResourceBundle, String, String)} with
   * the person who reads the preview, so that the greeting is the one the
   * administrator would get; a blank value is replaced by a sample one
   *
   * @param reader the reader of the preview, or null for sample names
   */
  public static String render(String template,
                              EmailBrand brand,
                              ResourceBundle bundle,
                              String lang,
                              String siteUrl,
                              Reader reader) {
    // the connected user: nothing is made up, a name that is not known leaves the greeting without one
    String full = clean(reader == null ? null : reader.fullName());
    String first = clean(reader == null ? null : reader.firstName());
    if (first.isEmpty()) {
      first = full;
    }
    String user = clean(reader == null ? null : reader.login());
    String site = siteUrl != null && SITE_URL.matcher(siteUrl.trim()).matches() ? withoutTrailingSlashes(siteUrl.trim())
                                                                                 : "https://example.com";
    // the links of the real emails: the address of the site, the page and a token that is not valid
    String langParam = "lang=" + (lang == null || lang.isBlank() ? "en" : lang.trim());
    EmailModel.Builder email = EmailModel.builder()
                                         .lang(lang)
                                         .fallbackLabel(text(bundle, "email.button.fallback", "If the button does not work, copy this address into your browser:"));
    switch (template == null ? "" : template) {
    case INVITATION -> email.title(text(bundle, "onboarding.email.header").trim() + " " + brand.getCompanyName())
                            .paragraph(EmailLayout.greeting(text(bundle, "onboarding.email.hello"), full))
                            .paragraph(text(bundle, "onboarding.email.welcome").trim() + " " + brand.getCompanyName())
                            .lead(EmailLayout.leadIn(text(bundle, "onboarding.email.accessLink")))
                            .button(text(bundle, "onboarding.email.join"), site + "/on-boarding?" + langParam + "&token=" + SAMPLE_TOKEN)
                            .note(text(bundle, "onboarding.email.linkValidity") + " "
                                + text(bundle, "onboarding.email.validity.days", "{0} days").replace("{0}", SAMPLE_VALIDITY))
                            .signoff(text(bundle, "onboarding.email.bye"));
    case PASSWORD_RESET -> email.title(text(bundle, "gatein.forgotPassword.email.header"))
                                .paragraph(EmailLayout.greeting(text(bundle, "gatein.forgotPassword.email.hi"), first))
                                .paragraph(text(bundle, "gatein.forgotPassword.email.message"))
                                .lead(text(bundle, "gatein.forgotPassword.email.linkDescription"))
                                .button(text(bundle, "gatein.forgotPassword.email.button", "Reset password"), site + "/forgot-password?token=" + SAMPLE_TOKEN + "&" + langParam)
                                .aside(text(bundle, "gatein.forgotPassword.email.helpMessage"));
    default -> throw new IllegalArgumentException("Unknown email template: " + template);
    }
    if (PASSWORD_RESET.equals(template) && !user.isEmpty()) {
      email.fact(text(bundle, "gatein.forgotPassword.email.username"), user);
    }
    return EmailLayout.render(email.build(), brand);
  }

  /**
   * The person who reads the preview.
   *
   * @param fullName the full name, for the invitation
   * @param firstName the first name, for the password reset
   * @param login the user name, for the password reset
   */
  public record Reader(String fullName, String firstName, String login) {
  }

  private static String withoutTrailingSlashes(String address) {
    int end = address.length();
    while (end > 0 && address.charAt(end - 1) == '/') {
      end--;
    }
    return address.substring(0, end);
  }

  private static String clean(String value) {
    return value == null ? "" : value.trim();
  }

  private static String text(ResourceBundle bundle, String key) {
    return text(bundle, key, key);
  }

  private static String text(ResourceBundle bundle, String key, String defaultValue) {
    try {
      return bundle == null ? defaultValue : bundle.getString(key);
    } catch (MissingResourceException _) {
      return defaultValue;
    }
  }

}
