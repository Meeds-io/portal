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
package org.exoplatform.services.organization.externalstore;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.component.test.*;
import org.exoplatform.services.listener.*;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.organization.externalstore.model.IDMEntityType;
import org.exoplatform.services.organization.idm.MembershipDAOImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.organization.idm.externalstore.PicketLinkIDMExternalStoreService;

import exo.portal.component.identiy.opendsconfig.opends.OpenDSService;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-external-ldap-store-configuration.xml") })
public class TestPLIDMExternalStoreImportService extends AbstractKernelTest {

  private static final String                     NICKNAME_PARAM           = "user.name.nickName";

  private OpenDSService                           openDSService            = new OpenDSService(null);

  private PicketLinkIDMExternalStoreService       externalStoreService;

  private IDMExternalStoreImportService           externalStoreImportService;

  private IDMQueueService                         queueService;

  private ListenerService                         listenerService;

  private PicketLinkIDMOrganizationServiceImpl    organizationService;

  private AtomicInteger                           listenerDeleteUser       = new AtomicInteger(0);

  private AtomicInteger                           listenerModifyUser       = new AtomicInteger(0);

  private AtomicInteger                           listenerAddUser          = new AtomicInteger(0);

  private Listener<IDMExternalStoreService, User> userAddedListener        = new Listener<IDMExternalStoreService, User>() {
                                                                             @Override
                                                                             public void onEvent(Event<IDMExternalStoreService, User> event) throws Exception {
                                                                               assertNotNull(event);
                                                                               assertNotNull(event.getSource());

                                                                               User user = event.getData();
                                                                               assertNotNull(user);

                                                                               assertNotNull(organizationService.getUserHandler()
                                                                                                                .findUserByName(user.getUserName()));

                                                                               listenerAddUser.incrementAndGet();
                                                                             }
                                                                           };

  private Listener<IDMExternalStoreService, User> userDeleteListener       = new Listener<IDMExternalStoreService, User>() {
                                                                             @Override
                                                                             public void onEvent(Event<IDMExternalStoreService, User> event) throws Exception {
                                                                               assertNotNull(event);
                                                                               assertNotNull(event.getSource());

                                                                               User user = event.getData();
                                                                               assertNotNull(user);

                                                                               assertNull(organizationService.getUserHandler()
                                                                                                             .findUserByName(user.getUserName()));

                                                                               listenerDeleteUser.incrementAndGet();
                                                                             }
                                                                           };

  private Listener<IDMExternalStoreService, User> userModifiedListener     = new Listener<IDMExternalStoreService, User>() {
                                                                             @Override
                                                                             public void onEvent(Event<IDMExternalStoreService, User> event) throws Exception {
                                                                               assertNotNull(event);
                                                                               assertNotNull(event.getSource());

                                                                               User user = event.getData();
                                                                               assertNotNull(user);

                                                                               assertNotNull(organizationService.getUserHandler()
                                                                                                                .findUserByName(user.getUserName()));

                                                                               listenerModifyUser.incrementAndGet();
                                                                             }
                                                                           };

  private UserEventListener                       errorOnUserEventListener = new UserEventListener() {
                                                                             @SuppressWarnings("serial")
                                                                             @Override
                                                                             public void preSave(User user,
                                                                                                 boolean isNew) throws Exception {
                                                                               throw new Exception("Fake exception !") {
                                                                                                                                                          @Override
                                                                                                                                                          public void printStackTrace(PrintStream s) {
                                                                                                                                                          }

                                                                                                                                                          @Override
                                                                                                                                                          public void printStackTrace(PrintWriter s) {
                                                                                                                                                          }

                                                                                                                                                          @Override
                                                                                                                                                          public void printStackTrace() {
                                                                                                                                                          }
                                                                                                                                                        };
                                                                             }
                                                                           };

