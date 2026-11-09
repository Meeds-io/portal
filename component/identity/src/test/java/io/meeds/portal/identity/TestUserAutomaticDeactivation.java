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

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.picketlink.idm.api.Attribute;
import org.picketlink.idm.impl.api.SimpleAttribute;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;
import org.exoplatform.services.organization.idm.UserDAOImpl;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/services/organization/TestOrganization-configuration.xml"),
})
public class TestUserAutomaticDeactivation extends AbstractKernelTest {// NOSONAR

  private static final String CREATION_SOURCE = "Source1";

  private OrganizationService organizationService;        // NOSONAR

  private UserDAOImpl         userDao;

  private String              userName;

  @Override
  protected void setUp() throws Exception {
    organizationService = getContainer().getComponentInstanceOfType(OrganizationService.class);
    userDao = (UserDAOImpl) organizationService.getUserHandler();
    begin();
    userName = UUID.randomUUID().toString();
    User user = userDao.createUserInstance(userName);
    user.setFirstName("First Name");
    user.setLastName("Last Name");
    user.setEmail("test@test.com");
    user.setCreationSource(CREATION_SOURCE);
    userDao.createUser(user, true);
    restartTransaction();
  }

  @Override
  protected void tearDown() throws Exception {
    userDao.removeUser(userName, true);
    end();
  }

  public void testShouldNotDisableUsersWhenNotInactive() {
    assertEquals(0, userDao.disableInactiveUsers(null, 5));
  }

  @SuppressWarnings("deprecation")
  public void testShouldDisableUsersWhenInactive() throws Exception {
    User user = userDao.findUserByName(userName, UserStatus.ANY);
    assertTrue(user.isEnabled());
    assertFalse(user.isAutomaticDeactivation());
    user.setLastLoginTime(Date.from(LocalDate.now()
                                             .minusDays(6)
                                             .atStartOfDay()
                                             .atZone(ZoneId.systemDefault())
                                             .toInstant()));
    userDao.saveUser(user, true);
    restartTransaction();

    assertEquals(1, userDao.disableInactiveUsers(null, 5));
    restartTransaction();

    user = userDao.findUserByName(userName, UserStatus.ANY);
    assertFalse(user.isEnabled());
    assertTrue(user.isAutomaticDeactivation());

    assertEquals(0, userDao.disableInactiveUsers(null, 5));
  }

  @SuppressWarnings("deprecation")
  public void testShouldNotDisableInactiveUsersOfExternalStore() throws Exception {
    User user = userDao.findUserByName(userName, UserStatus.ANY);
    user.setOriginatingStore(OrganizationService.EXTERNAL_STORE);
    user.setLastLoginTime(Date.from(LocalDate.now()
                                             .minusDays(6)
                                             .atStartOfDay()
                                             .atZone(ZoneId.systemDefault())
                                             .toInstant()));
    userDao.saveUser(user, true);
    restartTransaction();

    assertEquals(0, userDao.disableInactiveUsers(null, 5));
    restartTransaction();

    // Read back only after the job: the job does not evict the user cache, so a
    // read before it would leave a copy that no longer reflects the database
    user = userDao.findUserByName(userName, UserStatus.ANY);
    assertFalse(user.isInternalStore());
    assertTrue("External store user must stay enabled", user.isEnabled());
    assertFalse(user.isAutomaticDeactivation());
  }

  @SuppressWarnings("deprecation")
  public void testShouldDisableInactiveUsersOfInternalStore() throws Exception {
    User user = userDao.findUserByName(userName, UserStatus.ANY);
    user.setOriginatingStore(OrganizationService.INTERNAL_STORE);
    user.setLastLoginTime(Date.from(LocalDate.now()
                                             .minusDays(6)
                                             .atStartOfDay()
                                             .atZone(ZoneId.systemDefault())
                                             .toInstant()));
    userDao.saveUser(user, true);
    restartTransaction();

    assertEquals(1, userDao.disableInactiveUsers(null, 5));
    restartTransaction();

    // Read back only after the job, for the same reason as above
    user = userDao.findUserByName(userName, UserStatus.ANY);
    assertEquals(OrganizationService.INTERNAL_STORE, user.getOriginatingStore());
    assertFalse("Internal store user must be disabled", user.isEnabled());
    assertTrue(user.isAutomaticDeactivation());
  }

  public void testShouldNotDisableUserReactivatedWithinInactivityPeriod() throws Exception {
    disableAsInactiveThenReactivate(6, 5);

    assertEquals(0, userDao.disableInactiveUsers(null, 5));
    restartTransaction();

    User user = userDao.findUserByName(userName, UserStatus.ANY);
    assertTrue("User reactivated within the inactivity period must stay enabled", user.isEnabled());
    assertFalse(user.isAutomaticDeactivation());
  }

