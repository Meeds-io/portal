/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.spring.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.core.env.Environment;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.concurrent.ConcurrentFIFOExoCache;

/**
 * What a Spring cache does to the kernel cache behind it: in a cluster, the
 * kernel cache of an invalidated cache sends its puts, removes and clears to
 * the other nodes, so a value Spring loads must stay on the node that loaded
 * it, and an eviction must not.
 */
class KernelCacheManagerAdapterTest {

  private RecordingCache kernelCache;

  private Cache          cache;

  @BeforeEach
  void setUp() {
    kernelCache = new RecordingCache();
    CacheService cacheService = mock(CacheService.class);
    when(cacheService.getAllCacheInstances()).thenReturn(new ArrayList<>());
    when(cacheService.getCacheInstance("test.spring")).thenAnswer(invocation -> kernelCache);
    Environment environment = mock(Environment.class);
    when(environment.getProperty(anyString(), eq(""))).thenReturn("");
    cache = new KernelCacheManagerAdapter(cacheService, environment).getCache("test.spring");
  }

  @Test
  void putOfALoadedValueStaysOnTheNode() {
    cache.put("key", "loaded");

    assertEquals(List.of("putLocal key"), kernelCache.calls);
    assertEquals("loaded", cache.get("key").get());
  }

  @Test
  void synchronizedLoadStaysOnTheNode() {
    cache.get("key", () -> "loaded");

    assertEquals(List.of("putLocal key"), kernelCache.calls);
  }

  @Test
  void evictionReachesTheKernelCache() {
    cache.evict("key");

    assertEquals(List.of("remove key"), kernelCache.calls);
  }

  @Test
  void clearReachesTheKernelCache() {
    cache.clear();

    assertEquals(List.of("clear"), kernelCache.calls);
  }

  /**
   * A kernel cache that records how it is changed.
   */
  private static class RecordingCache extends ConcurrentFIFOExoCache<Serializable, Object> {

    private final List<String> calls = new ArrayList<>();

    private RecordingCache() {
      super("test.spring", 100);
    }

    @Override
    public void put(Serializable key, Object value) {
      calls.add("put " + key);
      super.put(key, value);
    }

    @Override
    public void putLocal(Serializable key, Object value) {
      calls.add("putLocal " + key);
      super.putLocal(key, value);
    }

    @Override
    public Object remove(Serializable key) {
      calls.add("remove " + key);
      return super.remove(key);
    }

    @Override
    public void clearCache() {
      calls.add("clear");
      super.clearCache();
    }
  }
}
