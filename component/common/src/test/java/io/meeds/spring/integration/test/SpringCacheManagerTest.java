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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.jpa.CommonsDAOJPAImplTest;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

import io.meeds.spring.module.service.TestCacheService;
import io.meeds.spring.module.service.TestCacheService.TestCacheException;

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

  /**
   * {@code @Cacheable(sync = true)} must load the value exactly once, however
   * many callers miss the cache concurrently. Before the adapter delegated to
   * {@code FutureExoCache}, its {@code get(key, valueLoader)} was a plain
   * check-then-load-then-put with no per-key locking, so every concurrent
   * caller ran the loader.
   */
  @Test
  public void syncCacheLoadsOnlyOnceUnderConcurrency() throws Exception {
    int key = 42;
    int concurrentCallers = 8;

    ExecutorService executor = Executors.newFixedThreadPool(concurrentCallers);
    List<Future<Integer>> results = new ArrayList<>();
    try {
      // Make every caller reach the cache at the same moment
      CyclicBarrier startTogether = new CyclicBarrier(concurrentCallers);
      for (int i = 0; i < concurrentCallers; i++) {
        results.add(executor.submit(() -> {
          startTogether.await(10, TimeUnit.SECONDS);
          return testCacheService.getSlow(key);
        }));
      }

      // One load is now in flight and blocked; let it finish
      assertTrue(testCacheService.getLoadStarted().await(10, TimeUnit.SECONDS));
      testCacheService.getLoadGate().countDown();

      for (Future<Integer> result : results) {
        assertEquals(key, result.get(10, TimeUnit.SECONDS).intValue());
      }
    } finally {
      executor.shutdownNow();
    }

    // The whole point: one execution of the method body, not eight
    assertEquals(1, testCacheService.getSyncLoadCount().get());

    ExoCache<Serializable, Object> syncCache = cacheService.getCacheInstance(TestCacheService.SYNC_CACHE_NAME);
    assertEquals(key, syncCache.get(key));

    testCacheService.removeSlow(key);
    assertNull(syncCache.get(key));
  }

  /**
   * A {@code @Cacheable(sync = true)} method must surface the exception it
   * threw, not the wrapper the future cache reports it as. Losing the type
   * turns a domain exception the REST contract maps to a status into an
   * {@code IllegalStateException}, i.e. a 500.
   */
  @Test
  public void syncCachePropagatesTheLoaderExceptionType() {
    TestCacheException thrown = assertThrows(TestCacheException.class, () -> testCacheService.getFailing(7));
    assertTrue(thrown.getMessage().contains("loader failed for 7"));

    // A failed load must leave nothing behind, so the next call retries
    ExoCache<Serializable, Object> syncCache = cacheService.getCacheInstance(TestCacheService.SYNC_CACHE_NAME);
    assertNull(syncCache.get(7));
    assertThrows(TestCacheException.class, () -> testCacheService.getFailing(7));
  }

  /**
   * Evicting must stop a reader from joining the load that was in flight when
   * the value was evicted: joining it would return the pre-eviction value and
   * let it be written back, leaving the cache stale until its TTL rather than
   * until the eviction.
   */
  @Test
  public void evictDoesNotLetALaterReaderJoinThePreEvictionLoad() throws Exception {
    int key = 77;
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> firstReader = executor.submit(() -> testCacheService.getSlowForEviction(key));
      // A load is now in flight and blocked inside the method body
      assertTrue(testCacheService.getEvictLoadStarted().await(10, TimeUnit.SECONDS));

      testCacheService.removeSlow(key);

      Future<Integer> readerAfterEviction = executor.submit(() -> testCacheService.getSlowForEviction(key));
      testCacheService.getEvictLoadGate().countDown();

      assertEquals(key, firstReader.get(10, TimeUnit.SECONDS).intValue());
      assertEquals(key, readerAfterEviction.get(10, TimeUnit.SECONDS).intValue());
    } finally {
      executor.shutdownNow();
    }

    // Two loads: the reader that arrived after the eviction ran its own rather
    // than sharing the one the eviction invalidated
    assertEquals(2, testCacheService.getEvictLoadCount().get());
  }

}
