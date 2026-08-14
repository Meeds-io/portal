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
package io.meeds.spring.kernel;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.core.env.Environment;

import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

import lombok.Setter;

public class KernelCacheManagerAdapter implements CacheManager {

  @Setter
  private CacheService       cacheService;

  @Setter
  private Environment        environment;

  private Map<String, Cache> cacheInstances = new ConcurrentHashMap<>();

  public KernelCacheManagerAdapter(CacheService cacheService, Environment environment) {
    this.cacheService = cacheService;
    this.environment = environment;
  }

  @Override
  public Collection<String> getCacheNames() {
    return cacheService.getAllCacheInstances().stream().map(ExoCache::getName).toList();
  }

  @Override
  public Cache getCache(String name) { // NOSONAR
    return cacheInstances.computeIfAbsent(name, k -> {
      boolean isNew = cacheService.getAllCacheInstances()
                                  .stream()
                                  .map(ExoCache::getName)
                                  .noneMatch(n -> StringUtils.equals(n, name));
      ExoCache<Serializable, Object> cacheInstance = cacheService.getCacheInstance(name);
      if (isNew) {
        String ttl = environment.getProperty("meeds.cache." + name + ".ttl", "");
        if (StringUtils.isNotBlank(ttl)) {
          cacheInstance.setLiveTime(Integer.parseInt(ttl));
        }

        String maxElements = environment.getProperty("meeds.cache." + name + ".max", "");
        if (StringUtils.isNotBlank(maxElements)) {
          cacheInstance.setMaxSize(Integer.parseInt(maxElements));
        }
      }

      // Backs Spring's @Cacheable(sync = true): FutureExoCache guarantees that
      // concurrent misses on the same key trigger a single load, the other
      // threads waiting on that same future instead of loading in parallel.
      // Scope note: one adapter — hence one FutureExoCache per name — exists per
      // Spring context, i.e. per WAR. Two WARs reading the same cache name would
      // not coalesce their misses with each other. Cache names are addon-scoped
      // in practice, so this is theoretical; do not assume otherwise.
      Loader<Serializable, Object, Callable<?>> loader = (valueLoader, cacheKey) -> valueLoader.call();
      FutureExoCache<Serializable, Object, Callable<?>> futureCache = new FutureExoCache<>(loader, cacheInstance);

      return new AbstractValueAdaptingCache(false) {

        @Override
        public String getName() {
          return cacheInstance.getName();
        }

        @Override
        public Object getNativeCache() {
          return cacheInstance;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Callable<T> valueLoader) {
          try {
            return (T) futureCache.get(valueLoader, getSerializableKey(key));
          } catch (RuntimeException e) {
            // Spring's sync path rethrows the cause of ValueRetrievalException,
            // so the loader's own exception has to be the cause: the caller must
            // still see the exception its method threw. FutureCache wraps it
            // twice on the way out, and losing the type would turn a domain
            // exception mapped to a 404 into a 500.
            throw new ValueRetrievalException(key, valueLoader, unwrapLoaderException(e));
          }
        }

        @Override
        public void put(Object key, Object value) {
          cacheInstance.put(getSerializableKey(key), value);
        }

        @Override
        public void evict(Object key) {
          Serializable serializableKey = getSerializableKey(key);
          // Invalidated before the value is dropped, so that a load completing
          // in between cannot write back what this eviction is about to remove.
          // Without it, a reader arriving just after the eviction would join the
          // pre-eviction load, get the stale value and let it be re-cached —
          // leaving the entry stale until the TTL instead of until the eviction.
          //
          // Not absolute: a load that has already passed its invalidation check
          // can still write between that check and its own put. That window is
          // a few instructions rather than a whole load duration, and the cache
          // TTL remains the backstop, as it is for every other staleness source
          // here.
          futureCache.removeFuture(serializableKey);
          cacheInstance.remove(serializableKey);
        }

        @Override
        public void clear() {
          futureCache.clearFutures();
          cacheInstance.clearCache();
        }

        @Override
        protected Object lookup(Object key) {
          key = getSerializableKey(key);
          return cacheInstance.get((Serializable) key);
        }

        private Serializable getSerializableKey(Object key) {
          if (key instanceof Serializable serializable) {
            return serializable;
          } else {
            return key.hashCode();
          }
        }

      };
    });
  }

  /**
   * Recovers the exception a {@code @Cacheable(sync = true)} method actually
   * threw. {@code FutureCache} reports a failed load as
   * {@code IllegalStateException(ExecutionException(original))}; handing that
   * wrapper to Spring as the cause would make every sync-cached method surface
   * its domain exceptions as an {@code IllegalStateException}, turning, for
   * instance, a not-found mapped to 404 into a 500.
   *
   * @param  exception the exception raised by the future cache
   * @return           the exception thrown by the value loader, or the given
   *                   one when it carries no such cause
   */
  private Throwable unwrapLoaderException(RuntimeException exception) {
    Throwable cause = exception.getCause();
    if (exception instanceof IllegalStateException && cause instanceof java.util.concurrent.ExecutionException
        && cause.getCause() != null) {
      return cause.getCause();
    }
    return exception;
  }

}
