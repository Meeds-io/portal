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
package io.meeds.web.security.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.Constants;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.rest.UserAccount;
import org.exoplatform.portal.rest.UserAccountCreationException;
import org.exoplatform.portal.rest.UserAccountRole;
import org.exoplatform.portal.rest.UserAccountService;
import org.exoplatform.portal.rest.UserAccountUpdateException;
import org.exoplatform.portal.rest.UserFieldValidator;
import org.exoplatform.portal.rest.UserRestResourcesV1;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.organization.idm.UserDAOImpl;
import org.exoplatform.services.organization.idm.cache.CacheableMembershipHandlerImpl;
import org.exoplatform.services.organization.idm.cache.CacheableUserHandlerImpl;
import org.exoplatform.services.organization.idm.cache.CacheableUserProfileHandlerImpl;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;

@Service
public class UserAccountServiceImpl implements UserAccountService {

  public static final String    PLATFORM_USERS_GROUP             = "/platform/users";

  public static final String    PLATFORM_EXTERNALS_GROUP         = "/platform/externals";

  public static final String    PLATFORM_ADMINISTRATORS_GROUP    = "/platform/administrators";

  public static final String    MEMBER_MEMBERSHIP_TYPE           = "member";

  /**
   * The letter every generated login starts with, so that it never starts
   * with a digit whatever the username policy
   */
  public static final String    GENERATED_USERNAME_PREFIX        = "u";

  public static final int       GENERATED_USERNAME_RANDOM_LENGTH = 11;

  public static final int       GENERATION_ATTEMPTS              = 5;

  /**
   * The message of the store's refusal of a login it already holds, raised
   * from the insert of a concurrent creation that the pre-check did not see
   */
  public static final String    IDENTITY_ALREADY_PRESENT_MESSAGE = "IdentityObject already present";

  private static final String   GENERATED_USERNAME_ALPHABET      = "abcdefghijklmnopqrstuvwxyz0123456789";

  private static final Log      LOG                              = ExoLogger.getLogger(UserAccountServiceImpl.class);

  private static final String   USERNAME_PREFIX                  = "USERNAME:";

  private static final String   PASSWORD_PREFIX                  = "PASSWORD:";

  private static final String   FIRSTNAME_PREFIX                 = "FIRSTNAME:";

  private static final String   LASTNAME_PREFIX                  = "LASTNAME:";

  private static final String   EMAIL_PREFIX                     = "EMAIL:";

  private final SecureRandom    random                           = new SecureRandom();

  /**
   * The username policy of the platform, the one a typed login meets: a
   * generated login is validated by the same rule
   */
  private UserFieldValidator    usernameValidator                = UserRestResourcesV1.USERNAME_VALIDATOR;

  @Autowired
  private OtpService            otpService;

  @Autowired
  private OrganizationService   organizationService;

  @Autowired
  private UserACL               userACL;

  @Autowired
  private LocaleConfigService   localeConfigService;

  /**
   * Looked up when first needed: a Kernel service, read through the
   * container rather than injected at boot
   */
  private PasswordRecoveryService passwordRecoveryService;

  @Override
  public void confirmSession(String username, String otpCode) throws IllegalAccessException {
    otpService.validateAndConsumeOtp(username, OTP_METHOD, otpCode);
  }

  @Override
  public String createUser(UserAccount account, Locale locale) throws UserAccountCreationException {
    if (account == null) {
      throw new IllegalArgumentException("USER:EMPTY");
    }
    Locale validationLocale = locale == null ? Locale.ENGLISH : locale;
    UserAccountRole role = UserAccountRole.parse(account.getRole());
    String language = validateLanguage(account.getLanguage());
    String givenUsername = StringUtils.trimToNull(account.getUserName());
    if (givenUsername != null) {
      validate(usernameValidator, USERNAME_PREFIX, validationLocale, givenUsername);
    }
    // The password is kept as typed, spaces included: an empty one means no
    // credential, a blank one is refused by the validator
    String password = StringUtils.isEmpty(account.getPassword()) ? null : account.getPassword();
    if (password != null) {
      validate(UserRestResourcesV1.PASSWORD_VALIDATOR, PASSWORD_PREFIX, validationLocale, password);
    }
    validate(UserRestResourcesV1.LASTNAME_VALIDATOR, LASTNAME_PREFIX, validationLocale, account.getLastName());
    validate(UserRestResourcesV1.FIRSTNAME_VALIDATOR, FIRSTNAME_PREFIX, validationLocale, account.getFirstName());
    validate(UserRestResourcesV1.EMAIL_VALIDATOR, EMAIL_PREFIX, validationLocale, account.getEmail());
    try {
      if (givenUsername != null && findUser(givenUsername) != null) {
        throw new IllegalArgumentException(USERNAME_ALREADY_EXISTS);
      }
      if (isEmailUsed(account.getEmail())) {
        throw new IllegalArgumentException(EMAIL_ALREADY_EXISTS);
      }
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new UserAccountCreationException(USER_CREATION_FAILED, e);
    }

    User user = givenUsername == null ? insertGenerated(account, password, validationLocale)
                                      : insertGiven(givenUsername, account, password);
    String username = user.getUserName();
    applyLanguage(username, language);
    applyRole(user, role);
    applyEnabled(username, account.isEnabled());
    readBack(user, role, language, account.isEnabled());
    return username;
  }

