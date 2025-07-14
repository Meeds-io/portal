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

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.QueryResult;
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.importer.Status;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.pom.data.ModelChange;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

public interface LayoutService {

  String PAGE_CREATED          = "org.exoplatform.portal.mop.service.LayoutService.pageCreated".intern();

  String PAGE_REMOVED          = "org.exoplatform.portal.mop.service.LayoutService.pageRemoved".intern();

  String PAGE_UPDATED          = "org.exoplatform.portal.mop.service.LayoutService.pageUpdated".intern();

  String PORTAL_CONFIG_CREATED = "org.exoplatform.portal.mop.service.LayoutService.portalConfigCreated".intern();

  String PORTAL_CONFIG_REMOVED = "org.exoplatform.portal.mop.service.LayoutService.portalConfigRemoved".intern();

  String PORTAL_CONFIG_UPDATED = "org.exoplatform.portal.mop.service.LayoutService.portalConfigUpdated".intern();

  /**
   * Create a PortalConfig in database <br>
   * Then broadcast PORTAL_CONFIG_CREATED event
   *
   * @param config {@link PortalConfig}
   */
  void create(PortalConfig config);

  /**
   * This method should update the PortalConfig object <br>
   * Then broadcast PORTAL_CONFIG_UPDATED event
   *
   * @param config {@link PortalConfig}
   */
  void save(PortalConfig config);

  /**
   * This method should load the PortalConfig object from db according to the
   * portalName
   *
   * @param portalName
   * @return {@link PortalConfig}
   */
  PortalConfig getPortalConfig(String portalName);

  /**
   * This method should load the PortalConfig object from db according to the
   * portalName and ownerType
   *
   * @param portalName
   * @param ownerType
   * @return {@link PortalConfig}
   */
  PortalConfig getPortalConfig(String ownerType, String portalName);

  /**
   * Retrieves {@link PortalConfig} of designated {@link SiteKey}
   * 
   * @param siteKey {@link SiteKey}
   * @return null if not found, else {@link PortalConfig}
   */
  PortalConfig getPortalConfig(SiteKey siteKey);

  /**
   * This method should load the PortalConfig object from db according to the
   * siteId
   *
   * @param siteId
   * @return {@link PortalConfig}
   */
  PortalConfig getPortalConfig(long siteId);

  /**
   * Remove the PortalConfig from the database <br>
   * Then broadcast PORTAL_CONFIG_REMOVED event
   *
   * @param config {@link PortalConfig}
   */
  void remove(PortalConfig config);

  /**
   * Removes all pages of a given site
   * 
   * @param siteKey {@link SiteKey}
   */
  void removePages(SiteKey siteKey);

  /**
   * This method should load the Page object from the database according to the
   * pageId
   *
   * @param pageId - String represent id of page, it must be valid pageId (3
   *          parts saparate by :: )
   * @return {@link Page}
   */
  Page getPage(String pageId);

  /**
   * This method should load the Page object from the database according to the
   * technical identifier of the page
   *
   * @param id - UUID of page
   * @return {@link Page}
   */
  Page getPage(long id);

  /**
   * Retrieves Page designated by its key
   * 
   * @param pageKey {@link PageKey}
   * @return {@link Page}
   */
  Page getPage(PageKey pageKey);

  /**
   * retrieves {@link PageContext} switch its key
   * 
   * @param pageKey {@link PageKey}
   * @return {@link PageContext}
   */
  PageContext getPageContext(PageKey pageKey);

  /**
   * Retrieves the list of pages in a give site
   * 
   * @param siteKey {@link SiteKey}
   * @return {@link List} of type {@link PageContext}
   */
  List<PageContext> findPages(SiteKey siteKey);

  /**
   * Search for pages using some criteria and pagination options
   * 
   * @param offset pagination offset
   * @param limit pagination limit
   * @param siteType {@link SiteType}
   * @param siteName site name
   * @param pageName page name search term
   * @param pageTitle page title search term
   * @return {@link QueryResult} containing list of pages
   */
  QueryResult<PageContext> findPages(int offset,
                                     int limit,
                                     SiteType siteType,
                                     String siteName,
                                     String pageName,
                                     String pageTitle);

  /**
   * Removes a given page from store
   * 
   * @param page {@link Page}
   */
  void remove(Page page);

  /**
   * Removes a page from store designated by its {@link PageKey}
   * 
   * @param pageKey {@link PageKey}
   */
  void remove(PageKey pageKey);

  /**
   * This method should create or update the given page object <br>
   * Then broasdcast PAGE_CREATED event
   *
   * @param page
   */
  void create(Page page);

  /**
   * Saves a page. If a page with the same id already exists then a merge
   * operation will occur, otherwise it throws {@link IllegalStateException}
   * <br>
   * The operation returns a list of the change object that describes the
   * changes that occured during the save operation. <br>
   * Then broadcast PAGE_UPDATED event
   *
   * @param page the page to save
   * @return the list of model changes that occurred during the save
   */
  List<ModelChange> save(Page page);

  /**
   * Saves a {@link PageContext} with its {@link PageState} in addition to the
   * layout defined in {@link Page} object
   * 
   * @param pageContext {@link PageContext} with title, description and
   *          permissions
   * @param page {@link Page} to save page layout and structure
   */
  void save(PageContext pageContext, Page page);

  /**
   * Saves a {@link PageContext} with its {@link PageState}
   * 
   * @param pageContext {@link PageContext} with title, description and
   *          permissions
   */
  void save(PageContext pageContext);

