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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.organization.idm.Config;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.organization.idm.cache.CacheableGroupHandlerImpl;
import org.exoplatform.services.organization.idm.cache.CacheableUserProfileHandlerImpl;
import org.exoplatform.services.organization.impl.UserProfileImpl;


@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration-new-user-listener.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/services/organization/TestOrganization-configuration.xml"),
})
public class TestOrganization extends AbstractKernelTest {

    protected static final String GROUP_1 = "testOrganization_group1";
    protected static final String GROUP_2 = "testOrganization_group2";
    protected static final String GROUP_3 = "testOrganization_group3";

    protected static final String USER_1 = "testOrganization_user1";
    protected static final String USER_2 = "testOrganization_user2";
    protected static final String USER_3 = "testOrganization_user3";
    protected static final String DEFAULT_PASSWORD = "defaultpassword";
    protected static final String DESCRIPTION = " Description";

    protected OrganizationService organizationService;

    protected UserHandler userHandler_;

    protected UserProfileHandler profileHandler_;

    protected GroupHandler groupHandler_;

    protected MembershipTypeHandler mtHandler_;

    protected MembershipHandler membershipHandler_;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        begin();
        PortalContainer container = getContainer();
        organizationService = (OrganizationService) container.getComponentInstance(OrganizationService.class);
        userHandler_ = organizationService.getUserHandler();
        profileHandler_ = organizationService.getUserProfileHandler();
        groupHandler_ = organizationService.getGroupHandler();
        mtHandler_ = organizationService.getMembershipTypeHandler();
        membershipHandler_ = organizationService.getMembershipHandler();

        resetData();

        createGroup(null, GROUP_1);
        createGroup(GROUP_1, GROUP_2);
        createGroup(GROUP_1, GROUP_3);