  @Override
  public void updateUser(UserAccount account, Locale locale, String modifier) throws UserAccountUpdateException {
    if (account == null || StringUtils.isBlank(account.getUserName())) {
      throw new IllegalArgumentException("USER:EMPTY");
    }
    Locale validationLocale = locale == null ? Locale.ENGLISH : locale;
    String username = account.getUserName();
    String password = StringUtils.isEmpty(account.getPassword()) ? null : account.getPassword();
    if (password != null) {
      validate(UserRestResourcesV1.PASSWORD_VALIDATOR, PASSWORD_PREFIX, validationLocale, password);
    }
    validate(UserRestResourcesV1.LASTNAME_VALIDATOR, LASTNAME_PREFIX, validationLocale, account.getLastName());
    validate(UserRestResourcesV1.FIRSTNAME_VALIDATOR, FIRSTNAME_PREFIX, validationLocale, account.getFirstName());
    validate(UserRestResourcesV1.EMAIL_VALIDATOR, EMAIL_PREFIX, validationLocale, account.getEmail());
    User user = readUser(username);
    if (user == null) {
      throw new IllegalArgumentException("USER:NOT_FOUND");
    }
    if (!StringUtils.equals(user.getEmail(), account.getEmail()) && isEmailUsedQuietly(account.getEmail())) {
      throw new IllegalArgumentException(EMAIL_ALREADY_EXISTS);
    }
    String language = checkLanguage(username, account.getLanguage());
    boolean enabled = account.isEnabled();
    if (user.isEnabled() && !enabled) {
      if (StringUtils.equals(modifier, username)) {
        throw new IllegalArgumentException(SELF_DISABLE);
      }
      if (StringUtils.equals(userACL.getSuperUser(), username)) {
        throw new IllegalArgumentException(DISABLE_SUPER_USER);
      }
    }
    // The password first: a connector refusal throws and nothing else is
    // written; a stored one is proven before the other writes
    if (password != null) {
      try {
        passwordRecoveryService().getActiveChangePasswordConnector().changePassword(username, password);
      } catch (Exception e) {
        throw new IllegalStateException("The password of " + username + " could not be changed", e);
      }
      verifyPassword(username, password);
    }
    writeFields(username, account);
    writeEnabled(username, user.isEnabled(), enabled);
    if (language != null) {
      try {
        updateLanguage(username, language);
      } catch (IllegalStateException e) {
        LOG.warn("The language {} of the updated account {} could not be stored", language, username, e);
      }
    }
    readBackUpdate(account, language);
  }

  @Override
  public String generateUsername() {
    StringBuilder username = new StringBuilder(GENERATED_USERNAME_PREFIX);
    for (int i = 0; i < GENERATED_USERNAME_RANDOM_LENGTH; i++) {
      username.append(GENERATED_USERNAME_ALPHABET.charAt(random.nextInt(GENERATED_USERNAME_ALPHABET.length())));
    }
    return username.toString();
  }

  private User insertGiven(String username, UserAccount account, String password) throws UserAccountCreationException {
    try {
      return insert(username, account, password);
    } catch (ObjectAlreadyExistsException e) {
      throw new IllegalArgumentException(USERNAME_ALREADY_EXISTS_AS_DELETED, e);
    } catch (UsernameRefusedException e) {
      throw new IllegalArgumentException(USERNAME_ALREADY_EXISTS, e);
    }
  }

