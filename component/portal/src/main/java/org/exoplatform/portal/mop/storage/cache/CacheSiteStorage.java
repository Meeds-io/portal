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
package org.exoplatform.portal.mop.storage.cache;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.dao.SiteDAO;
import org.exoplatform.portal.mop.storage.LayoutStorage;
import org.exoplatform.portal.mop.storage.NavigationStorage;
import org.exoplatform.portal.mop.storage.PageStorage;
import org.exoplatform.portal.mop.storage.SiteStorageImpl;
import org.exoplatform.portal.pom.data.PortalData;
import org.exoplatform.portal.pom.data.PortalKey;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.upload.UploadService;

import java.util.List;

public class CacheSiteStorage extends SiteStorageImpl {

  public static final String                                SITE_CACHE_NAME                = "portal.SiteService";

  public static final String                                SITE_KEYS_BY_FILTER_CACHE_NAME = "portal.SiteKeysByFilterService";

  public static final String                                SITE_KEY_BY_ID_CACHE_NAME      = "portal.SiteKeyById";

  private final FutureExoCache<SiteKey, PortalData, Object> siteFutureCache;

  private final ExoCache<SiteKey, PortalData>               siteCache;

  private final FutureExoCache<SiteFilter, Object, Object>  siteKeysByFilterFutureCache;

  private final ExoCache<SiteFilter, Object>                siteKeysByFilterCache;

  private final FutureExoCache<Long, SiteKey, Object>       siteKeysByIdFutureCache;

  private final ExoCache<Long, SiteKey>                     siteKeysByIdCache;

  public CacheSiteStorage(CacheService cacheService, // NOSONAR
                          SettingService settingService,
                          ConfigurationManager configurationManager,
                          NavigationStorage navigationStorage,
                          PageStorage pageStorage,
                          LayoutStorage layoutStorage,
                          SiteDAO siteDAO,
                          UploadService uploadService,
                          FileService fileService) {
    super(settingService,
          configurationManager,
          navigationStorage,
          pageStorage,
          layoutStorage,
          siteDAO,
          uploadService,
          fileService);
    this.siteCache = cacheService.getCacheInstance(SITE_CACHE_NAME);
    this.siteFutureCache = new FutureExoCache<>(new Loader<SiteKey, PortalData, Object>() {
      @Override
      public PortalData retrieve(Object context, SiteKey siteKey) throws Exception {
        PortalData portalData = CacheSiteStorage.super.getPortalConfig(siteKey);
        return portalData == null ? PortalData.NULL_OBJECT : portalData;
      }
    }, siteCache);
    this.siteKeysByFilterCache = cacheService.getCacheInstance(SITE_KEYS_BY_FILTER_CACHE_NAME);
    this.siteKeysByFilterFutureCache = new FutureExoCache<>(new Loader<SiteFilter, Object, Object>() {
      @Override
      public List<SiteKey> retrieve(Object context, SiteFilter siteFilter) throws Exception {
        return CacheSiteStorage.super.getSitesKeys(siteFilter);
      }
    }, siteKeysByFilterCache);
    this.siteKeysByIdCache = cacheService.getCacheInstance(SITE_KEY_BY_ID_CACHE_NAME);
    this.siteKeysByIdFutureCache = new FutureExoCache<>(new Loader<Long, SiteKey, Object>() {
      @Override
      public SiteKey retrieve(Object context, Long siteId) throws Exception {
        PortalData site = CacheSiteStorage.super.getPortalConfig(siteId);
        return site == null ? null : new SiteKey(site.getType(), site.getName());
      }
    }, siteKeysByIdCache);
  }

  @Override
  public void create(PortalData config) {
    try {
      super.create(config);
    } finally {
      siteFutureCache.remove(getSiteKey(config.getKey()));
      clearSiteKeysByFilterFutureCache();
    }
  }

  @Override
  public void save(PortalData config) {
    try {
      super.save(config);
    } finally {
      siteFutureCache.remove(getSiteKey(config.getKey()));
      clearSiteKeysByFilterFutureCache();
    }
  }

  @Override
  public void remove(PortalData config) {
    try {
      super.remove(config);
    } finally {
      siteFutureCache.remove(getSiteKey(config.getKey()));
      clearSiteKeysByFilterFutureCache();
    }
  }

  @Override
  public PortalData getPortalConfig(SiteKey key) {
    PortalData portalData = siteFutureCache.get(null, key);
    return portalData == null || portalData.isNull() ? null : portalData;
  }

  @Override
  public PortalData getPortalConfig(long siteId) {
    SiteKey siteKey = siteKeysByIdFutureCache.get(null, siteId);
    return siteKey == null ? null : getPortalConfig(siteKey);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<SiteKey> getSitesKeys(SiteFilter siteFilter) {
    return (List<SiteKey>) siteKeysByFilterFutureCache.get(null, siteFilter);
  }

  private SiteKey getSiteKey(PortalKey portalKey) {
    return new SiteKey(portalKey.getType(), portalKey.getId());
  }

  private void clearSiteKeysByFilterFutureCache() {
    this.siteKeysByFilterFutureCache.clear();
  }
}