  @Override
  public void setUp() throws Exception {
    setForceContainerReload(true);

    externalStoreService =
                         (PicketLinkIDMExternalStoreService) getContainer().getComponentInstanceOfType(IDMExternalStoreService.class);
    listenerService = getContainer().getComponentInstanceOfType(ListenerService.class);
    organizationService =
                        (PicketLinkIDMOrganizationServiceImpl) getContainer().getComponentInstanceOfType(OrganizationService.class);
    queueService = getContainer().getComponentInstanceOfType(IDMQueueService.class);

    externalStoreImportService = getContainer().getComponentInstanceOfType(IDMExternalStoreImportService.class);

    listenerAddUser.set(0);
    listenerModifyUser.set(0);

    listenerService.addListener(IDMExternalStoreService.USER_DELETED_FROM_EXTERNAL_STORE, userDeleteListener);
    listenerService.addListener(IDMExternalStoreService.USER_ADDED_FROM_EXTERNAL_STORE, userAddedListener);
    listenerService.addListener(IDMExternalStoreService.USER_MODIFIED_FROM_EXTERNAL_STORE, userModifiedListener);

    begin();

    // Make sure data is valid after running previous tests classes
    resetData();

    // Make sure data processing (especially import to queue) is not done in the same second
    // than the data creation (users and groups), otherwise users and groups could be imported twice
    Thread.sleep(2000);
  }

  @Override
  public void tearDown() throws Exception {
    deleteData();

    end();
    super.tearDown();
  }

  private void resetData() throws Exception {
    deleteData();

    openDSService.initLDAPServer();
    openDSService.populateLDIFFile("ldap/ldap/initial-opends-external.ldif");

    externalStoreService.initializeGroupTree(externalStoreService.getReversedGroupTypeMappings());
  }

  private void deleteData() throws Exception {
    openDSService.cleanUpDN("dc=portal,dc=example,dc=com");
    removeAllUsers();
    removeGroupTree(null);

    queueService.setLastCheckedTime(IDMEntityType.USER, null);
    queueService.setLastCheckedTime(IDMEntityType.GROUP, null);

    organizationService.getUserHandler().removeUserEventListener(errorOnUserEventListener);
  }

  private void removeAllUsers() throws Exception {
    ListAccess<User> allusers = organizationService.getUserHandler().findAllUsers();
    User[] users = allusers.load(0, allusers.getSize());
    for (User user : users) {
      organizationService.getUserHandler().removeUser(user.getUserName(), true);
    }
  }

  @Override
  public void beforeRunBare() {
    try {
      openDSService.start();
      openDSService.initLDAPServer();
    } catch (Exception e) {
      log.error("Error in starting up OPENDS", e);
      e.printStackTrace();
    }
    super.beforeRunBare();
  }

