/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.NestedMembership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.idm.AbstractDAOImpl;
import org.exoplatform.services.organization.idm.GroupDAOImpl;
import org.exoplatform.services.organization.idm.MembershipDAOImpl;
import org.exoplatform.services.organization.idm.MembershipImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;

/**
 * Nested groups stored as a cycle (what the write-time guard of
 * {@code linkGroups} does not stop: concurrent links, an import, a direct data
 * edit) must neither overflow the stack of the identity build nor of the guard.
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/services/organization/TestOrganization-configuration.xml"),
})
public class TestNestedGroupsCycle extends AbstractKernelTest {// NOSONAR

  private static final String GROUP_OUTSIDE = "/outside/the-cycle";

  private static final String GROUP_B       = "/organization/management/executive-board";

  private static final String GROUP_A       = "/organization/management/human-resources";

  private static final String DIAMOND_D = "/organization/management/executive-board";

  private static final String DIAMOND_B = "/organization/management/human-resources";

  private static final String DIAMOND_C = "/organization/management";

  private static final String DIAMOND_A = "/organization/operations";

  private static final String DIAMOND_E = "/organization/communication";

  private final List<NestedMembership> diamondLinks = new ArrayList<>();

  private PicketLinkIDMOrganizationServiceImpl organizationService;

  private boolean             forwardLinkStored;

  private boolean             reverseLinkStored;

  private GroupDAOImpl        groupDao;

  private MembershipDAOImpl   membershipDao;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    organizationService = (PicketLinkIDMOrganizationServiceImpl) getContainer().getComponentInstanceOfType(OrganizationService.class);
    membershipDao = (MembershipDAOImpl) organizationService.getMembershipHandler();
    groupDao = (GroupDAOImpl) organizationService.getGroupHandler();
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      for (int i = diamondLinks.size() - 1; i >= 0; i--) {
        groupDao.unlinkGroups(diamondLinks.get(i));
      }
      if (reverseLinkStored) {
        storeLinks(GROUP_B, GROUP_A, Collections.emptySet(), Collections.emptySet());
      }
      if (forwardLinkStored) {
        groupDao.unlinkGroups(NestedMembership.builder().groupId(GROUP_A).nestedGroupId(GROUP_B).build());
      }
    } finally {
      super.tearDown();
    }
  }

  public void testInheritedMembershipsStopOnCycle() throws Exception {
    storeCycleAInBAndBInA();
    Log log = replaceLog(membershipDao);

    Set<MembershipImpl> inherited = inheritedMemberships(new MembershipImpl("member", "root", GROUP_B, false));

    assertTrue(groupIds(inherited).containsAll(Set.of(GROUP_A, GROUP_B)));
    verify(log, times(1)).warn(contains("cycle"), eq("root"), eq(GROUP_B), eq(Set.of(GROUP_A, GROUP_B)));

    // The identity build of every member of a group in the cycle
    Collection<Membership> memberships = membershipDao.findMembershipsByUser("root", true);
    assertTrue(memberships.stream().anyMatch(m -> m.getGroupId().equals(GROUP_A)));
    assertTrue(memberships.stream().anyMatch(m -> m.getGroupId().equals(GROUP_B)));
  }

  public void testInheritedMembershipsSkipMissingGroup() throws Exception {
    Set<MembershipImpl> inherited = inheritedMemberships(new MembershipImpl("member", "root", "/does/not/exist", false));

    assertTrue(inherited.isEmpty());
  }

  public void testIsNestedInStopsOnCycle() throws Exception {
    storeCycleAInBAndBInA();
    Log log = replaceLog(groupDao);

    // A group outside the cycle is not reachable from it: false, one WARN naming the cycle
    assertFalse(isNestedIn(GROUP_OUTSIDE, GROUP_A));
    verify(log, times(1)).warn(contains("cycle"), eq(GROUP_OUTSIDE), eq(GROUP_A), eq(Set.of(GROUP_A, GROUP_B)));
  }

  public void testIsNestedInStillFindsGroupOnCycle() throws Exception {
    storeCycleAInBAndBInA();

    assertTrue(isNestedIn(GROUP_B, GROUP_A));
    assertTrue(isNestedIn(GROUP_A, GROUP_B));
  }

  public void testNoWarningOnAcyclicNesting() throws Exception {
    Log membershipLog = replaceLog(membershipDao);
    Log groupLog = replaceLog(groupDao);
    linkBInA();

    assertTrue(isNestedIn(GROUP_B, GROUP_A));
    assertFalse(isNestedIn(GROUP_A, GROUP_B));
    assertTrue(groupIds(inheritedMemberships(new MembershipImpl("member", "root", GROUP_B, false))).contains(GROUP_A));
    verify(membershipLog, times(0)).warn(anyString(), (Object[]) org.mockito.ArgumentMatchers.any());
    verify(groupLog, times(0)).warn(anyString(), (Object[]) org.mockito.ArgumentMatchers.any());
  }

  /**
   * D is nested in B (inherited type) and in C (fixed type {@code manager}),
   * B and C are nested in A, A in E. The walk reads the groups only, so the
   * memberships root already holds in the shared test database do not
   * matter. The groups above D are reached by two paths that yield two
   * different membership types: a walk that remembers a group across paths
   * instead of along the current one loses one of them.
   */
  public void testDiamondKeepsEveryPathOnAcyclicNesting() throws Exception {
    Log log = replaceLog(membershipDao);

    linkDiamond(DIAMOND_B, DIAMOND_D, NestedMembership.INHERIT_MEMBERSHIP_TYPE);
    linkDiamond(DIAMOND_C, DIAMOND_D, "manager");
    linkDiamond(DIAMOND_A, DIAMOND_B, NestedMembership.INHERIT_MEMBERSHIP_TYPE);
    linkDiamond(DIAMOND_A, DIAMOND_C, NestedMembership.INHERIT_MEMBERSHIP_TYPE);
    linkDiamond(DIAMOND_E, DIAMOND_A, NestedMembership.INHERIT_MEMBERSHIP_TYPE);

    Set<MembershipImpl> inherited = inheritedMemberships(new MembershipImpl("member", "root", DIAMOND_D, false));
    assertTrue(hasMembership(inherited, DIAMOND_A, "member"));
    assertTrue(hasMembership(inherited, DIAMOND_A, "manager"));
    assertTrue(hasMembership(inherited, DIAMOND_E, "member"));
    assertTrue(hasMembership(inherited, DIAMOND_E, "manager"));
    verify(log, times(0)).warn(anyString(), (Object[]) org.mockito.ArgumentMatchers.any());
  }

  private boolean hasMembership(Set<MembershipImpl> memberships, String groupId, String membershipType) {
    return memberships.stream().anyMatch(m -> m.getGroupId().equals(groupId) && m.getMembershipType().equals(membershipType));
  }

  private void linkDiamond(String groupId, String nestedGroupId, String membershipType) throws Exception {
    NestedMembership link = NestedMembership.builder()
                                            .groupId(groupId)
                                            .nestedGroupId(nestedGroupId)
                                            .membershipType(membershipType)
                                            .build();
    groupDao.linkGroups(link);
    diamondLinks.add(link);
  }

  private void linkBInA() throws Exception {
    groupDao.linkGroups(NestedMembership.builder().groupId(GROUP_A).nestedGroupId(GROUP_B).build());
    forwardLinkStored = true;
  }

  // Stores what linkGroups stores, without its circular dependency guard
  private void storeCycleAInBAndBInA() throws Exception {
    linkBInA();
    NestedMembership reverse = NestedMembership.builder()
                                               .groupId(GROUP_B)
                                               .nestedGroupId(GROUP_A)
                                               .nestedMembershipType(NestedMembership.ALL_MEMBERSHIP_TYPE)
                                               .membershipType(NestedMembership.INHERIT_MEMBERSHIP_TYPE)
                                               .build();
    storeLinks(GROUP_B, GROUP_A, new HashSet<>(Set.of(reverse)), new HashSet<>(Set.of(reverse)));
    reverseLinkStored = true;
  }

  private void storeLinks(String enclosingGroupId,
                          String nestedGroupId,
                          Set<NestedMembership> nestedMemberships,
                          Set<NestedMembership> enclosingMemberships) throws Exception {
    Method method = GroupDAOImpl.class.getDeclaredMethod("updateMembershipAttributes",
                                                         String.class,
                                                         String.class,
                                                         Set.class,
                                                         Set.class);
    method.setAccessible(true);
    try {
      method.invoke(groupDao, enclosingGroupId, nestedGroupId, nestedMemberships, enclosingMemberships);
    } finally {
      organizationService.flush();
    }
  }

  @SuppressWarnings("unchecked")
  private Set<MembershipImpl> inheritedMemberships(Membership membership) throws Exception {
    Method method = MembershipDAOImpl.class.getDeclaredMethod("getInheritedMemberships", Membership.class);
    method.setAccessible(true);
    return (Set<MembershipImpl>) method.invoke(membershipDao, membership);
  }

  private boolean isNestedIn(String parentGroupId, String memberGroupId) throws Exception {
    Method method = GroupDAOImpl.class.getDeclaredMethod("isNestedIn", String.class, String.class);
    method.setAccessible(true);
    return (boolean) method.invoke(groupDao, parentGroupId, memberGroupId);
  }

  private Set<String> groupIds(Set<MembershipImpl> memberships) {
    Set<String> groupIds = new HashSet<>();
    memberships.forEach(m -> groupIds.add(m.getGroupId()));
    return groupIds;
  }

  private Log replaceLog(AbstractDAOImpl dao) throws Exception {
    Log log = mock(Log.class);
    Field field = AbstractDAOImpl.class.getDeclaredField("log");
    field.setAccessible(true);
    field.set(dao, log);
    return log;
  }

}
