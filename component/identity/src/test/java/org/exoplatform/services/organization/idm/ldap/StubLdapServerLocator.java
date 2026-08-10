/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package org.exoplatform.services.organization.idm.ldap;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import javax.naming.NamingException;

/**
 * Test double that replaces the actual DNS SRV lookup and system clock with
 * fully controllable values, so tests never hit the network and never depend
 * on wall-clock time.
 * <p>
 * Background refreshes run synchronously by default, keeping most tests
 * deterministic without any timing games. Call {@link #runInBackgroundThread()}
 * to opt a specific test into the real {@link CompletableFuture}-based async
 * path instead, e.g. to verify that a refresh never blocks the caller.
 */
class StubLdapServerLocator extends LdapServerLocator {

  private List<SrvRecord>  recordsToReturn = Collections.emptyList();

  private NamingException  failure;

  private int               lookupCount;

  private long              now;

  private boolean           useRealAsync;

  private CompletableFuture<Void> lastBackgroundTask;

  private CountDownLatch    lookupStartedLatch;

  private CountDownLatch    lookupReleaseLatch;

  StubLdapServerLocator(String configuredProviderUrl) {
    super(configuredProviderUrl);
  }

  void setRecordsToReturn(List<SrvRecord> records) {
    this.recordsToReturn = records;
    this.failure = null;
  }

  void setFailure(NamingException failure) {
    this.failure = failure;
  }

  void setNow(long now) {
    this.now = now;
  }

  int getLookupCount() {
    return lookupCount;
  }

  /** From the next call on, run background refreshes on a real background thread. */
  void runInBackgroundThread() {
    this.useRealAsync = true;
  }

  /** Blocks the calling test until the last background refresh (if any) has completed. */
  void awaitBackgroundRefresh() {
    if (lastBackgroundTask != null) {
      lastBackgroundTask.join();
    }
  }

  /**
   * Makes the next {@link #lookupSrvRecords()} call count down {@code started}
   * as soon as it begins, then block until {@code release} counts down to 0 -
   * used to prove a caller reading the cached list is never blocked on it.
   */
  void blockNextLookupOn(CountDownLatch started, CountDownLatch release) {
    this.lookupStartedLatch = started;
    this.lookupReleaseLatch = release;
  }

  @Override
  List<SrvRecord> lookupSrvRecords() throws NamingException {
    lookupCount++;
    if (lookupStartedLatch != null) {
      lookupStartedLatch.countDown();
    }
    if (lookupReleaseLatch != null) {
      CountDownLatch release = lookupReleaseLatch;
      lookupStartedLatch = null;
      lookupReleaseLatch = null;
      try {
        release.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    if (failure != null) {
      throw failure;
    }
    return recordsToReturn;
  }

  @Override
  long currentTimeMillis() {
    return now;
  }

  @Override
  void runAsync(Runnable task) {
    if (useRealAsync) {
      lastBackgroundTask = CompletableFuture.runAsync(task);
    } else {
      // Run synchronously so background-refresh assertions stay deterministic by default.
      task.run();
    }
  }
}
