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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupEventListener;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.NestedMembership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.idm.GroupDAOImpl;
import org.exoplatform.services.organization.idm.MembershipDAOImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/services/organization/TestOrganization-configuration.xml"),
})
public class TestGroupAsMembership extends AbstractKernelTest {// NOSONAR

  private static final String GROUP_3 = "/organization/operations";

  private static final String GROUP_2 = "/organization/management/executive-board";

  private static final String GROUP_1 = "/organization/management/human-resources";

  private static final String GROUP_4 = "/organization/management";


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

  public void testUpdateGroupKeepsUnchangedLinkSentWithEmptyRoles() throws Exception {
    GroupEventRecorder recorder = new GroupEventRecorder();
    NestedMembership link = NestedMembership.builder()
                                            .groupId(GROUP_1)
                                            .nestedGroupId(GROUP_2)
                                            .membershipType(NestedMembership.INHERIT_MEMBERSHIP_TYPE)
                                            .nestedMembershipType(NestedMembership.ALL_MEMBERSHIP_TYPE)
                                            .build();
    try {
      groupDao.linkGroups(link);
      groupDao.addGroupEventListener(recorder);
      Group nestedGroup = groupDao.findGroupById(GROUP_2);
      assertEquals(1, nestedGroup.getEnclosingMemberships().size());

      // When the same link is sent back with empty roles
      nestedGroup.setEnclosingMemberships(new HashSet<>(Collections.singleton(NestedMembership.builder()
                                                                                              .groupId(GROUP_1)
                                                                                              .nestedGroupId(GROUP_2)
                                                                                              .membershipType("")
                                                                                              .nestedMembershipType("")
                                                                                              .build())));
      groupDao.updateGroup(nestedGroup, true);

      // Then
      Set<NestedMembership> enclosingMemberships = groupDao.findGroupById(GROUP_2).getEnclosingMemberships();
      assertNotNull(enclosingMemberships);
      assertEquals(Collections.singleton(link), enclosingMemberships);
      assertEquals(Collections.singleton(link), groupDao.getNestedMemberships(GROUP_1));
      assertTrue(recorder.events.isEmpty());
    } finally {
      groupDao.removeGroupEventListener(recorder);
      groupDao.unlinkGroups(link);
    }
  }

  public void testUpdateGroupReplacesLinkRemovingBeforeAdding() throws Exception {
    GroupEventRecorder recorder = new GroupEventRecorder();
    NestedMembership oldLink = NestedMembership.builder()
                                               .groupId(GROUP_1)
                                               .nestedGroupId(GROUP_2)
                                               .membershipType(NestedMembership.INHERIT_MEMBERSHIP_TYPE)
                                               .nestedMembershipType(NestedMembership.ALL_MEMBERSHIP_TYPE)
                                               .build();
    NestedMembership newLink = oldLink.toBuilder().groupId(GROUP_3).build();
    try {
      groupDao.linkGroups(oldLink);
      groupDao.addGroupEventListener(recorder);
      Group nestedGroup = groupDao.findGroupById(GROUP_2);

      // When
      nestedGroup.setEnclosingMemberships(new HashSet<>(Collections.singleton(NestedMembership.builder()
                                                                                              .groupId(GROUP_3)
                                                                                              .nestedGroupId(GROUP_2)
                                                                                              .build())));
      groupDao.updateGroup(nestedGroup, true);

      // Then
      assertEquals(Collections.singleton(newLink), groupDao.findGroupById(GROUP_2).getEnclosingMemberships());
      assertTrue(CollectionUtils.isEmpty(groupDao.getNestedMemberships(GROUP_1)));
      assertEquals(Collections.singleton(newLink), groupDao.getNestedMemberships(GROUP_3));
      assertEquals(List.of("unlink:" + GROUP_1, "link:" + GROUP_3), recorder.events);
    } finally {
      groupDao.removeGroupEventListener(recorder);
      groupDao.unlinkGroups(oldLink);
      groupDao.unlinkGroups(newLink);
    }
  }

