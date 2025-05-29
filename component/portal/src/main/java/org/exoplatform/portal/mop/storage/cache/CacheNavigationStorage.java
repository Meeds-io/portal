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
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.dao.NavigationDAO;
import org.exoplatform.portal.mop.dao.NodeDAO;
import org.exoplatform.portal.mop.dao.PageDAO;
import org.exoplatform.portal.mop.dao.SiteDAO;
import org.exoplatform.portal.mop.navigation.NavigationData;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.storage.DescriptionStorage;
import org.exoplatform.portal.mop.storage.NavigationStorageImpl;
import org.exoplatform.portal.mop.storage.cache.model.NavigationCacheSelector;
import org.exoplatform.portal.mop.storage.cache.model.NavigationDataCacheSelector;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class CacheNavigationStorage extends NavigationStorageImpl {

  private static final Log                                      LOG                   =
                                                                    ExoLogger.getExoLogger(CacheNavigationStorage.class);

  public static final String                                    NAVIGATION_CACHE_NAME = "portal.NavigationService";

  public static final String                                    NODE_CACHE_NAME       = "portal.NavigationNode";

  private final FutureExoCache<SiteKey, NavigationData, Object> navigationFutureCache;

  private final ExoCache<SiteKey, NavigationData>               navigationCache;

  private final FutureExoCache<Long, NodeData, Object>          nodeFutureCache;

  private final ExoCache<Long, NodeData>                        nodeCache;

  public CacheNavigationStorage(CacheService cacheService,
                                DescriptionStorage descriptionStorage,
                                NavigationDAO navigationDAO,
                                SiteDAO siteDAO,
                                NodeDAO nodeDAO,
                                PageDAO pageDAO) {
    super(descriptionStorage, navigationDAO, siteDAO, nodeDAO, pageDAO);
    this.navigationCache = cacheService.getCacheInstance(NAVIGATION_CACHE_NAME);
    this.navigationFutureCache = new FutureExoCache<>(new Loader<SiteKey, NavigationData, Object>() {
      @Override
      public NavigationData retrieve(Object context, SiteKey siteKey) throws Exception {
        NavigationData navigationData = CacheNavigationStorage.super.loadNavigationData(siteKey);
        return navigationData == null ? NavigationData.NULL_OBJECT : navigationData;
      }
    }, navigationCache);

    this.nodeCache = cacheService.getCacheInstance(NODE_CACHE_NAME);
    this.nodeFutureCache = new FutureExoCache<>(new Loader<Long, NodeData, Object>() {
      @Override
      public NodeData retrieve(Object context, Long nodeId) throws Exception {
        return CacheNavigationStorage.super.loadNode(nodeId);
      }
    }, nodeCache);
  }

  @Override
  public NodeData[] createNode(Long parentId, Long previousId, String name, NodeState state, Integer index) {
    try {
      return super.createNode(parentId, previousId, name, state, index);
    } finally {
      this.nodeFutureCache.remove(parentId);
    }
  }

  @Override
  public NodeData destroyNode(Long targetId) {
    NodeData nodeData = loadNode(targetId);
    try {
      return super.destroyNode(targetId);
    } finally {
      clearNodeCache(nodeData);
    }
  }

  @Override
  public NodeData loadNode(Long nodeId) {
    return nodeFutureCache.get(null, nodeId);
  }

  @Override
  public NodeData[] moveNode(Long targetId, Long fromId, Long toId, Long previousId) {
    NodeData nodeData = loadNode(targetId);
    NodeData fromNodeData = loadNode(fromId);
    NodeData toNodeData = loadNode(toId);
    try {
      return super.moveNode(targetId, fromId, toId, previousId);
    } finally {
      clearNodeCache(nodeData);
      clearNodeCache(fromNodeData);
      clearNodeCache(toNodeData);
    }
  }

  @Override
  public NodeData[] renameNode(Long targetId, Long parentId, String name) {
    NodeData nodeData = loadNode(targetId);
    NodeData parentNodeData = loadNode(parentId);
    try {
      return super.renameNode(targetId, parentId, name);
    } finally {
      clearNodeCache(nodeData);
      clearNodeCache(parentNodeData);
    }
  }

  @Override
  public NodeData updateNode(Long targetId, NodeState state) {
    NodeData nodeData = loadNode(targetId);
    try {
      return super.updateNode(targetId, state);
    } finally {
      clearNodeCache(nodeData);
    }
  }

  @Override
  public NavigationData loadNavigationData(SiteKey siteKey) {
    NavigationData navigationData = navigationFutureCache.get(null, siteKey);
    return navigationData == null || navigationData.isNull() ? null : navigationData;
  }

  @Override
  public void saveNavigation(SiteKey key, NavigationState state) {
    try {
      super.saveNavigation(key, state);
    } finally {
      clearNavigationByKey(key);
      clearNodeCache(key);
    }
  }

  @Override
  public boolean destroyNavigation(SiteKey siteKey) {
    try {
      return super.destroyNavigation(siteKey);
    } finally {
      clearNavigationByKey(siteKey);
      clearNodeCache(siteKey);
    }
  }

  @Override
  public boolean destroyNavigation(NavigationData data) {
    try {
      return super.destroyNavigation(data);
    } finally {
      if (data != null && data.getSiteKey() != null) {
        SiteKey siteKey = data.getSiteKey();
        clearNavigationByKey(siteKey);
        clearNodeCache(siteKey);
        if (data.getRootId() != null) {
          clearNodeCache(Long.parseLong(data.getRootId()));
        }
      }
    }
  }

  public void clearNavigationByKey(SiteKey siteKey) {
    if (siteKey != null) {
      try {
        this.navigationCache.select(new NavigationCacheSelector(siteKey));
        this.nodeCache.select(new NavigationDataCacheSelector(siteKey, null));
      } catch (Exception e) {
        LOG.error("Error clearing cache of navigation having site key {}", siteKey, e);
      }
    }
  }

  public void clearNodeCache(SiteKey siteKey) {
    try {
      this.nodeCache.select(new NavigationDataCacheSelector(siteKey, null));
      this.navigationCache.select(new NavigationCacheSelector(siteKey));
    } catch (Exception e) {
      LOG.error("Error clearing cache of nodes of site {}", siteKey, e);
    }
  }

  public void clearNodeCache(Long nodeId) {
    try {
      this.nodeCache.select(new NavigationDataCacheSelector(null, nodeId));
    } catch (Exception e) {
      LOG.error("Error clearing cache of node data with id", nodeId, e);
    }
  }

  public void clearNodeCache(NodeData nodeData) {
    if (nodeData != null) {
      try {
        SiteKey siteKey = nodeData.getState().getSiteKey();
        this.nodeCache.select(new NavigationDataCacheSelector(siteKey,
                                                              Long.parseLong(nodeData.getId())));
        if (nodeData.getParentId() != null) {
          this.nodeCache.select(new NavigationDataCacheSelector(null, Long.parseLong(nodeData.getParentId())));
        }
        this.navigationCache.select(new NavigationCacheSelector(siteKey));
      } catch (Exception e) {
        LOG.error("Error clearing cache of node data with id {} of page {}", nodeData.getId(), nodeData.getName(), e);
      }
    }
  }

}
