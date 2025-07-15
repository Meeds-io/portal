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

import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.portal.jdbc.entity.WindowEntity;
import org.exoplatform.portal.jdbc.entity.WindowEntity.AppType;
import org.exoplatform.portal.mop.dao.ContainerDAO;
import org.exoplatform.portal.mop.dao.PermissionDAO;
import org.exoplatform.portal.mop.dao.WindowDAO;
import org.exoplatform.portal.mop.storage.LayoutStorage;
import org.exoplatform.portal.mop.storage.cache.model.WindowData;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

public class CacheLayoutStorage extends LayoutStorage {

  public static final String                             PORTLET_PREFERENCES_CACHE_NAME = "portal.PortletPreferences";

  private final FutureExoCache<Long, WindowData, Object> portletPreferencesFutureCache;

  private final ExoCache<Long, WindowData>               portletPreferencesCache;

  public CacheLayoutStorage(CacheService cacheService,
                            WindowDAO windowDAO,
                            ContainerDAO containerDAO,
                            PermissionDAO permissionDAO) {
    super(windowDAO, containerDAO, permissionDAO);
    this.portletPreferencesCache = cacheService.getCacheInstance(PORTLET_PREFERENCES_CACHE_NAME);
    this.portletPreferencesFutureCache = new FutureExoCache<>(new Loader<Long, WindowData, Object>() {
      @Override
      public WindowData retrieve(Object context, Long siteKey) throws Exception {
        WindowEntity window = CacheLayoutStorage.super.findWindow(siteKey);
        if (window == null) {
          return WindowData.NULL_OBJECT;
        } else {
          return new WindowData(window.getId(),
                                window.getTitle(),
                                window.getIcon(),
                                window.getDescription(),
                                window.isShowInfoBar(),
                                window.isShowApplicationState(),
                                window.isShowApplicationMode(),
                                window.getTheme(),
                                window.getWidth(),
                                window.getHeight(),
                                window.getProperties(),
                                null,
                                window.getContentId(),
                                window.getCustomization());
        }
      }
    }, portletPreferencesCache);
  }

  @Override
  protected WindowEntity createWindow(WindowEntity dstW) {
    WindowEntity window = super.createWindow(dstW);
    if (window != null && window.getId() != null) {
      portletPreferencesCache.remove(window.getId());
    }
    return window;
  }

  @Override
  protected WindowEntity updateWindow(WindowEntity window) {
    try {
      return super.updateWindow(window);
    } finally {
      portletPreferencesCache.remove(window.getId());
    }
  }

  @Override
  protected WindowEntity findWindow(Long id) {
    WindowData window = portletPreferencesFutureCache.get(null, id);
    return window == null || window.isNull() ? null : toEntity(window);
  }

  @Override
  protected void deleteWindow(WindowEntity window) {
    try {
      super.deleteWindow(window);
    } finally {
      portletPreferencesCache.remove(window.getId());
    }
  }

  @Override
  protected void deleteWindowById(Long id) {
    try {
      super.deleteWindowById(id);
    } finally {
      portletPreferencesCache.remove(id);
    }
  }

  private WindowEntity toEntity(WindowData window) {
    WindowEntity windowEntity = new WindowEntity();
    windowEntity.setId(window.getId());
    windowEntity.setTitle(window.getTitle());
    windowEntity.setIcon(window.getIcon());
    windowEntity.setDescription(window.getDescription());
    windowEntity.setShowInfoBar(window.isShowInfoBar());
    windowEntity.setShowApplicationState(window.isShowApplicationState());
    windowEntity.setShowApplicationMode(window.isShowApplicationMode());
    windowEntity.setTheme(window.getTheme());
    windowEntity.setWidth(window.getWidth());
    windowEntity.setHeight(window.getHeight());
    windowEntity.setProperties(window.getProperties());
    windowEntity.setAppType(AppType.PORTLET);
    windowEntity.setContentId(window.getContentId());
    windowEntity.setCustomization(window.getCustomization());
    return windowEntity;
  }
}
