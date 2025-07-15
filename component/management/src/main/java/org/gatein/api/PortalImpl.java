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
package org.gatein.api;

import java.util.*;

import org.exoplatform.portal.mop.PageType;
import org.gatein.api.common.Filter;
import org.gatein.api.common.Pagination;
import org.gatein.api.internal.Parameters;
import org.gatein.api.navigation.Navigation;
import org.gatein.api.navigation.NavigationImpl;
import org.gatein.api.oauth.OAuthProvider;
import org.gatein.api.page.*;
import org.gatein.api.security.*;
import org.gatein.api.site.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.config.*;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.QueryResult;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.page.*;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.storage.DescriptionStorage;
import org.exoplatform.services.resources.ResourceBundleManager;
import org.exoplatform.services.security.*;

@Deprecated(forRemoval = true, since = "7.0")
public class PortalImpl implements Portal {

  private static final Query<PortalConfig> SITES      = new Query<>(
                                                                    org.exoplatform.portal.mop.SiteType.PORTAL.getName(),
                                                                    null,
                                                                    PortalConfig.class);

  private static final Query<PortalConfig> SPACES     = new Query<>(
                                                                    org.exoplatform.portal.mop.SiteType.GROUP.getName(),
                                                                    null,
                                                                    PortalConfig.class);

  private static final Query<PortalConfig> DASHBOARDS = new Query<>(
                                                                    org.exoplatform.portal.mop.SiteType.USER.getName(),
                                                                    null,
                                                                    PortalConfig.class);

  static final Log                         log        = ExoLogger.getLogger("org.gatein.api");

  private final LayoutService              layoutService;

  private final NavigationService          navigationService;

  private final DescriptionStorage         descriptionStorage;

  private final ResourceBundleManager      bundleManager;

  private final UserACL                    acl;

  private final Authenticator              authenticator;

  private final IdentityRegistry           identityRegistry;

  private final UserPortalConfigService    userPortalConfigService;

  public PortalImpl(LayoutService layoutService, // NOSONAR
                    NavigationService navigationService,
                    DescriptionStorage descriptionStorage,
                    ResourceBundleManager bundleManager,
                    Authenticator authenticator,
                    IdentityRegistry identityRegistry,
                    UserACL acl,
                    UserPortalConfigService userPortalConfigService) {
    this.layoutService = layoutService;
    this.navigationService = navigationService;
    this.descriptionStorage = descriptionStorage;
    this.bundleManager = bundleManager;
    this.authenticator = authenticator;
    this.identityRegistry = identityRegistry;
    this.acl = acl;
    this.userPortalConfigService = userPortalConfigService;
  }

  @Override
  public Site getSite(SiteId siteId) {
    Parameters.requireNonNull(siteId, "siteId");
    SiteKey siteKey = Util.from(siteId);

    try {
      PortalConfig portalConfig = layoutService.getPortalConfig(siteKey.getTypeName(), siteKey.getName());
      return (portalConfig == null) ? null : new SiteImpl(portalConfig);
    } catch (Throwable e) {
      throw new ApiException("Failed to get site", e);
    }
  }

  @Override
  public Site createSite(SiteId siteId) {
    String template = userPortalConfigService.getDefaultPortalTemplate();
    if (template == null) {
      template = ""; // This is valid if we're being executed within a test
                     // environment
    }
    return createSite(siteId, template);
  }

  @Override
  public Site createSite(SiteId siteId, String templateName) throws IllegalArgumentException, EntityAlreadyExistsException {
    if (getSite(siteId) != null) {
      throw new EntityAlreadyExistsException("Cannot create site. Site " + siteId + " already exists.");
    }
    Parameters.requireNonNull(templateName, "templateName");

    // Create new site
    return new SiteImpl(siteId, templateName);
  }

  @Override
  public List<Site> findSites(SiteQuery query) {
    Parameters.requireNonNull(query, "query");

    Pagination pagination = query.getPagination();
    if (pagination != null && query.getSiteTypes().size() > 1) {
      pagination = null; // set it to null so the internal DataStorage doesn't
                         // use it, and we manually page later
      log.warn("Pagination is not supported internally for SiteQuery's with multiple site types. Therefore this query has the possibility to perform poorly.");
    }
    boolean includeAll = query.isIncludeEmptySites();

    List<Site> sites = new ArrayList<Site>();
    for (SiteType type : query.getSiteTypes()) {
      List<Site> sitesFound;
      switch (type) {
      case SITE:
        sitesFound = findSites(pagination, SITES, Comparators.site(query.getSorting()), includeAll);
        break;
      case SPACE:
        sitesFound = findSites(pagination, SPACES, Comparators.site(query.getSorting()), includeAll);
        break;
      case DASHBOARD:
        sitesFound = findSites(pagination, DASHBOARDS, Comparators.site(query.getSorting()), includeAll);
        break;
      default:
        throw new AssertionError();
      }

      sites.addAll(sitesFound);

      // No reason to fetch anymore
      if (pagination != null && sites.size() >= pagination.getLimit()) {
        break;
      }
    }

    filter(sites, query.getFilter());

    // Manually do paging for multiple site types.
    if (query.getSiteTypes().size() > 1) {
      sites = paginate(sites, query.getPagination());
    }

    return sites;
  }

