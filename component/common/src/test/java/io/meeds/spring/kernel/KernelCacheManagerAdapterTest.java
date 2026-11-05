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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.cache.Cache;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import org.exoplatform.services.cache.CacheMode;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCacheConfig;
import org.exoplatform.services.cache.ExoCacheConfigPlugin;
import org.exoplatform.services.cache.concurrent.ConcurrentFIFOExoCache;

/**
 * What a Spring cache does to the kernel cache behind it: in a cluster, the
 * kernel cache of an invalidated cache sends its puts, removes and clears to
 * the other nodes, so a value Spring loads must stay on the node that loaded
 * it, and an eviction must not. The cache is shared by the nodes when the
 * properties give it a mode, which the cluster profile does.
 */
class KernelCacheManagerAdapterTest {

  private RecordingCache      kernelCache;

  private CacheService        cacheService;

  private Map<String, Object> properties;

  private Cache               cache;

  @BeforeEach
  void setUp() {
    kernelCache = new RecordingCache();
    cacheService = mock(CacheService.class);
    when(cacheService.getAllCacheInstances()).thenReturn(new ArrayList<>());
    when(cacheService.getCacheInstance("test.spring")).thenAnswer(invocation -> kernelCache);
    properties = new HashMap<>();
    cache = getCache();
  }

  @Test
  void cacheWithAModeIsConfiguredBeforeItsCreation() {
    properties.put("meeds.cache.cacheMode", "local");
    properties.put("meeds.cache.test.spring.cacheMode", "asyncInvalidation");
    properties.put("meeds.cache.test.spring.max", "50");
    properties.put("meeds.cache.test.spring.ttl", "60");

    getCache();

    ExoCacheConfig config = getClusterConfig();
    assertEquals("test.spring", config.getName());
    assertEquals(CacheMode.ASYNCINVALIDATION, config.getCacheMode());
    assertEquals(50, config.getMaxSize());
    assertEquals(60, config.getLiveTime());
    // A clustered cache refuses to be resized
    assertEquals(List.of(), kernelCache.calls);
  }

  @Test
  void cacheWithoutAModeOfItsOwnTakesTheDefaultOneAndSize() {
    properties.put("meeds.cache.cacheMode", "asyncInvalidation");

    getCache();

    ExoCacheConfig config = getClusterConfig();
    assertEquals(CacheMode.ASYNCINVALIDATION, config.getCacheMode());
    assertEquals(300, config.getMaxSize());
    assertEquals(600, config.getLiveTime());
  }

  @Test
  void localCacheIsResizedOnceCreated() {
    properties.put("meeds.cache.cacheMode", "asyncInvalidation");
    properties.put("meeds.cache.test.spring.cacheMode", "local");
    properties.put("meeds.cache.test.spring.ttl", "60");

    getCache();

    verify(cacheService, never()).addExoCacheConfig(any(ExoCacheConfigPlugin.class));
    assertEquals(List.of("liveTime 60"), kernelCache.calls);
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

  private Cache getCache() {
    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
    kernelCache.calls.clear();
    return new KernelCacheManagerAdapter(cacheService, environment).getCache("test.spring");
  }

  private ExoCacheConfig getClusterConfig() {
    ArgumentCaptor<ExoCacheConfigPlugin> plugin = ArgumentCaptor.forClass(ExoCacheConfigPlugin.class);
    InOrder order = inOrder(cacheService);
    order.verify(cacheService).addExoCacheConfig(plugin.capture());
    order.verify(cacheService).getCacheInstance("test.spring");
    List<ExoCacheConfig> configs = plugin.getValue().getConfigs();
    assertEquals(1, configs.size());
    return configs.get(0);
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

    @Override
    public void setLiveTime(long period) {
      calls.add("liveTime " + period);
      super.setLiveTime(period);
    }

    @Override
    public void setMaxSize(int max) {
      calls.add("maxSize " + max);
      super.setMaxSize(max);
    }
  }
}
