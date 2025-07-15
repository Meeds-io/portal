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
import java.time.*;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.gatein.portal.idm.impl.repository.ExoFallbackIdentityStoreRepository;
import org.gatein.portal.idm.impl.store.hibernate.ExoHibernateIdentityStoreImpl;
import org.picketlink.idm.impl.store.ldap.ExoLDAPIdentityStoreImpl;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.component.test.*;
import org.exoplatform.services.listener.*;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.organization.externalstore.model.IDMEntityType;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.organization.idm.externalstore.PicketLinkIDMExternalStoreService;

import exo.portal.component.identiy.opendsconfig.opends.OpenDSService;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-external-ldap-store-configuration.xml") })
public class TestPLIDMExternalStoreService extends AbstractKernelTest {

  private static final String                     NICKNAME_PARAM           = "user.name.nickName";

  private OpenDSService                           openDSService            = new OpenDSService(null);

  private PicketLinkIDMExternalStoreService       externalStoreService;

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

  public void setUp() throws Exception {
    setForceContainerReload(true);

    externalStoreService =
                         (PicketLinkIDMExternalStoreService) getContainer().getComponentInstanceOfType(IDMExternalStoreService.class);
    listenerService = getContainer().getComponentInstanceOfType(ListenerService.class);
    organizationService =
                        (PicketLinkIDMOrganizationServiceImpl) getContainer().getComponentInstanceOfType(OrganizationService.class);

    listenerAddUser.set(0);
    listenerModifyUser.set(0);

    listenerService.addListener(IDMExternalStoreService.USER_DELETED_FROM_EXTERNAL_STORE, userDeleteListener);
    listenerService.addListener(IDMExternalStoreService.USER_ADDED_FROM_EXTERNAL_STORE, userAddedListener);
    listenerService.addListener(IDMExternalStoreService.USER_MODIFIED_FROM_EXTERNAL_STORE, userModifiedListener);

    begin();

    // Make sure data is valid after running previous tests classes
    removeAllUsers();
    removeGroupTree(null);

    externalStoreService.initializeGroupTree(externalStoreService.getReversedGroupTypeMappings());
  }