  private User insertGenerated(UserAccount account, String password, Locale locale) throws UserAccountCreationException {
    for (int attempt = 1; attempt <= GENERATION_ATTEMPTS; attempt++) {
      String candidate = generateUsername();
      if (attempt == 1) {
        // A policy that refuses the generated form refuses every candidate:
        // the administrator is told to type a login instead
        String refusal = usernameValidator.validate(locale, candidate);
        if (StringUtils.isNotBlank(refusal)) {
          LOG.warn("The username policy (gatein.validators.userName.*) refuses the generated login form: {}", refusal);
          throw new IllegalArgumentException(USERNAME_GENERATION_NOT_ALLOWED);
        }
      }
      try {
        if (findUser(candidate) != null) {
          continue;
        }
        return insert(candidate, account, password);
      } catch (ObjectAlreadyExistsException | UsernameRefusedException e) {
        LOG.debug("The generated login {} is already held, another one is drawn", candidate, e);
      } catch (UserAccountCreationException e) {
        throw e;
      } catch (Exception e) {
        throw new UserAccountCreationException(USER_CREATION_FAILED, e);
      }
    }
    LOG.error("No free login among {} generated candidates, the account {} was not created",
              GENERATION_ATTEMPTS,
              account.getEmail());
    throw new UserAccountCreationException(USER_USERNAME_GENERATION_EXHAUSTED);
  }

  /**
   * Inserts the account under the given login
   *
   * @throws ObjectAlreadyExistsException when the login belonged to a deleted
   *           account, which is never created again
   * @throws UsernameRefusedException when the store already holds the login
   * @throws UserAccountCreationException on any other store failure: nothing
   *           was written
   */
  private User insert(String username, UserAccount account, String password) throws UserAccountCreationException,
                                                                             ObjectAlreadyExistsException,
                                                                             UsernameRefusedException {
    UserHandler userHandler = organizationService.getUserHandler();
    User user = userHandler.createUserInstance(username);
    user.setEmail(StringUtils.trim(account.getEmail()));
    user.setFirstName(StringUtils.trim(account.getFirstName()));
    user.setLastName(StringUtils.trim(account.getLastName()));
    if (password != null) {
      user.setPassword(password);
    }
    user.setCreationSource(CREATION_SOURCE_UI);
    try {
      userHandler.createUser(user, true);
      return user;
    } catch (Exception e) {
      // The IDM wraps the exceptions of its listeners and of the store: the
      // refusal is looked up in the cause chain
      int vetoIndex = ExceptionUtils.indexOfType(e, ObjectAlreadyExistsException.class);
      if (vetoIndex >= 0) {
        throw (ObjectAlreadyExistsException) ExceptionUtils.getThrowableList(e).get(vetoIndex);
      }
      if (isAlreadyPresent(e)) {
        throw new UsernameRefusedException(username, e);
      }
      throw new UserAccountCreationException(USER_CREATION_FAILED, e);
    }
  }

  @Override
  public String getLanguage(String username) {
    try {
      UserProfile profile = organizationService.getUserProfileHandler().findUserProfileByName(username);
      return profile == null ? null : StringUtils.trimToNull(profile.getAttribute(Constants.USER_LANGUAGE));
    } catch (Exception e) {
      throw new IllegalStateException("The preferred language of " + username + " could not be read", e);
    }
  }

  @Override
  public String checkLanguage(String username, String language) {
    String value = StringUtils.trimToNull(language);
    if (value == null || StringUtils.equals(value, readableLanguage(username))) {
      return null;
    }
    return validateLanguage(value);
  }

  /**
   * The stored language, null when it cannot be read: a language then
   * stands as asked, checked against the supported locales
   */
  private String readableLanguage(String username) {
    try {
      return getLanguage(username);
    } catch (IllegalStateException e) {
      LOG.warn("The preferred language of {} could not be read, the one asked is checked as new", username, e);
      return null;
    }
  }

  @Override
  public void updateLanguage(String username, String language) {
    String value = checkLanguage(username, language);
    if (value == null) {
      return;
    }
    try {
      UserProfileHandler profileHandler = organizationService.getUserProfileHandler();
      UserProfile profile = profileHandler.findUserProfileByName(username);
      if (profile == null) {
        profile = profileHandler.createUserProfileInstance(username);
      }
      profile.setAttribute(Constants.USER_LANGUAGE, value);
      profileHandler.saveUserProfile(profile, true);
    } catch (Exception e) {
      throw new IllegalStateException("The preferred language of " + username + " could not be stored", e);
    }
  }

