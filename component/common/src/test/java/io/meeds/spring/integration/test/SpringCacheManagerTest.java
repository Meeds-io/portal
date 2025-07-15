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

import java.io.Serializable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.jpa.CommonsDAOJPAImplTest;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

import io.meeds.spring.module.service.TestCacheService;

@SpringJUnitConfig(CommonsDAOJPAImplTest.class)
public class SpringCacheManagerTest extends CommonsDAOJPAImplTest { // NOSONAR

  @Autowired
  private CacheManager     cacheManager;

  @Autowired
  private CacheService     cacheService;

  @Autowired
  private TestCacheService testCacheService;

  @Test
  public void beansInjected() {
    assertNotNull(PortalContainer.getInstance().getComponentInstanceOfType(CacheService.class));
    assertNotNull(testCacheService);
    assertNotNull(cacheService);
    assertNotNull(cacheManager);
  }

  @Test
  public void cacheBehavior() {
    assertEquals(5, testCacheService.get(5));
    assertEquals(14, testCacheService.update(7));

    ExoCache<Serializable, Object> cacheInstance = cacheService.getCacheInstance(TestCacheService.CACHE_NAME);
    assertNotNull(cacheInstance);

    assertEquals(5, cacheInstance.get(5));
    assertEquals(14, cacheInstance.get(7));

    testCacheService.remove(5);
    assertNull(cacheInstance.get(5));
    assertEquals(14, cacheInstance.get(7));

    testCacheService.remove(7);
    assertNull(cacheInstance.get(5));
    assertNull(cacheInstance.get(7));
  }

}
