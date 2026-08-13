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
package io.meeds.spring.module.service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.Getter;

@Service
public class TestCacheService {

  public static final String     CACHE_NAME      = "cache-test";

  public static final String     SYNC_CACHE_NAME = "cache-test-sync";

  /** Counts how many times the sync-cached method body was actually executed. */
  @Getter
  private final AtomicInteger    syncLoadCount   = new AtomicInteger();

  /** Released by the test to let a slow load complete. */
  @Getter
  private final CountDownLatch   loadGate        = new CountDownLatch(1);

  /** Counted down as soon as a load starts, so the test knows one is in flight. */
  @Getter
  private final CountDownLatch   loadStarted     = new CountDownLatch(1);

  /** Same three, dedicated to the eviction test so the gates are independent. */
  @Getter
  private final AtomicInteger    evictLoadCount  = new AtomicInteger();

  @Getter
  private final CountDownLatch   evictLoadGate   = new CountDownLatch(1);

  @Getter
  private final CountDownLatch   evictLoadStarted = new CountDownLatch(1);

  @Cacheable(CACHE_NAME)
  public int get(int i) {
    return i;
  }

  /**
   * Deliberately slow, so that concurrent callers overlap. With
   * {@code sync = true} the body must run exactly once whatever the number of
   * concurrent callers.
   */
  @Cacheable(cacheNames = SYNC_CACHE_NAME, sync = true)
  public int getSlow(int i) {
    syncLoadCount.incrementAndGet();
    loadStarted.countDown();
    try {
      loadGate.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return i;
  }

  @CacheEvict(cacheNames = SYNC_CACHE_NAME)
  public void removeSlow(int i) {
    // Nothing, just cache eviction
  }

  /**
   * Throws the domain exception a caller is expected to see. With
   * {@code sync = true} the value flows through a future, which wraps a failed
   * load twice — the caller must still receive this exception, not the wrapper.
   */
  @Cacheable(cacheNames = SYNC_CACHE_NAME, sync = true)
  public int getFailing(int i) {
    throw new TestCacheException("loader failed for " + i);
  }

  /**
   * Same as {@link #getSlow(int)} but with its own gates and counter, so the
   * eviction test does not depend on whether another test already released the
   * shared ones.
   */
  @Cacheable(cacheNames = SYNC_CACHE_NAME, sync = true)
  public int getSlowForEviction(int i) {
    evictLoadCount.incrementAndGet();
    evictLoadStarted.countDown();
    try {
      evictLoadGate.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return i;
  }

  /** Stands for a domain exception the org's REST contract maps to a status. */
  public static class TestCacheException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TestCacheException(String message) {
      super(message);
    }
  }

  @CacheEvict(CACHE_NAME)
  public void remove(int i) {
    // Nothing just cache clear
  }

  @CachePut(CACHE_NAME)
  public int update(int i) {
    return i * 2;
  }

}
