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
package org.exoplatform.portal.rest;

import java.util.Locale;

/**
 * Administrator operations on user accounts, reached by the REST resources
 * of this module at call time through the container, since its
 * implementation lives in the web security module, above this one in the
 * module graph.
 */
public interface UserAccountService {

  /**
   * Name of the one-time-password method that confirms an administrator
   * session: a code sent by email, generated and validated for this purpose
   * only, so that a code issued for another flow never confirms a session
   */
  String OTP_METHOD                         = "userAccountEmail";

  /**
   * The creation source stamped on an account created by an administrator
   * from the users-management screen: such an account keeps the self-service
   * deactivation option
   */
  String CREATION_SOURCE_UI                 = "ui";

  /**
   * Code of an invalid role value, answered as 400
   */
  String ROLE_INVALID                       = "ROLE:INVALID";

  /**
   * Code of a language that is not a supported locale, answered as 400
   */
  String LANGUAGE_INVALID                   = "LANGUAGE:INVALID";

  /**
   * Code of a login already held by a live account, answered as 400
   */
  String USERNAME_ALREADY_EXISTS            = "USERNAME:ALREADY_EXISTS";

  /**
   * Code of a login once held by a deleted account, which is never created
   * again, answered as 400
   */
  String USERNAME_ALREADY_EXISTS_AS_DELETED = "USERNAME:ALREADY_EXISTS_AS_DELETED";

  /**
   * Code answered as 400 when the username policy of the platform refuses the
   * form of a generated login, so that the administrator types one instead
   */
  String USERNAME_GENERATION_NOT_ALLOWED    = "USERNAME:GENERATION_NOT_ALLOWED";

  /**
   * Code of an email already held by an account, answered as 400
   */
  String EMAIL_ALREADY_EXISTS               = "EMAIL:ALREADY_EXISTS";

  /**
   * Code answered as 500 when nothing was created: the store refused the
   * insert, or the read-back found no account of this request
   */
  String USER_CREATION_FAILED               = "USER:CREATION_FAILED";

  /**
   * Code answered as 500 when every generated login collided and nothing was
   * created
   */
  String USER_USERNAME_GENERATION_EXHAUSTED = "USER:USERNAME_GENERATION_EXHAUSTED";

  /**
   * Prefix of the code answered as 500 when the account was created but some
   * of its parts were not stored, and the account was deactivated:
   * {@code USER:CREATED_INCOMPLETE:<username>:<parts>}
   */
  String USER_CREATED_INCOMPLETE            = "USER:CREATED_INCOMPLETE";

  /**
   * Prefix of the code answered as 500 when the account was created, some of
   * its parts were not stored, and the deactivation did not hold either:
   * {@code USER:CREATED_INCOMPLETE_ENABLED:<username>:<parts>}
   */
  String USER_CREATED_INCOMPLETE_ENABLED    = "USER:CREATED_INCOMPLETE_ENABLED";

  /**
   * Code answered as 500 when the read-back after an update found a field
   * other than requested: the names, the email, the enabled flag, or the
   * language when the update carried one
   */
  String USER_UPDATE_INCOMPLETE             = "USER:UPDATE_INCOMPLETE";

  /**
   * Code answered as 500 when the new password of an internal-store account
   * does not authenticate after its change; nothing else is written
   */
  String USER_PASSWORD_NOT_STORED           = "USER:PASSWORD_NOT_STORED";

  /**
   * Code answered as 500 when the new password of a deactivated internal-store
   * account cannot be checked by the user handler in place; nothing else is
   * written
   */
  String USER_PASSWORD_NOT_VERIFIABLE       = "USER:PASSWORD_NOT_VERIFIABLE";

  /**
   * Code answered as 400 when an administrator deactivates their own account
   */
  String SELF_DISABLE                       = "SelfDisable";

  /**
   * Code answered as 400 when the super user's account is deactivated
   */
  String DISABLE_SUPER_USER                 = "DisableSuperUser";

