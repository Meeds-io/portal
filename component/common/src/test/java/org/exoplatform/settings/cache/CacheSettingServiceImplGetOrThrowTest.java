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
package org.exoplatform.settings.cache;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.settings.jpa.JPASettingServiceImpl;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class CacheSettingServiceImplGetOrThrowTest {

  @Test
  public void getOrThrowRaisesTheFailureOfALoadWhileGetAnswersNull() {
    JPASettingServiceImpl jpaService = mock(JPASettingServiceImpl.class);
    CacheService cacheService = mock(CacheService.class);
    when(cacheService.getCacheInstance(any(String.class))).thenReturn(mock(ExoCache.class));
    when(jpaService.get(any(Context.class), any(Scope.class), any(String.class))).thenThrow(new IllegalStateException("read failed"));
    CacheSettingServiceImpl service = new CacheSettingServiceImpl(jpaService, cacheService);

    assertNull(service.get(Context.GLOBAL, Scope.GLOBAL, "key"));
    IllegalStateException failure = assertThrows(IllegalStateException.class,
                                                 () -> service.getOrThrow(Context.GLOBAL, Scope.GLOBAL, "key"));
    assertNotNull(failure.getCause());
  }

  @Test
  public void getOrThrowAnswersNullForAnAbsentKey() {
    JPASettingServiceImpl jpaService = mock(JPASettingServiceImpl.class);
    CacheService cacheService = mock(CacheService.class);
    when(cacheService.getCacheInstance(any(String.class))).thenReturn(mock(ExoCache.class));
    when(jpaService.get(any(Context.class), any(Scope.class), any(String.class))).thenReturn(null);
    CacheSettingServiceImpl service = new CacheSettingServiceImpl(jpaService, cacheService);

    assertNull(service.getOrThrow(Context.GLOBAL, Scope.GLOBAL, "key"));
  }

  @Test
  public void getOrThrowRaisesWhenTheWaitForAConcurrentLoadIsInterrupted() throws Exception {
    JPASettingServiceImpl jpaService = mock(JPASettingServiceImpl.class);
    CacheService cacheService = mock(CacheService.class);
    when(cacheService.getCacheInstance(any(String.class))).thenReturn(mock(ExoCache.class));
    CountDownLatch loading = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    when(jpaService.get(any(Context.class), any(Scope.class), any(String.class))).thenAnswer(i -> {
      loading.countDown();
      release.await(10, TimeUnit.SECONDS);
      return new SettingValue("true");
    });
    CacheSettingServiceImpl service = new CacheSettingServiceImpl(jpaService, cacheService);
    Thread loader = new Thread(() -> service.get(Context.GLOBAL, Scope.GLOBAL, "key"));
    loader.start();
    try {
      assertTrue(loading.await(10, TimeUnit.SECONDS));

      Thread.currentThread().interrupt();
      IllegalStateException failure = assertThrows(IllegalStateException.class,
                                                   () -> service.getOrThrow(Context.GLOBAL, Scope.GLOBAL, "key"));
      assertNull(failure.getCause());
      assertTrue(failure.getMessage().contains("key"));
    } finally {
      Thread.interrupted();
      release.countDown();
      loader.join(10000);
    }
    assertFalse(loader.isAlive());
  }
}
