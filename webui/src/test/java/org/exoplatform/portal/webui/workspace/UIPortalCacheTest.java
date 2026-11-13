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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.services.cache.ExoCache;

/**
 * Pins what the cache of built sites sends to the other nodes of a cluster: the cache's async invalidation sends a
 * {@code put}, a {@code remove} and a {@code clearCache} to every node, and keeps a {@code putLocal} on its node.
 */
public class UIPortalCacheTest {

  private static final String         META_PORTAL = "dw";

  @SuppressWarnings("unchecked")
  private ExoCache<SiteKey, UIPortal> cache       = mock(ExoCache.class);

  private UIPortalCache               uiPortalCache = new UIPortalCache(cache);

  @Test
  public void testABuiltSiteStaysOnTheNodeThatBuiltIt() {
    UIPortal uiPortal = mock(UIPortal.class);
    SiteKey siteKey = SiteKey.portal("administration");
    when(uiPortal.getSiteKey()).thenReturn(siteKey);

    uiPortalCache.put(uiPortal);

    verify(cache).putLocal(siteKey, uiPortal);
    verify(cache, never()).put(siteKey, uiPortal);
  }

  @Test
  public void testAChangedSiteIsEvictedOnEveryNode() {
    SiteKey siteKey = SiteKey.portal("administration");

    uiPortalCache.evictChanged(siteKey, META_PORTAL);

    verify(cache).remove(siteKey);
    verify(cache, never()).clearCache();
  }

  @Test
  public void testAChangedMetaSiteEvictsEverySiteOnEveryNode() {
    uiPortalCache.evictChanged(SiteKey.portal(META_PORTAL), META_PORTAL);

    verify(cache).clearCache();
  }

  @Test
  public void testASiteOfAnotherTypeNamedAsTheMetaSiteEvictsOnlyItself() {
    SiteKey siteKey = SiteKey.group(META_PORTAL);

    uiPortalCache.evictChanged(siteKey, META_PORTAL);

    verify(cache).remove(siteKey);
    verify(cache, never()).clearCache();
  }

}
