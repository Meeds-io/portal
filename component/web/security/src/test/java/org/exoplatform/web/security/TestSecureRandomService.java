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
package org.exoplatform.web.security;

import java.security.SecureRandom;

import org.exoplatform.component.test.*;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.web.security.security.SecureRandomService;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/secure-random-service-configuration.xml")})
public class TestSecureRandomService extends AbstractKernelTest {
    protected SecureRandomService service;

    protected void setUp() throws Exception {
        PortalContainer container = getContainer();
        service = (SecureRandomService) container.getComponentInstanceOfType(SecureRandomService.class);
    }

    public void testGetSecureRandom() {
        /* let's look if the threading internals work */
        SecureRandom r = service.getSecureRandom();
        /* have we gotten anything at all? */
        assertNotNull(r);

        /* how can random be tested? */
        r.nextInt();
        r.nextInt();
    }

}