  @SuppressWarnings({ "unchecked" })
  public void testImportGroups() throws Exception {
    Collection<Group> allGroups = organizationService.getGroupHandler().getAllGroups();
    assertEquals("Parent groups of LDAP Groups should exist on internal store", 2, allGroups.size());

    ListAccess<String> allGroupsListAccess = externalStoreService.getAllOfType(IDMEntityType.GROUP, null);
    String[] groupIds = allGroupsListAccess.load(0, 100);
    int initialExternalGroupSize = groupIds.length;
    assertEquals("8 LDAP groups should be detected", 8, initialExternalGroupSize);

    externalStoreImportService.importModifiedEntitiesOfTypeToQueue(IDMEntityType.GROUP);
    assertEquals("All detected groups on external store should be added to the IDM processing queue",
                 initialExternalGroupSize,
                 queueService.countAll());
    externalStoreImportService.processQueueEntries();
    assertEquals("The queue should be empty once processed", 0, queueService.countAll());

    allGroups = organizationService.getGroupHandler().getAllGroups();
    assertEquals("Internal stored groups should equals to External groups + 2 parent LDAP groups",
                 initialExternalGroupSize + 2,
                 allGroups.size());

    // Add '/role_hierarchy/Delta' group to LDAP
    openDSService.populateLDIFFile("ldap/ldap/test-group-modification-opends.ldif");
    // Wait 1 second until operation finishes on LDAP Store
    Thread.sleep(1000);

    try {
      assertNotNull("'/role_hierarchy/Delta' should have been imported on LDAP",
                    externalStoreService.getEntity(IDMEntityType.GROUP, "/role_hierarchy/Delta"));

      Collection<Membership> memberships =
                                         ((MembershipDAOImpl) organizationService.getMembershipHandler()).findMembershipsByGroupId("/role_hierarchy/Delta");
      int initialMembershipsSize = memberships.size();

      externalStoreImportService.importModifiedEntitiesOfTypeToQueue(IDMEntityType.GROUP);
      assertEquals("A new Group should be detected on external store.", 1, queueService.countAll());
      externalStoreImportService.processQueueEntries();
      assertEquals("The queue should be purged once the new group imported on internal store", 0, queueService.countAll());

      allGroups = organizationService.getGroupHandler().getAllGroups();
      assertEquals("Internal stored groups should equals to External groups + 2 parent LDAP groups + new added group",
                   initialExternalGroupSize + 3,
                   allGroups.size());

      Group group = organizationService.getGroupHandler().findGroupById("/role_hierarchy/Delta");
      assertNotNull("New added group should be detected", group);
      assertFalse("New added group should be recognized as external mapped group", group.isInternalStore());
      assertEquals("New added group should be recognized as external mapped group",
                   OrganizationService.EXTERNAL_STORE,
                   group.getOriginatingStore());

      memberships =
                  ((MembershipDAOImpl) organizationService.getMembershipHandler()).findMembershipsByGroupId("/role_hierarchy/Delta");
      assertEquals("New added group memberships should be imported too", initialMembershipsSize + 1, memberships.size());

      // Delete New Group imported membership
      openDSService.populateLDIFFile("ldap/ldap/test-group-member-modification-opends.ldif");
      // Wait 1 second until operation finishes on LDAP Store
      Thread.sleep(1000);

      externalStoreImportService.importModifiedEntitiesOfTypeToQueue(IDMEntityType.GROUP);
      assertEquals("Membership modification on Group should be detected", 1, queueService.countAll());
      externalStoreImportService.processQueueEntries();
      assertEquals("Queue must be purged once the membership entity processing is successfully done", 0, queueService.countAll());
      allGroups = organizationService.getGroupHandler().getAllGroups();
      assertEquals("The group size should be kept the same as before membership modification",
                   initialExternalGroupSize + 3,
                   allGroups.size());
      group = organizationService.getGroupHandler().findGroupById("/role_hierarchy/Delta");
      assertNotNull("The modified group '/role_hierarchy/Delta' should exist on internal store", group);
      memberships =
                  ((MembershipDAOImpl) organizationService.getMembershipHandler()).findMembershipsByGroupId("/role_hierarchy/Delta");
      assertEquals("The deleted membership from external store should be deleted from internal store too", 0, memberships.size());

      // Delete '/role_hierarchy/Delta' group
      openDSService.cleanUpDN("cn=Delta,ou=Roles,o=test,dc=portal,dc=example,dc=com");
      externalStoreImportService.checkEntitiesToDeleteIntoQueue(IDMEntityType.GROUP);
      assertEquals("The group deletion should be detected.", 1, queueService.countAll());
      externalStoreImportService.processQueueEntries();
      assertEquals("The queue must be purged once the group deletion is processed on internal store.",
                   0,
                   queueService.countAll());
      allGroups = organizationService.getGroupHandler().getAllGroups();
      assertEquals("The group size should be equal to initialGroupSize + 2 parent LDAP groups, since the group '/role_hierarchy/Delta' is deleted.",
                   initialExternalGroupSize + 2,
                   allGroups.size());
      group = organizationService.getGroupHandler().findGroupById("/role_hierarchy/Delta");
      assertNull("The group '/role_hierarchy/Delta' should have been deleted from internal store.", group);
    } finally {
      if (externalStoreService.getEntity(IDMEntityType.GROUP, "/role_hierarchy/Delta") != null) {
        openDSService.cleanUpDN("cn=Delta,ou=Roles,o=test,dc=portal,dc=example,dc=com");
      }
    }
  }

