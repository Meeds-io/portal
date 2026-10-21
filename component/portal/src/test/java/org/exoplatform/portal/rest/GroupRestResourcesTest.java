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

import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.rest.model.GroupRestEntity;
import org.exoplatform.portal.rest.services.BaseRestServicesTestCase;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.NestedMembership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.search.GroupSearchService;
import org.exoplatform.services.rest.impl.ContainerResponse;

public class GroupRestResourcesTest extends BaseRestServicesTestCase {

  private static final String PARENT_GROUP_ID = "/company";

  private static final String ADMIN           = "admin";

  private OrganizationService organizationService;

  private GroupHandler        groupHandler;

  private GroupSearchService  groupSearchService;

  private MembershipHandler   membershipHandler;

  private UserACL             userACL;

  protected Class<?> getComponentClass() {
    return GroupRestResourcesV1.class;
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

    organizationService = mock(OrganizationService.class);
    groupHandler = mock(GroupHandler.class);
    groupSearchService = mock(GroupSearchService.class);
    membershipHandler = mock(MembershipHandler.class);
    userACL = mock(UserACL.class);
    when(organizationService.getGroupHandler()).thenReturn(groupHandler);
    when(organizationService.getMembershipHandler()).thenReturn(membershipHandler);

    getContainer().unregisterComponent(OrganizationService.class);
    getContainer().unregisterComponent(GroupSearchService.class);
    getContainer().unregisterComponent(UserACL.class);
    getContainer().registerComponentInstance("org.exoplatform.services.organization.OrganizationService", organizationService);
    getContainer().registerComponentInstance("org.exoplatform.services.organization.search.GroupSearchService",
                                             groupSearchService);
    getContainer().registerComponentInstance("org.exoplatform.portal.config.UserACL", userACL);
  }

  @Override
  public void tearDown() throws Exception {
    getContainer().unregisterComponent("org.exoplatform.services.organization.OrganizationService");
    getContainer().unregisterComponent("org.exoplatform.services.organization.search.GroupSearchService");
    getContainer().unregisterComponent("org.exoplatform.portal.config.UserACL");
    super.tearDown();
  }

  public void testGetGroups() throws Exception {
    startUserSession("root");
    Group sales = mockGroup("/company/sales");
    Group it = mockGroup("/company/it");
    mockGroupSearch(sales, it);

    ContainerResponse resp = launcher.service("GET", "/v1/groups?q=comp&returnSize=true", "", null, null, null);

    assertEquals(200, resp.getStatus());
    @SuppressWarnings("unchecked")
    CollectionEntity<GroupRestEntity> collection = (CollectionEntity<GroupRestEntity>) resp.getEntity();
    assertEquals(2, collection.getEntities().size());
    assertEquals(2, collection.getSize());
  }

  public void testGetGroupsAsTree() throws Exception {
    startUserSession("root");
    Group parent = mockGroup(PARENT_GROUP_ID);
    Group sales = mockGroup("/company/sales");
    when(sales.getParentId()).thenReturn(PARENT_GROUP_ID);
    mockGroupSearch(parent, sales);

    ContainerResponse resp = launcher.service("GET", "/v1/groups?q=comp&tree=true", "", null, null, null);

    assertEquals(200, resp.getStatus());
    @SuppressWarnings("unchecked")
    CollectionEntity<GroupRestEntity> collection = (CollectionEntity<GroupRestEntity>) resp.getEntity();
    // The child group is attached under its parent instead of being listed at root
    assertEquals(1, collection.getEntities().size());
    GroupRestEntity root = collection.getEntities().get(0);
    assertEquals(PARENT_GROUP_ID, root.getId());
    assertEquals(1, root.getChildren().size());
    assertEquals("/company/sales", root.getChildren().get(0).getId());
  }

  public void testGetNestedGroupsMandatoryGroupId() throws Exception {
    startUserSession("root");

    ContainerResponse resp = launcher.service("GET", "/v1/groups/nested", "", null, null, null);

    assertEquals(400, resp.getStatus());
  }

  public void testGetNestedGroupsParentGroupNotFound() throws Exception {
    startUserSession("root");
    when(groupHandler.findGroupById(PARENT_GROUP_ID)).thenReturn(null);

    ContainerResponse resp = launcher.service("GET", "/v1/groups/nested?groupId=" + PARENT_GROUP_ID, "", null, null, null);

    assertEquals(404, resp.getStatus());
  }

