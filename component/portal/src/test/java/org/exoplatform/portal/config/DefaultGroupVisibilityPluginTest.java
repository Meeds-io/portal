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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Test;

import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.impl.GroupImpl;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

public class DefaultGroupVisibilityPluginTest {

  @Test
  public void shouldHasPermissionWhenUserIsSuperUser() {
    // Given
    UserACL userACL = mock(UserACL.class);
    when(userACL.getSuperUser()).thenReturn("john");
    DefaultGroupVisibilityPlugin plugin = new DefaultGroupVisibilityPlugin(userACL);

    Identity userIdentity = new Identity("john", Arrays.asList(new MembershipEntry("/platform/users", "manager")));
    Group groupPlatform = new GroupImpl();
    groupPlatform.setId("/platform");
    Group groupPlatformUsers = new GroupImpl();
    groupPlatformUsers.setId("/platform/users");

    // When
    boolean hasPermissionOnPlatform = plugin.hasPermission(userIdentity, groupPlatform);
    boolean hasPermissionOnPlatformUsers = plugin.hasPermission(userIdentity, groupPlatformUsers);

    // Then
    assertTrue(hasPermissionOnPlatform);
    assertTrue(hasPermissionOnPlatformUsers);
  }

  @Test
  public void shouldHasPermissionWhenUserIsPlatformAdministrator() {
    // Given
    UserACL userACL = mock(UserACL.class);
    when(userACL.getSuperUser()).thenReturn("root");
    when(userACL.getAdminGroups()).thenReturn("/platform/administrators");
    DefaultGroupVisibilityPlugin plugin = new DefaultGroupVisibilityPlugin(userACL);

    Identity userIdentity = new Identity("john", Arrays.asList(new MembershipEntry("/platform/administrators", "manager")));
    Group groupPlatform = new GroupImpl();
    groupPlatform.setId("/platform");
    Group groupPlatformUsers = new GroupImpl();
    groupPlatformUsers.setId("/platform/users");

    // When
    boolean hasPermissionOnPlatform = plugin.hasPermission(userIdentity, groupPlatform);
    boolean hasPermissionOnPlatformUsers = plugin.hasPermission(userIdentity, groupPlatformUsers);

    // Then
    assertTrue(hasPermissionOnPlatform);
    assertTrue(hasPermissionOnPlatformUsers);
  }

  @Test
  public void shouldHasPermissionWhenUserIsInGivenGroup() {
    // Given
    UserACL userACL = mock(UserACL.class);
    when(userACL.getSuperUser()).thenReturn("root");
    when(userACL.getAdminGroups()).thenReturn("/platform/administrators");
    DefaultGroupVisibilityPlugin plugin = new DefaultGroupVisibilityPlugin(userACL);

    Identity userIdentity = new Identity("john",
                                         Arrays.asList(new MembershipEntry("/platform/developers", "manager"),
                                                       new MembershipEntry("/platform/testers", "member"),
                                                       new MembershipEntry("/organization/rh", "*")));
    Group groupPlatform = new GroupImpl();
    groupPlatform.setId("/platform");
    Group groupPlatformDevelopers = new GroupImpl();
    groupPlatformDevelopers.setId("/platform/developers");
    Group groupPlatformTesters = new GroupImpl();
    groupPlatformTesters.setId("/platform/testers");
    Group groupOrganization = new GroupImpl();
    groupOrganization.setId("/organization");
    Group groupOrganizationRh = new GroupImpl();
    groupOrganizationRh.setId("/organization/rh");
    Group groupDepartments = new GroupImpl();
    groupDepartments.setId("/departments");

    // When
    boolean hasPermissionOnPlatform = plugin.hasPermission(userIdentity, groupPlatform);
    boolean hasPermissionOnPlatformDevelopers = plugin.hasPermission(userIdentity, groupPlatformDevelopers);
    boolean hasPermissionOnPlatformTesters = plugin.hasPermission(userIdentity, groupPlatformTesters);
    boolean hasPermissionOnOrganization = plugin.hasPermission(userIdentity, groupOrganization);
    boolean hasPermissionOnOrganizationRh = plugin.hasPermission(userIdentity, groupOrganizationRh);
    boolean hasPermissionOnODepartments = plugin.hasPermission(userIdentity, groupDepartments);

    // Then
    assertTrue(hasPermissionOnPlatform);
    assertTrue(hasPermissionOnPlatformDevelopers);
    assertFalse(hasPermissionOnPlatformTesters);
    assertTrue(hasPermissionOnOrganization);
    assertTrue(hasPermissionOnOrganizationRh);
    assertFalse(hasPermissionOnODepartments);
  }

}