  private void applyLanguage(String username, String language) {
    if (language == null) {
      return;
    }
    try {
      UserProfileHandler profileHandler = organizationService.getUserProfileHandler();
      UserProfile profile = profileHandler.findUserProfileByName(username);
      if (profile == null) {
        profile = profileHandler.createUserProfileInstance(username);
      }
      profile.setAttribute(Constants.USER_LANGUAGE, language);
      profileHandler.saveUserProfile(profile, true);
    } catch (Exception e) {
      LOG.warn("The language {} of the created account {} could not be stored", language, username, e);
    }
  }

  private void applyRole(User user, UserAccountRole role) {
    String username = user.getUserName();
    try {
      switch (role) {
      case EXTERNAL -> {
        link(user, PLATFORM_EXTERNALS_GROUP, MEMBER_MEMBERSHIP_TYPE);
        unlink(username, PLATFORM_USERS_GROUP, MEMBER_MEMBERSHIP_TYPE);
      }
      case ADMINISTRATOR -> link(user, PLATFORM_ADMINISTRATORS_GROUP, userACL.getAdminMSType());
      default -> {
        // The platform users membership is granted at creation
      }
      }
    } catch (Exception e) {
      LOG.warn("The role {} of the created account {} could not be stored", role, username, e);
    }
  }

  private void applyEnabled(String username, boolean enabled) {
    if (enabled) {
      return;
    }
    try {
      organizationService.getUserHandler().setEnabled(username, false, true);
    } catch (Exception e) {
      LOG.warn("The created account {} could not be deactivated as requested", username, e);
    }
  }

  /**
   * Reads the persisted state back in a new transaction: the store commits
   * around every listener call, so what the request wrote is only known once
   * the caches are evicted and the account is read again
   */
  private void readBack(User created, UserAccountRole role, String language, boolean enabled) throws UserAccountCreationException {
    String username = created.getUserName();
    User stored = reload(username);
    if (stored == null || !Objects.equals(stored.getCreatedDate(), created.getCreatedDate())) {
      LOG.error("The account {} read back after its creation is not the one this request created, nothing is changed",
                username);
      throw new UserAccountCreationException(USER_CREATION_FAILED);
    }
    List<String> missing = missingParts(stored, role, language, enabled);
    if (missing.isEmpty()) {
      return;
    }
    String parts = String.join(",", missing);
    if (!stored.isEnabled()) {
      LOG.warn("The account {} was created deactivated with parts not stored: {}", username, parts);
      throw new UserAccountCreationException(USER_CREATED_INCOMPLETE + ":" + username + ":" + parts);
    }
    // The compensating deactivation acts on an account proven to be this
    // request's, and is read back the same way
    try {
      organizationService.getUserHandler().setEnabled(username, false, true);
    } catch (Exception e) {
      LOG.warn("The incomplete account {} could not be deactivated", username, e);
    }
    User disabled = reload(username);
    if (disabled != null && !disabled.isEnabled()) {
      LOG.warn("The account {} was created with parts not stored, and deactivated: {}", username, parts);
      throw new UserAccountCreationException(USER_CREATED_INCOMPLETE + ":" + username + ":" + parts);
    }
    LOG.error("The account {} was created with parts not stored and stays active: {}", username, parts);
    throw new UserAccountCreationException(USER_CREATED_INCOMPLETE_ENABLED + ":" + username + ":" + parts);
  }

  private User reload(String username) throws UserAccountCreationException {
    try {
      return reloadUser(username);
    } catch (Exception e) {
      throw new UserAccountCreationException(USER_CREATION_FAILED, e);
    }
  }

  private User reloadUser(String username) throws Exception {
    restartTransaction();
    evict(username);
    return findUser(username);
  }

  private List<String> missingParts(User stored, UserAccountRole role, String language, boolean enabled) {
    List<String> missing = new ArrayList<>();
    String username = stored.getUserName();
    if (!hasExpectedMemberships(username, role)) {
      missing.add(PART_MEMBERSHIPS);
    }
    if (language != null && !StringUtils.equals(language, storedLanguage(username))) {
      missing.add(PART_LANGUAGE);
    }
    if (stored.isEnabled() != enabled) {
      missing.add(PART_ENABLED);
    }
    return missing;
  }

