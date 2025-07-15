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
package org.exoplatform.portal.mop.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.QueryResult;
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Utils;
import org.exoplatform.portal.mop.importer.Status;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.storage.LayoutStorage;
import org.exoplatform.portal.mop.storage.PageStorage;
import org.exoplatform.portal.mop.storage.SiteStorage;
import org.exoplatform.portal.pom.data.ModelChange;
import org.exoplatform.portal.pom.data.ModelData;
import org.exoplatform.portal.pom.data.PageData;
import org.exoplatform.portal.pom.data.PortalData;
import org.exoplatform.portal.pom.data.PortalKey;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import lombok.SneakyThrows;

public class LayoutServiceImpl implements LayoutService {

  private static final String OWNER_ID_TEMPLATE = "@owner_id@";

  private static final Log       LOG                     = ExoLogger.getLogger(LayoutServiceImpl.class);

  private static final String    SITE_DEAULT_BANNER_PATH = "site.default.banner.path";

  private ListenerService        listenerService;

  private SiteStorage            siteStorage;

  private PageStorage            pageStorage;

  private LayoutStorage          layoutStorage;

  private Map<String, Container> sharedLayouts           = new HashMap<>();

  private FileService            fileService;

  private PortalContainer        portalContainer;

  private InitParams             initParams;

  public LayoutServiceImpl(ListenerService listenerService,
                           SiteStorage siteStorage,
                           PageStorage pageStorage,
                           LayoutStorage layoutStorage,
                           FileService fileService,
                           PortalContainer portalContainer,
                           InitParams initParams) {
    this.listenerService = listenerService;
    this.siteStorage = siteStorage;
    this.pageStorage = pageStorage;
    this.layoutStorage = layoutStorage;
    this.fileService = fileService;
    this.portalContainer = portalContainer;
    this.initParams = initParams;
  }

  @Override
  public void create(PortalConfig config) {
    siteStorage.create(config);
    broadcastEvent(PORTAL_CONFIG_CREATED, config);
  }

  @Override
  public void savePortalFromTemplate(SiteKey sourceSiteKey,
                                     SiteKey targetSiteKey,
                                     String permission) throws ObjectNotFoundException {
    PortalConfig templatePortalConfig = getPortalConfig(sourceSiteKey);
    if (templatePortalConfig == null) {
      throw new ObjectNotFoundException(String.format("Site template %s wasn't found", sourceSiteKey));
    }
    boolean exists = getPortalConfig(targetSiteKey) != null;
    PortalConfig portalConfig = templatePortalConfig.clone();
    portalConfig.resetStorage();
    portalConfig.setType(targetSiteKey.getTypeName());
    portalConfig.setName(targetSiteKey.getName());
    portalConfig.setBannerFileId(0);
    applyPermissionTemplate(portalConfig.getPortalLayout());
    if (StringUtils.isNotBlank(permission)) {
      portalConfig.setAccessPermissions(getPermissionsFromTemplate(portalConfig.getAccessPermissions(), permission));
      portalConfig.setEditPermission(getPermissionFromTemplate(portalConfig.getEditPermission(), permission));
    } else if (StringUtils.equalsIgnoreCase(sourceSiteKey.getTypeName(), PortalConfig.GROUP_TYPE)) {
      String groupId = sourceSiteKey.getName();
      portalConfig.setAccessPermissions(getPermissionsToTemplate(portalConfig.getAccessPermissions(), groupId));
      portalConfig.setEditPermission(getPermissionToTemplate(portalConfig.getEditPermission(), groupId));
    }
    if (exists) {
      siteStorage.save(portalConfig.build());
      broadcastEvent(PORTAL_CONFIG_UPDATED, portalConfig);
    } else {
      siteStorage.create(portalConfig.build());
      broadcastEvent(PORTAL_CONFIG_CREATED, portalConfig);
    }
  }