  private List<Site> findSites(Pagination pagination,
                               Query<PortalConfig> query,
                               Comparator<PortalConfig> comparator,
                               boolean includeAllSites) {
    try {
        ListAccess<PortalConfig> access = layoutService.find2(query, comparator);
        int size = access.getSize();
        int offset = pagination != null ? pagination.getOffset() : 0;
        int limit = pagination != null ?  pagination.getLimit() : size;
        if (offset >= size) {
          return Collections.emptyList();
        }
        PortalConfig[] sites = loadSites(includeAllSites, access, size, offset, limit);
        return fromList(Arrays.asList(sites).subList(offset, sites.length));
    } catch (Throwable e) {
      throw new ApiException("Failed to query for sites", e);
    }
  }

  @Override
  public void saveSite(Site site) {
    Parameters.requireNonNull(site, "site");
    ((SiteImpl) site).save(layoutService, userPortalConfigService);
  }

  @Override
  public boolean removeSite(SiteId siteId) {
    Site site = getSite(siteId);
    if (site == null) {
      return false;
    }
    SiteKey siteKey = Util.from(Parameters.requireNonNull(siteId, "siteId"));
    PortalConfig data = new PortalConfig(siteKey.getTypeName(), siteKey.getName());
    try {
      layoutService.remove(data);
      return true;
    } catch (Throwable t) {
      throw new ApiException("Failed to remove site " + siteId, t);
    }
  }

  @Override
  public Navigation getNavigation(SiteId siteId) {
    Parameters.requireNonNull(siteId, "siteId");

    try {
      NavigationContext ctx = navigationService.loadNavigation(Util.from(siteId));
      if (ctx == null)
        return null;

      return new NavigationImpl(siteId, navigationService, ctx, descriptionStorage, bundleManager);
    } catch (Throwable t) {
      throw new ApiException("Failed to load navigation", t);
    }
  }

  @Override
  public Page getPage(PageId pageId) {
    Parameters.requireNonNull(pageId, "pageId");

    try {
      PageContext context = layoutService.getPageContext(Util.from(pageId));
      return (context == null) ? null : new PageImpl(context);
    } catch (Throwable e) {
      throw new ApiException("Failed to get page", e);
    }
  }

  @Override
  public Page createPage(PageId pageId) throws EntityAlreadyExistsException {
    if (getPage(pageId) != null) {
      throw new EntityAlreadyExistsException("Cannot create page. Page " + pageId + " already exists.");
    }

    if (getSite(pageId.getSiteId()) == null) {
      throw new EntityNotFoundException("Site " + pageId.getSiteId() + " doesn't exist");
    }

    Permission edit = Permission.any("platform", "administrators");

    PageState pageState = new PageState(pageId.getPageName(),
                                        null,
                                        false,
                                        false,
                                        false,
                                        null,
                                        null,
                                        Arrays.asList(Util.from(Permission.everyone())),
                                        Util.from(edit)[0],
                                        PageType.PAGE.name(),
                                        null);

    PageImpl p = new PageImpl(new PageContext(Util.from(pageId), pageState));
    p.setCreate(true);
    return p;
  }

  @Override
  public List<Page> findPages(PageQuery query) {
    Pagination pagination = query.getPagination();
    Iterator<PageContext> iterator;
    if (pagination == null) {
      if (query.getSiteType() == null || query.getSiteName() == null)
        throw new IllegalArgumentException("Pagination is required when site type or site name is null.");

      SiteKey siteKey = Util.from(new SiteId(query.getSiteType(), query.getSiteName()));
      iterator = layoutService.findPages(siteKey).iterator();
    } else {
      QueryResult<PageContext> result = layoutService.findPages(pagination.getOffset(),
                                                                pagination.getLimit(),
                                                                Util.from(query.getSiteType()),
                                                                query.getSiteName(),
                                                                null,
                                                                query.getDisplayName());

      iterator = result.iterator();
    }

    List<Page> pages = new ArrayList<Page>();
    while (iterator.hasNext()) {
      pages.add(new PageImpl(iterator.next()));
    }

    filter(pages, query.getFilter());

    return pages;
  }

  @Override
  public void savePage(Page page) {
    Parameters.requireNonNull(page, "page");

    if (getSite(page.getSiteId()) == null) {
      throw new EntityNotFoundException("Site " + page.getSiteId() + " doesn't exist");
    }

    if (((PageImpl) page).isCreate() && getPage(page.getId()) != null) {
      // There is still a small chance someone else creates the page, but this
      // is currently the best we can do
      throw new EntityAlreadyExistsException("Cannot create page. Page " + page.getId() + " already exists.");
    }

    PageContext context = ((PageImpl) page).getPageContext();

    try {
      layoutService.save(context);
    } catch (Throwable t) {
      throw new ApiException("Failed to save page " + page.getId(), t);
    }
  }

