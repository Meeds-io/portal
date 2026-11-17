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
package io.meeds.portal.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIApplication;

public class StaticPortalStateManagerTest {

  @Test
  public void testConcurrentFirstRequestsBuildTheRootOnce() throws Exception {
    AtomicInteger builds = new AtomicInteger();
    CountDownLatch building = new CountDownLatch(1);
    StaticPortalStateManager stateManager = new StaticPortalStateManager() {
      @Override
      UIApplication createStaticUIApplication(WebuiRequestContext context, WebuiApplication app) {
        builds.incrementAndGet();
        building.countDown();
        try {
          // Leaves the second request time to reach the build too
          Thread.sleep(300);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        return mock(UIApplication.class);
      }
    };
    WebuiRequestContext context = mock(WebuiRequestContext.class);
    when(context.getApplication()).thenReturn(mock(WebuiApplication.class));
    ExecutorService requests = Executors.newFixedThreadPool(2);
    try {
      Future<UIApplication> first = requests.submit(() -> stateManager.restoreUIRootComponent(context));
      building.await(5, TimeUnit.SECONDS);
      Future<UIApplication> second = requests.submit(() -> stateManager.restoreUIRootComponent(context));

      assertSame(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
      assertEquals(1, builds.get());
    } finally {
      requests.shutdownNow();
    }
  }

}
