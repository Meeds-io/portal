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

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import lombok.SneakyThrows;

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The sites a node built, by site key. Each node builds a site from the stored
 * site and keeps its build: a build is never sent to the other nodes. A site
 * created, saved or removed evicts the site's build on every node; a change of
 * the meta site evicts every build on every node, since a displayed site is
 * built inside the meta site's layout. A page is built at each request, so a
 * page change leaves the builds in place.
 */
public class UIPortalCache {

  public static final String                    CACHE_NAME               = "portal.UIPortal";

  private static final Log                      LOG                      = ExoLogger.getLogger(UIPortalCache.class);

  /**
   * Delay, in seconds, of the second eviction of a changed site. A node can
   * build the site from the stored site it read before the site's own cache
   * was invalidated there, and keep that build after the first eviction: the
   * two invalidations travel apart. The second eviction removes such a build.
   */
  public static final long                      SECOND_EVICTION_DELAY    = 10;

  private static final ScheduledExecutorService SECOND_EVICTION_EXECUTOR =
                                                                         Executors.newSingleThreadScheduledExecutor(runnable -> {
                                                                           Thread thread =
                                                                                         new Thread(runnable,
                                                                                                    "UIPortalCache-second-eviction");
                                                                           thread.setDaemon(true);
                                                                           return thread;
                                                                         });

  private final ExoCache<SiteKey, UIPortal>     cache;

  private final Supplier<String>                metaPortal;

  private final ScheduledExecutorService        secondEvictionExecutor;

  public UIPortalCache(ExoCache<SiteKey, UIPortal> cache, Supplier<String> metaPortal) {
    this(cache, metaPortal, SECOND_EVICTION_EXECUTOR);
  }

  UIPortalCache(ExoCache<SiteKey, UIPortal> cache,
                Supplier<String> metaPortal,
                ScheduledExecutorService secondEvictionExecutor) {
    this.cache = cache;
    this.metaPortal = metaPortal;
    this.secondEvictionExecutor = secondEvictionExecutor;
  }

  /**
   * Listens to the changes of sites made on this node; the evictions they make
   * reach every node
   *
   * @param listenerService the node's listener service
   */
  public void listenTo(ListenerService listenerService) {
    SiteListener siteListener = new SiteListener(this);
    listenerService.addListener(LayoutService.PORTAL_CONFIG_CREATED, siteListener);
    listenerService.addListener(LayoutService.PORTAL_CONFIG_UPDATED, siteListener);
    listenerService.addListener(LayoutService.PORTAL_CONFIG_REMOVED, siteListener);
  }

  public UIPortal get(SiteKey siteKey) {
    return cache.get(siteKey);
  }

  /**
   * Keeps a site this node built, without notifying the other nodes: a
   * notified put would evict the other nodes' builds of the site, and each of
   * them would evict this one when it builds the site in turn.
   *
   * @param uiPortal the built site
   */
  public void put(UIPortal uiPortal) {
    cache.putLocal(uiPortal.getSiteKey(), uiPortal);
  }

  /**
   * Evicts the build of a site on every node
   *
   * @param siteKey the site's key
   */
  public void remove(SiteKey siteKey) {
    cache.remove(siteKey);
  }

  /**
   * Evicts, on every node, the builds a change of a site makes stale: the
   * site's own build, or every build when the site is the meta site; then
   * evicts them a second time after {@link #SECOND_EVICTION_DELAY} seconds
   *
   * @param siteKey the changed site's key
   */
  public void evictChanged(SiteKey siteKey) {
    evictNow(siteKey);
    secondEvictionExecutor.schedule(() -> {
      try {
        evictNow(siteKey);
      } catch (RuntimeException e) {
        LOG.warn("Error while evicting the build of site {} a second time", siteKey, e);
      }
    }, SECOND_EVICTION_DELAY, TimeUnit.SECONDS);
  }

  /**
   * Evicts the builds of this node only, leaving the other nodes' builds in
   * place: a build found stale on this node says nothing of theirs, and a
   * change made anywhere already evicts every node's build
   */
  public void clearLocal() {
    for (UIPortal uiPortal : getLocalSites()) {
      SiteKey siteKey = uiPortal.getSiteKey();
      if (siteKey != null) {
        cache.removeLocal(siteKey);
      }
    }
  }

  /**
   * @return the builds of this node only
   */
  @SneakyThrows
  public List<? extends UIPortal> getLocalSites() {
    return cache.getCachedObjects();
  }

  private void evictNow(SiteKey siteKey) {
    String metaPortalName = metaPortal.get();
    if (metaPortalName != null && siteKey.equals(SiteKey.portal(metaPortalName))) {
      cache.clearCache();
    } else {
      cache.remove(siteKey);
    }
  }

  @Asynchronous
  public static class SiteListener extends Listener<LayoutService, PortalConfig> {

    private final UIPortalCache uiPortalCache;

    public SiteListener(UIPortalCache uiPortalCache) {
      this.uiPortalCache = uiPortalCache;
    }

    @Override
    public void onEvent(Event<LayoutService, PortalConfig> event) {
      PortalConfig site = event.getData();
      SiteKey siteKey = site == null ? null : site.getSiteKey();
      if (siteKey != null) {
        uiPortalCache.evictChanged(siteKey);
      }
    }
  }

}