  @SuppressWarnings("unchecked")
  public void testImportUsers() throws Exception {
    assertEquals("Initial listenerAddUser invocation count must be 0", 0, listenerAddUser.get());
    assertEquals("Initial listenerModifyUser invocation count must be 0", 0, listenerModifyUser.get());
    assertEquals("Initial listenerDeleteUser invocation count must be 0", 0, listenerDeleteUser.get());

    // Make sure that an LDAP group is created to import user memberships
    createLDAPGroup("Admin");

    externalStoreImportService.importModifiedEntitiesOfTypeToQueue(IDMEntityType.USER);

    assertEquals("Queue size should be greater than 0 to import all users", 11, queueService.countAll());
    externalStoreImportService.processQueueEntries();
    assertEquals("Queue should be purged once processed.", 0, queueService.countAll());
    int initialUsersSize = organizationService.getUserHandler().findAllUsers().getSize();
    assertEquals("The listener 'user creation' should be triggered as many as external users count.",
                 initialUsersSize,
                 listenerAddUser.get());
    assertEquals("The listener 'user modification' shouldn't be triggered.", 0, listenerModifyUser.get());
    assertEquals("The listener 'user deletion' shouldn't be triggered.", 0, listenerDeleteUser.get());

    ListAccess<User> allUsersListAccess = organizationService.getUserHandler().findAllUsers();
    assertEquals("The internal created users count should be equal to external users count.",
                 initialUsersSize,
                 allUsersListAccess.getSize());
    assertEquals("The internal created users count should be equal to external users count.",
                 initialUsersSize,
                 allUsersListAccess.load(0, allUsersListAccess.getSize()).length);

    externalStoreImportService.importModifiedEntitiesOfTypeToQueue(IDMEntityType.USER);
    assertEquals("No new user should be detected to be imported on internal store", 0, queueService.countAll());
    externalStoreImportService.processQueueEntries();
    assertEquals("Queue must be empty", 0, queueService.countAll());
    assertEquals("The listener 'user creation' shouldn't be triggered again", initialUsersSize, listenerAddUser.get());
    assertEquals("The listener 'user modification' shouldn't be triggered", 0, listenerModifyUser.get());
    assertEquals("The listener 'user deletion' shouldn't be triggered", 0, listenerDeleteUser.get());

    // Test on added users in external store
    ListAccess<String> allExternalUsernames = externalStoreService.getAllOfType(IDMEntityType.USER, null);
    initialUsersSize = allExternalUsernames.load(0, Integer.MAX_VALUE).length;

    // Add users 'jduke10' and 'jduke11'. 'jduke10' is added to group
    // /role_hierarchy/Admin at the same time
    openDSService.populateLDIFFile("ldap/ldap/test-user-modification-opends.ldif");
    // Wait 1 second until operation finishes on LDAP Store
    Thread.sleep(1000);
    openDSService.populateLDIFFile("ldap/ldap/test-user-group-modification-opends.ldif");
    // Wait 1 second until operation finishes on LDAP Store
    Thread.sleep(1000);

    try {
      allExternalUsernames = externalStoreService.getAllOfType(IDMEntityType.USER, null);
      assertEquals("External users count should be (initialUsersSize + 2) after importing LDIF.",
                   initialUsersSize + 2,
                   allExternalUsernames.load(0, Integer.MAX_VALUE).length);

      Collection<Membership> memberships =
                                         ((MembershipDAOImpl) organizationService.getMembershipHandler()).findMembershipsByGroupId("/role_hierarchy/Admin");
      assertNotNull("'/role_hierarchy/Admin' should have memberships since LDIF is successfully imported", memberships);
      int initialMembershipsCount = memberships.size();

      assertNotNull("'jduke10' should have been imported on LDAP", externalStoreService.getEntity(IDMEntityType.USER, "jduke10"));
      assertNotNull("'jduke11' should have been imported on LDAP", externalStoreService.getEntity(IDMEntityType.USER, "jduke11"));

      externalStoreImportService.importModifiedEntitiesOfTypeToQueue(IDMEntityType.USER);
      assertEquals("Two new users should be detected as added on external store", 2, queueService.countAll());
      externalStoreImportService.processQueueEntries();
      assertEquals("Queue must be purged once the two new users are imported", 0, queueService.countAll());
      allUsersListAccess = organizationService.getUserHandler().findAllUsers();
      assertEquals("Internal users size should equal to initialUsersSize + two added users",
                   initialUsersSize + 2,
                   allUsersListAccess.getSize());
      assertEquals("Internal users size should equal to initialUsersSize + two added users",
                   initialUsersSize + 2,
                   allUsersListAccess.load(0, allUsersListAccess.getSize()).length);
      assertEquals("The listener 'user creation' should be triggered two more times",
                   initialUsersSize + 2,
                   listenerAddUser.get());
      assertEquals("The listener 'user modification' shouldn't be triggered since we created two new users only",
                   0,
                   listenerModifyUser.get());
      assertEquals("The listener 'user deletion' shouldn't be triggered since we created two new users only",
                   0,
                   listenerDeleteUser.get());
      UserProfile userProfile = organizationService.getUserProfileHandler().findUserProfileByName("jduke10");
      assertNotNull("The user 'jduke10' profile should have been imported", userProfile);
      assertEquals("The user 'jduke10' profile should have been imported with attribute '" + NICKNAME_PARAM + "'",
                   "Duke10",
                   userProfile.getAttribute(NICKNAME_PARAM));

      memberships = organizationService.getMembershipHandler().findMembershipsByUser("jduke10");
      assertNotNull("User 'jduke10' memberships should have been imported", memberships);
      assertEquals("User 'jduke10' single membership should have been imported", 1, memberships.size());
      memberships =
                  ((MembershipDAOImpl) organizationService.getMembershipHandler()).findMembershipsByGroupId("/role_hierarchy/Admin");
      assertNotNull("'/role_hierarchy/Admin' should have memberships since LDIF is successfully imported", memberships);
      assertEquals("User 'jduke10' membership should have been imported on group '/role_hierarchy/Admin'",
                   initialMembershipsCount + 1,
                   memberships.size());

      // Test on modified users in external store

      // Modify last name attribute of user 'jduke10' from 'Duke10' to 'Duke'
      openDSService.populateLDIFFile("ldap/ldap/test-user-attribute-modification-opends.ldif");
      User user = externalStoreService.getEntity(IDMEntityType.USER, "jduke10");
      assertEquals("User 'jduke10' should have been modified on external store", "Duke", user.getLastName());

      externalStoreImportService.importModifiedEntitiesOfTypeToQueue(IDMEntityType.USER);
      assertTrue("User 'jduke10' attribute modification should have been detected.", queueService.countAll() > 0);
      externalStoreImportService.processQueueEntries();
      assertEquals("Queue must be purged once 'jduke10' user modified", 0, queueService.countAll());
      allUsersListAccess = organizationService.getUserHandler().findAllUsers();
      assertEquals("User 'jduke10' modification import operation shouldn't modify internal users count",
                   initialUsersSize + 2,
                   allUsersListAccess.getSize());
      assertEquals("User 'jduke10' modification import operation shouldn't modify internal users count",
                   initialUsersSize + 2,
                   allUsersListAccess.load(0, allUsersListAccess.getSize()).length);
      user = organizationService.getUserHandler().findUserByName("jduke10");
      assertEquals("User 'jduke10' modification import operation should have modified user last name",
                   "Duke",
                   user.getLastName());
      assertEquals("The listener 'user creation' shouldn't be triggered once a modification is imported on internal store",
                   initialUsersSize + 2,
                   listenerAddUser.get());
      assertEquals("The listener 'user modification' should be triggered once a modification is imported on internal store",
                   1,
                   listenerModifyUser.get());
      assertEquals("The listener 'user deletion' shouldn't be triggered once a modification is imported on internal store",
                   0,
                   listenerDeleteUser.get());

      // Test on deleted users from external store
      openDSService.cleanUpDN("uid=jduke10,ou=People,o=test,dc=portal,dc=example,dc=com");
      openDSService.cleanUpDN("uid=jduke11,ou=People,o=test,dc=portal,dc=example,dc=com");
      // Wait 1 second until operation finishes on LDAP Store
      Thread.sleep(1000);

      externalStoreImportService.checkEntitiesToDeleteIntoQueue(IDMEntityType.USER);
      assertEquals("Users deletion should have been detected.", 2, queueService.countAll());
      externalStoreImportService.processQueueEntries();
      assertEquals("Queue must be purged once deletion is processed on internal store", 0, queueService.countAll());
      allUsersListAccess = organizationService.getUserHandler().findAllUsers();
      assertEquals("Internal users size should equals to initialUsersSize once two users are deleted",
                   initialUsersSize,
                   allUsersListAccess.getSize());
      assertEquals("Internal users size should equals to initialUsersSize once two users are deleted",
                   initialUsersSize,
                   allUsersListAccess.load(0, allUsersListAccess.getSize()).length);
      assertEquals("The listener 'user creation' shouldn't be triggered once a deletion is made on internal store",
                   initialUsersSize + 2,
                   listenerAddUser.get());
      assertEquals("The listener 'user modification' shouldn't be triggered once a deletion is made on internal store",
                   1,
                   listenerModifyUser.get());
      assertEquals("The listener 'user deletion' should be triggered once two deletions are made on internal store",
                   2,
                   listenerDeleteUser.get());
      userProfile = organizationService.getUserProfileHandler().findUserProfileByName("jduke10");
      assertNull("The user 'jduke10' profile should be deleted once the user is deleted from internal store", userProfile);
      memberships =
                  ((MembershipDAOImpl) organizationService.getMembershipHandler()).findMembershipsByGroupId("/role_hierarchy/Admin");
      assertNotNull(memberships);
      assertEquals("The user 'jduke10' membership in Group '/role_hierarchy/Admin' should have been deleted",
                   initialMembershipsCount,
                   memberships.size());



      // Add users 'jduke12' and 'jduke13'
      openDSService.populateLDIFFile("ldap/ldap/test-user-disable-opends.ldif");
      // Wait 1 second until operation finishes on LDAP Store
      Thread.sleep(1000);
      externalStoreImportService.importModifiedEntitiesOfTypeToQueue(IDMEntityType.USER);
      assertEquals("Users creation should have been detected.", 2, queueService.countAll());
      externalStoreImportService.processQueueEntries();
      allUsersListAccess = organizationService.getUserHandler().findAllUsers();

      assertEquals("Internal users size should equals to initialUsersSize because users were disabled and were not deleted",
              initialUsersSize + 2,
              allUsersListAccess.getSize());

      externalStoreImportService.setDeleteMissingEntriesFromInternalStore(false);
      // Test disable users when users deleted from external store
      openDSService.cleanUpDN("uid=jdukeX,ou=People,o=test,dc=portal,dc=example,dc=com");
      openDSService.cleanUpDN("uid=jdukeY,ou=People,o=test,dc=portal,dc=example,dc=com");
      // Wait 1 second until operation finishes on LDAP Store
      Thread.sleep(1000);
      externalStoreImportService.checkEntitiesToDeleteIntoQueue(IDMEntityType.USER);
      assertEquals("Users deletion should have been detected.", 2, queueService.countAll());
      externalStoreImportService.processQueueEntries();

      allUsersListAccess = organizationService.getUserHandler().findAllUsers(UserStatus.ANY);

      assertEquals("Internal users size should equals to initialUsersSize because users were disabled and were not deleted",
              initialUsersSize + 2,
              allUsersListAccess.getSize());
      assertNotNull(organizationService.getUserHandler().findUserByName("jdukeX", UserStatus.DISABLED));
      assertNotNull(organizationService.getUserHandler().findUserByName("jdukeY", UserStatus.DISABLED));

    } finally {
      if (externalStoreService.getEntity(IDMEntityType.USER, "jduke10") != null) {
        openDSService.cleanUpDN("uid=jduke10,ou=People,o=test,dc=portal,dc=example,dc=com");
      }
      if (externalStoreService.getEntity(IDMEntityType.USER, "jduke11") != null) {
        openDSService.cleanUpDN("uid=jduke11,ou=People,o=test,dc=portal,dc=example,dc=com");
      }
    }
  }
  
