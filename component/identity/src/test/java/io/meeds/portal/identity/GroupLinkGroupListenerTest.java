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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.NestedMembership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.security.ConversationRegistry;

import io.meeds.portal.identity.listener.GroupLinkGroupListener;

/**
 * Linking or unlinking a group invalidates, on every node, the conversations
 * of the group's members read from the store, whichever node they signed in
 * through.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class GroupLinkGroupListenerTest {

  private static final String    GROUP_ID = "/organization/management/executive-board";

  @Mock
  private ConversationRegistry   conversationRegistry;

  @Mock(extraInterfaces = ComponentRequestLifecycle.class)
  private OrganizationService    organizationService;

  private GroupHandler           groupHandler;

  @InjectMocks
  private GroupLinkGroupListener listener;

  @Before
  public void setUp() throws Exception {
    groupHandler = stubMembers(organizationService);
  }

  /**
   * On the IDM store, the members are invalidated now and once more after
   * the link commits: a node that read the memberships before the commit
   * builds the conversation again
   */
  @Test
  public void testALinkInvalidatesTheMembersAgainAfterTheCommit() throws Exception {
    PicketLinkIDMOrganizationServiceImpl idmOrganizationService = mock(PicketLinkIDMOrganizationServiceImpl.class);
    stubMembers(idmOrganizationService);
    GroupLinkGroupListener idmListener = new GroupLinkGroupListener();
    setField(idmListener, "organizationService", idmOrganizationService);
    setField(idmListener, "conversationRegistry", conversationRegistry);

    idmListener.linkGroups(NestedMembership.builder().nestedGroupId(GROUP_ID).nestedMembershipType("member").build());

    ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);
    verify(idmOrganizationService).runNowAndAfterTransaction(action.capture());
    action.getValue().run();
    verify(conversationRegistry, timeout(1000)).invalidateUser("john");
  }

  @Test
  public void testAMembersLinkInvalidatesTheMembersOfThatType() throws Exception {
    listener.linkGroups(NestedMembership.builder().nestedGroupId(GROUP_ID).nestedMembershipType("member").build());

    verify(conversationRegistry, timeout(1000)).invalidateUser("john");
    verify(conversationRegistry, never()).invalidateUser("mary");
  }

  @Test
  public void testAnUnlinkOfAllTypesInvalidatesEveryMember() throws Exception {
    listener.unlinkGroups(NestedMembership.builder()
                                          .nestedGroupId(GROUP_ID)
                                          .nestedMembershipType(NestedMembership.ALL_MEMBERSHIP_TYPE)
                                          .build());

    verify(conversationRegistry, timeout(1000)).invalidateUser("john");
    verify(conversationRegistry, timeout(1000)).invalidateUser("mary");
  }

  /**
   * The members of a group linked into the linked group, at any depth, hold
   * no stored membership in it and inherit the link: they are invalidated,
   * whatever their membership type
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testTheMembersOfTheGroupsLinkedIntoItAreInvalidatedToo() throws Exception {
    String childId = "/organization/management/executive-board/assistants";
    String grandChildId = childId + "/interns";
    when(groupHandler.getNestedMemberships(GROUP_ID)).thenReturn(Set.of(NestedMembership.builder()
                                                                                       .groupId(GROUP_ID)
                                                                                       .nestedGroupId(childId)
                                                                                       .build()));
    when(groupHandler.getNestedMemberships(childId)).thenReturn(Set.of(NestedMembership.builder()
                                                                                       .groupId(childId)
                                                                                       .nestedGroupId(grandChildId)
                                                                                       .build(),
                                                                       NestedMembership.builder()
                                                                                       .groupId(childId)
                                                                                       .nestedGroupId(GROUP_ID)
                                                                                       .build()));
    for (String[] groupAndUser : new String[][] { { childId, "carl" }, { grandChildId, "ines" } }) {
      Group linked = mock(Group.class);
      when(linked.getId()).thenReturn(groupAndUser[0]);
      when(groupHandler.findGroupById(groupAndUser[0])).thenReturn(linked);
      ListAccess<Membership> memberships = mock(ListAccess.class);
      when(organizationService.getMembershipHandler().findAllMembershipsByGroup(linked)).thenReturn(memberships);
      Membership member = membership(groupAndUser[1], "manager");
      when(memberships.getSize()).thenReturn(1);
      when(memberships.load(0, 1)).thenReturn(new Membership[] { member });
    }

    listener.linkGroups(NestedMembership.builder().nestedGroupId(GROUP_ID).nestedMembershipType("member").build());

    verify(conversationRegistry, timeout(1000)).invalidateUser("carl");
    verify(conversationRegistry, timeout(1000)).invalidateUser("ines");
    verify(conversationRegistry, timeout(1000)).invalidateUser("john");
    verify(conversationRegistry, after(300).never()).invalidateUser("mary");
  }

  @Test
  public void testAnUnknownGroupInvalidatesNobody() throws Exception {
    listener.linkGroups(NestedMembership.builder().nestedGroupId("/unknown").nestedMembershipType("member").build());

    verify(conversationRegistry, after(300).never()).invalidateUser(anyString());
  }

  /**
   * The listener runs the read on a thread that holds no request: it opens
   * one of its own around the read of the group's members, and closes it
   */
  @Test
  public void testTheMembersAreReadInARequestOfTheirOwn() throws Exception {
    listener.linkGroups(NestedMembership.builder().nestedGroupId(GROUP_ID).nestedMembershipType("member").build());

    verify(conversationRegistry, timeout(1000)).invalidateUser("john");
    ComponentRequestLifecycle lifecycle = (ComponentRequestLifecycle) organizationService;
    InOrder order = inOrder(lifecycle, groupHandler);
    order.verify(lifecycle).startRequest(any());
    order.verify(groupHandler).findGroupById(GROUP_ID);
    order.verify(lifecycle).endRequest(any());
  }

  @SuppressWarnings("unchecked")
  private GroupHandler stubMembers(OrganizationService service) throws Exception {
    Group group = mock(Group.class);
    GroupHandler handler = mock(GroupHandler.class);
    when(service.getGroupHandler()).thenReturn(handler);
    when(group.getId()).thenReturn(GROUP_ID);
    when(handler.findGroupById(GROUP_ID)).thenReturn(group);
    MembershipHandler membershipHandler = mock(MembershipHandler.class);
    when(service.getMembershipHandler()).thenReturn(membershipHandler);
    ListAccess<Membership> memberships = mock(ListAccess.class);
    when(membershipHandler.findAllMembershipsByGroup(group)).thenReturn(memberships);
    Membership john = membership("john", "member");
    Membership mary = membership("mary", "manager");
    when(memberships.getSize()).thenReturn(2);
    when(memberships.load(0, 2)).thenReturn(new Membership[] { john, mary });
    return handler;
  }

  private void setField(Object target, String name, Object value) throws Exception {
    java.lang.reflect.Field field = GroupLinkGroupListener.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private Membership membership(String userName, String type) {
    Membership membership = mock(Membership.class);
    org.mockito.Mockito.lenient().when(membership.getUserName()).thenReturn(userName);
    org.mockito.Mockito.lenient().when(membership.getMembershipType()).thenReturn(type);
    return membership;
  }

}
