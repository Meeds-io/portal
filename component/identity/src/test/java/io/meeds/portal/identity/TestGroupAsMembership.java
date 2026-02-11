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
package io.meeds.portal.identity;

import static org.junit.Assert.assertThrows;

import java.util.Collection;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.NestedMembership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.idm.GroupDAOImpl;
import org.exoplatform.services.organization.idm.MembershipDAOImpl;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/services/organization/TestOrganization-configuration.xml"),
})
public class TestGroupAsMembership extends AbstractKernelTest {// NOSONAR

  private static final String GROUP_3 = "/organization/operations";

  private static final String GROUP_2 = "/organization/management/executive-board";

  private static final String GROUP_1 = "/organization/management/human-resources";

  private OrganizationService organizationService;                                 // NOSONAR

  private GroupDAOImpl        groupDao;

  private MembershipDAOImpl   membershipDao;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    organizationService = getContainer().getComponentInstanceOfType(OrganizationService.class);
    membershipDao = (MembershipDAOImpl) organizationService.getMembershipHandler();
    groupDao = (GroupDAOImpl) organizationService.getGroupHandler();
  }

  @SuppressWarnings({ "unchecked" })
  public void testLinkGroupAsGroupMember() throws Exception {
    Group parentGroup = groupDao.findGroupById(GROUP_1);
    Group nestedGroup = groupDao.findGroupById(GROUP_2);
    Group parentOfParentGroup = groupDao.findGroupById(GROUP_3);

    // Given
    Collection<Membership> memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(nestedGroup.getId())));
    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())));
    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentOfParentGroup.getId())));

    // When
    groupDao.linkGroups(NestedMembership.builder()
                                        .groupId(parentGroup.getId())
                                        .nestedGroupId(nestedGroup.getId())
                                        .build());

    // Then
    Set<NestedMembership> updatedNestedMemberships = groupDao.getNestedMemberships(parentGroup.getId());
    assertNotNull(updatedNestedMemberships);
    assertTrue(updatedNestedMemberships.stream().anyMatch(m -> m.getNestedGroupId().equals(nestedGroup.getId())));
    assertTrue(updatedNestedMemberships.stream()
                                       .anyMatch(m -> m.getNestedGroupId().equals(nestedGroup.getId())
                                                      && m.isIncludeAllMembershipTypes()
                                                      && m.isInheritMembershipType()));
    assertNull(groupDao.findGroupById(parentGroup.getId()).getEnclosingMemberships());

    Set<NestedMembership> updatedEnclosingMemberships = groupDao.findGroupById(nestedGroup.getId()).getEnclosingMemberships();
    assertNotNull(updatedEnclosingMemberships);
    assertTrue(updatedEnclosingMemberships.stream().anyMatch(m -> m.getGroupId().equals(parentGroup.getId())));
    assertTrue(updatedEnclosingMemberships.stream()
                                          .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())
                                                         && m.isIncludeAllMembershipTypes()
                                                         && m.isInheritMembershipType()));
    assertTrue(CollectionUtils.isEmpty(groupDao.getNestedMemberships(nestedGroup.getId())));

    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(nestedGroup.getId())));
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())));
    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentOfParentGroup.getId())));

    // When
    groupDao.linkGroups(NestedMembership.builder()
                                        .groupId(parentOfParentGroup.getId())
                                        .nestedGroupId(parentGroup.getId())
                                        .build());

    // Then
    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(nestedGroup.getId())));
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())));
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(parentOfParentGroup.getId())));

    // When
    groupDao.unlinkGroups(NestedMembership.builder()
                                          .groupId(parentOfParentGroup.getId())
                                          .nestedGroupId(parentGroup.getId())
                                          .build());

    // Then
    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(nestedGroup.getId())));
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())));
    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentOfParentGroup.getId())));

    groupDao.unlinkGroups(NestedMembership.builder()
                                          .groupId(parentGroup.getId())
                                          .nestedGroupId(nestedGroup.getId())
                                          .build());

    // Then
    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(nestedGroup.getId())));
    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())));
    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentOfParentGroup.getId())));
  }

  @SuppressWarnings({ "unchecked" })
  public void testLinkGroupAsMemberOfMultipleGroups() throws Exception {

    Group parentGroup1 = groupDao.findGroupById(GROUP_1);
    Group parentGroup2 = groupDao.findGroupById(GROUP_3);
    Group memberGroup = groupDao.findGroupById(GROUP_2);

    // Given
    Collection<Membership> memberships = membershipDao.findMembershipsByUser("root", true);

    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(memberGroup.getId())));
    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentGroup1.getId())));

    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentGroup2.getId())));

    // When (link multiple)
    groupDao.linkGroups(NestedMembership.builder()
                                        .groupId(parentGroup1.getId())
                                        .nestedGroupId(memberGroup.getId())
                                        .build());
    groupDao.linkGroups(NestedMembership.builder()
                                        .groupId(parentGroup2.getId())
                                        .nestedGroupId(memberGroup.getId())
                                        .build());

    // Then
    Group updatedMemberGroup = groupDao.findGroupById(memberGroup.getId());
    assertNotNull(updatedMemberGroup.getEnclosingMemberships());
    assertEquals(2, updatedMemberGroup.getEnclosingMemberships().size());

    memberships = membershipDao.findMembershipsByUser("root", true);

    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(memberGroup.getId())));
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(parentGroup1.getId())));
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(parentGroup2.getId())));

    // When (unlink multiple)
    groupDao.unlinkGroups(NestedMembership.builder()
                                          .groupId(parentGroup1.getId())
                                          .nestedGroupId(memberGroup.getId())
                                          .build());

    // Then
    memberships = membershipDao.findMembershipsByUser("root", true);

    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(memberGroup.getId())));
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(parentGroup2.getId())));
    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentGroup1.getId())));
  }

  @SuppressWarnings({ "unchecked" })
  public void testDoNotLinkGroup() throws Exception {

    Group parentGroup = groupDao.findGroupById(GROUP_1);
    Group memberGroup = groupDao.findGroupById(GROUP_2);
    Group parentOfParentGroup = groupDao.findGroupById(GROUP_3);

    // Given
    Collection<Membership> memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(memberGroup.getId())));
    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())));

    // When
    groupDao.linkGroups(NestedMembership.builder()
                                        .groupId(parentGroup.getId())
                                        .nestedGroupId(memberGroup.getId())
                                        .build());

    // Then
    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(memberGroup.getId())));
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())));
    // when
    assertThrows(IllegalStateException.class,
                 () -> groupDao.linkGroups(NestedMembership.builder()
                                                           .groupId(memberGroup.getId())
                                                           .nestedGroupId(parentGroup.getId())
                                                           .build()));

    // When
    groupDao.linkGroups(NestedMembership.builder()
                                        .groupId(parentOfParentGroup.getId())
                                        .nestedGroupId(parentGroup.getId())
                                        .build());

    // Then
    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(parentOfParentGroup.getId())));

    assertThrows(IllegalStateException.class,
                 () -> groupDao.linkGroups(NestedMembership.builder()
                                                           .groupId(memberGroup.getId())
                                                           .nestedGroupId(parentOfParentGroup.getId())
                                                           .build()));

    // When
    groupDao.unlinkGroups(NestedMembership.builder()
                                          .groupId(parentGroup.getId())
                                          .nestedGroupId(memberGroup.getId())
                                          .build());
    groupDao.unlinkGroups(NestedMembership.builder()
                                          .groupId(parentOfParentGroup.getId())
                                          .nestedGroupId(parentGroup.getId())
                                          .build());

    // Then
    memberships = membershipDao.findMembershipsByUser("root", true);
    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentOfParentGroup.getId())));
  }

  @SuppressWarnings({ "unchecked" })
  public void testIsMemberOfEnclosingGroup() throws Exception {
    String parentMembershipType = "manager";
    Group parentGroup = groupDao.findGroupById(GROUP_1);

    String nestedMembershipType = "member";
    Group nestedGroup = groupDao.findGroupById(GROUP_2);

    Group parentOfParentGroup = groupDao.findGroupById(GROUP_3);
    String parentOfParentMembershipType = "validator";

    // Given
    Collection<Membership> memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(nestedGroup.getId())
                                         && m.getMembershipType().equals(nestedMembershipType)));
    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())));
    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentOfParentGroup.getId())));

    // When
    groupDao.linkGroups(NestedMembership.builder()
                                        .groupId(parentGroup.getId())
                                        .membershipType(parentMembershipType)
                                        .nestedGroupId(nestedGroup.getId())
                                        .nestedMembershipType(nestedMembershipType)
                                        .build());

    // Then
    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(nestedGroup.getId())
                                         && m.getMembershipType().equals(nestedMembershipType)));
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())
                                         && m.getMembershipType().equals(parentMembershipType)));
    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentOfParentGroup.getId())));

    // When
    groupDao.linkGroups(NestedMembership.builder()
                                        .groupId(parentOfParentGroup.getId())
                                        .membershipType(parentOfParentMembershipType)
                                        .nestedGroupId(parentGroup.getId())
                                        .nestedMembershipType(parentMembershipType)
                                        .build());

    // Then
    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(nestedGroup.getId())
                                         && m.getMembershipType().equals(nestedMembershipType)));
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())
                                         && m.getMembershipType().equals(parentMembershipType)));
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(parentOfParentGroup.getId())
                                         && m.getMembershipType().equals(parentOfParentMembershipType)));

    // When
    groupDao.unlinkGroups(NestedMembership.builder()
                                          .groupId(parentGroup.getId())
                                          .nestedGroupId(nestedGroup.getId())
                                          .nestedMembershipType(nestedMembershipType)
                                          .build());

    // Then
    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(nestedGroup.getId())
                                         && m.getMembershipType().equals(nestedMembershipType)));
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())
                                         && m.getMembershipType().equals(parentMembershipType)));
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(parentOfParentGroup.getId())
                                         && m.getMembershipType().equals(parentOfParentMembershipType)));

    // When
    groupDao.unlinkGroups(NestedMembership.builder()
                                          .groupId(parentGroup.getId())
                                          .membershipType(parentMembershipType)
                                          .nestedGroupId(nestedGroup.getId())
                                          .build());

    // Then
    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(nestedGroup.getId())
                                         && m.getMembershipType().equals(nestedMembershipType)));
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())
                                         && m.getMembershipType().equals(parentMembershipType)));
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(parentOfParentGroup.getId())
                                         && m.getMembershipType().equals(parentOfParentMembershipType)));

    // When
    groupDao.unlinkGroups(NestedMembership.builder()
                                          .groupId(parentGroup.getId())
                                          .membershipType(parentMembershipType)
                                          .nestedGroupId(nestedGroup.getId())
                                          .nestedMembershipType(nestedMembershipType)
                                          .build());

    // Then
    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(nestedGroup.getId())
                                         && m.getMembershipType().equals(nestedMembershipType)));
    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())));
    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentOfParentGroup.getId())));

    // When
    groupDao.unlinkGroups(NestedMembership.builder()
                                          .groupId(parentOfParentGroup.getId())
                                          .membershipType(parentOfParentMembershipType)
                                          .nestedGroupId(parentGroup.getId())
                                          .nestedMembershipType(parentMembershipType)
                                          .build());

    // Then
    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(nestedGroup.getId())
                                         && m.getMembershipType().equals(nestedMembershipType)));
    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())));
    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentOfParentGroup.getId())));
  }

}