  private boolean hasExpectedMemberships(String username, UserAccountRole role) {
    Collection<Membership> memberships;
    try {
      memberships = organizationService.getMembershipHandler().findMembershipsByUser(username);
    } catch (Exception e) {
      LOG.warn("The memberships of the created account {} could not be read back", username, e);
      return false;
    }
    if (memberships == null) {
      memberships = Collections.emptyList();
    }
    boolean platformUser = holds(memberships, PLATFORM_USERS_GROUP, MEMBER_MEMBERSHIP_TYPE);
    return switch (role) {
      case EXTERNAL -> holds(memberships, PLATFORM_EXTERNALS_GROUP, MEMBER_MEMBERSHIP_TYPE) && !platformUser;
      case ADMINISTRATOR -> platformUser && holds(memberships, PLATFORM_ADMINISTRATORS_GROUP, userACL.getAdminMSType());
      default -> platformUser;
    };
  }

  /**
   * Proves that the new password of an internal-store account authenticates,
   * read in a new transaction: the internal connector saves through the
   * store's swallowing flush. A deactivated account cannot sign in, so its
   * password is checked against the store directly, which the store's own
   * handler class alone offers
   */
  private void verifyPassword(String username, String password) throws UserAccountUpdateException {
    restartTransaction();
    evict(username);
    try {
      User stored = findUser(username);
      if (stored == null || !stored.isInternalStore()) {
        return;
      }
      UserHandler userHandler = organizationService.getUserHandler();
      boolean authenticated;
      if (stored.isEnabled()) {
        authenticated = userHandler.authenticate(username, password);
      } else if (userHandler instanceof UserDAOImpl userDAO) {
        authenticated = userDAO.authenticateDB(stored, password);
      } else {
        LOG.error("The new password of the deactivated account {} cannot be checked by {}",
                  username,
                  userHandler.getClass().getName());
        throw new UserAccountUpdateException(USER_PASSWORD_NOT_VERIFIABLE);
      }
      if (!authenticated) {
        LOG.error("The new password of the account {} does not authenticate after its change", username);
        throw new UserAccountUpdateException(USER_PASSWORD_NOT_STORED);
      }
    } catch (UserAccountUpdateException e) {
      throw e;
    } catch (Exception e) {
      throw new UserAccountUpdateException(USER_PASSWORD_NOT_STORED, e);
    }
  }

  private void writeFields(String username, UserAccount account) {
    try {
      User user = findUser(username);
      if (user == null || (StringUtils.equals(account.getEmail(), user.getEmail())
                           && StringUtils.equals(account.getLastName(), user.getLastName())
                           && StringUtils.equals(account.getFirstName(), user.getFirstName()))) {
        return;
      }
      user.setEmail(account.getEmail());
      user.setFirstName(account.getFirstName());
      user.setLastName(account.getLastName());
      user.setDisplayName(account.getFirstName() + " " + account.getLastName());
      organizationService.getUserHandler().saveUser(user, true);
    } catch (Exception e) {
      LOG.warn("The names and email of the account {} could not be stored", username, e);
    }
  }

  private void writeEnabled(String username, boolean storedEnabled, boolean enabled) {
    if (storedEnabled == enabled) {
      return;
    }
    try {
      organizationService.getUserHandler().setEnabled(username, enabled, true);
    } catch (Exception e) {
      LOG.warn("The account {} could not be {}", username, enabled ? "enabled" : "deactivated", e);
    }
  }

  /**
   * Reads the edited fields back in a new transaction, the language only when
   * the update carried one: the store commits around every listener and
   * swallows a failed flush, so only a new read tells what it holds
   */
  private void readBackUpdate(UserAccount account, String language) throws UserAccountUpdateException {
    String username = account.getUserName();
    User stored;
    try {
      stored = reloadUser(username);
    } catch (Exception e) {
      throw new UserAccountUpdateException(USER_UPDATE_INCOMPLETE, e);
    }
    List<String> mismatches = new ArrayList<>();
    if (stored == null) {
      mismatches.add("account");
    } else {
      if (!StringUtils.equals(stored.getFirstName(), account.getFirstName())) {
        mismatches.add("firstName");
      }
      if (!StringUtils.equals(stored.getLastName(), account.getLastName())) {
        mismatches.add("lastName");
      }
      if (!StringUtils.equals(stored.getEmail(), account.getEmail())) {
        mismatches.add("email");
      }
      if (stored.isEnabled() != account.isEnabled()) {
        mismatches.add("enabled");
      }
      if (language != null && !StringUtils.equals(language, storedLanguage(username))) {
        mismatches.add("language");
      }
    }
    if (!mismatches.isEmpty()) {
      LOG.error("The account {} read back after its update differs from the request: {}", username, mismatches);
      throw new UserAccountUpdateException(USER_UPDATE_INCOMPLETE);
    }
  }

