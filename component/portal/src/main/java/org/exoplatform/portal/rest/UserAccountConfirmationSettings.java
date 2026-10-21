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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.management.annotations.Impact;
import org.exoplatform.management.annotations.ImpactType;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.annotations.ManagedName;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;

/**
 * Settings of the one-time-password confirmation of administrator actions on
 * user accounts: the switch that turns the confirmation off, changeable at
 * runtime through JMX only, and the technical accounts exempted from it,
 * read once from the server configuration and written by nothing else. The
 * component is deliberately not exposed on the REST management endpoint: an
 * administrator session could otherwise turn the confirmation off. It is
 * started with the container, so that its JMX attribute exists from boot,
 * before any guarded request.
 */
@Managed
@NameTemplate({ @Property(key = "view", value = "portal"), @Property(key = "service", value = "security"),
                @Property(key = "type", value = "userAccountConfirmation") })
@ManagedDescription("One-time-password confirmation of administrator actions on user accounts")
public class UserAccountConfirmationSettings implements Startable {

  public static final String  ENABLED_PARAM          = "enabled";

  public static final String  EXEMPTED_USERS_PARAM   = "exemptedUsers";

  public static final String  CASE_INSENSITIVE_PARAM = "caseInsensitive";

  private static final Log    LOG                    = ExoLogger.getLogger(UserAccountConfirmationSettings.class);

  private final Log                 logger;

  private final OrganizationService organizationService;

  private final UserACL             userACL;

  private final boolean             configuredEnabled;

  private final boolean             caseInsensitive;

  private final Set<String>         exemptedUsers;

  /**
   * Written by the JMX thread, read by every request thread
   */
  private volatile boolean          enabled;

  public UserAccountConfirmationSettings(InitParams params, OrganizationService organizationService, UserACL userACL) {
    this(parseBoolean(params, ENABLED_PARAM, true),
         parseList(params, EXEMPTED_USERS_PARAM),
         parseBoolean(params, CASE_INSENSITIVE_PARAM, true),
         organizationService,
         userACL,
         LOG);
  }

  UserAccountConfirmationSettings(boolean enabled,
                                  Collection<String> exemptedUsers,
                                  boolean caseInsensitive,
                                  OrganizationService organizationService,
                                  UserACL userACL,
                                  Log logger) {
    this.logger = logger;
    this.organizationService = organizationService;
    this.userACL = userACL;
    this.configuredEnabled = enabled;
    this.enabled = enabled;
    this.caseInsensitive = caseInsensitive;
    Set<String> users = caseInsensitive ? new TreeSet<>(String.CASE_INSENSITIVE_ORDER) : new HashSet<>();
    exemptedUsers.stream().map(StringUtils::trim).filter(StringUtils::isNotBlank).forEach(users::add);
    this.exemptedUsers = Collections.unmodifiableSet(users);
  }

  @Override
  public void start() {
    if (!configuredEnabled) {
      logger.warn("The one-time-password confirmation of administrator actions on user accounts is configured OFF:"
          + " every guarded request is let through. Switch it back on once the emergency is over.");
    }
  }

  @Override
  public void stop() {
    // Nothing to release
  }

  @Managed
  @Impact(ImpactType.READ)
  @ManagedDescription("Whether administrator actions on user accounts require a one-time-password confirmation")
  public boolean isEnabled() {
    return enabled;
  }

  @Managed
  @Impact(ImpactType.WRITE)
  @ManagedDescription("Switches the one-time-password confirmation on or off on this node until the next restart,"
      + " which restores the configured value. Emergency or malfunction only.")
  public void setEnabled(@ManagedName("enabled")
                         @ManagedDescription("true to require the confirmation, false to let every request through")
                         boolean enabled) {
    boolean previous = this.enabled;
    this.enabled = enabled;
    logger.warn("The one-time-password confirmation of administrator actions on user accounts was switched from {} to {}"
        + " on this node (configured value: {}, restored at the next restart)", previous, enabled, configuredEnabled);
  }

  /**
   * @return the configured value, which a restart restores
   */
  public boolean isConfiguredEnabled() {
    return configuredEnabled;
  }

  /**
   * @return whether a technical account is listed at all, so that a request
   *         with no exempted account configured never looks the user up
   */
  public boolean hasExemptedUsers() {
    return !exemptedUsers.isEmpty();
  }

  /**
   * Whether the request's user is one of the technical accounts listed in the
   * server configuration. The user name is resolved to the account's stored
   * name, read again by that name with the enabled status (the key every
   * eviction of the user cache uses, so a deactivated account is refused on
   * its next request), and matched with the store's case rule. The super user
   * is never exempted, whatever the list holds. A lookup that fails exempts
   * nobody.
   *
   * @param remoteUser user name as the client signed in
   * @return true when the account is exempted from the confirmation
   */
  public boolean isExempted(String remoteUser) {
    if (StringUtils.isBlank(remoteUser) || exemptedUsers.isEmpty()) {
      return false;
    }
    try {
      User user = organizationService.getUserHandler().findUserByName(remoteUser, UserStatus.ENABLED);
      if (user == null || StringUtils.isBlank(user.getUserName())) {
        return false;
      }
      String storedName = user.getUserName();
      User enabledUser = organizationService.getUserHandler().findUserByName(storedName, UserStatus.ENABLED);
      if (enabledUser == null || !enabledUser.isEnabled()) {
        return false;
      }
      if (isSuperUser(storedName)) {
        return false;
      }
      return exemptedUsers.contains(storedName);
    } catch (Exception e) {
      logger.warn("Error resolving the account of user {} to check the technical-account exemption, the request stays guarded",
               remoteUser,
               e);
      return false;
    }
  }

  private boolean isSuperUser(String storedName) {
    String superUser = userACL.getSuperUser();
    return caseInsensitive ? StringUtils.equalsIgnoreCase(storedName, superUser) : StringUtils.equals(storedName, superUser);
  }

  private static boolean parseBoolean(InitParams params, String name, boolean defaultValue) {
    ValueParam param = params == null ? null : params.getValueParam(name);
    return param == null || StringUtils.isBlank(param.getValue()) ? defaultValue : Boolean.parseBoolean(param.getValue().trim());
  }

  private static Collection<String> parseList(InitParams params, String name) {
    ValueParam param = params == null ? null : params.getValueParam(name);
    return param == null || StringUtils.isBlank(param.getValue()) ? Collections.emptyList()
                                                                   : Arrays.asList(param.getValue().split(","));
  }

}
