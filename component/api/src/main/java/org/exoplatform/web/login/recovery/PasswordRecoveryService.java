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

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import org.exoplatform.services.organization.User;

public interface PasswordRecoveryService {
  void addConnector(ChangePasswordConnector connector);

  String verifyToken(String tokenId, String type);

  String verifyToken(String tokenId);

  boolean changePass(final String tokenId, final String tokenType, final String username, final String password);

  public boolean sendRecoverPasswordEmail(User user, Locale defaultLocale, HttpServletRequest req);

  public boolean sendOnboardingEmail(User user, Locale defaultLocale, StringBuilder url);

  /**
   * Sends the onboarding email with a message written by the administrator,
   * shown above the link, and the validity of that link
   *
   * @param user the invited user
   * @param locale the locale of the email
   * @param url the base URL of the platform
   * @param message the administrator's message, plain text, blank for none
   * @return true when the email was sent
   */
  default boolean sendOnboardingEmail(User user, Locale locale, StringBuilder url, String message) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return the validity of an onboarding link, in milliseconds, as
   *         configured on the platform
   */
  default long getOnboardingTokenValidity() {
    throw new UnsupportedOperationException();
  }

  public String sendExternalRegisterEmail(String sender,
                                          String email,
                                          Locale locale,
                                          String space,
                                          StringBuilder url) throws Exception; // NOSONAR

  /**
   * Send An Onboarding email to registered user on the platform. The
   * registration can be made through space invitation or through a register
   * form, thus, the space and sender invitations can be null.
   * 
   * @param  sender          Username who sent the invitation from space
   * @param  email           the invited email
   * @param  locale          the user {@link Locale}
   * @param  space           the space from which the user was invited
   * @param  url             the base url of the current installation
   * @param  spaceInvitation whether this is a space invitation or not
   * @return                 generated token for external user registration
   * @throws Exception       when IDM or any other exception occurs while
   *                           generating token or sending email
   */
  default String sendExternalRegisterEmail(String sender,
                                          String email,
                                          Locale locale,
                                          String space,
                                          StringBuilder url,
                                          boolean spaceInvitation) throws Exception {// NOSONAR
    throw new UnsupportedOperationException();
  }

  public boolean sendAccountCreatedConfirmationEmail(String sender, Locale locale, StringBuilder url);

  public boolean allowChangePassword(String username) throws Exception; // NOSONAR

  String getPasswordRecoverURL(String tokenId, String lang);

  String getOnboardingURL(String tokenId, String lang);

  String getExternalRegistrationURL(String tokenId, String lang);

  public ChangePasswordConnector getActiveChangePasswordConnector();

  /**
   * Remove used Token
   * 
   * @param tokenId
   * @param type
   */
  void deleteToken(String tokenId, String type);

  /**
   * Sends verification email to user to continue registration
   * 
   * @param data
   * @param username
   * @param firstName
   * @param lastName
   * @param email
   * @param locale
   * @param url
   * @return true if sent, else false
   */
  boolean sendAccountVerificationEmail(String data, String username, String firstName, String lastName, String email, Locale locale, StringBuilder url);

  default String sendExternalRegisterEmail(String sender,
                                           String email,
                                           Locale locale,
                                           String space,
                                           StringBuilder url,
                                           boolean spaceInvitation,
                                           String initialUri) throws Exception {
    throw new UnsupportedOperationException();
  }
}