        createUser(USER_1, GROUP_1);
        createUser(USER_2, GROUP_2);
        createUser(USER_3, GROUP_1);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
          resetData();
        } finally {
          end();
          super.tearDown();
        }
    }

    public void testIDMConfiguration(){
        PicketLinkIDMOrganizationServiceImpl idmService = getContainer().getComponentInstanceOfType(PicketLinkIDMOrganizationServiceImpl.class);
        Config config =idmService.getConfiguration();
        assertTrue(config.isCountPaginatedUsers());
        assertFalse(config.isSkipPaginationInMembershipQuery());
    }

    public void testFindGroupNotFound() throws Exception {
        GroupHandler groupHander = organizationService.getGroupHandler();
        Group group = groupHander.findGroupById(GROUP_1 + "NOTFOUND");
        assertNull(group);
    }

    public void testFindGroupCaseInsensitive() throws Exception {
      GroupHandler groupHandler = organizationService.getGroupHandler();
      Group group = groupHandler.createGroupInstance();
      group.setGroupName("TOTO");
      group.setLabel("TOTO");
      groupHandler.addChild(null, group, true);

      group = groupHandler.createGroupInstance();
      group.setGroupName("toto");
      group.setLabel("toto");
      groupHandler.addChild(null, group, true);

      group = groupHandler.findGroupById("toto");
      assertNotNull(group);
      group = groupHandler.findGroupById("TOTO");
      assertNotNull(group);
      group = groupHandler.findGroupById("ToTO");
      assertNull(group);
    }

    public void testFindGroupAfterDelete() throws Exception {
        GroupHandler groupHander = organizationService.getGroupHandler();
        Collection<Group> rootGroups = groupHander.findGroups(null);
        int rootGroupsSize = rootGroups.size();

        String testGroupId = "TestGroupToDelete";
        createGroup(null, testGroupId);
        rootGroups = groupHander.findGroups(null);
        assertEquals(rootGroupsSize + 1, rootGroups.size());

        deleteGroup(testGroupId);
        rootGroups = groupHander.findGroups(null);
        assertEquals(rootGroupsSize, rootGroups.size());
    }

    public void testSaveAndFindGroupFromRoot() throws Exception {
        GroupHandler handler = organizationService.getGroupHandler();
        assertTrue(handler instanceof CacheableGroupHandlerImpl);

        Collection<?> allGroups = handler.findGroups(null);
        Group newGroup = handler.createGroupInstance();
        newGroup.setGroupName("abc");
        newGroup.setLabel("abc");
        handler.addChild(null, newGroup, true);

        allGroups = handler.findGroups(null);
        assertTrue(allGroups.size() > 0);
        boolean found = false;
        for (Object object : allGroups) {
          if (((Group) object).getId().equals("/abc")) {
            found = true;
          }
        }
        assertTrue(found);
    }

    public void testFindGroupFromRoot() throws Exception {
        GroupHandler handler = organizationService.getGroupHandler();
        Collection allGroups = handler.findGroups(null);
        assertTrue(allGroups.size() > 0);
    }

    public void testFindGroupById() throws Exception {
        GroupHandler groupHandler = organizationService.getGroupHandler();
        Group group = groupHandler.findGroupById(GROUP_1);
        assertNotNull(group);
        assertEquals(GROUP_1, group.getGroupName());
        assertEquals(GROUP_1 + DESCRIPTION, group.getDescription());

        group = groupHandler.findGroupById("/" + GROUP_1 + "/" + GROUP_3);
        assertNotNull(group);
        assertEquals(GROUP_3, group.getGroupName());
    }

    public void testFindGroupOfUser() {
        GroupHandler groupHandler = organizationService.getGroupHandler();
        try {
            Collection<Group> groups = groupHandler.findGroupsOfUser(USER_1);
            assertNotNull(groups);
            assertTrue(groups.size() >= 1);
        } catch (Exception e) {
            fail();
        }
    }

    public void testFindGroupsOfUserByKeyword() throws Exception {
        GroupHandler groupHandler = organizationService.getGroupHandler();
        List<String> excludedGroupsTypes = new ArrayList<>();
        Collection<Group> groups = groupHandler.findGroupsOfUserByKeyword("john","us",excludedGroupsTypes);
        assertNotNull(groups);
        assertEquals(1,groups.size());
        assertEquals(1,groupHandler.findGroupsOfUserByKeyword("john","ad",excludedGroupsTypes).size());
        assertEquals(1,groupHandler.findGroupsOfUserByKeyword("demo","us",excludedGroupsTypes).size());
        assertEquals(0,groupHandler.findGroupsOfUserByKeyword("demo","ad",excludedGroupsTypes).size());
        excludedGroupsTypes.add("platform_type");
        assertEquals(0,groupHandler.findGroupsOfUserByKeyword("john","ad",excludedGroupsTypes).size());
    }

    public void testFindAllGroupsByKeyword() throws Exception
    {
        GroupHandler groupHandler = organizationService.getGroupHandler();
        List<String> excludedGroupsTypes = new ArrayList<>();
        Collection<Group> groups = groupHandler.findAllGroupsByKeyword("us", excludedGroupsTypes);
        assertNotNull(groups);
        assertEquals(2, groups.size());
        assertEquals(1, groupHandler.findAllGroupsByKeyword("ad", excludedGroupsTypes).size());
        assertEquals(3, groupHandler.findAllGroupsByKeyword("test", excludedGroupsTypes).size());
        excludedGroupsTypes.add("root_type");
        assertEquals(1, groupHandler.findAllGroupsByKeyword("us", excludedGroupsTypes).size());
        assertEquals(2, groupHandler.findAllGroupsByKeyword("test", excludedGroupsTypes).size());
    }

    public void testConsistencyMembershipListAccess() throws Exception {
        GroupHandler groupHandler = organizationService.getGroupHandler();
        Group group = groupHandler.findGroupById(GROUP_1);
        User testUser = userHandler_.findUserByName("root");

        MembershipType mt = mtHandler_.findMembershipType("test");
        if (mt == null) {
            mt = mtHandler_.createMembershipTypeInstance();
            mt.setName("test");
            mtHandler_.createMembershipType(mt, true);
        }
        membershipHandler_.linkMembership(testUser, group, mt, true);


        ListAccess<Membership> listAccess = membershipHandler_.findAllMembershipsByGroup(group);
        assertEquals(1, listAccess.getSize());
        try {
            Membership[] mbs = listAccess.load(0, 3);
        } catch (Exception e) {
            fail();
        }
    }

    public void testFindUserByGroup() throws Exception {
        GroupHandler groupHandler = organizationService.getGroupHandler();
        Group group = groupHandler.findGroupById(GROUP_1);
        UserHandler uHandler = organizationService.getUserHandler();
        PageList users = uHandler.findUsersByGroup("/platform/administrators");
        assertTrue(users.getAvailable() > 0);

        List iterator = users.getAll();
        for (Object test : iterator) {
            User a = (User) test;
        }
    }

    public void testDisplayName() throws Exception {
        UserHandler uHandler = organizationService.getUserHandler();
        User john = uHandler.findUserByName("john");

        assertNotNull(john);

        // Test that fullName is working correctly for "john"
        assertEquals("John Anthony", john.getFullName());
        john.setFullName("Johnny Something");
        uHandler.saveUser(john, false);
        john = uHandler.findUserByName("john");
        assertEquals("Johnny Something", john.getFullName());

        // Now delete fullName and assert that it's "firstName lastName"
        john.setFullName(null);
        uHandler.saveUser(john, false);
        john = uHandler.findUserByName("john");
        assertEquals("John Anthony", john.getFullName());
    }

    public void testCreateDuplicateMembershipType() throws Exception {
        /* Create a membershipType */
        String testType = "testCreateDuplicateMembershipType";
        MembershipType mt = mtHandler_.createMembershipTypeInstance();
        mt.setName(testType);
        mt.setDescription("This is a test");
        mt.setOwner("exo");

        MembershipType mt1 = mtHandler_.createMembershipTypeInstance();
        mt1.setName(testType);
        mt1.setDescription("a duplicate");
        mt1.setOwner("exo1");

        try {
            mtHandler_.createMembershipType(mt, true);
            assertEquals("Expect mebershiptype is:", testType, mtHandler_.findMembershipType(testType).getName());
            mtHandler_.createMembershipType(mt1, true);
            fail("Exception should be thrown");
        } catch (Exception ex) {
          // Expected
        }
        MembershipType membershipType = mtHandler_.findMembershipType(mt.getName());
        assertNotNull("Membership type " + testType + " must be exist", membershipType);
        assertEquals("Expect mebershiptype is:", testType, mtHandler_.findMembershipType(testType).getName());
    }

    public void testFindUserProfile() throws Exception {
        // Given
        UserProfileHandler userProfileHandler = organizationService.getUserProfileHandler();
        UserProfile userProfile = new UserProfileImpl(USER_1);
        userProfile.setAttribute("user.employer", "eXo");
        userProfileHandler.saveUserProfile(userProfile, false);
        if (userProfileHandler instanceof CacheableUserProfileHandlerImpl) {
            ((CacheableUserProfileHandlerImpl) userProfileHandler).clearCache();
        }

        // When
        UserProfile fetchedUserProfile = userProfileHandler.findUserProfileByName(USER_1);

        // Then
        assertNotNull(fetchedUserProfile);
        assertEquals(USER_1, fetchedUserProfile.getUserName());
    }

    public void testNotFindUserProfile() throws Exception {
        // Given
        UserProfileHandler userProfileHandler = organizationService.getUserProfileHandler();
        if (userProfileHandler instanceof CacheableUserProfileHandlerImpl) {
            ((CacheableUserProfileHandlerImpl) userProfileHandler).clearCache();
        }

        // When
        UserProfile userProfile = userProfileHandler.findUserProfileByName("not_existing_user");

        // Then
        assertNull(userProfile);
    }

    public void testCreateValidMembershiptype() throws Exception {
      //Given
      String testType = "testType";
      String description ="this a long description!this a long description!this a long description!this a long description!this a long description!this a long description!this a long description!this a long description!this a long description!this a long description!this a long description!";
      MembershipType mt = mtHandler_.createMembershipTypeInstance();
      mt.setDescription(description);
      mt.setName(testType);
      MembershipType mt1 = null;
      //When
      try {
        mt1 = mtHandler_.createMembershipType(mt, false);
        fail("Exception should be thrown");
      }
      catch (Exception ex) {
          assertEquals(ex.getMessage(),"The membership type description field cannot exceed 255 characters");
      }
      //Then
      assertNull(mt1);
    }

    protected void createGroup(String parent, String name) {
        restartTransaction();
        GroupHandler groupHandler = organizationService.getGroupHandler();
        try {
            Group parentGroup = null;
            if (parent != null) {
                parentGroup = groupHandler.findGroupById(parent);
            }
            Group newGroup = groupHandler.createGroupInstance();
            newGroup.setGroupName(name);
            newGroup.setDescription(name + DESCRIPTION);
            newGroup.setLabel(name);
            groupHandler.addChild(parentGroup, newGroup, true);
        } catch (Exception e) {
            fail("Error on create group [" + name + "] " + e.getMessage(), e);
        }
    }

    private void deleteGroup(String name) {
        restartTransaction();
        GroupHandler groupHandler = organizationService.getGroupHandler();
        try {
            Group group = groupHandler.findGroupById(name);
            if(group == null) {
              log.warn("Cannot find group with id '" + name + "'");
            } else {
              Collection<Group> groups = groupHandler.findGroups(group);
              for (Group childGroup : groups) {
                groupHandler.removeGroup(childGroup, true);
              }
              groupHandler.removeGroup(group, true);
            }
        } catch (Exception e) {
          log.error("Error while deleting group", e);
        }
    }

    protected void createUser(String username, String... groups) throws Exception {
        restartTransaction();
        UserHandler userHandler = organizationService.getUserHandler();
        User user = userHandler.createUserInstance(username);
        user.setPassword(DEFAULT_PASSWORD);
        user.setFirstName("default");
        user.setLastName("default");
        user.setEmail(username + "@exoportal.org");
        if (groups.length > 0) {
            user.setOrganizationId(groups[0]);            
        }

        userHandler.createUser(user, true);
    }

    protected void deleteUser(String username) throws Exception {
      restartTransaction();
      UserHandler userHandler = organizationService.getUserHandler();
      if (userHandler.findUserByName(username) != null) {
        userHandler.removeUser(username, true);
      }
    }

    private void resetData() throws Exception {
      deleteGroup("/" + GROUP_1);

      deleteUser(USER_1);
      deleteUser(USER_2);
      deleteUser(USER_3);
    }

}
