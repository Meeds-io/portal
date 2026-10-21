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
package org.exoplatform.commons.cache.future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class FutureCacheTest {

  @Test
  void testClearFuturesInvalidatesTheLoadsInFlight() throws Exception {
    CountDownLatch loading = new CountDownLatch(1);
    CountDownLatch cleared = new CountDownLatch(1);
    FutureMap<String, String, Object> futureCache = new FutureMap<>((context, key) -> {
      loading.countDown();
      cleared.await(10, TimeUnit.SECONDS);
      return "previous value";
    });
    AtomicReference<String> loadedValue = new AtomicReference<>();
    Thread reader = new Thread(() -> loadedValue.set(futureCache.get(null, "key")));
    reader.start();
    try {
      assertTrue(loading.await(10, TimeUnit.SECONDS));
      futureCache.clearFutures();
    } finally {
      cleared.countDown();
      reader.join(10000);
    }

    assertEquals("previous value", loadedValue.get(), "The load in flight returns its value to its reader");
    assertNull(futureCache.get("key"), "The load in flight caches its value after the clear");
  }

}