  @Override
  public void tearDown() throws Exception {
    removeAllUsers();
    organizationService.getUserHandler().removeUserEventListener(errorOnUserEventListener);

    removeGroupTree(null);

    end();
    super.tearDown();
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
      openDSService.populateLDIFFile("ldap/ldap/initial-opends-external.ldif");
    } catch (Exception e) {
      log.error("Error in starting up OPENDS", e);
      e.printStackTrace();
    }
    super.beforeRunBare();
  }

  public void testConfiguration() throws Exception {
    assertTrue("External Store is not enabled", externalStoreService.isEnabled());
    assertTrue("External Store doesn't update user on login", externalStoreService.isUpdateInformationOnLogin());
    assertNotNull("External Store doesn't have a managed entity types", externalStoreService.getManagedEntityTypes());
    assertEquals("Incoherent managed types size", 7, externalStoreService.getManagedEntityTypes().size());

    assertTrue("'USER' entity is not managed", externalStoreService.getManagedEntityTypes().contains(IDMEntityType.USER));
    assertTrue("'IDMUSER' entity is not managed",
               externalStoreService.getManagedEntityTypes().contains(PicketLinkIDMExternalStoreService.IDMUSER));
    assertTrue("'USER_MEMBERSHIPS' entity is not managed",
               externalStoreService.getManagedEntityTypes().contains(IDMEntityType.USER_MEMBERSHIPS));
    assertTrue("'USER_PROFILE' entity is not managed",
               externalStoreService.getManagedEntityTypes().contains(IDMEntityType.USER_PROFILE));
    assertTrue("'GROUP' entity is not managed", externalStoreService.getManagedEntityTypes().contains(IDMEntityType.GROUP));
    assertTrue("'GROUP_MEMBERSHIPS' entity is not managed",
               externalStoreService.getManagedEntityTypes().contains(IDMEntityType.GROUP_MEMBERSHIPS));
    assertTrue("'MEMBERSHIP' entity is not managed",
               externalStoreService.getManagedEntityTypes().contains(IDMEntityType.MEMBERSHIP));

    ExoFallbackIdentityStoreRepository exoFallbackIdentityStoreRepository = externalStoreService.getFallbackStoreRepository();
    assertTrue("External Store Repository doesn't exist", exoFallbackIdentityStoreRepository.hasExternalStore());
    assertEquals("External Store Repository name is incoherent",
                 "PortalLDAPStore",
                 exoFallbackIdentityStoreRepository.getExternalStoreId());
    assertFalse("External Store is not active", exoFallbackIdentityStoreRepository.isUseExternalStore());

    assertTrue("External Identity Store implementation is not recognized",
               exoFallbackIdentityStoreRepository.getExternalIdentityStore() instanceof ExoLDAPIdentityStoreImpl);
    assertTrue("Internal Identity Store implementation is not recognized",
               exoFallbackIdentityStoreRepository.getDefaultIdentityStore() instanceof ExoHibernateIdentityStoreImpl);
  }

  public void testAuthenticateNewUser() throws Exception {
    String username = "jduke1";
    String password = "theduke";

    assertEquals("Initial listenerAddUser invocation count must be 0", 0, listenerAddUser.get());
    assertEquals("Initial listenerModifyUser invocation count must be 0", 0, listenerModifyUser.get());

    // Make sure that an LDAP group is created to import user memberships
    createLDAPGroup("User");
    createLDAPGroup("Echo");

    User user = organizationService.getUserHandler().findUserByName(username);
    assertNull("User '" + username + "' shouldn't exist in internal store", user);
    UserProfile userProfile = organizationService.getUserProfileHandler().findUserProfileByName(username);
    assertTrue("User Profile '" + username + "' shouldn't exist in internal store",
               userProfile == null || userProfile.getUserInfoMap() == null || userProfile.getUserInfoMap().isEmpty());

    ListAccess<User> allusers = organizationService.getUserHandler().findAllUsers();
    assertEquals("Initial users count in internal store should be 0", 0, allusers.getSize());

    boolean authenticated = organizationService.getUserHandler().authenticate(username, "fakePassword");
    assertFalse("User '" + username + "' shouldn't be able to authenticate with wrong password", authenticated);
    user = organizationService.getUserHandler().findUserByName(username);
    assertNull("User '" + username + "' shouldn't be added on internal store if not authenticated", user);
    allusers = organizationService.getUserHandler().findAllUsers();
    assertEquals("No user should be added on internal store when no successful auhentication is made", 0, allusers.getSize());
    assertEquals("User add listener shouldn't be triggered when the no user is added on internal store",
                 0,
                 listenerAddUser.get());
    assertEquals("User modify listener shouldn't be triggered when the no user is modified on internal store",
                 0,
                 listenerModifyUser.get());

    authenticated = organizationService.getUserHandler().authenticate(username, password);
    assertTrue("The user '" + username + "' should be authenticated on external store", authenticated);
    user = organizationService.getUserHandler().findUserByName(username);
    assertNotNull("The user should be added on internal store", user);
    assertEquals("The user first name attribute should be the same in internal and external stores",
                 "Java Duke1",
                 user.getFirstName());
    assertEquals("The user last name attribute should be the same in internal and external stores", "Duke1", user.getLastName());
    assertEquals("The user email attribute should be the same in internal and external stores",
                 "email@email.com",
                 user.getEmail());
    assertFalse("The user should be recognized as coming from external store", user.isInternalStore());

    assertEquals("The listener 'user creation' should be triggered once the user '" + username
        + "' is successfully authenticated", 1, listenerAddUser.get());
    assertEquals("The listener 'user modification' shouldn't be triggered once the user '" + username
        + "' is successfully authenticated and created on internal store", 0, listenerModifyUser.get());

    allusers = organizationService.getUserHandler().findAllUsers();
    assertEquals("Exactly one user should exist in internal store once user '" + username + "' successfully authenticated",
                 1,
                 allusers.getSize());

    userProfile = organizationService.getUserProfileHandler().findUserProfileByName(username);
    assertNotNull("User Profile of '" + username + "' should exist in internal store once authenticated", userProfile);
    assertNotNull("User Profile of '" + username + "' should have at least one attribute switch PLIDM LDAP configuration mapping",
                  userProfile.getUserInfoMap());
    assertEquals("User Profile of '" + username + "' should have at the attribute '" + NICKNAME_PARAM
        + "' stored in internal store switch PLIDM LDAP configuration mapping",
                 "Duke1",
                 userProfile.getAttribute(NICKNAME_PARAM));

    Collection<Membership> memberships = organizationService.getMembershipHandler().findMembershipsByUser(username);
    assertNotNull("Memberships of user '" + username + "' should be imported from external store once successfully authenticated",
                  memberships);
    assertTrue("Memberships of user '" + username
        + "' should be imported from external store once successfully authenticated, thus it has to be greater than 0",
               memberships.size() > 0);

    boolean found = false;
    for (Membership membership : memberships) {
      found |= membership.getGroupId().equals("/role_hierarchy/Echo");
    }
    assertTrue("Membership 'member:/role_hierarchy/Echo' should be found on internal store once the user successfully authenticated",
               found);
  }

  public void testAuthenticateNewUserWithExceptionOnListener() throws Exception {
    Listener<IDMExternalStoreService, User> exceptionListener = new Listener<IDMExternalStoreService, User>() {
      @SuppressWarnings("serial")
      @Override
      public void onEvent(Event<IDMExternalStoreService, User> event) throws Exception {
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
    listenerService.addListener(IDMExternalStoreService.USER_ADDED_FROM_EXTERNAL_STORE, exceptionListener);

    // The exception on listener shouldn't block authentication
    testAuthenticateNewUser();
  }

  public void testAuthenticateNewUserWithExceptionOnCreate() throws Exception {
    organizationService.getUserHandler().addUserEventListener(errorOnUserEventListener);

    assertEquals("Initial listenerAddUser invocation count must be 0", 0, listenerAddUser.get());
    assertEquals("Initial listenerModifyUser invocation count must be 0", 0, listenerModifyUser.get());

    User user = organizationService.getUserHandler().findUserByName("jduke1");
    assertNull("User 'jduke1' shouldn't exist in internal store", user);

    ListAccess<User> allusers = organizationService.getUserHandler().findAllUsers();
    assertEquals("No user should be added on internal store when no successful auhentication is made", 0, allusers.getSize());

    try {
      externalStoreService.authenticate("jduke1", "theduke");
      fail("The authentication should fail when the user creation fails");
    } catch (Exception e) {
    }

    assertEquals("The listener 'user creation' shouldn't be triggered once the user 'jduke1' authentication fails",
                 0,
                 listenerAddUser.get());
    assertEquals("The listener 'user modification' shouldn't be triggered once the user 'jduke1' authentication fails",
                 0,
                 listenerModifyUser.get());
    user = organizationService.getUserHandler().findUserByName("jduke1");
    assertNull("The user 'jduke1' shouldn't be imported on internal store given the authentication has failed", user);
    allusers = organizationService.getUserHandler().findAllUsers();
    assertEquals("No uer should exist on internal store given the authentication has failed", 0, allusers.getSize());
  }

  public void testAuthenticateExistingUserInInternalStore() throws Exception {
    String username = "jduke1";
    String internalStorePassword = "password";
    String externalStorePassword = "theduke";

    createUser(username, externalStorePassword);

    assertEquals("Initial listenerAddUser invocation count must be 0", 0, listenerAddUser.get());
    assertEquals("Initial listenerModifyUser invocation count must be 0", 0, listenerModifyUser.get());

    User user = organizationService.getUserHandler().findUserByName(username);
    assertNotNull("The user '" + username + "' has been created on internal store, thus it should exist", user);

    ListAccess<User> allusers = organizationService.getUserHandler().findAllUsers();
    assertEquals("Only one user should exist on internal store", 1, allusers.getSize());

    boolean authenticated = organizationService.getUserHandler().authenticate(username, "fakePassword");
    assertFalse("The user '" + username + "' shouldn't be able to authenticated using a wrong password", authenticated);
    allusers = organizationService.getUserHandler().findAllUsers();
    assertEquals("Only one user should exist on internal store", 1, allusers.getSize());
    assertEquals("The listener 'user creation' shouldn't be triggered when the user authentication fails",
                 0,
                 listenerAddUser.get());
    assertEquals("The listener 'user modification' shouldn't be triggered when the user authentication fails",
                 0,
                 listenerModifyUser.get());

    authenticated = organizationService.getUserHandler().authenticate(username, internalStorePassword);
    assertTrue("The user should be able to authenticate using internal store password", authenticated);
    allusers = organizationService.getUserHandler().findAllUsers();
    assertEquals("Only one user should exist on internal store once user '" + username
        + "' successfully authenticates using internal store password", 1, allusers.getSize());
    assertEquals("The listener 'user creation' shouldn't be triggered when the user authentication succeeds using internal store",
                 0,
                 listenerAddUser.get());
    assertEquals("The listener 'user modification' shouldn't be triggered when the user authentication succeeds using internal store",
                 0,
                 listenerModifyUser.get());

    authenticated = organizationService.getUserHandler().authenticate(username, externalStorePassword);
    assertTrue("The user '" + username + "' should be able to authenticate using external store password", authenticated);
    allusers = organizationService.getUserHandler().findAllUsers();
    assertEquals("Only one user should exist on internal store even after authentication succeeds on external store",
                 1,
                 allusers.getSize());
    assertEquals("The listener 'user creation' shouldn't be triggered when the user authentication succeeds using external store since it already exists",
                 0,
                 listenerAddUser.get());
    assertEquals("The listener 'user modification' should be triggered when the user authentication succeeds"
        + " using external store since its attributes are different on external store", 1, listenerModifyUser.get());
  }

  public void testUserSwitchFromInternalStoreToExternalStore() throws Exception {
    String username = "jduke1";
    String internalStorePassword = "password";
    String externalStorePassword = "theduke";

    // Get user entity from LDAP using the exact value of user attributes
    User user = externalStoreService.getEntity(IDMEntityType.USER, username);
    // Change originating store and password for user creation on internal store
    // simulation using different password from external store
    user.setOriginatingStore(null);
    user.setPassword(internalStorePassword);
    organizationService.getUserHandler().createUser(user, true);

    assertEquals("Initial listenerAddUser invocation count must be 0", 0, listenerAddUser.get());
    assertEquals("Initial listenerModifyUser invocation count must be 0", 0, listenerModifyUser.get());

    user = organizationService.getUserHandler().findUserByName(username);
    assertNotNull("The user '" + username + "' has been created on internal store, thus it should exist", user);
    assertTrue("The user '" + username + "' has been created on internal store, thus it should be recognized as internal user",
               user.isInternalStore());

    ListAccess<User> allusers = organizationService.getUserHandler().findAllUsers();
    assertEquals("Only one user should exist on internal store", 1, allusers.getSize());

    boolean authenticated = organizationService.getUserHandler().authenticate(username, internalStorePassword);
    assertTrue("User '" + username
        + "' should be able to authenticate using internal store password since it's not yet recognized as external user",
               authenticated);
    allusers = organizationService.getUserHandler().findAllUsers();
    assertEquals("Only one user should exist on internal store after successfully authenticated on internal store",
                 1,
                 allusers.getSize());
    assertEquals("The listener 'user creation' shouldn't be triggered when the user authentication succeeds using internal store",
                 0,
                 listenerAddUser.get());
    assertEquals("The listener 'user modification' shouldn't be triggered when the user authentication succeeds using internal store",
                 0,
                 listenerModifyUser.get());

    authenticated = organizationService.getUserHandler().authenticate(username, externalStorePassword);
    assertTrue("User should be able to authenticate using external store password", authenticated);
    allusers = organizationService.getUserHandler().findAllUsers();
    assertEquals("Only one user should exist on internal store after successfully authenticated on external store",
                 1,
                 allusers.getSize());
    assertEquals("The listener 'user creation' shouldn't be triggered when the user authentication succeeds using internal store",
                 0,
                 listenerAddUser.get());
    assertEquals("The listener 'user modification' shouldn't be triggered when the user authentication succeeds using external store since the user attributes are still the same as on LDAP",
                 0,
                 listenerModifyUser.get());

    user = organizationService.getUserHandler().findUserByName(username);
    assertFalse("The user should be recognized now as coming from external store", user.isInternalStore());
    assertEquals("The user should be recognized now as coming from external store",
                 OrganizationService.EXTERNAL_STORE,
                 user.getOriginatingStore());

    // The user should not be able to authenticate with internal password
    // anymore
    authenticated = organizationService.getUserHandler().authenticate(username, internalStorePassword);
    assertFalse("The user shouldn't be able to authenticate using internal store password anymore once he's recognized as coming from external store to not have two valid passwords",
                authenticated);
  }

  public void testPopulateUserInformationOnLogin() throws Exception {
    boolean updateInformationOnLogin = externalStoreService.isUpdateInformationOnLogin();
    externalStoreService.setUpdateInformationOnLogin(false);
    try {
      String username = "jduke1";
      String password = "theduke";
  
      // Get user entity from LDAP using the exact value of user attributes
      User user = externalStoreService.getEntity(IDMEntityType.USER, username);
      // Change originating store and firstName for user creation on internal store
      // simulation using empty user data fields
      user.setOriginatingStore(null);
      user.setPassword(null);
      user.setFirstName(null);
      organizationService.getUserHandler().createUser(user, true);
  
      assertEquals("Initial listenerAddUser invocation count must be 0", 0, listenerAddUser.get());
      assertEquals("Initial listenerModifyUser invocation count must be 0", 0, listenerModifyUser.get());
  
      user = organizationService.getUserHandler().findUserByName(username);
      assertNotNull("The user '" + username + "' has been created on internal store, thus it should exist", user);
      assertNull("The user '" + username + "' has been created on internal store and should have null firstName field", user.getFirstName());
      assertTrue("The user '" + username + "' has been created on internal store, thus it should be recognized as internal user",
                 user.isInternalStore());
  
      boolean authenticated = organizationService.getUserHandler().authenticate(username, password);
  
      assertTrue("User should be able to authenticate using external store password", authenticated);
  
      assertEquals("The listener 'user creation' shouldn't be triggered when the user authentication succeeds using internal store",
                   0,
                   listenerAddUser.get());
      assertEquals("The listener 'user modification' should be triggered once when the user authentication succeeds using external store since the user attributes are modified from LDAP",
                   1,
                   listenerModifyUser.get());
  
      user = organizationService.getUserHandler().findUserByName(username);
      assertNotNull("The user should be recognized now as coming from external store", user.getFirstName());
      assertEquals("The user should be recognized now as coming from external store",
                   OrganizationService.EXTERNAL_STORE,
                   user.getOriginatingStore());
    } finally {
      externalStoreService.setUpdateInformationOnLogin(updateInformationOnLogin);
    }
  }

  public void testExternalUserChangePassword() throws Exception {
    String username = "jduke1";
    String password = "theduke";

    boolean authenticated = organizationService.getUserHandler().authenticate(username, password);
    assertTrue("The user should be able to authenticate using external store password", authenticated);
    User user = organizationService.getUserHandler().findUserByName(username);
    assertNotNull("Once the user authenticated, it should be created on internal store", user);
    assertFalse("The user should be recognized as coming from external store", user.isInternalStore());

    user.setPassword("newPassword");
    try {
      organizationService.getUserHandler().saveUser(user, true);
      fail("External user shouldn't be able to change password");
    } catch (Exception e) {
      // Nothing to do
    }
  }

  public void testAuthenticateExistingUserWithExceptionOnListener() throws Exception {
    Listener<IDMExternalStoreService, User> exceptionListener = new Listener<IDMExternalStoreService, User>() {
      @SuppressWarnings("serial")
      @Override
      public void onEvent(Event<IDMExternalStoreService, User> event) throws Exception {
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
    listenerService.addListener(IDMExternalStoreService.USER_MODIFIED_FROM_EXTERNAL_STORE, exceptionListener);

    String username = "jduke1";
    String password = "theduke";

    createUser(username, null);

    assertEquals("Initial listenerAddUser invocation count must be 0", 0, listenerAddUser.get());
    assertEquals("Initial listenerModifyUser invocation count must be 0", 0, listenerModifyUser.get());

    User user = organizationService.getUserHandler().findUserByName(username);
    assertNotNull("The user should exists o internal store once created", user);

    boolean authenticated = organizationService.getUserHandler().authenticate(username, "fakePassword");
    assertFalse("The user shouldn't be able to authenticate using wrong password", authenticated);
    assertEquals("The listener 'user creation' shouldn't be triggered", 0, listenerAddUser.get());
    assertEquals("The listener 'user modification' shouldn't be triggered", 0, listenerModifyUser.get());

    User foundUser = organizationService.getUserHandler().findUserByName(username);
    assertEquals("The user email shouldn't be changed from external store since the authentication fails",
                 user.getEmail(),
                 foundUser.getEmail());

    authenticated = organizationService.getUserHandler().authenticate(username, password);
    assertTrue("The user should be able to authenticate using external store password", authenticated);
    assertEquals("The listener 'user creation' shouldn't be triggered", 0, listenerAddUser.get());
    assertEquals("The listener 'user modification' should be triggered once the LDAP attributes are different from internal store attributes",
                 1,
                 listenerModifyUser.get());

    foundUser = organizationService.getUserHandler().findUserByName(username);
    assertFalse("The uer email should be changed using LDAP attribute value", user.getEmail().equals(foundUser.getEmail()));
  }

  public void testAuthenticateExistingUserWithExceptionOnModify() throws Exception {
    organizationService.getUserHandler().addUserEventListener(errorOnUserEventListener);

    assertEquals("Initial listenerAddUser invocation count must be 0", 0, listenerAddUser.get());
    assertEquals("Initial listenerModifyUser invocation count must be 0", 0, listenerModifyUser.get());

    ListAccess<User> allusers = organizationService.getUserHandler().findAllUsers();
    assertEquals("No user should exists in internal store", 0, allusers.getSize());

    try {
      externalStoreService.authenticate("jduke1", "theduke");
      fail("The authentication should fail when the user creation fails");
    } catch (Exception e) {
    }

    assertEquals("The listener 'user creation' shouldn't be triggered when authentication fails", 0, listenerAddUser.get());
    assertEquals("The listener 'user modification' shouldn't be triggered when authentication fails",
                 0,
                 listenerModifyUser.get());
  }

  public void testGetUserFromExternalStore() throws Exception {
    User user = externalStoreService.getEntity(IDMEntityType.USER, "jduke1");
    assertNotNull("The user 'jduke1' should exist in external store", user);
    assertEquals("The user 'jduke1' first name should equals to external store attribute", "Java Duke1", user.getFirstName());
    assertEquals("The user 'jduke1' last name should equals to external store attribute", "Duke1", user.getLastName());
    assertEquals("The user 'jduke1' email should equals to external store attribute", "email@email.com", user.getEmail());

    // add user on internal store
    createUser("testuser", "testuser");
    // Test if it's retrieved from external store service
    user = externalStoreService.getEntity(IDMEntityType.USER, "testuser");
    assertNull("The created user 'testuser' on internal store shouldn't exist on external store", user);
  }

  public void testGetGroupFromExternalStore() throws Exception {
    Group group = organizationService.getGroupHandler().findGroupById("/organization_hierarchy/OrganizationC");
    assertNull("The Group '/organization_hierarchy/OrganizationC' shouldn't exist on internal store", group);

    group = externalStoreService.getEntity(IDMEntityType.GROUP, "/organization_hierarchy/OrganizationC");
    assertNotNull("The Group '/organization_hierarchy/OrganizationC' should exist on external store", group);
    assertEquals("OrganizationC", group.getGroupName());
    assertEquals("OrganizationC", group.getLabel());
    assertEquals("Some organization", group.getDescription());
    assertEquals("/organization_hierarchy/OrganizationC", group.getId());
    assertEquals("/organization_hierarchy", group.getParentId());

    group = organizationService.getGroupHandler().findGroupById("/organization_hierarchy/OrganizationC");
    assertNull("The Group '/organization_hierarchy/OrganizationC' shouldn't exist on internal store "
        + "once retrived from external store without explicit import operation", group);

    try {
      group = externalStoreService.getEntity(IDMEntityType.GROUP, "/organization_hierarchy");
    } catch (Exception e) {
      // Exception may be thrown when entity not found
    }
    assertNull("Internal parent group '/organization_hierarchy' shouldn't exist on external store", group);
  }

  public void testGetMembershipFromExternalStore() throws Exception {
    String membershipId = "member:jduke1:/organization_hierarchy/OrganizationD";
    Membership membership = organizationService.getMembershipHandler().findMembership(membershipId);
    assertNull("Membership '" + membershipId + "' shouldn't exist on internal store", membership);

    membership = externalStoreService.getEntity(IDMEntityType.MEMBERSHIP, membershipId);
    assertNotNull("Membership '" + membershipId + "' should exist on external store", membership);
    assertEquals("/organization_hierarchy/OrganizationD", membership.getGroupId());
    assertEquals("jduke1", membership.getUserName());
    assertEquals("member", membership.getMembershipType());

    membership = organizationService.getMembershipHandler().findMembership(membershipId);
    assertNull("Membership '" + membershipId + "' shouldn't exist on internal store "
        + "once retrieved from external store without explicit data import operation", membership);
  }

  public void testGetUserMembershipsFromExternalStore() throws Exception {
    Collection<?> memberships = externalStoreService.getEntity(IDMEntityType.USER_MEMBERSHIPS, "jduke1");
    assertEquals("User 'jduke1' should have 3 memberships on external store", 3, memberships.size());
    for (Object object : memberships) {
      Membership membership = (Membership) object;
      assertEquals("Mapped memberships from external store should have membership type = member",
                   "member",
                   membership.getMembershipType());
      assertEquals("User 'jduke1' memberships should have username = 'jduke1'", "jduke1", membership.getUserName());
      if (!membership.getGroupId().equals("/organization_hierarchy/OrganizationD")
          && !membership.getGroupId().equals("/role_hierarchy/Echo") && !membership.getGroupId().equals("/role_hierarchy/User")) {
        fail("Group id '" + membership.getGroupId() + "' is not expected");
      }
    }
  }

  public void testGetGroupMembershipsFromExternalStore() throws Exception {
    @SuppressWarnings("unchecked")
    List<Membership> membershipsList = externalStoreService.getEntity(IDMEntityType.GROUP_MEMBERSHIPS,
                                                                      "/organization_hierarchy/OrganizationD");
    assertNotNull("Group '/organization_hierarchy/OrganizationD' should have memberships on external store", membershipsList);
    assertEquals("Group '/organization_hierarchy/OrganizationD' should have 3 memberships on external store",
                 3,
                 membershipsList.size());
    for (Object object : membershipsList) {
      Membership membership = (Membership) object;
      assertEquals("Mapped memberships from external store should have membership type = member",
                   "member",
                   membership.getMembershipType());
      assertEquals("Group '/organization_hierarchy/OrganizationD' memberships should have groupId = '/organization_hierarchy/OrganizationD'",
                   "/organization_hierarchy/OrganizationD",
                   membership.getGroupId());
      if (!membership.getUserName().equals("jduke") && !membership.getUserName().equals("jduke1")
          && !membership.getUserName().equals("jduke2")) {
        fail("Username '" + membership.getUserName() + "' is not expected");
      }
    }
  }

  public void testGetAllUsers() throws Exception {
    String rdbmsuser = "rdbmsuser";
    createUser(rdbmsuser, "password");

    ListAccess<String> allUsersListAccess = externalStoreService.getAllOfType(IDMEntityType.USER, null);
    String[] usernames = allUsersListAccess.load(0, Integer.MAX_VALUE);
    assertNotNull("No user was found on external store", usernames);
    assertTrue("No user was found on external store", usernames.length > 0);
    for (String username : usernames) {
      assertFalse("Internal user 'rdbmsuser' shouldn't be retrieved from external store query", rdbmsuser.equals(username));
    }
  }

  public void testGetModifiedUsers() throws Exception {
    LocalDateTime searchDate = getLocalDateTime().minusSeconds(1);
    // Add users 'jduke10' and 'jduke11'. 'jduke10' is added to group
    // /role_hierarchy/Admin at the same time
    openDSService.populateLDIFFile("ldap/ldap/test-user-modification-opends.ldif");
    // Wait 1 second until operation finishes on LDAP Store
    Thread.sleep(1000);

    try {
      ListAccess<String> modifiedUsersListAccess = externalStoreService.getAllOfType(IDMEntityType.USER, searchDate);
      String[] usernames = modifiedUsersListAccess.load(0, Integer.MAX_VALUE);
      assertNotNull("No user was detected as modified", usernames);
      assertEquals("Detected modified users count should equal to 2", 2, usernames.length);
      for (String username : usernames) {
        assertTrue("Detected modified user '" + username + "' isn't expected",
                   "jduke11".equals(username) || "jduke10".equals(username));
      }
    } finally {
      openDSService.cleanUpDN("uid=jduke10,ou=People,o=test,dc=portal,dc=example,dc=com");
      openDSService.cleanUpDN("uid=jduke11,ou=People,o=test,dc=portal,dc=example,dc=com");
    }
  }

  public void testGetModifiedGroups() throws Exception {
    LocalDateTime searchDate = getLocalDateTime().minusSeconds(1);

    ListAccess<String> modifiedGroupsListAccess = externalStoreService.getAllOfType(IDMEntityType.GROUP, null);
    String[] groupIds = modifiedGroupsListAccess.load(0, Integer.MAX_VALUE);
    assertNotNull(groupIds);
    assertEquals("LDAP Groups count is incoherent", 8, groupIds.length);

    openDSService.populateLDIFFile("ldap/ldap/test-group-modification-opends.ldif");
    // Wait 1 second until operation finishes on LDAP Store
    Thread.sleep(1000);

    try {
      modifiedGroupsListAccess = externalStoreService.getAllOfType(IDMEntityType.GROUP, null);
      groupIds = modifiedGroupsListAccess.load(0, Integer.MAX_VALUE);
      assertNotNull(groupIds);
      assertEquals("LDAP Groups count is incoherent after single group import", 9, groupIds.length);

      modifiedGroupsListAccess = externalStoreService.getAllOfType(IDMEntityType.GROUP, searchDate);
      groupIds = modifiedGroupsListAccess.load(0, Integer.MAX_VALUE);
      assertNotNull("No modified group is detected", groupIds);
      assertTrue("Detected modified group count is incoherent, it should be greater or equal to 1, got " + groupIds.length,
                 groupIds.length >= 1);
      boolean found = false;
      for (String groupId : groupIds) {
        if (groupId.equals("/role_hierarchy/Delta")) {
          found = true;
        }
      }
      assertTrue("Added group '/role_hierarchy/Delta' on external store should have been detected.", found);
    } finally {
      openDSService.cleanUpDN("cn=Delta,ou=Roles,o=test,dc=portal,dc=example,dc=com");
    }
  }

  private User createUser(String username, String password) throws Exception {
    UserHandler userHandler = organizationService.getUserHandler();
    User user = userHandler.createUserInstance(username);
    if (StringUtils.isNotBlank(password)) {
      user.setPassword("password");
    }
    user.setFirstName("default");
    user.setLastName("default");
    user.setEmail(username + "@exoportal.org");
    try {
      userHandler.createUser(user, true);
      return user;
    } catch (Exception e) {
      fail("Error on create user " + user.getUserName(), e);
      throw e;
    }
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

  private LocalDateTime getLocalDateTime() {
    return ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
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