  public void testUpdateGroupRefusingAddedLinkKeepsStoredLinkAndFiresNoLinkEvent() throws Exception {
    GroupEventRecorder recorder = new GroupEventRecorder();
    NestedMembership storedLink = NestedMembership.builder()
                                                  .groupId(GROUP_1)
                                                  .nestedGroupId(GROUP_2)
                                                  .membershipType(NestedMembership.INHERIT_MEMBERSHIP_TYPE)
                                                  .nestedMembershipType(NestedMembership.ALL_MEMBERSHIP_TYPE)
                                                  .build();
    NestedMembership reverseLink = NestedMembership.builder()
                                                   .groupId(GROUP_2)
                                                   .nestedGroupId(GROUP_3)
                                                   .membershipType(NestedMembership.INHERIT_MEMBERSHIP_TYPE)
                                                   .nestedMembershipType(NestedMembership.ALL_MEMBERSHIP_TYPE)
                                                   .build();
    try {
      groupDao.linkGroups(storedLink);
      groupDao.linkGroups(reverseLink);
      // Commit the stored links, as a previous request would have, since a refused update rolls the open transaction back
      commitIdmTransaction();
      groupDao.addGroupEventListener(recorder);
      Group storedGroup = groupDao.findGroupById(GROUP_2);
      String storedLabel = storedGroup.getLabel();
      Group updatedGroup = groupDao.createGroupInstance();
      updatedGroup.setId(storedGroup.getId());
      updatedGroup.setParentId(storedGroup.getParentId());
      updatedGroup.setGroupName(storedGroup.getGroupName());
      updatedGroup.setDescription(storedGroup.getDescription());
      updatedGroup.setLabel("Refused label");

      // When the label changes and the stored link is replaced by one that would create a cycle
      updatedGroup.setEnclosingMemberships(new HashSet<>(Collections.singleton(NestedMembership.builder()
                                                                                               .groupId(GROUP_3)
                                                                                               .nestedGroupId(GROUP_2)
                                                                                               .build())));
      assertThrows(IllegalStateException.class, () -> groupDao.updateGroup(updatedGroup, true));

      // Then
      assertEquals(storedLabel, groupDao.findGroupById(GROUP_2).getLabel());
      assertEquals(Collections.singleton(storedLink), groupDao.findGroupById(GROUP_2).getEnclosingMemberships());
      assertEquals(Collections.singleton(storedLink), groupDao.getNestedMemberships(GROUP_1));
      assertTrue(recorder.events.isEmpty());
    } finally {
      groupDao.removeGroupEventListener(recorder);
      try {
        groupDao.unlinkGroups(storedLink);
      } finally {
        try {
          groupDao.unlinkGroups(reverseLink);
        } finally {
          commitIdmTransaction();
        }
      }
    }
  }

  public void testUpdateGroupRefusingOneOfTwoAddedLinksStoresNoLinkAndFiresNoLinkEvent() throws Exception {
    GroupEventRecorder recorder = new GroupEventRecorder();
    NestedMembership nestedLink = NestedMembership.builder()
                                                  .groupId(GROUP_2)
                                                  .nestedGroupId(GROUP_3)
                                                  .membershipType(NestedMembership.INHERIT_MEMBERSHIP_TYPE)
                                                  .nestedMembershipType(NestedMembership.ALL_MEMBERSHIP_TYPE)
                                                  .build();
    try {
      groupDao.linkGroups(nestedLink);
      // Commit the stored link, as a previous request would have, since a refused update rolls the open transaction back
      commitIdmTransaction();
      groupDao.addGroupEventListener(recorder);
      Group storedGroup = groupDao.findGroupById(GROUP_2);
      String storedLabel = storedGroup.getLabel();
      Group updatedGroup = groupDao.createGroupInstance();
      updatedGroup.setId(storedGroup.getId());
      updatedGroup.setParentId(storedGroup.getParentId());
      updatedGroup.setGroupName(storedGroup.getGroupName());
      updatedGroup.setDescription(storedGroup.getDescription());
      updatedGroup.setLabel("Refused label");

      // When the update removes nothing and adds one valid link and one that would create a cycle
      updatedGroup.setEnclosingMemberships(new HashSet<>(List.of(NestedMembership.builder()
                                                                                 .groupId(GROUP_1)
                                                                                 .nestedGroupId(GROUP_2)
                                                                                 .build(),
                                                                 NestedMembership.builder()
                                                                                 .groupId(GROUP_3)
                                                                                 .nestedGroupId(GROUP_2)
                                                                                 .build())));
      assertThrows(IllegalStateException.class, () -> groupDao.updateGroup(updatedGroup, true));

      // Then no link is kept: the refusal rolls back the request's IDM transaction,
      // and no link event fires: every addition is checked before any is stored
      assertEquals(storedLabel, groupDao.findGroupById(GROUP_2).getLabel());
      assertTrue(CollectionUtils.isEmpty(groupDao.findGroupById(GROUP_2).getEnclosingMemberships()));
      assertTrue(CollectionUtils.isEmpty(groupDao.getNestedMemberships(GROUP_1)));
      assertTrue(recorder.events.isEmpty());
    } finally {
      groupDao.removeGroupEventListener(recorder);
      try {
        groupDao.unlinkGroups(nestedLink);
      } finally {
        commitIdmTransaction();
      }
    }
  }

