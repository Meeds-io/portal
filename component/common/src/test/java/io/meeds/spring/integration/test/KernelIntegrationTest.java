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
package io.meeds.spring.integration.test;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.exoplatform.jpa.CommonsDAOJPAImplTest;

import io.meeds.spring.module.dao.TestJpaDao;
import io.meeds.spring.module.service.TestExcludedService;
import io.meeds.spring.module.service.TestService;
import io.meeds.spring.module.storage.TestStorage;

@SpringJUnitConfig(CommonsDAOJPAImplTest.class)
public class KernelIntegrationTest extends CommonsDAOJPAImplTest { // NOSONAR

  @Test
  public void beansInjected() {
    assertNotNull("Spring Bean @Service layer isn't included as Kernel component",
                  getContainer().getComponentInstanceOfType(TestService.class));
    assertNull("Spring Bean @Service layer and @Exlude shouldn't be defined as Kernel component",
               getContainer().getComponentInstanceOfType(TestExcludedService.class));
    assertNull("Spring Bean @Repository (Storage) layer shouldn't be defined as Kernel component",
               getContainer().getComponentInstanceOfType(TestStorage.class));
    assertNull("Spring Bean Dao layer shouldn't be defined as Kernel component",
               getContainer().getComponentInstanceOfType(TestJpaDao.class));
  }

}
