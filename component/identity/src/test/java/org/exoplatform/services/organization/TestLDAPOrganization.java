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

import exo.portal.component.identiy.opendsconfig.opends.OpenDSService;
import junit.framework.Assert;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.idm.*;
import org.exoplatform.services.organization.idm.cache.CacheableUserHandlerImpl;
import org.picketlink.idm.api.Attribute;
import org.picketlink.idm.impl.api.SimpleAttribute;

import java.util.*;


@ConfiguredBy({
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-ldap-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/services/organization/TestOrganization-configuration.xml") })
public class TestLDAPOrganization extends TestOrganization {

  private static Log                      log                             = ExoLogger.getLogger(TestLDAPOrganization.class.getName());

  protected UserHandler uHandler;

  OpenDSService                           openDSService                   = new OpenDSService(null);

  PortalContainer container;

  OrganizationService organization;

  @Override
  protected void beforeRunBare() {
    try {
      openDSService.start();
      openDSService.initLDAPServer();
    } catch (Exception e) {
      log.error("Error in starting up OPENDS", e);
      e.printStackTrace();
    }
    super.beforeRunBare();
  }

  @Override
  protected void setUp() throws Exception {
    container = PortalContainer.getInstance();
    organization = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);
    uHandler = organization.getUserHandler();
    super.setUp();
    synchronizeUsers();
  }

  public void synchronizeUsers() throws Exception {
    begin();
    assertNotNull("Cannot find OrganizationService", organization);
    UserDAOImpl userHandler = (UserDAOImpl) organization.getUserHandler();
    ListAccess<User> userListAccess = organization.getUserHandler().findAllUsers();
    User[] users = userListAccess.load(0, userListAccess.getSize());
      end();
    for (User user : users) {
      String username = user.getUserName();

      if (user.getCreatedDate() == null) {
        user.setCreatedDate(new Date());
      }
      log.info("Invoke " + username + " user synchronization ");
      Collection<UserEventListener> userDAOListeners = userHandler.getUserEventListeners();
      for (UserEventListener userEventListener : userDAOListeners) {
        begin();
        try {
          userEventListener.preSave(user, true);
        } catch (Exception e) {
          log.info("\t\tFailed to call preSave for " + username + " User with listener : " + userEventListener.getClass());
          e.printStackTrace();
        } finally {
          end();
        }
      }
      for (UserEventListener userEventListener : userDAOListeners) {
        try {
          begin();
          userEventListener.postSave(user, true);
        } catch (Exception e) {
          log.info("\t\tFailed to call postSave for " + username + " User with listener : " + userEventListener.getClass());
          e.printStackTrace();
        } finally {
          end();
        }
      }
    }
  }

  public void testIDMConfiguration(){
    PicketLinkIDMOrganizationServiceImpl idmService = container.getComponentInstanceOfType(PicketLinkIDMOrganizationServiceImpl.class);
    Config config =idmService.getConfiguration();
    assertFalse(config.isCountPaginatedUsers());
    assertTrue(config.isSkipPaginationInMembershipQuery());
  }

  public void testFindGroupHierachy() throws Exception {
      GroupHandler handler = organizationService.getGroupHandler();
      Group group = handler.findGroupById("/organization_hierarchy/OrganizationC");

      Collection childGoups = handler.findGroups(group);
      Assert.assertTrue(childGoups.size() > 0);
  }

  public void testMoveGroup() throws Exception {
    GroupHandler handler = organizationService.getGroupHandler();
    Group group = handler.findGroupById("/organization/communication/press-and-media");
    assertNotNull(group);
    Group communication = handler.findGroupById("/organization/communication");
    int communitcationChildren = handler.findGroups(communication).size();


    Group parentOrigin = handler.findGroupById("/organization/communication");
    Group parentTarget = handler.findGroupById("/organization/operations");


    handler.moveGroup(parentOrigin,parentTarget,group);

    assertEquals(communitcationChildren - 1, handler.findGroups(communication).size());
    assertNotNull(handler.findGroupById("/organization/operations/press-and-media"));


  }

  public void testFindFilteredGroup() throws Exception {
    GroupHandler handler = organizationService.getGroupHandler();
    Group filteredGroup = handler.findGroupById("/organization_hierarchy/filteredSubOrganizationC");
    assertNull(filteredGroup);

    Group existingGroup =  handler.findGroupById("/organization_hierarchy/OrganizationC");
    assertNotNull(existingGroup);
  }

  public void testFindUser() throws Exception {
    assertNotNull(organization);
    begin();
    User test2 = organization.getUserHandler().findUserByName("admin");
    assertNotNull(test2);
    openDSService.cleanUpDN("uid=admin,ou=People,o=test,dc=portal,dc=example,dc=com");
    if (organization.getUserHandler() instanceof CacheableUserHandlerImpl) {
      ((CacheableUserHandlerImpl) organization.getUserHandler()).clearCache();
    }
    test2 = organization.getUserHandler().findUserByName("admin");
    assertNull(test2);
    end();
  }

  public void testEnabledUserDetection() throws Exception{
    User u = uHandler.createUserInstance("test");
    ((UserImpl) u).setEnabled(true);

    Map<String, Attribute> attrs = new HashMap<>();
    Attribute attr = new SimpleAttribute("enabled");
    Attribute attr1 = new SimpleAttribute("enabled");
    Attribute attr2 = new SimpleAttribute("enabled");
    Attribute attr3 = new SimpleAttribute("enabled");
    // 65536 represent an enabled user whose password never expires
    attr.addValue(65536);
    // 65538 represent an disabled user whose password never expires
    attr1.addValue(65538);
    // 8388608 represent an enabled user whose password expired
    attr2.addValue(8388608);
    // 83886010 represent an disabled user whose password expired
    attr3.addValue(8388610);

    attrs.put("enabled",attr);
    assertFalse(EntityMapperUtils.populateUser(u,attrs,true));
    attrs.remove("enabled",attr);
    attrs.put("enabled",attr1);
    assertTrue(EntityMapperUtils.populateUser(u,attrs,true));
    attrs.remove("enabled",attr1);
    attrs.put("enabled",attr2);
    assertTrue(EntityMapperUtils.populateUser(u,attrs,true));
    attrs.remove("enabled",attr2);
    attrs.put("enabled",attr3);
    assertTrue(EntityMapperUtils.populateUser(u,attrs,true));

  }

  public void testFindEnabledUsers() throws Exception {
    Query query = new Query();
    query.setUserName("*");

    ListAccess<User> listEnabled = organization.getUserHandler().findUsersByQuery(query, UserStatus.ENABLED);
    ListAccess<User> listDisabled = organization.getUserHandler().findUsersByQuery(query, UserStatus.DISABLED);
    ListAccess<User> listAll =organization.getUserHandler().findUsersByQuery(query, UserStatus.ANY);
    assertNotNull(listDisabled);
    assertNotNull(listAll);
    assertNotNull(listEnabled);
    assertEquals(0, listDisabled.load(0, listDisabled.getSize()).length);
    assertEquals(listAll.load(0, listAll.getSize()).length, listEnabled.load(0, listEnabled.getSize()).length);

    User adminUser = organization.getUserHandler().findUserByName("jduke", UserStatus.ENABLED);
    assertNotNull(adminUser);
    adminUser = organization.getUserHandler().findUserByName("jduke", UserStatus.DISABLED);
    assertNull(adminUser);
    adminUser = organization.getUserHandler().findUserByName("jduke", UserStatus.ANY);
    assertNotNull(adminUser);

    organization.getUserHandler().setEnabled("jduke", false, false );
    adminUser = organization.getUserHandler().findUserByName("jduke", UserStatus.ENABLED);
    assertNull(adminUser);
    adminUser = organization.getUserHandler().findUserByName("jduke", UserStatus.DISABLED);
    assertNotNull(adminUser);
    adminUser = organization.getUserHandler().findUserByName("jduke", UserStatus.ANY);
    assertNotNull(adminUser);

    ListAccess<User> newListEnabled = organization.getUserHandler().findUsersByQuery(query, UserStatus.ENABLED);
    ListAccess<User> newListDisabled = organization.getUserHandler().findUsersByQuery(query, UserStatus.DISABLED);
    ListAccess<User> newListAll = organization.getUserHandler().findUsersByQuery(query, UserStatus.ANY);

    assertNotNull(newListEnabled);
    assertNotNull(newListDisabled);
    assertNotNull(newListAll);
    assertEquals(1, newListDisabled.load(0, newListDisabled.getSize()).length);
    assertEquals("jduke", newListDisabled.load(0,1)[0].getUserName());
    for(int i=0; i< newListEnabled.getSize(); i++){
      assertNotSame("jduke",newListEnabled.load(i,1)[0].getUserName() );
    }
  }

  public void testFindUsersWithPagination() throws Exception {
    begin();
    ListAccess<User> userListAccess = organization.getUserHandler().findAllUsers();

    coherenceTestofReturnedResults(userListAccess);

    Query query = new Query();
    query.setUserName("jduke");

    userListAccess = organization.getUserHandler().findUsersByQuery(query);
    coherenceTestofReturnedResults(userListAccess);
  }

  private void coherenceTestofReturnedResults(ListAccess<User> userListAccess) throws Exception {
    int usersCount = userListAccess.getSize();
    assertTrue(usersCount > 3);

    int pageSize = 2;
    int index = 0;

    Set<String> foundUsernames = new HashSet<>();
    while (index < pageSize * 2 && index < usersCount) {
      int usersToLoad = (index + pageSize) > usersCount ? (usersCount - index) : pageSize;
      User[] users = userListAccess.load(index, usersToLoad);
      index += users.length;
      assertTrue(index <= usersCount);
      for (User user : users) {
        if(user != null && StringUtils.isBlank(user.getUserName())) {
          continue;
        }
        assertFalse(foundUsernames.contains(user.getUserName()));
        foundUsernames.add(user.getUserName());
      }
    }
  }

  public void testFindUsers() throws Exception
  {
    Query query = new Query();
    query.setEmail("email@test");

    // try to find user by email
    assertSizeEquals(1, uHandler.findUsersByQuery(query), UserStatus.ENABLED);
    assertSizeEquals(1, uHandler.findUsersByQuery(query, UserStatus.ENABLED), UserStatus.ENABLED);
    assertSizeEquals(1, uHandler.findUsersByQuery(query, UserStatus.ANY), UserStatus.ANY);

    // try to find user by name with mask
    query = new Query();
    query.setUserName("*tolik*");
    assertSizeEquals(1, uHandler.findUsersByQuery(query));

    // try to find user by name with mask
    query = new Query();
    query.setUserName("tol*");
    assertSizeEquals(1, uHandler.findUsersByQuery(query));

    // try to find user by name with mask
    query = new Query();
    query.setUserName("*lik");
    assertSizeEquals(4, uHandler.findUsersByQuery(query));

    // try to find user by name explicitly
    query = new Query();
    query.setUserName("tolik");
    assertSizeEquals(1, uHandler.findUsersByQuery(query));

    // try to find user by part of name without mask
    query = new Query();
    query.setUserName("tol");
    assertSizeEquals(1, uHandler.findUsersByQuery(query));

    query = new Query();
    query.setUserName("*olik");

    ListAccess<User> users = uHandler.findUsersByQuery(query);

    assertSizeEquals(4, users, UserStatus.ENABLED);
    assertSizeEquals(4, uHandler.findUsersByQuery(query, UserStatus.ENABLED), UserStatus.ENABLED);
    assertSizeEquals(4, uHandler.findUsersByQuery(query, UserStatus.ANY), UserStatus.ANY);

    User[] allPage = users.load(0, 4);
    User[] page1 = users.load(0, 2);
    User[] page2 = users.load(2, 2);

    assertEquals(allPage[0].getUserName(), page1[0].getUserName());
    assertEquals(allPage[1].getUserName(), page1[1].getUserName());
    assertEquals(allPage[2].getUserName(), page2[0].getUserName());
    assertEquals(allPage[3].getUserName(), page2[1].getUserName());

    try
    {
      users.load(0, 0);
    }
    catch (Exception e)
    {
      fail("Exception is not expected");
    }

    // try to load more than exist
    try
    {
      users.load(0, 5);
      fail("Exception is expected");
    }
    catch (Exception e)
    {
    }

    // try to load more than exist
    try
    {
      users.load(1, 4);
      fail("Exception is expected");
    }
    catch (Exception e)
    {
      //expected
    }

    try
    {
      // Disable the user tolik
      uHandler.setEnabled("tolik", false, true);

      assertSizeEquals(3, uHandler.findUsersByQuery(query), UserStatus.ENABLED);
      assertSizeEquals(3, uHandler.findUsersByQuery(query, UserStatus.ENABLED), UserStatus.ENABLED);
      assertSizeEquals(1, uHandler.findUsersByQuery(query, UserStatus.DISABLED), UserStatus.DISABLED);
      assertSizeEquals(4, uHandler.findUsersByQuery(query, UserStatus.ANY), UserStatus.ANY);

      // Enable the user tolik
      uHandler.setEnabled("tolik", true, true);

      assertSizeEquals(4, uHandler.findUsersByQuery(query), UserStatus.ENABLED);
      assertSizeEquals(4, uHandler.findUsersByQuery(query, UserStatus.ENABLED), UserStatus.ENABLED);
      assertSizeEquals(0, uHandler.findUsersByQuery(query, UserStatus.DISABLED), UserStatus.DISABLED);
      assertSizeEquals(4, uHandler.findUsersByQuery(query, UserStatus.ANY), UserStatus.ANY);
    }
    catch (UnsupportedOperationException e)
    {
      fail();
    }
  }

  protected void assertSizeEquals(int expectedSize, ListAccess<User> list, UserStatus status) throws Exception
  {
    int size;
    assertEquals(expectedSize, size = list.getSize());
    User[] values = list.load(0, size);
    size = 0;
    for (int i = 0; i < values.length; i++)
    {
      User usr = values[i];
      if (usr != null && status.matches(usr.isEnabled()))
      {
        size++;
      }
    }
    assertEquals(expectedSize, size);
  }

  protected void assertSizeEquals(int expectedSize, ListAccess<?> list) throws Exception
  {
    int size;
    assertEquals(expectedSize, size = list.getSize());
    Object[] values = list.load(0, size);
    size = 0;
    for (int i = 0; i < values.length; i++)
    {
      if (values[i] != null)
      {
        size++;
      }
    }
    assertEquals(expectedSize, size);
  }
}
