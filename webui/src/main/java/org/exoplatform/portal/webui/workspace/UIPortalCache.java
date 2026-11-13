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

import lombok.SneakyThrows;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.services.cache.ExoCache;

/**
 * The sites a node built, by site key. Each node builds a site from the stored site and keeps its build: a build is
 * never sent to the other nodes, while a change of a site evicts its build on every node. A change of the meta site
 * evicts every build on every node, since a displayed site is built inside the meta site's layout.
 */
public class UIPortalCache {

  public static final String                CACHE_NAME = "portal.UIPortal";

  private final ExoCache<SiteKey, UIPortal> cache;

  public UIPortalCache(ExoCache<SiteKey, UIPortal> cache) {
    this.cache = cache;
  }

  public UIPortal get(SiteKey siteKey) {
    return cache.get(siteKey);
  }

  /**
   * Keeps a site this node built, without notifying the other nodes: a notified put would evict the other nodes'
   * builds of the site, and each of them would evict this one when it builds the site in turn.
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
   * Evicts, on every node, the builds a change of a site makes stale: the site's own build, or every build when the
   * site is the meta site
   *
   * @param siteKey the changed site's key
   * @param metaPortal the meta site's name
   */
  public void evictChanged(SiteKey siteKey, String metaPortal) {
    if (metaPortal != null && siteKey.equals(SiteKey.portal(metaPortal))) {
      cache.clearCache();
    } else {
      cache.remove(siteKey);
    }
  }

  /**
   * @return the builds of this node only
   */
  @SneakyThrows
  public List<? extends UIPortal> getLocalSites() {
    return cache.getCachedObjects();
  }

  /**
   * Evicts every build on every node
   */
  public void clear() {
    cache.clearCache();
  }

}
