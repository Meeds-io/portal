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
package org.exoplatform.services.organization;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;

/**
 * Created by exo on 5/5/16.
 */
@ConfiguredBy({
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-ldap-user-only-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/services/organization/TestOrganization-configuration.xml") })
public class TestLDAPUserOnlyOrganization extends TestLDAPOrganization {
  public void synchronizeUsers() throws Exception {
    // Disable synchronization of users
  }

  public void testGetUserGroups() throws Exception {
    begin();

    // Given
    User testUser = userHandler_.findUserByName("jduke");
    Group group = groupHandler_.findGroupById(GROUP_1);
    MembershipType mt1 = mtHandler_.createMembershipTypeInstance();
    mt1.setName("test");
    mtHandler_.createMembershipType(mt1, true);
    membershipHandler_.linkMembership(testUser, group, mt1, true);

    // 
    ListAccess<User> users = userHandler_.findUsersByGroupId(GROUP_1);
    assertTrue("Group memberships is empty", users.getSize() > 0);

    User[] usersArray = users.load(0, users.getSize());
    assertTrue("Loaded group memberships is empty which is not coherent with the size of ListAccess", usersArray.length > 0);

    boolean foundUser = false;
    for (int i = 0; i < usersArray.length && !foundUser; i++) {
      User user = usersArray[i];
        foundUser |= user.getUserName().equals(testUser.getUserName());
    }
    assertTrue("User test is not found in group " + GROUP_1, foundUser);
  }

  public void testFindUser() throws Exception {
    // Disable this test because it deletes an entry from LDAP
  }

  public void testFindFilteredGroup() throws Exception {
    // Needed only to build
  }

}
