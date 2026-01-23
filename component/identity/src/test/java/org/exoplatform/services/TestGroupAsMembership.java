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
package org.exoplatform.services;

import java.util.Collection;
import java.util.Set;

import io.meeds.portal.permlink.model.PermanentLinkObject;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.idm.GroupDAOImpl;
import org.exoplatform.services.organization.idm.MembershipDAOImpl;

import static org.junit.Assert.assertThrows;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/services/organization/TestOrganization-configuration.xml"),
})
public class TestGroupAsMembership extends AbstractKernelTest {// NOSONAR

  private OrganizationService organizationService; // NOSONAR

  private GroupDAOImpl        groupDao;

  private MembershipDAOImpl   membershipDao;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    organizationService = getContainer().getComponentInstanceOfType(OrganizationService.class);
    membershipDao = (MembershipDAOImpl) organizationService.getMembershipHandler();
    groupDao = (GroupDAOImpl) organizationService.getGroupHandler();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testLinkGroupAsGroupMember() throws Exception {
    Group parentGroup = groupDao.findGroupById("/organization/management/human-resources");
    Group memberGroup = groupDao.findGroupById("/organization/management/executive-board");
    Group parentOfParentGroup = groupDao.findGroupById("/organization/operations");

    // Given
    Collection memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(memberGroup.getId())));
    assertFalse(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(parentGroup.getId())));
    assertFalse(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(parentOfParentGroup.getId())));

    // When
    groupDao.linkGroups(parentGroup.getId(), memberGroup.getId());

    // Then
    Group updatedMemberGroup = groupDao.findGroupById("/organization/management/executive-board");
    Set<String> inheritedGroups = updatedMemberGroup.getInheritedGroups();
    assertNotNull(inheritedGroups);
    assertTrue(inheritedGroups.contains(parentGroup.getId()));
    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(memberGroup.getId())));
    assertTrue(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(parentGroup.getId())));
    assertFalse(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(parentOfParentGroup.getId())));

    // When
    groupDao.linkGroups(parentOfParentGroup.getId(), parentGroup.getId());

    //Then
    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(memberGroup.getId())));
    assertTrue(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(parentGroup.getId())));
    assertTrue(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(parentOfParentGroup.getId())));

    // When
    groupDao.unlinkGroups(parentGroup.getId(), memberGroup.getId());
    groupDao.unlinkGroups(parentOfParentGroup.getId(), parentGroup.getId());

    // Then
    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(memberGroup.getId())));
    assertFalse(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(parentGroup.getId())));

    updatedMemberGroup = groupDao.findGroupById("/organization/management/executive-board");
    inheritedGroups = updatedMemberGroup.getNestedGroups();
    assertNull(inheritedGroups);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testLinkGroupAsMemberOfMultipleGroups() throws Exception {

    Group parentGroup1 = groupDao.findGroupById("/organization/management/human-resources");
    Group parentGroup2 = groupDao.findGroupById("/organization/operations");
    Group memberGroup = groupDao.findGroupById("/organization/management/executive-board");

    // Given
    Collection memberships = membershipDao.findMembershipsByUser("root", true);

    assertTrue(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(memberGroup.getId())));
    assertFalse(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(parentGroup1.getId())));

    assertFalse(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(parentGroup2.getId())));

    // When (link multiple)
    groupDao.linkGroups(parentGroup1.getId(), memberGroup.getId());
    groupDao.linkGroups(parentGroup2.getId(), memberGroup.getId());

    // Then
    Group updatedMemberGroup = groupDao.findGroupById(memberGroup.getId());
    assertNotNull(updatedMemberGroup.getInheritedGroups());
    assertEquals(2, updatedMemberGroup.getInheritedGroups().size());

    memberships = membershipDao.findMembershipsByUser("root", true);

    assertTrue(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(memberGroup.getId())));
    assertTrue(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(parentGroup1.getId())));
    assertTrue(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(parentGroup2.getId())));

    // When (unlink multiple)
    groupDao.unlinkGroups(parentGroup1.getId(), memberGroup.getId());

    // Then
    memberships = membershipDao.findMembershipsByUser("root", true);

    assertTrue(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(memberGroup.getId())));
    assertTrue(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(parentGroup2.getId())));

    assertFalse(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(parentGroup1.getId())));
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void testDoNotLinkGroup() throws Exception {

    Group parentGroup = groupDao.findGroupById("/organization/management/human-resources");
    Group memberGroup = groupDao.findGroupById("/organization/management/executive-board");
    Group parentOfParentGroup = groupDao.findGroupById("/organization/operations");

    // Given
    Collection memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(memberGroup.getId())));
    assertFalse(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(parentGroup.getId())));

    // When
    groupDao.linkGroups(parentGroup.getId(), memberGroup.getId());

    // Then
    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(memberGroup.getId())));
    assertTrue(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(parentGroup.getId())));
    //when
    assertThrows(IllegalStateException.class, () -> groupDao.linkGroups(memberGroup.getId(), parentGroup.getId()));

    // When
    groupDao.linkGroups(parentOfParentGroup.getId(), parentGroup.getId());

    //Then
    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
            .anyMatch(m -> ((Membership) m).getGroupId().equals(parentOfParentGroup.getId())));

    // When
    assertThrows(IllegalStateException.class, () -> groupDao.linkGroups(memberGroup.getId(), parentOfParentGroup.getId()));
    groupDao.unlinkGroups(parentGroup.getId(), memberGroup.getId());
    groupDao.unlinkGroups(parentOfParentGroup.getId(), parentGroup.getId());
  }

}