  /**
   * A read before any write: its failure is the store's, nothing was
   * written or read back
   */
  private User readUser(String username) {
    try {
      return findUser(username);
    } catch (Exception e) {
      throw new IllegalStateException("The account " + username + " could not be read", e);
    }
  }

  private boolean isEmailUsedQuietly(String email) {
    try {
      return isEmailUsed(email);
    } catch (Exception e) {
      throw new IllegalStateException("The accounts holding an email could not be read", e);
    }
  }

  private PasswordRecoveryService passwordRecoveryService() {
    if (passwordRecoveryService == null) {
      passwordRecoveryService = ExoContainerContext.getService(PasswordRecoveryService.class);
    }
    return passwordRecoveryService;
  }

  /**
   * Ends the current transactions and opens new ones, so that the next read
   * sees what the store committed; the seam a unit test replaces, since the
   * real one needs a container
   */
  protected void restartTransaction() {
    RequestLifeCycle.restartTransaction();
  }

  void setUsernameValidator(UserFieldValidator usernameValidator) {
    this.usernameValidator = usernameValidator;
  }

  private static boolean holds(Collection<Membership> memberships, String groupId, String membershipType) {
    return memberships.stream()
                      .anyMatch(m -> StringUtils.equals(m.getGroupId(), groupId)
                                     && StringUtils.equals(m.getMembershipType(), membershipType));
  }

  private String storedLanguage(String username) {
    try {
      UserProfile profile = organizationService.getUserProfileHandler().findUserProfileByName(username);
      return profile == null ? null : profile.getAttribute(Constants.USER_LANGUAGE);
    } catch (Exception e) {
      LOG.warn("The language of the account {} could not be read back", username, e);
      return null;
    }
  }

  private void evict(String username) {
    if (organizationService.getUserHandler() instanceof CacheableUserHandlerImpl cachedUsers) {
      cachedUsers.clearCache(username);
    }
    if (organizationService.getUserProfileHandler() instanceof CacheableUserProfileHandlerImpl cachedProfiles) {
      cachedProfiles.clearCache(username);
    }
    if (organizationService.getMembershipHandler() instanceof CacheableMembershipHandlerImpl cachedMemberships) {
      cachedMemberships.clearMembershipCache(username);
    }
  }

  private void link(User user, String groupId, String membershipTypeName) throws Exception {
    Group group = organizationService.getGroupHandler().findGroupById(groupId);
    MembershipType membershipType = organizationService.getMembershipTypeHandler().findMembershipType(membershipTypeName);
    if (group == null || membershipType == null) {
      throw new IllegalStateException("Group %s or membership type %s not found".formatted(groupId, membershipTypeName));
    }
    organizationService.getMembershipHandler().linkMembership(user, group, membershipType, true);
  }

  private void unlink(String username, String groupId, String membershipTypeName) throws Exception {
    MembershipHandler membershipHandler = organizationService.getMembershipHandler();
    Membership membership = membershipHandler.findMembershipByUserGroupAndType(username, groupId, membershipTypeName);
    if (membership != null) {
      membershipHandler.removeMembership(membership.getId(), true);
    }
  }

  private String validateLanguage(String language) {
    String value = StringUtils.trimToNull(language);
    if (value == null) {
      return null;
    }
    Collection<LocaleConfig> configs = localeConfigService.getLocalConfigs();
    if (configs != null) {
      for (LocaleConfig config : configs) {
        if (StringUtils.equals(config.getLocaleName(), value)) {
          return config.getLocaleName();
        }
      }
    }
    throw new IllegalArgumentException(LANGUAGE_INVALID);
  }

  private User findUser(String username) throws Exception {
    return organizationService.getUserHandler().findUserByName(username, UserStatus.ANY);
  }

  private boolean isEmailUsed(String email) throws Exception {
    Query query = new Query();
    query.setEmail(StringUtils.trim(email));
    return organizationService.getUserHandler().findUsersByQuery(query, UserStatus.ANY).getSize() > 0;
  }

  private static boolean isAlreadyPresent(Throwable e) {
    return ExceptionUtils.getThrowableList(e)
                         .stream()
                         .map(Throwable::getMessage)
                         .anyMatch(message -> message != null && message.contains(IDENTITY_ALREADY_PRESENT_MESSAGE));
  }

  private static void validate(UserFieldValidator validator, String prefix, Locale locale, String value) {
    String message = validator.validate(locale, value);
    if (StringUtils.isNotBlank(message)) {
      throw new IllegalArgumentException(prefix + message);
    }
  }

}