  public void testGetNestedGroupsSortedWithSize() throws Exception {
    startUserSession("root");
    mockGroup(PARENT_GROUP_ID);
    mockGroup("/company/sales");
    mockGroup("/company/it");
    mockGroup("/marketing/design");
    Set<NestedMembership> nestedMemberships = new LinkedHashSet<>();
    nestedMemberships.add(new NestedMembership(null, PARENT_GROUP_ID, null, "/company/sales"));
    nestedMemberships.add(new NestedMembership(null, PARENT_GROUP_ID, null, "/company/it"));
    nestedMemberships.add(new NestedMembership(null, PARENT_GROUP_ID, null, "/marketing/design"));
    when(groupHandler.getNestedMemberships(PARENT_GROUP_ID)).thenReturn(nestedMemberships);

    ContainerResponse resp = launcher.service("GET",
                                              "/v1/groups/nested?groupId=" + PARENT_GROUP_ID + "&returnSize=true",
                                              "",
                                              null,
                                              null,
                                              null);

    assertEquals(200, resp.getStatus());
    @SuppressWarnings("unchecked")
    CollectionEntity<GroupRestEntity> collection = (CollectionEntity<GroupRestEntity>) resp.getEntity();
    assertEquals(3, collection.getSize());
    assertEquals(3, collection.getEntities().size());
    // Nested group ids are sorted case-insensitively
    assertEquals("/company/it", collection.getEntities().get(0).getId());
    assertEquals("/company/sales", collection.getEntities().get(1).getId());
    assertEquals("/marketing/design", collection.getEntities().get(2).getId());
  }

  public void testGetNestedGroupsPaginated() throws Exception {
    startUserSession("root");
    mockGroup(PARENT_GROUP_ID);
    mockGroup("/company/sales");
    mockGroup("/company/it");
    Set<NestedMembership> nestedMemberships = new LinkedHashSet<>();
    nestedMemberships.add(new NestedMembership(null, PARENT_GROUP_ID, null, "/company/sales"));
    nestedMemberships.add(new NestedMembership(null, PARENT_GROUP_ID, null, "/company/it"));
    when(groupHandler.getNestedMemberships(PARENT_GROUP_ID)).thenReturn(nestedMemberships);

    ContainerResponse resp = launcher.service("GET",
                                              "/v1/groups/nested?groupId=" + PARENT_GROUP_ID + "&offset=1&limit=1",
                                              "",
                                              null,
                                              null,
                                              null);

    assertEquals(200, resp.getStatus());
    @SuppressWarnings("unchecked")
    CollectionEntity<GroupRestEntity> collection = (CollectionEntity<GroupRestEntity>) resp.getEntity();
    // Size not returned when returnSize is false
    assertEquals(0, collection.getSize());
    assertEquals(1, collection.getEntities().size());
    assertEquals("/company/sales", collection.getEntities().get(0).getId());
  }

  private Group mockGroup(String groupId) throws Exception {
    Group group = mock(Group.class);
    when(group.getId()).thenReturn(groupId);
    when(groupHandler.findGroupById(groupId)).thenReturn(group);
    return group;
  }

  private void mockGroupSearch(Group... groups) throws Exception {
    ListAccess<Group> listAccess = new ListAccess<Group>() {
      public Group[] load(int index, int length) {
        return Arrays.copyOfRange(groups, index, Math.min(index + length, groups.length));
      }

      public int getSize() {
        return groups.length;
      }
    };
    when(groupSearchService.searchGroups("comp")).thenReturn(listAccess);
  }


  public void testCreateGroupRequiresConfirmedSession() throws Exception {
    startUserSession(ADMIN);
    ContainerResponse response = getResponse("POST", "/v1/groups", "{\"groupName\":\"\"}", unconfirmedContext(ADMIN));
    assertOtpRequired(response);
    response = getResponse("POST", "/v1/groups", "{\"groupName\":\"\"}", confirmedContext(ADMIN));
    assertEquals(400, response.getStatus());
    assertEquals("NAME:MANDATORY", response.getEntity());
    verify(groupHandler, never()).addChild(any(), any(), anyBoolean());
  }

  public void testUpdateGroupRequiresConfirmedSession() throws Exception {
    startUserSession(ADMIN);
    String body = "{\"id\":\"/unknown\",\"groupName\":\"unknown\"}";
    ContainerResponse response = getResponse("PUT", "/v1/groups", body, unconfirmedContext(ADMIN));
    assertOtpRequired(response);
    verify(groupHandler, never()).findGroupById(anyString());
    response = getResponse("PUT", "/v1/groups", body, confirmedContext(ADMIN));
    assertEquals(404, response.getStatus());
    verify(groupHandler, never()).updateGroup(any(), anyBoolean());
  }