  @Override
  public void savePagesFromTemplate(SiteKey sourceSiteTemplate,
                                    SiteKey targetSiteKey,
                                    String permission) throws ObjectNotFoundException {
    List<PageContext> sourceTemplatePages = findPages(sourceSiteTemplate);
    if (CollectionUtils.isNotEmpty(sourceTemplatePages)) {
      sourceTemplatePages.stream().map(PageContext::getKey).forEach(k -> savePageFromTemplate(k, targetSiteKey, permission));
    }
  }

  @Override
  @SneakyThrows
  public void savePageFromTemplate(PageKey sourcePageKey,
                                   SiteKey targetSiteKey,
                                   String permission) {
    Page page = getPage(sourcePageKey);
    page = new Page(page.build());
    page.resetStorage();
    page.setOwnerType(targetSiteKey.getTypeName());
    page.setOwnerId(targetSiteKey.getName());
    applyPermissionTemplate(page.getChildren());
    if (StringUtils.isNotBlank(permission)) {
      page.setAccessPermissions(getPermissionsFromTemplate(page.getAccessPermissions(), permission));
      page.setEditPermission(getPermissionFromTemplate(page.getEditPermission(), permission));
    } else if (StringUtils.equalsIgnoreCase(sourcePageKey.getSite().getTypeName(), PortalConfig.GROUP_TYPE)) {
      String groupId = sourcePageKey.getSite().getName();
      page.setAccessPermissions(getPermissionsToTemplate(page.getAccessPermissions(), groupId));
      page.setEditPermission(getPermissionToTemplate(page.getEditPermission(), groupId));
    }
    pageStorage.savePage(new PageContext(page.getPageKey(), Utils.toPageState(page)));
    pageStorage.save(page.build());
    broadcastEvent(PAGE_CREATED, page);
  }

  @Override
  public void save(PortalConfig config) {
    siteStorage.save(config);
    broadcastEvent(PORTAL_CONFIG_UPDATED, config);
  }

  @Override
  public void remove(PortalConfig config) {
    siteStorage.remove(config);
    broadcastEvent(PORTAL_CONFIG_REMOVED, config);
  }

  @Override
  public PortalConfig getPortalConfig(String portalName) {
    return getPortalConfig(PortalConfig.PORTAL_TYPE, portalName);
  }

  @Override
  public PortalConfig getPortalConfig(SiteKey siteKey) {
    PortalData portalData = siteStorage.getPortalConfig(siteKey);
    return portalData == null ? null : new PortalConfig(portalData);
  }

  @Override
  public PortalConfig getPortalConfig(long siteId) {
    PortalData portalData = siteStorage.getPortalConfig(siteId);
    return portalData == null ? null : new PortalConfig(portalData);
  }

  @Override
  public PortalConfig getPortalConfig(String ownerType, String portalName) {
    return siteStorage.getPortalConfig(ownerType, portalName);
  }

  @Override
  public List<String> getSiteNames(SiteType siteType, int offset, int limit) {
    return siteStorage.getSiteNames(siteType, offset, limit);
  }

  @Override
  public void create(Page page) {
    pageStorage.save(page.build());
    broadcastEvent(PAGE_CREATED, page);
  }

  @Override
  public List<ModelChange> save(Page page) {
    pageStorage.save(page.build());
    broadcastEvent(PAGE_UPDATED, page);
    // Not used, kept as is to not break the API definition
    return Collections.<ModelChange> emptyList();
  }

  @Override
  public void save(PageContext pageContext, Page page) {
    pageStorage.savePage(pageContext);
    pageStorage.save(page.build());
    broadcastEvent(PAGE_UPDATED, page);
  }

  @Override
  public void save(PageContext pageContext) {
    pageStorage.savePage(pageContext);
    broadcastEvent(PAGE_UPDATED, pageStorage.getPage(pageContext.getKey()));
  }

  @Override
  public void removePages(SiteKey siteKey) {
    List<PageContext> pages = pageStorage.loadPages(siteKey);
    pages.forEach(context -> remove(context.getKey()));
  }

  @Override
  public void remove(PageKey pageKey) {
    Page page = pageStorage.getPage(pageKey);
    remove(page);
  }

  @Override
  public void remove(Page page) {
    pageStorage.destroyPage(page.getPageKey());
  }

