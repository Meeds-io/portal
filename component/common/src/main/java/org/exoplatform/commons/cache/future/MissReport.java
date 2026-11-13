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

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Counts the loads of one future cache that found no value. The cache keeps no
 * entry for them, so each read of such a key calls the loader again, most often
 * a store query: a key read again and again while the store does not hold it is
 * a wrong call, or a value that should be cached as absent.
 * <p>
 * The count is logged at info level, at most once per period and cache: a load
 * that finds no value is routine (a login typed wrong, a name the store does not
 * hold), so the count is a diagnosis, not an alert. The keys and their callers
 * are logged at debug level only, since a key can be what a user typed, a
 * password typed in a login field included.
 */
class MissReport {

  /** The period between two reports of a cache, in seconds; 0 turns them off. */
  static final String  PERIOD_PROPERTY       = "meeds.cache.missReport.period";

  static final long    DEFAULT_PERIOD_SECONDS = 300;

  private static final long NEVER            = Long.MIN_VALUE;

  private static final Log  LOG              = ExoLogger.getLogger(FutureCache.class);

  private final String      cacheName;

  private final long        periodMillis;

  private final LongSupplier clock;

  private final AtomicLong  misses           = new AtomicLong();

  private final AtomicLong  lastReport       = new AtomicLong(NEVER);

  MissReport(String cacheName) {
    this(cacheName, periodSeconds(), System::currentTimeMillis);
  }

  MissReport(String cacheName, long periodSeconds, LongSupplier clock) {
    this.cacheName = cacheName;
    this.periodMillis = periodSeconds * 1000;
    this.clock = clock;
  }

  /**
   * Counts a load that found no value, and logs the count when the period of
   * the previous report is over
   *
   * @param  key the key whose load found no value
   * @return     the count the report logged, 0 when none was logged
   */
  long onMiss(Object key) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Cache {}: no value for the key {}", cacheName, key, new Exception("Caller of the read"));
    }
    if (periodMillis <= 0) {
      return 0;
    }
    misses.incrementAndGet();
    long now = clock.getAsLong();
    long last = lastReport.get();
    if ((last != NEVER && now - last < periodMillis) || !lastReport.compareAndSet(last, now)) {
      return 0;
    }
    long count = misses.getAndSet(0);
    LOG.info("Cache {}: {} read(s) found no value since the previous report. The cache keeps no entry for them, so each one called its loader again, most often a store query. The keys and their callers are logged at debug level by {}",
             cacheName,
             count,
             FutureCache.class.getName());
    return count;
  }

  long pendingMisses() {
    return misses.get();
  }

  private static long periodSeconds() {
    return parsePeriod(PropertyManager.getProperty(PERIOD_PROPERTY));
  }

  /**
   * @param  value the property's value
   * @return       the period in seconds: the default when the value is not set,
   *               not a number, negative, or too large to count in
   *               milliseconds
   */
  static long parsePeriod(String value) {
    if (value == null || value.isBlank()) {
      return DEFAULT_PERIOD_SECONDS;
    }
    long seconds;
    try {
      seconds = Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      seconds = -1;
    }
    if (seconds < 0 || seconds > Long.MAX_VALUE / 1000) {
      LOG.warn("Property {} is not a number of seconds from 0, which turns the report off: {}. {} is used",
               PERIOD_PROPERTY,
               value,
               DEFAULT_PERIOD_SECONDS);
      return DEFAULT_PERIOD_SECONDS;
    }
    return seconds;
  }
}