  /**
   * The parts a read-back reports missing, in this order
   */
  String PART_MEMBERSHIPS                   = "MEMBERSHIPS";

  String PART_LANGUAGE                      = "LANGUAGE";

  String PART_ENABLED                       = "ENABLED";

  /**
   * Validates and consumes a one-time-password code sent to the user by the
   * {@link #OTP_METHOD} method. A code validates once: a second validation of
   * the same code, even within its validity window, is refused.
   *
   * @param username user who received the code
   * @param otpCode code typed by the user
   * @throws IllegalAccessException when the code is blank, wrong, expired,
   *           already consumed, or when the user exceeded the allowed
   *           attempts
   */
  void confirmSession(String username, String otpCode) throws IllegalAccessException;

  /**
   * Creates an account as an administrator described it: the login is the
   * one given, validated, or a generated one; the password is optional, and a
   * blank one leaves the account with no credential until the user sets one
   * from the onboarding email; the role decides the memberships; the
   * language, when given, is stored as the user's preferred language. Once
   * written, the persisted state is read back in a new transaction, and an
   * account whose parts were not all stored is deactivated.
   *
   * @param account the account to create
   * @param locale the locale of the validation messages
   * @return the login of the created account, generated or given
   * @throws IllegalArgumentException with the code of the refused field as
   *           message: {@code USERNAME:}, {@code PASSWORD:},
   *           {@code FIRSTNAME:}, {@code LASTNAME:} or {@code EMAIL:}
   *           followed by the validation message, or one of
   *           {@link #USERNAME_ALREADY_EXISTS},
   *           {@link #USERNAME_ALREADY_EXISTS_AS_DELETED},
   *           {@link #USERNAME_GENERATION_NOT_ALLOWED},
   *           {@link #EMAIL_ALREADY_EXISTS}, {@link #ROLE_INVALID},
   *           {@link #LANGUAGE_INVALID}
   * @throws UserAccountCreationException when the store did not complete the
   *           creation as requested, with the code as message
   */
  String createUser(UserAccount account, Locale locale) throws UserAccountCreationException;

  /**
   * Updates an existing account as an administrator edited it: names, email,
   * enabled flag, a password when given and a language when given. Every
   * refusal is checked before the first write. The password is changed
   * first, through the active change-password connector, and an
   * internal-store account's new password is then proven to authenticate.
   * The other writes follow; they are then read back in a new transaction.
   *
   * @param account the account as edited, identified by its login
   * @param locale the locale of the validation messages
   * @param modifier the login of the administrator who edits it
   * @throws IllegalArgumentException with the code of the refused field as
   *           message: {@code PASSWORD:}, {@code FIRSTNAME:},
   *           {@code LASTNAME:} or {@code EMAIL:} followed by the validation
   *           message, or one of {@link #EMAIL_ALREADY_EXISTS},
   *           {@link #LANGUAGE_INVALID}, {@link #SELF_DISABLE},
   *           {@link #DISABLE_SUPER_USER}, {@code USER:EMPTY} or
   *           {@code USER:NOT_FOUND}; nothing is written then
   * @throws UserAccountUpdateException when the store did not complete the
   *           update as requested, with the code as message
   * @throws IllegalStateException with no code when the connector refuses the
   *           password (an external store's account) or a store read before
   *           any write fails; nothing is written then
   */
  void updateUser(UserAccount account, Locale locale, String modifier) throws UserAccountUpdateException;

  /**
   * Reads the preferred language stored for an account
   *
   * @param username the login of the account
   * @return the preferred language stored for the account, which may no
   *         longer be a supported locale; null when none is stored
   */
  String getLanguage(String username);

  /**
   * @return a candidate login that is not guessable from the user's name,
   *         email or creation order: a letter followed by eleven characters
   *         drawn at random from lowercase letters and digits
   */
  String generateUsername();

}