  public void testShouldCountInactivityFromReactivationOverLongPeriod() throws Exception {
    disableAsInactiveThenReactivate(400, 365);
    setReactivationTime(ZonedDateTime.now().minusDays(30));

    assertEquals(0, userDao.disableInactiveUsers(null, 365));
    restartTransaction();

    User user = userDao.findUserByName(userName, UserStatus.ANY);
    assertTrue("User reactivated 30 days ago must stay enabled with a 365-day inactivity period", user.isEnabled());
    assertFalse(user.isAutomaticDeactivation());
  }

  public void testShouldDisableUserReactivatedBeforeInactivityPeriod() throws Exception {
    disableAsInactiveThenReactivate(10, 5);
    setReactivationTime(ZonedDateTime.now().minusDays(6));

    assertEquals(1, userDao.disableInactiveUsers(null, 5));
    restartTransaction();

    User user = userDao.findUserByName(userName, UserStatus.ANY);
    assertFalse("User inactive for the whole period since the reactivation must be disabled again", user.isEnabled());
    assertTrue(user.isAutomaticDeactivation());
  }

  @SuppressWarnings("deprecation")
  public void testShouldDisableInactiveUsersBeyondFirstPage() throws Exception {
    List<String> createdUserNames = new ArrayList<>();
    List<String> activeUserNames = new ArrayList<>();
    Date recentLogin = new Date();
    Date oldLogin = Date.from(LocalDate.now()
                                       .minusDays(6)
                                       .atStartOfDay()
                                       .atZone(ZoneId.systemDefault())
                                       .toInstant());
    try {
      // inactive user is pushed onto a later page. Newly created users get
      // lastLoginTime = now automatically (NewUserEventListener), so they all
      // count as active.
      for (int i = 0; i < 100; i++) {
        String activeUserName = UUID.randomUUID().toString();
        User activeUser = userDao.createUserInstance(activeUserName);
        activeUser.setFirstName("Active");
        activeUser.setLastName("User " + i);
        activeUser.setEmail(activeUserName + "@test.com");
        activeUser.setCreationSource(CREATION_SOURCE);
        userDao.createUser(activeUser, true);
        activeUserNames.add(activeUserName);
        createdUserNames.add(activeUserName);
      }

      String inactiveUserName = UUID.randomUUID().toString();
      User inactiveUser = userDao.createUserInstance(inactiveUserName);
      inactiveUser.setFirstName("Inactive");
      inactiveUser.setLastName("User");
      inactiveUser.setEmail(inactiveUserName + "@test.com");
      inactiveUser.setCreationSource(CREATION_SOURCE);
      userDao.createUser(inactiveUser, true);
      createdUserNames.add(inactiveUserName);
      restartTransaction();

      // Backdate the login time AFTER creation: NewUserEventListener only resets
      // lastLoginTime for new users, so saveUser (not "new") keeps our value.
      inactiveUser = userDao.findUserByName(inactiveUserName, UserStatus.ANY);
      inactiveUser.setLastLoginTime(oldLogin);
      userDao.saveUser(inactiveUser, true);
      restartTransaction();

      assertEquals(1, userDao.disableInactiveUsers(null, 5));
      restartTransaction();

      inactiveUser = userDao.findUserByName(inactiveUserName, UserStatus.ANY);
      assertFalse("Inactive user should have been disabled", inactiveUser.isEnabled());
      assertTrue(inactiveUser.isAutomaticDeactivation());

      User activeUser = userDao.findUserByName(activeUserNames.get(0), UserStatus.ANY);
      assertTrue("Active user should remain enabled", activeUser.isEnabled());
    } finally {
      for (String createdUserName : createdUserNames) {
        userDao.removeUser(createdUserName, true);
      }
      restartTransaction();
    }
  }

  @SuppressWarnings("deprecation")
  private void disableAsInactiveThenReactivate(int lastLoginDaysAgo, int inactiveDays) throws Exception {
    User user = userDao.findUserByName(userName, UserStatus.ANY);
    user.setLastLoginTime(Date.from(LocalDate.now()
                                             .minusDays(lastLoginDaysAgo)
                                             .atStartOfDay()
                                             .atZone(ZoneId.systemDefault())
                                             .toInstant()));
    userDao.saveUser(user, true);
    restartTransaction();

    assertEquals(1, userDao.disableInactiveUsers(null, inactiveDays));
    restartTransaction();

    // No read back here: the job does not evict the user cache, so a cached
    // copy read now would hide a later automatic deactivation
    assertTrue(userDao.setEnabled(userName, true, true).isEnabled());
    restartTransaction();
  }

  private void setReactivationTime(ZonedDateTime reactivationTime) throws Exception {
    getContainer().getComponentInstanceOfType(PicketLinkIDMService.class)
                  .getIdentitySession()
                  .getAttributesManager()
                  .updateAttributes(userName,
                                    new Attribute[] { new SimpleAttribute("reactivationTime",
                                                                          String.valueOf(reactivationTime.toInstant()
                                                                                                         .toEpochMilli())) });
    restartTransaction();
  }

}
