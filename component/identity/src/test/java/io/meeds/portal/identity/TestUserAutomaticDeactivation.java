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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;
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

  /**
   * Reproduces the case where inactive users are not disabled when they fall
   * beyond the first page of results. Users are paginated 100 at a time and
   * sorted by last login time so that inactive accounts (the oldest logins) are
   * processed first; if the ordering is reversed, an installation with enough
   * recently-active users hides the inactive ones on later pages and they are
   * never disabled.
   */
  @SuppressWarnings("deprecation")
  public void testShouldDisableInactiveUsersBeyondFirstPage() throws Exception {
    List<String> createdUserNames = new ArrayList<>();
    List<String> activeUserNames = new ArrayList<>();
    Date oldLogin = Date.from(LocalDate.now()
                                       .minusDays(6)
                                       .atStartOfDay()
                                       .atZone(ZoneId.systemDefault())
                                       .toInstant());
    try {
      // Create more active users than the pagination page size (100) so that an
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

}