  public void testDeleteGroupRequiresConfirmedSession() throws Exception {
    startUserSession(ADMIN);
    ContainerResponse response = launcher.service("DELETE", "/v1/groups?groupId=/unknown", "", null, null, unconfirmedContext(ADMIN));
    assertOtpRequired(response);
    verify(groupHandler, never()).findGroupById(anyString());
    response = launcher.service("DELETE", "/v1/groups?groupId=/unknown", "", null, null, confirmedContext(ADMIN));
    assertEquals(404, response.getStatus());
    verify(groupHandler, never()).removeGroup(any(), anyBoolean());
  }

  public void testCreateMembershipRequiresConfirmedSessionAfterPermissionCheck() throws Exception {
    startUserSession(ADMIN);
    String body = "{\"groupId\":\"" + PARENT_GROUP_ID + "\",\"membershipType\":\"member\"}";

    // Not allowed on the group: today's answer, without the confirmation code
    ContainerResponse response = getResponse("POST", "/v1/groups/memberships", body, unconfirmedContext(ADMIN));
    assertEquals(403, response.getStatus());
    assertNotEquals(UserAccountConfirmation.OTP_REQUIRED, response.getEntity());

    when(userACL.isAdministrator(any())).thenReturn(true);
    response = getResponse("POST", "/v1/groups/memberships", body, unconfirmedContext(ADMIN));
    assertOtpRequired(response);
    verify(membershipHandler, never()).findMembership(any());

    response = getResponse("POST", "/v1/groups/memberships", body, confirmedContext(ADMIN));
    assertEquals(400, response.getStatus());
    assertEquals("USER:MANDATORY", response.getEntity());
    verify(membershipHandler, never()).linkMembership(any(), any(), any(), anyBoolean());
  }

  public void testCreateMultipleMembershipsRequiresConfirmedSession() throws Exception {
    startUserSession(ADMIN);
    String body = "[{\"groupId\":\"\"}]";
    ContainerResponse response = getResponse("POST", "/v1/groups/memberships/bulk", body, unconfirmedContext(ADMIN));
    assertOtpRequired(response);
    response = getResponse("POST", "/v1/groups/memberships/bulk", body, confirmedContext(ADMIN));
    assertEquals(400, response.getStatus());
    assertEquals("GROUP_ID:MANDATORY", response.getEntity());
    verify(membershipHandler, never()).linkMembership(any(), any(), any(), anyBoolean());
  }

  public void testUpdateMembershipRequiresConfirmedSession() throws Exception {
    startUserSession(ADMIN);
    String body = "{\"groupId\":\"" + PARENT_GROUP_ID + "\"}";
    ContainerResponse response = getResponse("PUT", "/v1/groups/memberships", body, unconfirmedContext(ADMIN));
    assertOtpRequired(response);
    response = getResponse("PUT", "/v1/groups/memberships", body, confirmedContext(ADMIN));
    assertEquals(400, response.getStatus());
    verify(membershipHandler, never()).findMembership(any());
    verify(membershipHandler, never()).removeMembership(any(), anyBoolean());
  }

  public void testDeleteMembershipRequiresConfirmedSessionAfterPermissionCheck() throws Exception {
    startUserSession(ADMIN);
    String path = "/v1/groups/memberships?membershipId=member:john:" + PARENT_GROUP_ID;

    ContainerResponse response = launcher.service("DELETE", path, "", null, null, unconfirmedContext(ADMIN));
    assertEquals(403, response.getStatus());
    assertNotEquals(UserAccountConfirmation.OTP_REQUIRED, response.getEntity());

    when(userACL.isAdministrator(any())).thenReturn(true);
    response = launcher.service("DELETE", path, "", null, null, unconfirmedContext(ADMIN));
    assertOtpRequired(response);
    verify(membershipHandler, never()).findMembership(any());

    response = launcher.service("DELETE", path, "", null, null, confirmedContext(ADMIN));
    assertEquals(404, response.getStatus());
    verify(membershipHandler, never()).removeMembership(any(), anyBoolean());
  }

  private void assertOtpRequired(ContainerResponse response) {
    assertEquals(403, response.getStatus());
    assertEquals(UserAccountConfirmation.OTP_REQUIRED, response.getEntity());
  }

}
