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
package org.exoplatform.portal.config;

import java.util.Collection;

import org.exoplatform.services.organization.Group;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

/**
 * Default implementation of GroupVisibilityPlugin which allows to see a group
 * if any of these conditions is fulfilled:
 * * the given user is the super user
 * * the given user is a platform administrator
 * * the given user is a manager of the group
 */
public class DefaultGroupVisibilityPlugin extends GroupVisibilityPlugin {

  private UserACL userACL;

  public DefaultGroupVisibilityPlugin(UserACL userACL) {
    this.userACL = userACL;
  }

  /**
   * Check if the given identity can see the given group. The methods
   * org.exoplatform.services.security.Identity.isMemberOf(String, String) and
   * org.exoplatform.portal.config.UserACL.hasPermission(Identity, String) are not
   * used to return parent groups as well, in order to return a complete groups
   * tree.
   *
   * @param userIdentity The user identity
   * @param group The group to check
   * @return true if the user identity has permission to see the given group
   */
  @Override
  public boolean hasPermission(Identity userIdentity, Group group) {
    Collection<MembershipEntry> userMemberships = userIdentity.getMemberships();
    return userACL.getSuperUser().equals(userIdentity.getUserId())
        || userMemberships.stream()
                          .anyMatch(userMembership -> userMembership.getGroup().equals(userACL.getAdminGroups())
                              || ((userMembership.getGroup().equals(group.getId())
                                  || userMembership.getGroup().startsWith(group.getId() + "/"))
                                  && (userMembership.getMembershipType().equals("*")
                                      || userMembership.getMembershipType().equals("manager"))));
  }
}