  public void testUpdateGroupSendingLinkNamingAnotherNestedGroupAppliesItToUpdatedGroup() throws Exception {
    GroupEventRecorder recorder = new GroupEventRecorder();
    NestedMembership expectedLink = NestedMembership.builder()
                                                    .groupId(GROUP_1)
                                                    .nestedGroupId(GROUP_2)
                                                    .membershipType(NestedMembership.INHERIT_MEMBERSHIP_TYPE)
                                                    .nestedMembershipType(NestedMembership.ALL_MEMBERSHIP_TYPE)
                                                    .build();
    try {
      groupDao.addGroupEventListener(recorder);
      Group group = groupDao.findGroupById(GROUP_2);

      // When the sent link names another group as the nested one
      group.setEnclosingMemberships(new HashSet<>(Collections.singleton(NestedMembership.builder()
                                                                                        .groupId(GROUP_1)
                                                                                        .nestedGroupId(GROUP_3)
                                                                                        .build())));
      groupDao.updateGroup(group, true);

      // Then the link is stored for the updated group, and for no other one
      assertEquals(Collections.singleton(expectedLink), groupDao.findGroupById(GROUP_2).getEnclosingMemberships());
      assertEquals(Collections.singleton(expectedLink), groupDao.getNestedMemberships(GROUP_1));
      assertTrue(CollectionUtils.isEmpty(groupDao.findGroupById(GROUP_3).getEnclosingMemberships()));
      assertEquals(List.of("link:" + GROUP_1), recorder.events);
    } finally {
      groupDao.removeGroupEventListener(recorder);
      try {
        groupDao.unlinkGroups(expectedLink);
      } finally {
        try {
          groupDao.unlinkGroups(expectedLink.toBuilder().nestedGroupId(GROUP_3).build());
        } finally {
          commitIdmTransaction();
        }
      }
    }
  }

  public void testUpdateGroupKeepsUnchangedLinkSentWithoutNestedGroupId() throws Exception {
    GroupEventRecorder recorder = new GroupEventRecorder();
    NestedMembership link = NestedMembership.builder()
                                            .groupId(GROUP_1)
                                            .nestedGroupId(GROUP_2)
                                            .membershipType(NestedMembership.INHERIT_MEMBERSHIP_TYPE)
                                            .nestedMembershipType(NestedMembership.ALL_MEMBERSHIP_TYPE)
                                            .build();
    try {
      groupDao.linkGroups(link);
      groupDao.addGroupEventListener(recorder);
      Group nestedGroup = groupDao.findGroupById(GROUP_2);

      // When the same link is sent back with no nested group id and empty roles
      nestedGroup.setEnclosingMemberships(new HashSet<>(Collections.singleton(NestedMembership.builder()
                                                                                              .groupId(GROUP_1)
                                                                                              .build())));
      groupDao.updateGroup(nestedGroup, true);

      // Then
      assertEquals(Collections.singleton(link), groupDao.findGroupById(GROUP_2).getEnclosingMemberships());
      assertEquals(Collections.singleton(link), groupDao.getNestedMemberships(GROUP_1));
      assertTrue(recorder.events.isEmpty());
    } finally {
      groupDao.removeGroupEventListener(recorder);
      groupDao.unlinkGroups(link);
    }
  }

