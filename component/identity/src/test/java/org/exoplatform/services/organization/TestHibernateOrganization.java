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

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;

/**
 * Tests on OrganizationService, only related to Hibernate
 */
@ConfiguredBy({ 
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/services/organization/TestOrganization-configuration.xml") })
public class TestHibernateOrganization extends AbstractKernelTest {

  protected static final String DEFAULT_PASSWORD = "defaultpassword";

  protected OrganizationService organizationService;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    begin();
    PortalContainer container = getContainer();
    organizationService = container.getComponentInstanceOfType(OrganizationService.class);
  }

  @Override
  protected void tearDown() throws Exception {
    end();

    super.tearDown();
  }

  /**
   * Test that a rollback is well performed when an exception occurs during the
   * transaction
   *
   * @throws Exception
   */
  public void testTransactionRollback() throws Exception {
    try {
      // Try to create the same user twice
      createUser("userRollback");
      createUser("userRollback");
      fail("The user creation must fail since we cannot create 2 users with the same username");
    } catch (Exception e) {
      // expected exception
    } finally {
      RequestLifeCycle.end();
    }

    // Check that the users creations have been rollbacked
    User userRollback = organizationService.getUserHandler().findUserByName("userRollback");
    assertNull("User should not have been created", userRollback);

    try {
      // Now create the user only once
      RequestLifeCycle.begin(getContainer());
      // No exception is thrown during the transaction commit of this user creation
      createUser("userRollback");
    } finally {
      RequestLifeCycle.end();
    }

    // Check that the user is well created
    userRollback = organizationService.getUserHandler().findUserByName("userRollback");
    assertNotNull("User should have been created", userRollback);

    RequestLifeCycle.begin(getContainer());
  }

  protected void createUser(String username, String... groups) throws Exception {
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
}