  @Override
  public Page getPage(String pageId) {
    return pageStorage.getPage(pageId);
  }

  @Override
  public Page getPage(long id) {
    return pageStorage.getPage(id);
  }

  @Override
  public Page getPage(PageKey pageKey) {
    return pageStorage.getPage(pageKey);
  }

  @Override
  public PageContext getPageContext(PageKey pageKey) {
    return pageStorage.loadPage(pageKey);
  }

  @Override
  public List<PageContext> findPages(SiteKey siteKey) {
    return pageStorage.loadPages(siteKey);
  }

  @Override
  public QueryResult<PageContext> findPages(int offset,
                                            int limit,
                                            SiteType siteKey,
                                            String siteName,
                                            String pageName,
                                            String pageTitle) {
    return pageStorage.findPages(offset, limit, siteKey, siteName, pageName, pageTitle);
  }

  @Override
  public Portlet load(ApplicationState state) {
    return layoutStorage.load(state);
  }

  @Override
  public ApplicationState save(ApplicationState state, Portlet preferences) {
    return layoutStorage.save(state, preferences);
  }

  @Override
  public String getId(ApplicationState state) {
    return layoutStorage.getId(state);
  }

  @Override
  public Application getApplicationModel(String applicationStorageId) {
    return layoutStorage.getApplicationModel(applicationStorageId);
  }

  @Override
  public Container getSharedLayout(String siteName) {
    String cacheKey = siteName;
    if (StringUtils.isBlank(cacheKey)) {
      cacheKey = "DEFAULT-SITE";
    }
    if (PropertyManager.isDevelopping() || !sharedLayouts.containsKey(cacheKey)) {
      Container sharedLayout = siteStorage.getSharedLayout(siteName);
      if (sharedLayout == null && StringUtils.isNotBlank(siteName)) {
        // Return default shared layout if dedicated shared layout wasn't found
        return getSharedLayout(null);
      } else {
        sharedLayouts.put(cacheKey, sharedLayout);
      }
    }
    return sharedLayouts.get(cacheKey);
  }

  @Override
  public Status getImportStatus() {
    return siteStorage.getImportStatus();
  }

  @Override
  public void saveImportStatus(Status status) {
    siteStorage.saveImportStatus(status);
  }