  /**
   * Retrieved {@link Application} model switch its storage identifier
   * 
   * @param applicationStorageId
   * @return {@link Application}
   */
  Application getApplicationModel(String applicationStorageId);

  /**
   * Return contentId according to each state (transient, persistent, clone)
   * 
   * @param state
   * @return {@link ApplicationState} of type {@link Portlet}
   */
  String getId(ApplicationState state);

  /**
   * Return content state. If can't find, return null
   * 
   * @param state - ApplicationState object
   * @return {@link Portlet}
   */
  Portlet load(ApplicationState state);

  /**
   * Save content state <br>
   * 
   * @param state - ApplicationState object. It must be CloneApplicationState or
   *          PersistentApplicationState object
   * @param preferences - object to be saved
   * @return {@link ApplicationState} typed with {@link Portlet}
   */
  ApplicationState save(ApplicationState state, Portlet preferences);

  /**
   * Return LazyPageList of object (sorted) which type and other info determined
   * in Query object
   * 
   * @param <T> could be of type {@link Page} or {@link PortalConfig}
   * @param q - Query object
   * @param sortComparator {@link Comparator} used for sorting results
   * @return {@link LazyPageList}
   */
  <T> LazyPageList<T> find(Query<T> q, Comparator<T> sortComparator);

  /**
   * Return ListAccess, we can retrieved array of object (sorted) in database
   * through this.
   * 
   * @param <T> could be of type {@link Page} or {@link PortalConfig}
   * @param q - Query object
   * @param sortComparator - Comparator object, used to sort the result list
   * @return {@link LazyPageList}
   */
  <T> ListAccess<T> find2(Query<T> q, Comparator<T> sortComparator);

  /**
   * Return shared layout containing common layout of all sites (user, group and
   * sites). This will retrieve the layout from
   * /conf/portal/portal/sharedlayout-{siteName}.xml else if not found, retrieve
   * it from /conf/portal/portal/sharedlayout.xml
   * 
   * @param siteName
   * @return {@link Container}
   */
  Container getSharedLayout(String siteName);

  /**
   * Retrieves all site type names with pagination.
   * 
   * @param siteType {@link SiteType}
   * @param offset offset of the query
   * @param limit limit of the list to fetch
   * @return {@link List} of corresponding site names, else empty list if not
   *         found
   */
  List<String> getSiteNames(SiteType siteType, int offset, int limit);

  default Status getImportStatus() { // NOSONAR
    throw new UnsupportedOperationException();
  }

  default void saveImportStatus(Status status) { // NOSONAR
    throw new UnsupportedOperationException();
  }

  /**
   * Retrieves sites according to the given site filter
   * 
   * @param siteFilter {@link SiteFilter} site filter for results filtering
   * @return {@link List} of sites portal config
   */
  default List<PortalConfig> getSites(SiteFilter siteFilter) {
    throw new UnsupportedOperationException();
  }

  /**
   * Retrieve site banner as stream
   *
   * @param siteName name of the portal site
   * @return found {@link InputStream}
   * @throws ObjectNotFoundException When site not found or file attachment
   */
  default InputStream getSiteBannerStream(String siteName) throws ObjectNotFoundException, IOException {
    throw new UnsupportedOperationException();
  }

  /**
   * Retrieve default site banner as stream
   *
   * @param siteName name of the portal site
   * @return found {@link InputStream}
   */
  default InputStream getDefaultSiteBannerStream(String siteName) {
    throw new UnsupportedOperationException();
  }

  /**
   * remove site banner
   *
   * @param siteName name of the portal site
   * @throws ObjectNotFoundException When site not found or file attachment
   */
  default void removeSiteBanner(String siteName) throws ObjectNotFoundException {
    throw new UnsupportedOperationException();
  }

  /**
   * @param sourceSiteTemplate {@link SiteKey} of template
   * @param targetSiteKey {@link SiteKey} to create
   */
  default void savePortalFromTemplate(SiteKey sourceSiteTemplate,
                                      SiteKey targetSiteKey) throws ObjectNotFoundException {
    savePortalFromTemplate(sourceSiteTemplate, targetSiteKey, null);
  }

  /**
   * @param sourceSiteTemplate {@link SiteKey} of template
   * @param targetSiteKey {@link SiteKey} to create
   * @param permission Permission to use to change pattern "@owenr@" if existing
   *          in pages and portal layouts
   */
  default void savePortalFromTemplate(SiteKey sourceSiteTemplate,
                                      SiteKey targetSiteKey,
                                      String permission) throws ObjectNotFoundException {
    throw new UnsupportedOperationException();
  }

  /**
   * @param sourceSiteTemplate {@link SiteKey} of template
   * @param targetSiteKey {@link SiteKey} to create
   * @param permission Permission to use to change pattern "@owenr@" if existing
   *          in pages and portal layouts
   * @throws ObjectNotFoundException
   */
  default void savePagesFromTemplate(SiteKey sourceSiteTemplate,
                                     SiteKey targetSiteKey,
                                     String permission) throws ObjectNotFoundException {
    throw new UnsupportedOperationException();
  }

  /**
   * @param sourcePageTemplateKey Site template's Page Key
   * @param targetSiteKey destination Site Key
   * @param permission Permission to use to change pattern "@owenr@" if existing
   *          in pages and portal layouts
   */
  default void savePageFromTemplate(PageKey sourcePageTemplateKey,
                                    SiteKey targetSiteKey,
                                    String permission) throws ObjectNotFoundException {
    throw new UnsupportedOperationException();
  }

}