  public void testUpdateGroupAsMembership() throws Exception {
    Group parentGroup = groupDao.findGroupById(GROUP_4);
    Group nestedGroup = groupDao.findGroupById(GROUP_2);

    // Given
    Collection<Membership> memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(nestedGroup.getId())));
    assertFalse(memberships.stream()
                           .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())));
    // When
    String membershipType = "member";
    NestedMembership nestedMembership = NestedMembership.builder()
                                                      .groupId(parentGroup.getId())
                                                      .membershipType(membershipType)
                                                      .nestedGroupId(nestedGroup.getId())
                                                      .nestedMembershipType(membershipType)
                                                      .build();
    nestedGroup.setEnclosingMemberships(new HashSet<>(Collections.singletonList(nestedMembership)));
    groupDao.updateGroup(nestedGroup, true);

    // Then
    Set<NestedMembership> updatedNestedMemberships = groupDao.getNestedMemberships(parentGroup.getId());
    assertNotNull(updatedNestedMemberships);
    assertTrue(updatedNestedMemberships.stream().anyMatch(m -> m.getNestedGroupId().equals(nestedGroup.getId())));
    assertTrue(updatedNestedMemberships.stream()
                                       .anyMatch(m -> m.getNestedGroupId().equals(nestedGroup.getId())
                                           && m.getNestedMembershipType().equals(membershipType)));
    assertNull(groupDao.findGroupById(parentGroup.getId()).getEnclosingMemberships());

    Set<NestedMembership> updatedEnclosingMemberships = groupDao.findGroupById(nestedGroup.getId()).getEnclosingMemberships();
    assertNotNull(updatedEnclosingMemberships);
    assertTrue(updatedEnclosingMemberships.stream().anyMatch(m -> m.getGroupId().equals(parentGroup.getId())));
    assertTrue(updatedEnclosingMemberships.stream()
                                          .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())
                                             && m.getMembershipType().equals(membershipType)));
    assertTrue(CollectionUtils.isEmpty(groupDao.getNestedMemberships(nestedGroup.getId())));

    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(nestedGroup.getId())));
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())));

    //when
    nestedGroup.setEnclosingMemberships(new HashSet<>(Collections.emptyList()));
    groupDao.updateGroup(nestedGroup, true);

    //
    updatedNestedMemberships = groupDao.getNestedMemberships(parentGroup.getId());
    assertTrue(CollectionUtils.isEmpty(updatedNestedMemberships));

    updatedEnclosingMemberships = groupDao.findGroupById(nestedGroup.getId()).getEnclosingMemberships();
    assertTrue(CollectionUtils.isEmpty(updatedEnclosingMemberships));

    memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(nestedGroup.getId())));
    assertFalse(memberships.stream()
                          .anyMatch(m -> m.getGroupId().equals(parentGroup.getId())));
  }

  public void testRefusedGroupRemovalKeepsNestedLinks() throws Exception {
    Group groupWithChildren = groupDao.findGroupById(GROUP_4);
    Group enclosingGroup = groupDao.findGroupById(GROUP_3);
    Group nestedGroup = groupDao.findGroupById(GROUP_1);
    NestedMembership enclosingLink = NestedMembership.builder()
                                                     .groupId(enclosingGroup.getId())
                                                     .nestedGroupId(groupWithChildren.getId())
                                                     .build();
    NestedMembership nestedLink = NestedMembership.builder()
                                                  .groupId(groupWithChildren.getId())
                                                  .nestedGroupId(nestedGroup.getId())
                                                  .build();
    groupDao.linkGroups(enclosingLink);
    groupDao.linkGroups(nestedLink);
    try {
      // Given
      Group linkedGroup = groupDao.findGroupById(GROUP_4);
      assertTrue(linkedGroup.getEnclosingMemberships().stream()
                            .anyMatch(m -> m.getGroupId().equals(enclosingGroup.getId())));
      assertTrue(groupDao.getNestedMemberships(GROUP_4).stream()
                         .anyMatch(m -> m.getNestedGroupId().equals(nestedGroup.getId())));

      // When
      IllegalStateException refusal = assertThrows(IllegalStateException.class,
                                                   () -> groupDao.removeGroup(linkedGroup, true));
      assertTrue(refusal.getMessage().contains("has at least one child group"));

      // Then
      Group stillThere = groupDao.findGroupById(GROUP_4);
      assertNotNull(stillThere);
      assertNotNull(stillThere.getEnclosingMemberships());
      assertTrue(stillThere.getEnclosingMemberships().stream()
                           .anyMatch(m -> m.getGroupId().equals(enclosingGroup.getId())));
      assertTrue(groupDao.getNestedMemberships(GROUP_4).stream()
                         .anyMatch(m -> m.getNestedGroupId().equals(nestedGroup.getId())));
    } finally {
      groupDao.unlinkGroups(enclosingLink);
      groupDao.unlinkGroups(nestedLink);
    }
  }

  public void testRemoveLeafGroupUnlinksItsNestedGroups() throws Exception {
    Group enclosingGroup = groupDao.findGroupById(GROUP_3);
    Group nestedGroup = groupDao.findGroupById(GROUP_2);
    Group leaf = groupDao.createGroupInstance();
    leaf.setGroupName("leafToRemove");
    leaf.setLabel("leafToRemove");
    groupDao.addChild(null, leaf, true);
    String leafId = leaf.getId();
    NestedMembership enclosingLink = NestedMembership.builder()
                                                     .groupId(enclosingGroup.getId())
                                                     .nestedGroupId(leafId)
                                                     .build();
    NestedMembership nestedLink = NestedMembership.builder()
                                                  .groupId(leafId)
                                                  .nestedGroupId(nestedGroup.getId())
                                                  .build();
    try {
      // Given
      groupDao.linkGroups(enclosingLink);
      groupDao.linkGroups(nestedLink);
      assertTrue(groupDao.getNestedMemberships(GROUP_3).stream()
                         .anyMatch(m -> m.getNestedGroupId().equals(leafId)));
      assertTrue(groupDao.findGroupById(GROUP_2).getEnclosingMemberships().stream()
                         .anyMatch(m -> m.getGroupId().equals(leafId)));

      // When
      groupDao.removeGroup(groupDao.findGroupById(leafId), true);

      // Then
      assertNull(groupDao.findGroupById(leafId));
      assertTrue(CollectionUtils.isEmpty(groupDao.getNestedMemberships(GROUP_3))
                 || groupDao.getNestedMemberships(GROUP_3).stream()
                            .noneMatch(m -> m.getNestedGroupId().equals(leafId)));
      Set<NestedMembership> enclosing = groupDao.findGroupById(GROUP_2).getEnclosingMemberships();
      assertTrue(CollectionUtils.isEmpty(enclosing)
                 || enclosing.stream().noneMatch(m -> m.getGroupId().equals(leafId)));
    } finally {
      if (groupDao.findGroupById(leafId) != null) {
        groupDao.removeGroup(groupDao.findGroupById(leafId), false);
      }
    }
  }

  public void testCreateGroupRefusingOneOfTwoLinksStoresNoLinkAndFiresNoLinkEvent() throws Exception {
    GroupEventRecorder recorder = new GroupEventRecorder();
    String createdGroupId = GROUP_4 + "/linkedcreation";
    try {
      commitIdmTransaction();
      groupDao.addGroupEventListener(recorder);
      Group parentGroup = groupDao.findGroupById(GROUP_4);
      Group createdGroup = groupDao.createGroupInstance();
      createdGroup.setGroupName("linkedcreation");
      createdGroup.setLabel("Linked creation");

      // When a new group is sent with a valid enclosing link, then one naming a missing group
      Set<NestedMembership> enclosingMemberships = new LinkedHashSet<>();
      enclosingMemberships.add(NestedMembership.builder().groupId(GROUP_1).build());
      enclosingMemberships.add(NestedMembership.builder().groupId("/organization/missing").build());
      createdGroup.setEnclosingMemberships(enclosingMemberships);
      assertThrows(IllegalStateException.class, () -> groupDao.addChild(parentGroup, createdGroup, true));

      // Then no link is kept: the refusal rolls back the request's IDM transaction,
      // and no link event fires: every addition is checked before any is stored
      assertTrue(CollectionUtils.isEmpty(groupDao.getNestedMemberships(GROUP_1)));
      assertTrue(recorder.events.isEmpty());
    } finally {
      groupDao.removeGroupEventListener(recorder);
      try {
        Group leftover = groupDao.findGroupById(createdGroupId);
        if (leftover != null) {
          groupDao.removeGroup(leftover, false);
        }
      } finally {
        commitIdmTransaction();
      }
    }
  }

  private static class GroupEventRecorder extends GroupEventListener {

    private final List<String> events = new ArrayList<>();

    @Override
    public void linkGroups(NestedMembership nestedMembership) {
      events.add("link:" + nestedMembership.getGroupId());
    }

    @Override
    public void unlinkGroups(NestedMembership nestedMembership) {
      events.add("unlink:" + nestedMembership.getGroupId());
    }
  }

  private void commitIdmTransaction() {
    PicketLinkIDMOrganizationServiceImpl idmService = (PicketLinkIDMOrganizationServiceImpl) organizationService;
    idmService.endRequest(getContainer());
    idmService.startRequest(getContainer());
  }

}
