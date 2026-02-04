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
import java.util.Date;
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
    assertEquals(0, userDao.disableInactiveUsers(5));
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

    assertEquals(1, userDao.disableInactiveUsers(5));
    restartTransaction();

    user = userDao.findUserByName(userName, UserStatus.ANY);
    assertFalse(user.isEnabled());
    assertTrue(user.isAutomaticDeactivation());

    assertEquals(0, userDao.disableInactiveUsers(5));
  }

}
