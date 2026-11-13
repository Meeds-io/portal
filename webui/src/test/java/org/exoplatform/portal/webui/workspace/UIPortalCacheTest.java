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
package org.exoplatform.portal.webui.workspace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.invalidation.AsyncInvalidationExoCache;
import org.exoplatform.services.cache.invalidation.HashCode;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;

/**
 * The cache of built sites over the cache the cluster profile builds: an
 * asynchronously invalidated cache, whose values stay on their node and whose
 * replicated cache carries, to every node, what a {@code put}, a
 * {@code remove} or a {@code clearCache} sends.
 */
public class UIPortalCacheTest {

  private static final String                        META_PORTAL = "dw";

  private ExoCache<SiteKey, HashCode<UIPortal>>      replicated;

  private ScheduledExecutorService                   secondEvictionExecutor;

  private UIPortalCache                              uiPortalCache;

  private Map<String, Listener<LayoutService, ?>>   listeners;

  @Before
  @SuppressWarnings("unchecked")
  public void setUp() {
    replicated = mock(ExoCache.class);
    when(replicated.getName()).thenReturn(UIPortalCache.CACHE_NAME);
    when(replicated.getMaxSize()).thenReturn(5000);
    when(replicated.getLiveTime()).thenReturn(86400L);
    secondEvictionExecutor = mock(ScheduledExecutorService.class);
    uiPortalCache = new UIPortalCache(new AsyncInvalidationExoCache<>(replicated), () -> META_PORTAL, secondEvictionExecutor);
    listeners = new HashMap<>();
    ListenerService listenerService = mock(ListenerService.class);
    doAnswer(invocation -> listeners.put(invocation.getArgument(0), invocation.getArgument(1))).when(listenerService)
                                                                                                .addListener(any(String.class),
                                                                                                             any(Listener.class));
    uiPortalCache.listenTo(listenerService);
  }

  @Test
  public void testABuiltSiteStaysOnTheNodeThatBuiltIt() {
    UIPortal administration = build(SiteKey.portal("administration"));

    assertSame(administration, uiPortalCache.get(SiteKey.portal("administration")));
    verify(replicated, never()).put(any(), any());
  }

  @Test
  public void testASavedSiteIsEvictedOnEveryNode() throws Exception {
    build(new SiteKey(PortalConfig.PORTAL_TYPE, "administration"));

    // The key of the stored site, as the event carries it
    fire(LayoutService.PORTAL_CONFIG_UPDATED, new PortalConfig(PortalConfig.PORTAL_TYPE, "administration"));

    assertNull(uiPortalCache.get(SiteKey.portal("administration")));
    verify(replicated).put(eq(SiteKey.portal("administration")), any());
  }

  @Test
  public void testACreatedOrRemovedSiteIsEvictedOnEveryNode() throws Exception {
    build(SiteKey.group("/spaces/team"));
    fire(LayoutService.PORTAL_CONFIG_REMOVED, new PortalConfig(PortalConfig.GROUP_TYPE, "/spaces/team"));
    assertNull(uiPortalCache.get(SiteKey.group("/spaces/team")));

    build(SiteKey.group("/spaces/team"));
    fire(LayoutService.PORTAL_CONFIG_CREATED, new PortalConfig(PortalConfig.GROUP_TYPE, "/spaces/team"));
    assertNull(uiPortalCache.get(SiteKey.group("/spaces/team")));
  }

  @Test
  public void testASavedOrRemovedPageEvictsItsSiteOnEveryNode() throws Exception {
    build(SiteKey.portal("administration"));
    fire(LayoutService.PAGE_UPDATED, page("portal::administration::home"));
    assertNull(uiPortalCache.get(SiteKey.portal("administration")));
    verify(replicated).put(eq(SiteKey.portal("administration")), any());

    build(SiteKey.portal("administration"));
    fire(LayoutService.PAGE_REMOVED, page("portal::administration::home"));
    assertNull(uiPortalCache.get(SiteKey.portal("administration")));
  }

  @Test
  public void testAChangedMetaSiteEvictsEverySiteOnEveryNode() throws Exception {
    build(SiteKey.portal("administration"));
    build(SiteKey.portal(META_PORTAL));

    fire(LayoutService.PORTAL_CONFIG_UPDATED, new PortalConfig(PortalConfig.PORTAL_TYPE, META_PORTAL));

    assertNull(uiPortalCache.get(SiteKey.portal("administration")));
    assertNull(uiPortalCache.get(SiteKey.portal(META_PORTAL)));
    // A clear reaches the other nodes as a marker, not as a site key
    @SuppressWarnings({ "unchecked", "rawtypes" })
    ExoCache untyped = replicated;
    ArgumentCaptor<Serializable> sent = ArgumentCaptor.forClass(Serializable.class);
    verify(untyped).put(sent.capture(), any());
    assertFalse(sent.getValue() instanceof SiteKey);
  }

  @Test
  public void testAChangedSiteIsEvictedASecondTimeAfterTheDelay() throws Exception {
    build(SiteKey.portal("administration"));
    fire(LayoutService.PORTAL_CONFIG_UPDATED, new PortalConfig(PortalConfig.PORTAL_TYPE, "administration"));
    ArgumentCaptor<Runnable> secondEviction = ArgumentCaptor.forClass(Runnable.class);
    verify(secondEvictionExecutor).schedule(secondEviction.capture(),
                                            eq(UIPortalCache.SECOND_EVICTION_DELAY),
                                            eq(TimeUnit.SECONDS));

    // Built again from the stored site read before its own invalidation
    build(SiteKey.portal("administration"));
    secondEviction.getValue().run();

    assertNull(uiPortalCache.get(SiteKey.portal("administration")));
  }

  @Test
  public void testAStaleBuildClearsTheBuildsOfThisNodeOnly() {
    build(SiteKey.portal("administration"));
    build(SiteKey.portal(META_PORTAL));

    uiPortalCache.clearLocal();

    assertTrue(uiPortalCache.getLocalSites().isEmpty());
    verify(replicated, never()).put(any(), any());
  }

  @Test
  public void testTheSiteAndPageChangesAreListenedTo() {
    assertEquals(5, listeners.size());
    assertTrue(listeners.keySet()
                        .containsAll(List.of(LayoutService.PORTAL_CONFIG_CREATED,
                                             LayoutService.PORTAL_CONFIG_UPDATED,
                                             LayoutService.PORTAL_CONFIG_REMOVED,
                                             LayoutService.PAGE_UPDATED,
                                             LayoutService.PAGE_REMOVED)));
  }

  private UIPortal build(SiteKey siteKey) {
    UIPortal uiPortal = mock(UIPortal.class);
    when(uiPortal.getSiteKey()).thenReturn(siteKey);
    uiPortalCache.put(uiPortal);
    return uiPortal;
  }

  private Page page(String pageId) {
    Page page = new Page();
    page.setPageId(pageId);
    return page;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void fire(String eventName, Object data) throws Exception {
    Listener listener = listeners.get(eventName);
    listener.onEvent(new Event(eventName, null, data));
  }

}
