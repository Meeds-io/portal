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
package org.exoplatform.settings.impl;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.api.settings.data.SettingKey;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.testing.BaseTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.settings.cache.CacheSettingServiceImpl;

import jakarta.persistence.EntityTransaction;

/** test just for the cache
 * Created by The eXo Platform SAS Author : eXoPlatform bangnv@exoplatform.com
 * Nov 22, 2012
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.settings-configuration-local.xml")
})
public class CacheSettingTest extends BaseTestCase {
  protected static SettingService               settingService;

  private ExoCache<SettingKey, SettingValue<?>> settingCache;

  private EntityManagerService                  entityManagerService;

  private final static String SETTING_CACHING_NAME = "commons.SettingService";


  @Override
  public void setUp() throws Exception {
    super.setUp();
    settingCache = getService(CacheService.class).getCacheInstance(SETTING_CACHING_NAME);
    settingService = getService(SettingService.class);
    entityManagerService = getService(EntityManagerService.class);
    assertNotNull("Cannot find Setting Service", settingService);
    assertTrue("settingService should be of type CacheSettingServiceImpl instead of " + settingService.getClass().getName(), settingService instanceof CacheSettingServiceImpl);
    ConversationState c = new ConversationState(new Identity("test"));
    ConversationState.setCurrent(c);  
    settingCache.clearCache();
  }

  public void testSetAndSimpleRemove() {

    // set a simple Key and String Value: the write evicts, the read loads it
    assertEquals(0, settingCache.getCacheSize());
    settingService.set(Context.USER, Scope.SPACE, "a", SettingValue.create("b"));
    assertEquals(0, settingCache.getCacheSize());
    assertEquals("b", settingService.get(Context.USER, Scope.SPACE, "a").getValue());
    assertEquals(1, settingCache.getCacheSize());
    assertEquals("b", settingCache.get(new SettingKey(Context.USER, Scope.SPACE, "a")).getValue());

    // set a new value of a cached key
    settingService.set(Context.USER, Scope.SPACE, "a", SettingValue.create("c"));
    assertNull(settingCache.get(new SettingKey(Context.USER, Scope.SPACE, "a")));
    assertEquals("c", settingService.get(Context.USER, Scope.SPACE, "a").getValue());

    // remove
    settingService.remove(Context.USER, Scope.SPACE, "a");
    assertEquals(0, settingCache.getCacheSize());
    assertNull(settingService.get(Context.USER, Scope.SPACE, "a"));

    // set a simple Key and Long Value: the store keeps and returns its text
    settingService.set(Context.USER, Scope.SPACE, "b", SettingValue.create(Long.valueOf(987)));
    assertEquals("987", settingService.get(Context.USER, Scope.SPACE, "b").getValue());
    assertEquals("987", settingCache.get(new SettingKey(Context.USER, Scope.SPACE, "b")).getValue());
    settingService.remove(Context.USER, Scope.SPACE, "b");
    assertNull(settingCache.get(new SettingKey(Context.USER, Scope.SPACE, "b")));

    // set a simple Key and Double Value
    settingService.set(Context.USER, Scope.SPACE, "b", SettingValue.create(Double.valueOf(1.5)));
    assertEquals("1.5", settingService.get(Context.USER, Scope.SPACE, "b").getValue());
    assertEquals("1.5", settingCache.get(new SettingKey(Context.USER, Scope.SPACE, "b")).getValue());
    settingService.remove(Context.USER, Scope.SPACE, "b");

    // set a simple key and Boolean Value
    settingService.set(Context.USER, Scope.SPACE, "b", SettingValue.create(true));
    assertEquals("true", settingService.get(Context.USER, Scope.SPACE, "b").getValue());
    assertEquals("true", settingCache.get(new SettingKey(Context.USER, Scope.SPACE, "b")).getValue());
    settingService.remove(Context.USER, Scope.SPACE, "b");
    assertNull(settingCache.get(new SettingKey(Context.USER, Scope.SPACE, "b")));
  }

  public void testSetAndRemovePage() {
    assertEquals(0, settingCache.getCacheSize());
    settingService.set(Context.GLOBAL, Scope.PAGE.id("name1"), "x1", SettingValue.create("y1"));
    settingService.set(Context.GLOBAL, Scope.PAGE.id("name1"), "x2", SettingValue.create("y2"));
    assertEquals("y1", settingService.get(Context.GLOBAL, Scope.PAGE.id("name1"), "x1").getValue());
    assertEquals("y2", settingService.get(Context.GLOBAL, Scope.PAGE.id("name1"), "x2").getValue());
    assertEquals(2, settingCache.getCacheSize());
    settingService.remove(Context.GLOBAL, Scope.PAGE.id("name1"));
    assertEquals(0, settingCache.getCacheSize());
    assertNull(settingService.get(Context.GLOBAL, Scope.PAGE.id("name1"), "x1"));
    assertNull(settingService.get(Context.GLOBAL, Scope.PAGE.id("name1"), "x2"));
  }

  public void testSetAndRemoveUser() {
    assertEquals(0, settingCache.getCacheSize());
    settingService.set(Context.USER, Scope.PORTAL.id("name1"), "x1", SettingValue.create("y1"));
    settingService.set(Context.USER, Scope.PORTAL.id("name2"), "x2", SettingValue.create("y2"));
    settingService.set(Context.USER, Scope.PAGE.id("name3"), "x3", SettingValue.create("y3"));
    settingService.set(Context.USER, Scope.PAGE.id("name4"), "x4", SettingValue.create("y4"));
    // verify
    assertEquals("y1", settingService.get(Context.USER, Scope.PORTAL.id("name1"), "x1").getValue());
    assertEquals("y2", settingService.get(Context.USER, Scope.PORTAL.id("name2"), "x2").getValue());
    assertEquals("y3", settingService.get(Context.USER, Scope.PAGE.id("name3"), "x3").getValue());
    assertEquals("y4", settingService.get(Context.USER, Scope.PAGE.id("name4"), "x4").getValue());
    assertEquals(4, settingCache.getCacheSize());
    // remove all
    settingService.remove(Context.USER);
    assertEquals(0, settingCache.getCacheSize());
    assertNull(settingService.get(Context.USER, Scope.PORTAL.id("name1"), "x1"));
    assertNull(settingService.get(Context.USER, Scope.PAGE.id("name4"), "x4"));
  }

  public void testSettingLoadInFlightDoesNotCacheWhatItReadBeforeASet() throws Exception {
    SettingKey key = new SettingKey(Context.USER, Scope.SPACE, "inflight");
    settingService.set(Context.USER, Scope.SPACE, "inflight", SettingValue.create("before"));

    assertLoadInFlightIsNotCached(key,
                                  () -> settingService.set(Context.USER,
                                                           Scope.SPACE,
                                                           "inflight",
                                                           SettingValue.create("after")));
    assertEquals("after", settingService.get(Context.USER, Scope.SPACE, "inflight").getValue());
  }

  public void testSettingLoadInFlightDoesNotCacheWhatItReadBeforeAScopeRemove() throws Exception {
    SettingKey key = new SettingKey(Context.GLOBAL, Scope.PAGE.id("inflight"), "x");
    settingService.set(Context.GLOBAL, Scope.PAGE.id("inflight"), "x", SettingValue.create("removed"));

    assertLoadInFlightIsNotCached(key, () -> settingService.remove(Context.GLOBAL, Scope.PAGE.id("inflight")));
    assertNull(settingService.get(Context.GLOBAL, Scope.PAGE.id("inflight"), "x"));
  }

  /**
   * Starts a load of the key that read the former value and waits, runs the
   * write, then lets the load end: the value it read must not be cached
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void assertLoadInFlightIsNotCached(SettingKey key, Runnable write) throws Exception {
    Field field = CacheSettingServiceImpl.class.getDeclaredField("futureExoCache");
    field.setAccessible(true);
    Object original = field.get(settingService);
    SettingValue<?> stale = SettingValue.create("stale");
    CountDownLatch loading = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    FutureExoCache<SettingKey, SettingValue, SettingService> blocked = new FutureExoCache<>((service, k) -> {
      loading.countDown();
      release.await(10, SECONDS);
      return stale;
    }, (ExoCache) settingCache);
    field.set(settingService, blocked);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<SettingValue> load = executor.submit(() -> blocked.get(settingService, key));
      assertTrue(loading.await(10, SECONDS));

      write.run();
      release.countDown();
      assertSame(stale, load.get(10, SECONDS));

      assertNull(settingCache.get(key));
    } finally {
      release.countDown();
      executor.shutdownNow();
      field.set(settingService, original);
    }
  }

  public void testSettingCachedAgainBeforeTheCommitOfASetIsEvictedAfterIt() {
    SettingKey key = new SettingKey(Context.USER, Scope.SPACE, "set");
    settingService.set(Context.USER, Scope.SPACE, "set", SettingValue.create("before"));
    inTransaction(() -> {
      settingService.set(Context.USER, Scope.SPACE, "set", SettingValue.create("after"));
      // A read on another thread, before the commit, loads the former value
      settingCache.put(key, SettingValue.create("before"));
    });

    assertNull(settingCache.get(key));
    assertEquals("after", settingService.get(Context.USER, Scope.SPACE, "set").getValue());
  }

  public void testSettingCachedAgainBeforeTheCommitOfARemoveIsEvictedAfterIt() {
    SettingKey key = new SettingKey(Context.USER, Scope.SPACE, "remove");
    settingService.set(Context.USER, Scope.SPACE, "remove", SettingValue.create("removed"));
    inTransaction(() -> {
      settingService.remove(Context.USER, Scope.SPACE, "remove");
      settingCache.put(key, SettingValue.create("removed"));
    });

    assertNull(settingCache.get(key));
    assertNull(settingService.get(Context.USER, Scope.SPACE, "remove"));
  }

  public void testSettingsCachedAgainBeforeTheCommitOfAScopeRemoveAreEvictedAfterIt() {
    SettingKey key = new SettingKey(Context.GLOBAL, Scope.PAGE.id("scope"), "x");
    settingService.set(Context.GLOBAL, Scope.PAGE.id("scope"), "x", SettingValue.create("removed"));
    inTransaction(() -> {
      settingService.remove(Context.GLOBAL, Scope.PAGE.id("scope"));
      settingCache.put(key, SettingValue.create("removed"));
    });

    assertNull(settingCache.get(key));
    assertNull(settingService.get(Context.GLOBAL, Scope.PAGE.id("scope"), "x"));
  }

  public void testSettingsCachedAgainBeforeTheCommitOfAContextRemoveAreEvictedAfterIt() {
    SettingKey key = new SettingKey(Context.USER, Scope.PORTAL.id("context"), "x");
    settingService.set(Context.USER, Scope.PORTAL.id("context"), "x", SettingValue.create("removed"));
    inTransaction(() -> {
      settingService.remove(Context.USER);
      settingCache.put(key, SettingValue.create("removed"));
    });

    assertNull(settingCache.get(key));
    assertNull(settingService.get(Context.USER, Scope.PORTAL.id("context"), "x"));
  }

  private void inTransaction(Runnable work) {
    EntityTransaction transaction = entityManagerService.getEntityManager().getTransaction();
    transaction.begin();
    try {
      work.run();
    } finally {
      transaction.commit();
    }
  }

}