  @Override
  public boolean removePage(PageId pageId) {
    Parameters.requireNonNull(pageId, "pageId");

    try {
      PageKey pageKey = Util.from(pageId);
      PageContext pageContext = layoutService.getPageContext(pageKey);
      if (pageContext == null) {
        return false;
      } else {
        layoutService.remove(pageKey);
        return true;
      }
    } catch (PageServiceException e) {
      if (e.getError() == PageError.NO_SITE) {
        throw new EntityNotFoundException("Cannot remove page '" + pageId.getPageName() + "'. Site " + pageId.getSiteId()
            + " does not exist.");
      } else {
        throw new ApiException("Failed to remove page " + pageId, e);
      }
    } catch (Throwable t) {
      throw new ApiException("Failed to remove page " + pageId, t);
    }
  }

  @Override
  public boolean hasPermission(User user, Permission permission) {
    if (permission.isAccessibleToEveryone()) {
      return true;
    }

    Identity identity;
    if (user == User.anonymous()) {
      identity = new Identity(IdentityConstants.ANONIM);
    } else {
      try {
        identity = identityRegistry.getIdentity(user.getId());
      } catch (Throwable t) {
        throw new ApiException("Failed top retrieve identity", t);
      }
    }

    if (identity == null) {
      try {
        identity = authenticator.createIdentity(user.getId());
      } catch (Throwable t) {
        throw new ApiException("Failed to retrieve user identity", t);
      }

      if (identity == null) {
        throw new EntityNotFoundException("User not found");
      }

      try {
        identityRegistry.register(identity);
      } catch (Throwable t) {
        throw new ApiException("Failed to register identity", t);
      }
    }

    try {
      for (Membership membership : permission.getMemberships()) {
        if (acl.hasPermission(identity, membership.toString())) {
          return true;
        }
      }
      return false;

    } catch (Throwable t) {
      throw new ApiException("Failed to check permissions", t);
    }
  }

  @Override
  public OAuthProvider getOAuthProvider(String oauthProviderKey) {
    throw new UnsupportedOperationException();
  }

  private static <T> void filter(List<T> list, Filter<T> filter) {
    if (filter == null)
      return;

    for (Iterator<T> iterator = list.iterator(); iterator.hasNext();) {
      if (!filter.accept(iterator.next())) {
        iterator.remove();
      }
    }
  }

  private static <T> List<T> paginate(List<T> list, Pagination pagination) {
    if (pagination == null)
      return list;
    if (pagination.getOffset() >= list.size())
      return Collections.emptyList();

    if (pagination.getOffset() + pagination.getLimit() > list.size()) {
      return new ArrayList<T>(list.subList(pagination.getOffset(), list.size()));
    } else {
      return new ArrayList<T>(list.subList(pagination.getOffset(), pagination.getOffset() + pagination.getLimit()));
    }
  }

  private static List<Site> fromList(List<PortalConfig> internalSites) {
    List<Site> sites = new ArrayList<Site>(internalSites.size());
    for (PortalConfig internalSite : internalSites) {
      if (internalSite == null)
        continue;

      sites.add(new SiteImpl(internalSite));
    }
    return sites;
  }

  private PortalConfig[] loadSites(boolean includeAllSites,
                                   ListAccess<PortalConfig> access,
                                   int size,
                                   int offset,
                                   int limit) throws Exception {
    PortalConfig[] sites = new PortalConfig[Math.min(limit + offset, size)];
    PortalConfig[] loaded;
    int loadIndex = 0;
    int loadLength = sites.length;
    int index = 0;
    int length = 0;
    while (index < sites.length && loadIndex < size) {
      // Load sites from backend filtering empty sites if needed
      // (includeAllSites=false)
      loaded = load(access, loadIndex, loadLength, includeAllSites);

      // Copy contents to sites array
      int copyLength = Math.min(loaded.length, sites.length - index);
      System.arraycopy(loaded, 0, sites, index, copyLength);

      // Update what has been copied
      index = index + loaded.length;
      length = length + copyLength;
      if (length == sites.length) {
        break;
      }

      // Update what has been loaded
      loadIndex = loadIndex + sites.length;
      loadLength = loadLength + limit;
      if (loadLength + loadIndex > size) {
        loadLength = size - loadIndex;
      }
    }
    return sites;
  }

  private PortalConfig[] load(ListAccess<PortalConfig> access, int start, int end, boolean includeAllSites) throws Exception {
    PortalConfig[] loaded = access.load(start, end);
    List<PortalConfig> list = new ArrayList<PortalConfig>(loaded.length);
    for (PortalConfig pc : loaded) {
      if (pc == null)
        continue;

      NavigationContext ctx = null;
      if (!includeAllSites) {
        try {
          ctx = navigationService.loadNavigation(new SiteKey(pc.getType(), pc.getName()));
        } catch (Throwable t) {
          throw new ApiException("Failed to find sites", t);
        }
      }

      if (includeAllSites || ctx != null) {
        list.add(pc);
      }
    }

    return list.toArray(new PortalConfig[list.size()]);
  }

}