  @Override
  public <T> LazyPageList<T> find(Query<T> q, Comparator<T> sortComparator) {
    return new LazyPageList<>(find2(q, sortComparator), 10);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> ListAccess<T> find2(Query<T> q, Comparator<T> sortComparator) {
    Class<T> type = q.getClassType();
    if (type == Page.class) {
      Bilto<Page, PageData> bilto = new Bilto<Page, PageData>((Query<Page>) q,
                                                              PageData.class,
                                                              (Comparator<Page>) sortComparator) {
        @Override
        protected Page create(PageData pageData) {
          return new Page(pageData);
        }
      };
      return (ListAccess<T>) bilto.execute();
    } else if (type == PortalConfig.class) {
      Bilto<PortalConfig, PortalData> bilto = new Bilto<PortalConfig, PortalData>((Query<PortalConfig>) q,
                                                                                  PortalData.class,
                                                                                  (Comparator<PortalConfig>) sortComparator) {
        @Override
        protected PortalConfig create(PortalData portalData) {
          return new PortalConfig(portalData);
        }
      };
      return (ListAccess<T>) bilto.execute();
    } else {
      throw new UnsupportedOperationException("Cannot query type " + type);
    }
  }

  @SuppressWarnings({
                      "unchecked", "rawtypes"
  })
  public <T> LazyPageList<T> findLazyPageList(Query<T> q) { // NOSONAR
    Class<T> type = q.getClassType();
    if (PageData.class.equals(type)) {
      throw new UnsupportedOperationException("Use PageService.findPages to instead of");
    } else if (PortalData.class.equals(type)) {
      String ownerType = q.getOwnerType();

      ListAccess<PortalData> la = new ListAccess<PortalData>() {

        private int          cacheOffset;

        private int          cacheLimit;

        private List<String> siteNames;

        public PortalData[] load(int offset, int limit) throws Exception {
          SiteType siteType = ownerType == null ? SiteType.PORTAL : SiteType.valueOf(ownerType.toUpperCase());
          return getSiteNames(offset, limit).stream()
                                            .map(siteName -> siteStorage.getPortalConfig(new SiteKey(siteType.getName(),
                                                                                                     siteName)))
                                            .toList()
                                            .toArray(new PortalData[siteNames.size()]);
        }

        public int getSize() throws Exception {
          return getSiteNames(0, -1).size();
        }

        private List<String> getSiteNames(int offset, int limit) {
          if (siteNames == null || cacheOffset != offset || cacheLimit != limit) {
            SiteType siteType = ownerType == null ? SiteType.PORTAL : SiteType.valueOf(ownerType.toUpperCase());
            siteNames = siteStorage.getSiteNames(siteType, offset, limit);
            cacheOffset = offset;
            cacheLimit = limit;
          }
          return siteNames;
        }
      };
      return new LazyPageList(la, 10);
    } else if (PortalKey.class.equals(type) && ("portal".equals(q.getOwnerType()) || "group".equals(q.getOwnerType()))) {
      String ownerType = q.getOwnerType();
      ListAccess<PortalKey> la = new ListAccess<PortalKey>() {
        private List<String> siteNames;

        public PortalKey[] load(int offset, int limit) throws Exception {
          return getSiteNames(offset, limit).stream()
                                            .map(siteName -> new PortalKey(ownerType, siteName))
                                            .toList()
                                            .toArray(new PortalKey[siteNames.size()]);
        }

        public int getSize() throws Exception {
          return getSiteNames(0, -1).size();
        }

        private List<String> getSiteNames(int offset, int limit) {
          if (siteNames == null) {
            SiteType siteType = SiteType.PORTAL;
            if (ownerType != null) {
              siteType = SiteType.valueOf(ownerType.toUpperCase());
            }
            siteNames = siteStorage.getSiteNames(siteType, offset, limit);
          }
          return siteNames;
        }
      };
      return new LazyPageList(la, 10);
    } else {
      throw new UnsupportedOperationException("Could not perform search on query " + q);
    }
  }

  @Override
  public List<PortalConfig> getSites(SiteFilter filter) {
    List<SiteKey> portalKeysList = siteStorage.getSitesKeys(filter);
    return portalKeysList.isEmpty() ? Collections.emptyList() :
                                    portalKeysList.stream()
                                                  .map(siteKey -> new PortalConfig(siteStorage.getPortalConfig(siteKey)))
                                                  .toList();
  }

  @Override
  public InputStream getSiteBannerStream(String siteName) throws ObjectNotFoundException {
    PortalConfig portalConfig = getPortalConfig(siteName);
    if (portalConfig == null) {
      throw new ObjectNotFoundException(String.format("site with name %s doesn't exist", siteName));
    }
    if (portalConfig.getBannerFileId() == 0) {
      throw new ObjectNotFoundException(String.format("site with name %s doesn't have a bannerId", siteName));
    }
    try {
      FileItem fileItem = fileService.getFile(portalConfig.getBannerFileId());
      return fileItem == null || fileItem.getFileInfo() == null ? null : fileItem.getAsStream();
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public InputStream getDefaultSiteBannerStream(String siteName) {
    InputStream defaultSiteBanner = portalContainer.getPortalContext()
                                                   .getResourceAsStream(System.getProperty("sites." + siteName +
                                                       ".defaultBannerPath",
                                                                                           "/images/sites/banner/" +
                                                                                               siteName.toLowerCase() + ".png"));
    if (defaultSiteBanner == null && initParams != null) {
      ValueParam siteDefaultBannerPath = initParams.getValueParam(SITE_DEAULT_BANNER_PATH);
      if (siteDefaultBannerPath != null && siteDefaultBannerPath.getValue() != null) {
        defaultSiteBanner = portalContainer.getPortalContext().getResourceAsStream(siteDefaultBannerPath.getValue());
      }
    }
    return defaultSiteBanner != null ? defaultSiteBanner : new ByteArrayInputStream(new byte[] {});
  }

  @Override
  public void removeSiteBanner(String siteName) throws ObjectNotFoundException {
    PortalConfig portalConfig = getPortalConfig(siteName);
    if (portalConfig == null) {
      throw new ObjectNotFoundException("site with name " + siteName + " doesn't exist");
    }
    if (portalConfig.getBannerFileId() == 0) {
      throw new ObjectNotFoundException("site with name " + siteName + " doesn't have a bannerId");
    }
    fileService.deleteFile(portalConfig.getBannerFileId());
  }

  private abstract class Bilto<O extends ModelObject, D extends ModelData> {

    final Query<O>      q;

    final Class<D>      dataType;

    final Comparator<O> cp;

    Bilto(Query<O> q, Class<D> dataType, Comparator<O> cp) {
      this.q = q;
      this.dataType = dataType;
      this.cp = cp;
    }

    protected abstract O create(D d);

    ListAccess<O> execute() {
      Query<D> delegateQ = new Query<>(q, dataType);
      LazyPageList<D> r = findLazyPageList(delegateQ);
      List<D> tmp;
      try {
        tmp = r.getAll();
      } catch (Exception e) {
        throw new IllegalStateException("Error retrieving element with query " + q, e);
      }
      tmp = sort(tmp, this.cp);
      final List<D> list = tmp;
      return new ListAccess<O>() {
        public int getSize() {
          return list.size();
        }

        @SuppressWarnings("unchecked")
        public O[] load(int index, int length) {
          O[] pages = (O[]) Array.newInstance(q.getClassType(), length);
          int i = 0;
          for (D data : list.subList(index, index + length)) {
            pages[i++] = create(data);
          }
          return pages;
        }
      };
    }

    private List<D> sort(List<D> list, final Comparator<O> comparator) {
      if (comparator != null) {
        List<D> tmpList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
          tmpList.add(list.get(i));
        }
        Collections.sort(tmpList, (D d1, D d2) -> {
          O o1 = create(d1);
          O o2 = create(d2);
          return comparator.compare(o1, o2);
        });
        return tmpList;
      } else {
        return list;
      }
    }
  }

  protected void broadcastEvent(String eventName, Object data) {
    try {
      listenerService.broadcast(eventName, this, data);
    } catch (Exception e) {
      LOG.warn("Error when broadcasting notification " + eventName + " for " + data, e);
    }
  }

  private String[] getPermissionsFromTemplate(String[] accessPermissions, String permission) {
    if (ArrayUtils.isEmpty(accessPermissions)) {
      return accessPermissions;
    }
    return Arrays.stream(accessPermissions)
                 .map(p -> getPermissionFromTemplate(p, permission))
                 .toArray(String[]::new);
  }

  private String[] getPermissionsToTemplate(String[] accessPermissions, String permission) {
    if (ArrayUtils.isEmpty(accessPermissions)) {
      return accessPermissions;
    }
    return Arrays.stream(accessPermissions)
                 .map(p -> getPermissionToTemplate(p, permission))
                 .toArray(String[]::new);
  }

  private String getPermissionToTemplate(String selectedPermission, String permission) {
    return selectedPermission.replace(permission, OWNER_ID_TEMPLATE);
  }

  private String getPermissionFromTemplate(String selectedPermission, String permission) {
    return selectedPermission.replace(OWNER_ID_TEMPLATE, permission);
  }

  private void applyPermissionTemplate(ModelObject model) {
    if (model instanceof Container container) {
      container.setAccessPermissions(new String[] {UserACL.EVERYONE});
      ArrayList<ModelObject> children = container.getChildren();
      applyPermissionTemplate(children);
    } else if (model instanceof Application application) {
      application.setAccessPermissions(new String[] {UserACL.EVERYONE});
    }
  }

  private void applyPermissionTemplate(ArrayList<ModelObject> children) {
    if (CollectionUtils.isNotEmpty(children)) {
      children.forEach(this::applyPermissionTemplate);
    }
  }

}
