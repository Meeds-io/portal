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
import org.exoplatform.services.organization.idm.IdmHibernateService;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration-local.xml") })
public class TestBootstrap extends AbstractKernelTest {

    public void testWorkspace() throws Exception {
        PortalContainer container = PortalContainer.getInstance();
        IdmHibernateService idmHibernateService = container.getComponentInstanceOfType(IdmHibernateService.class);
        assertNotNull(idmHibernateService);
        OrganizationService organization = (OrganizationService) container
                .getComponentInstanceOfType(OrganizationService.class);
        assertNotNull(organization);
    }

    public void testBasicOperation() throws Exception {
        PortalContainer container = PortalContainer.getInstance();
        OrganizationService organization = (OrganizationService) container
                .getComponentInstanceOfType(OrganizationService.class);
        assertNotNull(organization);

        begin();
        User test = organization.getUserHandler().createUserInstance("testUser");
        organization.getUserHandler().createUser(test, false);

        test = organization.getUserHandler().findUserByName("toto");
        assertNull(test);
        test = organization.getUserHandler().findUserByName("testuser");
        assertNotNull(test);
        assertEquals("testUser", test.getUserName());
        end();
    }

}