  public void testLargeImportUsers() throws Exception {
    assertEquals("Initial listenerAddUser invocation count must be 0", 0, listenerAddUser.get());
    assertEquals("Initial listenerModifyUser invocation count must be 0", 0, listenerModifyUser.get());
    assertEquals("Initial listenerDeleteUser invocation count must be 0", 0, listenerDeleteUser.get());
    
    // Make sure that an LDAP group is created to import user memberships
    createLDAPGroup("Admin");
    //import large number of user (more than page size configured to 10)
    openDSService.populateLDIFFile("ldap/ldap/test-100-users-opends.ldif");
    // Wait 10 second until operation finishes on LDAP Store
  
    Thread.sleep(2000);
    
    externalStoreImportService.importModifiedEntitiesOfTypeToQueue(IDMEntityType.USER);
    
    assertEquals("Queue size should be greater than 0 to import all users", 111, queueService.countAll());
    externalStoreImportService.processQueueEntries();
    assertEquals("Queue should be purged once processed.", 0, queueService.countAll());
    int initialUsersSize = organizationService.getUserHandler().findAllUsers().getSize();
    assertEquals("The listener 'user creation' should be triggered as many as external users count.",
                 initialUsersSize,
                 listenerAddUser.get());
    assertEquals("The listener 'user modification' shouldn't be triggered.", 0, listenerModifyUser.get());
    assertEquals("The listener 'user deletion' shouldn't be triggered.", 0, listenerDeleteUser.get());
    
    ListAccess<User> allUsersListAccess = organizationService.getUserHandler().findAllUsers();
    assertEquals("The internal created users count should be equal to external users count.",
                 initialUsersSize,
                 allUsersListAccess.getSize());
    assertEquals("The internal created users count should be equal to external users count.",
                 initialUsersSize,
                 allUsersListAccess.load(0, allUsersListAccess.getSize()).length);
    
  }

  private void removeGroupTree(Group group) throws Exception {
    Collection<Group> groups = organizationService.getGroupHandler().findGroups(group);
    if (groups != null && groups.size() > 0) {
      for (Group childgroup : groups) {
        removeGroupTree(childgroup);
      }
    }
    if (group != null) {
      organizationService.getGroupHandler().removeGroup(group, true);
    }
  }

  private void createLDAPGroup(String groupName) throws Exception {
    Group parentLDAPGroup = organizationService.getGroupHandler().findGroupById("/role_hierarchy");
    if (parentLDAPGroup == null) {
      fail("Parent LDAP Group /role_hierarchy wasn't created");
    }
    Group ldapGroup = organizationService.getGroupHandler().createGroupInstance();
    ldapGroup.setId("/role_hierarchy/" + groupName);
    ldapGroup.setGroupName(groupName);
    organizationService.getGroupHandler().addChild(parentLDAPGroup, ldapGroup, true);
  }

}
