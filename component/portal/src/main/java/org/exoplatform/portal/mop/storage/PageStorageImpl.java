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
package org.exoplatform.portal.mop.storage;

import static org.exoplatform.portal.mop.storage.utils.MOPUtils.parseJsonArray;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.config.NoSuchDataException;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.jdbc.entity.ComponentEntity;
import org.exoplatform.portal.jdbc.entity.PageEntity;
import org.exoplatform.portal.jdbc.entity.PermissionEntity;
import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.PageType;
import org.exoplatform.portal.mop.QueryResult;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Utils;
import org.exoplatform.portal.mop.dao.PageDAO;
import org.exoplatform.portal.mop.dao.SiteDAO;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageError;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageServiceException;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.pom.data.ComponentData;
import org.exoplatform.portal.pom.data.PageData;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class PageStorageImpl implements PageStorage {

  private static final Log    LOG                  = ExoLogger.getLogger(PageStorageImpl.class);

  protected ListenerService   listenerService;

  protected LayoutStorage     layoutStorage;

  protected SiteDAO           siteDAO;

  protected PageDAO           pageDAO;

  public PageStorageImpl(ListenerService listenerService,
                         LayoutStorage layoutStorage,
                         SiteDAO siteDAO,
                         PageDAO pageDAO) {
    this.listenerService = listenerService;
    this.layoutStorage = layoutStorage;
    this.siteDAO = siteDAO;
    this.pageDAO = pageDAO;
  }

  @Override
  public Page getPage(String pageKey) {
    PageKey key = PageKey.parse(pageKey);
    return key == null ? null : getPage(key);
  }

  @Override
  public Page getPage(long id) {
    PageKey key = getPageKey(id);
    return key == null ? null : getPage(key);
  }

  @Override
  public Page getPage(PageKey key) {
    if (key == null) {
      return null;
    }
    PageData pageData = getPage(key.toPomPageKey());
    if (pageData == null) {
      return null;
    } else {
      return new Page(pageData);
    }
  }

  @Override
  public PageContext loadPage(PageKey key) {
    if (key == null) {
      return null;
    }

    PageData pageData = getPage(key.toPomPageKey());
    if (pageData == null) {
      return null;
    } else {
      return new PageContext(key, Utils.toPageState(pageData));
    }
  }

  /**
   * <p>
   * Load all the pages of a specific site. Note that this method can
   * potentially raise performance issues if the number of pages is very large
   * and should be used with cautions. That's the motiviation for not having
   * this method on the {@link PageStorage} interface.
   * </p>
   *
   * @param siteKey the site key
   * @return the list of pages
   * @throws PageServiceException anything that would prevent the operation to
   *           succeed
   */
  public List<PageContext> loadPages(SiteKey siteKey) {
    if (siteKey == null) {
      throw new IllegalArgumentException("No null site key accepted");
    }
    return findPages(siteKey.getType(), siteKey.getName(), null, 0, -1);
  }

  @Override
  public boolean savePage(PageContext page) {
    if (page == null) {
      throw new IllegalArgumentException("PageContext is mandatory");
    }

    PageEntity entity = pageDAO.findByKey(page.getKey());
    boolean created = false;
    if (entity == null) {
      entity = new PageEntity();
      applyPageContextToEntity(entity, page);
      entity = pageDAO.create(entity);
      created = true;
    } else {
      applyPageContextToEntity(entity, page);
      entity = pageDAO.update(entity);
    }

    PageState state = page.getState();
    if (state != null) {
      savePagePermissions(PageEntity.class.getName(),
                          entity.getId(),
                          state.getAccessPermissions(),
                          Arrays.asList(state.getEditPermission()));
    }

    if (created) {
      broadcastEvent(EventType.PAGE_CREATED, page.getKey());
    } else {
      broadcastEvent(EventType.PAGE_UPDATED, page.getKey());
    }
    return created;
  }

  @Override
  public boolean destroyPage(PageKey key) {
    if (key == null) {
      throw new IllegalArgumentException("PageKey is mandatory");
    }

    PageEntity page = pageDAO.findByKey(key);
    if (page != null) {
      String pageBody = page.getPageBody();
      JSONArray children = parseJsonArray(pageBody);
      layoutStorage.deleteChildren(children);
      layoutStorage.deletePermissions(PageEntity.class.getName(), page.getId());
      pageDAO.delete(page);

      broadcastEvent(EventType.PAGE_DESTROYED, key);
      return true;
    }
    return false;
  }

  @Override
  public boolean destroyPages(SiteKey siteKey) {
    List<PageKey> pageKeys = findPageKeys(siteKey.getType(), siteKey.getName(), null, 0, -1);
    pageKeys.forEach(this::destroyPage);
    return !pageKeys.isEmpty();
  }

  @Override
  public PageContext clone(PageKey srcPageKey, PageKey dstPageKey) {
    if (srcPageKey == null) {
      throw new IllegalArgumentException("No null source accepted");
    }
    if (dstPageKey == null) {
      throw new IllegalArgumentException("No null destination accepted");
    }

    PageEntity pageSrc = pageDAO.findByKey(srcPageKey);
    if (pageSrc == null) {
      throw new PageServiceException(PageError.CLONE_NO_SRC_PAGE,
                                     String.format("Could not clone non existing page %s from site of type %s with id %s",
                                                   srcPageKey.getName(),
                                                   srcPageKey.getSite().getType(),
                                                   srcPageKey.getSite().getName()));
    } else {
      PageEntity pageDst = pageDAO.findByKey(dstPageKey);
      if (pageDst != null) {
        throw new PageServiceException(PageError.CLONE_DST_ALREADY_EXIST,
                                       String.format("Could not clone page %s to existing page %s with id %s",
                                                     dstPageKey.getName(),
                                                     dstPageKey.getSite().getType(),
                                                     dstPageKey.getSite().getName()));
      } else {
        SiteKey siteKey = dstPageKey.getSite();
        SiteEntity owner = siteDAO.findByKey(siteKey);
        if (owner == null) {
          throw new PageServiceException(PageError.CLONE_NO_DST_SITE,
                                         String.format("Could not clone page %s to non existing site of type %s with id %s",
                                                       dstPageKey.getName(),
                                                       siteKey.getTypeName(),
                                                       siteKey.getName()));
        }

        pageDst = new PageEntity();
        applyPageContextToEntity(pageDst, buildPageContext(pageSrc));
        List<ComponentEntity> children = layoutStorage.clone(PageEntity.class.getName(), pageSrc.getPageBody());
        pageDst.setChildren(children);
        pageDst.setPageBody(((JSONArray) pageDst.toJSON().get("children")).toJSONString());
        //

        pageDst.setName(dstPageKey.getName());
        pageDst.setOwner(owner);
        pageDst = pageDAO.create(pageDst);
        layoutStorage.clonePermissions(PageEntity.class.getName(), pageDst.getId(), pageSrc.getId());

        PageContext pageContext = buildPageContext(pageDst);
        broadcastEvent(EventType.PAGE_CREATED, dstPageKey);
        return pageContext;
      }
    }
  }

  @Override
  public QueryResult<PageContext> findPages(int from,
                                            int limit,
                                            SiteType siteType,
                                            String siteName,
                                            String pageName,
                                            String pageTitle) {
    List<PageContext> pages = findPages(siteType, siteName, pageTitle, from, limit);
    return new QueryResult<>(from, pages.size(), pages);
  }

  @Override
  public void save(PageData page) {
    SiteKey siteKey = new SiteKey(page.getKey().getType(), page.getKey().getId());
    org.exoplatform.portal.mop.page.PageKey mopKey =
                                                   new org.exoplatform.portal.mop.page.PageKey(siteKey, page.getKey().getName());

    PageEntity dst = pageDAO.findByKey(mopKey);
    if (dst == null) {
      throw new NoSuchDataException("The page " + page.getKey() + " not found");
    }
    List<ComponentData> children = page.getChildren();

    JSONArray pageBodyJson = parseJsonArray(dst.getPageBody());
    List<ComponentEntity> newPageBody = layoutStorage.saveChildren(pageBodyJson, children);
    dst.setChildren(newPageBody);
    dst.setPageBody(((JSONArray) dst.toJSON().get("children")).toJSONString());

    pageDAO.update(dst);
    broadcastEvent(EventType.PAGE_UPDATED, mopKey);
  }

  @Override
  public PageData getPage(org.exoplatform.portal.pom.data.PageKey key) {
    SiteKey siteKey = new SiteKey(key.getType(), key.getId());
    org.exoplatform.portal.mop.page.PageKey pageKey = new org.exoplatform.portal.mop.page.PageKey(siteKey, key.getName());
    PageEntity entity = pageDAO.findByKey(pageKey);
    return buildPageData(entity);
  }

  protected PageData buildPageData(PageEntity entity) {
    if (entity == null) {
      return null;
    }

    List<String> accessPermissions = layoutStorage.getPermissions(PageEntity.class.getName(),
                                                                  entity.getId(),
                                                                  PermissionEntity.TYPE.ACCESS);
    List<String> editPermissions = layoutStorage.getPermissions(PageEntity.class.getName(),
                                                                entity.getId(),
                                                                PermissionEntity.TYPE.EDIT);
    String editPermission = CollectionUtils.isEmpty(editPermissions) ? null : editPermissions.get(0);

    List<ComponentData> children = layoutStorage.buildChildren(parseJsonArray(entity.getPageBody()));

    return new PageData("page_" + entity.getId(), // storageId
                        null, // id
                        entity.getName(), // name
                        null, // icon
                        null, // template
                        entity.getFactoryId(), // factoryId
                        entity.getDisplayName(), // title
                        entity.getDescription(), // description
                        null, // width
                        null, // height
                        null, // cssClass
                        entity.getProfiles(), // profiles
                        accessPermissions, // accessPermissions
                        children, // children
                        entity.getOwnerType().getName(), // ownerType
                        entity.getOwnerId(), // ownerId
                        editPermission, // editPermission
                        entity.isShowMaxWindow(), // showMaxWindow
                        entity.isHideSharedLayout(),
                        entity.isShowSharedLayout(),
                        entity.getPageType() != null ? entity.getPageType().name() : null,
                        entity.getLink());
  }

  protected void broadcastEvent(String eventName, org.exoplatform.portal.mop.page.PageKey pageKey) {
    try {
      listenerService.broadcast(eventName, this, pageKey);
    } catch (Exception e) {
      LOG.warn("Error when broadcasting notification " + eventName + " for page " + pageKey, e);
    }
  }

  protected PageKey getPageKey(long id) {
    PageEntity pageEntity = pageDAO.find(id);
    return pageEntity == null ? null : new PageKey(pageEntity.getOwnerType(), pageEntity.getOwnerId(), pageEntity.getName());
  }

  private List<PageContext> findPages(SiteType siteType, String siteName, String pageTitle, int from, int limit) {
    List<PageKey> pageKeys = findPageKeys(siteType, siteName, pageTitle, from, limit);
    return pageKeys.stream()
                   .map(this::loadPage)
                   .toList();
  }

  private List<PageKey> findPageKeys(SiteType siteType, String siteName, String pageTitle, int from, int limit) {
    try {
      ListAccess<PageKey> pagesListAccess = pageDAO.findByQuery(siteType, siteName, pageTitle, from, limit);
      PageKey[] pageKeys = pagesListAccess.load(0, pagesListAccess.getSize());
      return Arrays.asList(pageKeys);
    } catch (Exception ex) {
      throw new IllegalStateException(String.format("Error retrieving pages using query %s, %s, %s", siteType, siteName, pageTitle));
    }
  }

  private void applyPageContextToEntity(PageEntity entity, PageContext pageContext) {
    PageState state = pageContext.getState();
    if (state != null) {
      entity.setDescription(state.getDescription());
      entity.setDisplayName(state.getDisplayName());
      entity.setFactoryId(state.getFactoryId());
      entity.setShowMaxWindow(state.isShowMaxWindow());
      entity.setHideSharedLayout(state.isHideSharedLayout());
      entity.setShowSharedLayout(state.isShowSharedLayout());
      entity.setPageType(!StringUtils.isBlank(state.getType()) ? PageType.valueOf(state.getType()) : PageType.PAGE);
      entity.setProfiles(state.getProfiles());
      entity.setLink(state.getLink());
    } else {
      entity.setPageType(PageType.PAGE);
    }

    SiteKey siteKey = pageContext.getKey().getSite();
    entity.setOwner(siteDAO.findByKey(siteKey));
    entity.setName(pageContext.getKey().getName());
  }

  private PageContext buildPageContext(PageEntity entity) {
    PageData pageData = buildPageData(entity);
    return new PageContext(pageData.getKey().toMopPageKey(), Utils.toPageState(pageData));
  }

  private void savePagePermissions(String objectType,
                                   long objectId,
                                   List<String> accessPermissions,
                                   List<String> editPermissions) {
    layoutStorage.savePermissions(objectType,
                                  objectId,
                                  PermissionEntity.TYPE.ACCESS,
                                  accessPermissions);
    layoutStorage.savePermissions(objectType,
                                  objectId,
                                  PermissionEntity.TYPE.EDIT,
                                  editPermissions);
  }

}
