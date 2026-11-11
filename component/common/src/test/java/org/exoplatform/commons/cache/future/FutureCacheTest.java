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
import java.util.concurrent.atomic.AtomicLong;
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

  @Test
  void testALoadThatFindsNoValueIsCountedAndNotCached() {
    FutureMap<String, String, Object> futureCache = new FutureMap<>((context, key) -> "known".equals(key) ? "value" : null);

    assertNull(futureCache.get(null, "unknown"));
    assertNull(futureCache.get(null, "unknown"));
    assertNull(futureCache.get(null, "unknown"));
    assertEquals("value", futureCache.get(null, "known"));

    assertNull(futureCache.get("unknown"), "No entry is kept for a key the loader found no value for");
    assertEquals(2,
                 futureCache.missReport().pendingMisses(),
                 "The first miss is reported at once, the two others wait for the next report, the hit is not counted");
  }

  @Test
  void testTheMissesAreReportedOncePerPeriod() {
    AtomicLong now = new AtomicLong(1_000_000);
    MissReport report = new MissReport("cache", 60, now::get);

    assertEquals(1, report.onMiss("a"), "The first miss is reported at once");
    assertEquals(0, report.onMiss("b"));
    now.addAndGet(59_999);
    assertEquals(0, report.onMiss("c"), "No second report within the period");
    now.addAndGet(1);
    assertEquals(3, report.onMiss("d"), "The report counts every miss since the previous one");
    assertEquals(0, report.pendingMisses());
  }

  @Test
  void testAPeriodOutOfRangeFallsBackToTheDefault() {
    assertEquals(MissReport.DEFAULT_PERIOD_SECONDS, MissReport.parsePeriod(null));
    assertEquals(MissReport.DEFAULT_PERIOD_SECONDS, MissReport.parsePeriod("not a number"));
    assertEquals(MissReport.DEFAULT_PERIOD_SECONDS, MissReport.parsePeriod("-1"));
    assertEquals(MissReport.DEFAULT_PERIOD_SECONDS, MissReport.parsePeriod(String.valueOf(Long.MAX_VALUE)));
    assertEquals(0, MissReport.parsePeriod("0"));
    assertEquals(60, MissReport.parsePeriod(" 60 "));
  }

  @Test
  void testAPeriodOfZeroTurnsTheReportOff() {
    MissReport report = new MissReport("cache", 0, () -> 0);

    assertEquals(0, report.onMiss("a"));
    assertEquals(0, report.pendingMisses());
  }

}
