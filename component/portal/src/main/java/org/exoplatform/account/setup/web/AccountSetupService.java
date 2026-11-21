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
package org.exoplatform.account.setup.web;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;

public class AccountSetupService {

  private static final Log    LOG                             = ExoLogger.getLogger(AccountSetup.class);

  public final static String  ACCOUNT_SETUP_NODE              = "accountSetup";

  public final static String  ACCOUNT_SETUP_SKIP_PROPERTY     = "accountsetup.skip";

  private final static String ADMIN_FIRST_NAME                = "root";

  private final static String PLATFORM_USERS_GROUP            = "/platform/administrators";

  private final static String PLATFORM_WEB_CONTRIBUTORS_GROUP = "/platform/web-contributors";

  private final static String PLATFORM_DEVELOPERS_GROUP       = "/developers";

  private final static String PLATFORM_PLATFORM_USERS_GROUP   = "/platform/users";

  private final static String MEMBERSHIP_TYPE_MANAGER         = "*";

  private SettingService      settingService;

  private OrganizationService organizationService;

  private Boolean             skipSetup                       = null;

  public AccountSetupService(SettingService settingService, OrganizationService organizationService) {
    this.settingService = settingService;
    this.organizationService = organizationService;
  }

  public void setSkipSetup(boolean skipSetup) {
    this.skipSetup = skipSetup;
    settingService.set(Context.GLOBAL, Scope.GLOBAL, ACCOUNT_SETUP_NODE, SettingValue.create("true"));
  }

  public boolean mustSkipAccountSetup() {
    if (Boolean.TRUE.equals(skipSetup)) {
      return true;
    }
    Boolean setupDone = readSetupDone(false);
    if (Boolean.TRUE.equals(setupDone)) {
      skipSetup = true;
    }
    // An uncertain answer counts as "done": no setup screen is offered
    return setupDone != Boolean.FALSE;
  }

  /**
   * @return true when a read of the stored state, never of a cache,
   *         positively says that the setup is not done
   */
  public boolean isAccountSetupAllowed() {
    return readSetupDone(true) == Boolean.FALSE;
  }

  /**
   * Creates the first administrator only when the stored state says that the
   * setup is not done, checked under a lock on this node. Before the setup is
   * done, a concurrent submission on this node waits on its request thread
   * until the first one has finished. Once the setup is done, the action's
   * own check refuses the request before this method, and the stored-state
   * check below refuses one that a cached read let through.
   *
   * @return true when the setup ran (each step's failure is logged), false when
   *         the request was refused and nothing has been changed
   */
  public synchronized boolean createAccount(String userNameAccount,
                                            String firstNameAccount,
                                            String lastNameAccount,
                                            String emailAccount,
                                            String userPasswordAccount,
                                            String adminPassword) {
    if (!isAccountSetupAllowed()) {
      LOG.warn("Account setup refused: the platform is already set up or its state is unknown");
      return false;
    }
    MembershipTypeHandler membershipTypeHandler = organizationService.getMembershipTypeHandler();
    UserHandler userHandler = organizationService.getUserHandler();

    RequestLifeCycle.begin((ComponentRequestLifecycle) organizationService);
    try {
      // Set password for admin user
      try {
        User adminUser = userHandler.findUserByName(ADMIN_FIRST_NAME);
        adminUser.setPassword(adminPassword);
        organizationService.getUserHandler().saveUser(adminUser, false);
      } catch (Exception e) {
        LOG.error("Can not set password to the created user", e);
      }

      // Create user account
      User user = userHandler.createUserInstance(userNameAccount);
      user.setPassword(userPasswordAccount);
      user.setFirstName(firstNameAccount);
      user.setLastName(lastNameAccount);
      user.setEmail(emailAccount);

      try {
        userHandler.createUser(user, true);
      } catch (Exception e) {
        LOG.error("Can not create User", e);
      }

      MembershipType membershipType;
      // Assign the membership "*:/platform/users" to the created user
      try {
        Group group = organizationService.getGroupHandler().findGroupById(PLATFORM_PLATFORM_USERS_GROUP);
        membershipType = membershipTypeHandler.findMembershipType(MEMBERSHIP_TYPE_MANAGER);
        organizationService.getMembershipHandler().linkMembership(user, group, membershipType, true);
      } catch (Exception e) {
        LOG.error("Can not assign *:/platform/users membership to the created user", e);
      }

      // Assign the membership "*:/platform/administrators" to the created user
      try {
        Group group = organizationService.getGroupHandler().findGroupById(PLATFORM_USERS_GROUP);
        if (group != null) {
          membershipType = membershipTypeHandler.findMembershipType(MEMBERSHIP_TYPE_MANAGER);
          organizationService.getMembershipHandler().linkMembership(user, group, membershipType, true);
        }
      } catch (Exception e) {
        LOG.error("Can not assign *:/platform/administrators membership to the created user", e);
      }

      // Assign the membership "*:/platform/web-contributors" to the created
      // user
      try {
        Group group = organizationService.getGroupHandler().findGroupById(PLATFORM_WEB_CONTRIBUTORS_GROUP);
        if (group != null) {
          membershipType = membershipTypeHandler.findMembershipType(MEMBERSHIP_TYPE_MANAGER);
          organizationService.getMembershipHandler().linkMembership(user, group, membershipType, true);
        }
      } catch (Exception e) {
        LOG.error("Can not assign *:/platform/web-contributors membership to the created user", e);
      }

      setSkipSetup(true);
      return true;
    } finally {
      RequestLifeCycle.end();
    }
  }

  /**
   * @param fromStore true to read the stored state, false to allow a cached
   *          read
   * @return TRUE when the setup is done or skipped, FALSE when it positively
   *         is not, null when this can't be told (the read failed)
   */
  private Boolean readSetupDone(boolean fromStore) {
    String propertySetupSkip = PropertyManager.getProperty(ACCOUNT_SETUP_SKIP_PROPERTY);
    if (propertySetupSkip == null) {
      LOG.debug("Property accountsetup.skip not found in configuration.properties");
      propertySetupSkip = "false";
    }
    if (propertySetupSkip.equals("true") || PropertyManager.isDevelopping() || Boolean.TRUE.equals(skipSetup)) {
      return Boolean.TRUE;
    }
    try {
      SettingValue<?> node = fromStore ? settingService.getFromStore(Context.GLOBAL, Scope.GLOBAL, ACCOUNT_SETUP_NODE)
                                       : settingService.getOrThrow(Context.GLOBAL, Scope.GLOBAL, ACCOUNT_SETUP_NODE);
      return node != null;
    } catch (Exception e) {
      LOG.warn("Can not read the account setup state: {}", e.getMessage());
      LOG.debug("Account setup state read failure", e);
      return null;
    }
  }
}
