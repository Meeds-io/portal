/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.portal.mop.PageType;
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.storage.DescriptionStorage;
import org.exoplatform.portal.mop.storage.PageStorage;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import io.meeds.common.ContainerTransactional;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Created by The eXo Platform SAS Apr 19, 2007 This service is used to load the PortalConfig, Page config and Navigation config
 * for a given user.
 */
public class UserPortalConfigService implements Startable {

  private static final String     PUBLIC_SITE_NAME               = "public";

  private static final Scope      HOME_PAGE_URI_PREFERENCE_SCOPE = Scope.PORTAL.id("HOME");

  private static final String     HOME_PAGE_URI_PREFERENCE_KEY   = "HOME_PAGE_URI";

  public static final String      DEFAULT_GLOBAL_PORTAL       = "global";

  public static final String      DEFAULT_GROUP_SITE_TEMPLATE = "group";

  public static final String      DEFAULT_USER_SITE_TEMPLATE  = "user";

  protected final SiteFilter      siteFilter                     = new SiteFilter(SiteType.PORTAL,
                                                                                  null,
                                                                                  null,
                                                                                  true,
                                                                                  true,
                                                                                  false,
                                                                                  false,
                                                                                  0,
                                                                                  0);

    LayoutService layoutService;

    UserACL userACL_;

    OrganizationService orgService_;

    private SettingService          settingService;

    private NewPortalConfigListener newPortalConfigListener_;

    /** . */
    final NavigationService navigationService;

    /** . */
    final DescriptionStorage descriptionStorage;

    final PageStorage        pageStorage;

    /** . */
    boolean createUserPortal;

    /** . */
    boolean destroyUserPortal;
    
    /** . */
    String globalPortal_;

    /** . */
    String defaultGroupSiteTemplate;

    /** . */
    String defaultUserSiteTemplate;

    /** . */
    private final ImportMode defaultImportMode;

    private PortalConfig metaPortalConfig;

    private Log log = ExoLogger.getLogger("Portal:UserPortalConfigService");

    public UserPortalConfigService(UserACL userACL, // NOSONAR
                                   LayoutService storage,
                                   OrganizationService orgService,
                                   NavigationService navService,
                                   DescriptionStorage descriptionService,
                                   PageStorage pageStorage,
                                   SettingService settingService,
                                   InitParams params) {

        //
        ValueParam createUserPortalParam = params == null ? null : params.getValueParam("create.user.portal");
        boolean createUserPortal = createUserPortalParam == null
                || createUserPortalParam.getValue().toLowerCase().trim().equals("true");

        //
        ValueParam destroyUserPortalParam = params == null ? null : params.getValueParam("destroy.user.portal");
        boolean destroyUserPortal = destroyUserPortalParam == null
                || destroyUserPortalParam.getValue().toLowerCase().trim().equals("true");

        //
        ValueParam defaultImportModeParam = params == null ? null : params.getValueParam("default.import.mode");
        ImportMode defaultImportMode = defaultImportModeParam == null ? ImportMode.CONSERVE : ImportMode
                .valueOf(defaultImportModeParam.getValue().toUpperCase().trim());

        //
        ValueParam globalPortalParam = params == null ? null : params.getValueParam("global.portal");
        this.globalPortal_ = globalPortalParam == null ? DEFAULT_GLOBAL_PORTAL : globalPortalParam.getValue();

        ValueParam defaultGroupSiteTemplateParam = params == null ? null : params.getValueParam("default.groupSite.template");
        String defaultGroupSiteTemplate = defaultGroupSiteTemplateParam == null ? DEFAULT_GROUP_SITE_TEMPLATE : defaultGroupSiteTemplateParam.getValue();

        //
        ValueParam defaultUserSiteTemplateParam = params == null ? null : params.getValueParam("default.userSite.template");
        String defaultUserSiteTemplate = defaultUserSiteTemplateParam == null ? DEFAULT_USER_SITE_TEMPLATE : defaultUserSiteTemplateParam.getValue();

        //
        this.layoutService = storage;
        this.orgService_ = orgService;
        this.settingService = settingService;
        this.userACL_ = userACL;
        this.navigationService = navService;
        this.descriptionStorage = descriptionService;
        this.pageStorage = pageStorage;
        this.createUserPortal = createUserPortal;
        this.destroyUserPortal = destroyUserPortal;
        this.defaultImportMode = defaultImportMode;
        this.defaultGroupSiteTemplate = defaultGroupSiteTemplate;
        this.defaultUserSiteTemplate = defaultUserSiteTemplate;
        this.siteFilter.setExcludedSiteName(globalPortal_);
    }

    public PageStorage getPageService() {
        return pageStorage;
    }

    public LayoutService getDataStorage() {
        return layoutService;
    }

    public ImportMode getDefaultImportMode() {
        return defaultImportMode;
    }

    public boolean getCreateUserPortal() {
        return createUserPortal;
    }

    public void setCreateUserPortal(boolean createUserPortal) {
        this.createUserPortal = createUserPortal;
    }

    public boolean getDestroyUserPortal() {
        return destroyUserPortal;
    }

    public void setDestroyUserPortal(boolean destroyUserPortal) {
        this.destroyUserPortal = destroyUserPortal;
    }

    public String getDefaultGroupSiteTemplate() {
      return defaultGroupSiteTemplate;
    }

    public void setDefaultGroupSiteTemplate(String defaultGroupSiteTemplate) {
      this.defaultGroupSiteTemplate = defaultGroupSiteTemplate;
    }

    public String getDefaultUserSiteTemplate() {
      return defaultUserSiteTemplate;
    }

    public void setDefaultUserSiteTemplate(String defaultUserSiteTemplate) {
      this.defaultUserSiteTemplate = defaultUserSiteTemplate;
    }

    /**
     * Returns the navigation service associated with this service.
     *
     * @return the navigation service;
     */
    public NavigationService getNavigationService() {
        return navigationService;
    }

    public DescriptionStorage getDescriptionService() {
        return descriptionStorage;
    }

    public UserACL getUserACL() {
        return userACL_;
    }

    public OrganizationService getOrganizationService() {
        return orgService_;
    }

    /**
     * <p>
     * Build and returns an instance of <code>UserPortalConfig</code>.
     * </p>
     * <br>
     * <p>
     * To return a valid config, the current thread must be associated with an identity that will grant him access to the portal
     * as returned by the {@link UserACL#hasAccessPermission(org.exoplatform.portal.config.model.PortalConfig, Identity)} method.
     * </p>
     * <br>
     * The navigation loaded on the <code>UserPortalConfig</code> object are obtained according to the specified user
     * argument. The portal navigation is always loaded. If the specified user is null then the navigation of the guest
     * group as configured by UserACL#getGuestsGroup is also loaded, otherwise
     * the navigations are loaded according to the following rules:
     * <br>
     * <ul> <li>The navigation corresponding to the user is loaded.</li> <li>When the user is root according to the value
     * returned by UserACL#getSuperUser then the navigation of all groups are
     * loaded.</li> <li>When the user is not root, then all its groups are added except the guest group as configued per
     * UserACL#getGuestsGroup.</li> </ul>
     * <br>
     * All the navigations are sorted using the value returned by {@link org.exoplatform.portal.config.model.PageNavigation#getPriority()}.
     *
     * @param portalName the portal name
     * @param username the user name
     * @return the config
     */
    public UserPortalConfig getUserPortalConfig(String portalName, String username) {
        return getUserPortalConfig(portalName, username, LocaleContextInfoUtils.getUserLocale(username));
    }

    public UserPortalConfig getUserPortalConfig(String portalName, String accessUser, Locale locale) {
        PortalConfig portal = layoutService.getPortalConfig(portalName);
        if (portal == null || !userACL_.hasAccessPermission(portal, userACL_.getUserIdentity(accessUser))) {
            return null;
        }
        return new UserPortalConfig(portal, this, portalName, accessUser, locale);
    }

    /**
     * Compute and returns the list that the specified user can manage. If the user is root then all existing groups are
     * returned otherwise the list is computed from the groups in which the user has a configured membership. The membership is
     * configured from the value returned by UserACL#getMakableMT
     *
     * @param remoteUser the user to get the makable navigations
     * @param withSite true if a site must exist
     * @return the list of groups
     * @throws Exception any exception
     */
    public List<String> getMakableNavigations(String remoteUser, boolean withSite) throws Exception {
        Collection<Group> groups;
        if (remoteUser.equals(userACL_.getSuperUser())) {
            groups = orgService_.getGroupHandler().getAllGroups();
        } else {
            groups = orgService_.getGroupHandler().resolveGroupByMembership(remoteUser, userACL_.getMakableMT());
        }

        //
        List<String> list = new ArrayList<String>();
        if (groups != null) {
            Set<String> existingNames = null;
            if (withSite) {
                existingNames = new HashSet<String>();
                Query<PortalConfig> q = new Query<PortalConfig>("group", null, PortalConfig.class);
                LazyPageList<PortalConfig> lpl = layoutService.find(q, null);
                for (PortalConfig groupSite : lpl.getAll()) {
                    existingNames.add(groupSite.getName());
                }
            }

            //
            for (Group group : groups) {
                String groupId = group.getId().trim();
                if (existingNames == null || existingNames.contains(groupId)) {
                    list.add(groupId);
                }
            }
        }

        //
        return list;
    }

/**
     * Returns a boolean hasNav according to if the user has at least one makable navigation i.e if he
     * belongs to at least one group
     * @param remoteUser the user to get the makable navigations
     * @param withSite whether or not check if siteConfig exists.
     * @return true or false
     * @throws Exception any exception
     */
    public boolean hasMakableNavigations(String remoteUser, boolean withSite) throws Exception {
        Collection<Group> groups;
        boolean hasNav = false;
        if (remoteUser == null) {
            hasNav = false;
        } else if (remoteUser.equals(userACL_.getSuperUser())) {
            hasNav = true; // as the super user is member of all groups
        } else {
            groups = orgService_.getGroupHandler().resolveGroupByMembership(remoteUser, userACL_.getMakableMT());
            if (groups != null && groups.size() > 0) {
                  if (withSite) {
                      for (Group group : groups) {
                        PortalConfig cfg = layoutService.getPortalConfig(PortalConfig.GROUP_TYPE, group.getId());
                        if (cfg != null) {
                          hasNav = true;
                          break;
                        }
                      }
                  } else {
                      hasNav = true;
                  }
              }
        }
 
        return hasNav;
	}
    /**
     * Create a user site for the specified user. It will perform the following:
     * <ul>
     * <li>create the user site by calling {@link #createUserPortalConfig(String, String, String)} which may create a site or
     * not according to the default configuration</li>
     * <li>if not site exists then it creates a site then it creates an empty site</li>
     * <li>if not navigation exists for the user site then it creates an empty navigation</li>
     * </ul>
     *
     * @param userName the user name
     */
    public void createUserSite(String userName) {
        // Create the portal using default template
        createUserPortalConfig(PortalConfig.USER_TYPE, userName, getDefaultUserSiteTemplate());

        // Need to insert the corresponding user site if needed
        PortalConfig cfg = layoutService.getPortalConfig(PortalConfig.USER_TYPE, userName);
        if (cfg == null) {
            cfg = new PortalConfig(PortalConfig.USER_TYPE, userName);
            cfg.useMetaPortalLayout();
            layoutService.create(cfg);
        }

        // Create a blank navigation if needed
        SiteKey key = SiteKey.user(userName);
        NavigationContext nav = navigationService.loadNavigation(key);
        if (nav == null) {
            nav = new NavigationContext(key, new NavigationState(5));
            navigationService.saveNavigation(nav);
        }
    }

    /**
     * Create a group site for the specified group. It will perform the following:
     * <ul>
     * <li>create the group site by calling {@link #createUserPortalConfig(String, String, String)} which may create a site or
     * not according to the default configuration</li>
     * <li>if not site exists then it creates a site then it creates an empty site</li>
     * </ul>
     *
     * @param groupId the group id
     */
    public void createGroupSite(String groupId) {
        // Create the portal using default template
        createUserPortalConfig(PortalConfig.GROUP_TYPE, groupId, getDefaultGroupSiteTemplate());

        // Need to insert the corresponding group site
        PortalConfig cfg = layoutService.getPortalConfig(PortalConfig.GROUP_TYPE, groupId);
        if (cfg == null) {
            cfg = new PortalConfig(PortalConfig.GROUP_TYPE, groupId);
            cfg.useMetaPortalLayout();
            layoutService.create(cfg);
        }
    }

    /**
     * This method should create a the portal config, pages and navigation according to the template name.
     *
     * @param siteType the site type
     * @param siteName the Site name
     * @param template the template to use
     */
    public void createUserPortalConfig(String siteType, String siteName, String template) {
      NewPortalConfig portalConfig = null;
      if (StringUtils.isBlank(template)) {
        portalConfig = new NewPortalConfig();
        portalConfig.setUseMetaPortalLayout(true);
      } else {
        String templatePath = newPortalConfigListener_.getTemplateConfig(siteType, template);
        portalConfig = new NewPortalConfig(templatePath);
        portalConfig.setTemplateName(template);
      }

      portalConfig.setOwnerType(siteType);
      newPortalConfigListener_.createPortalConfig(portalConfig, siteName);
      newPortalConfigListener_.createPage(portalConfig, siteName);
      newPortalConfigListener_.createPageNavigation(portalConfig, siteName);
    }

    /**
     * This method should create a the portal config, pages and navigation
     * according to the template name.
     *
     * @param siteType the site type
     * @param siteName the Site name
     * @param template the template to use
     * @param templatePath the template path to use
     */
    public void createUserPortalConfig(String siteType, String siteName, String template, String templatePath) {
      NewPortalConfig portalConfigPlugin = new NewPortalConfig(templatePath);
      portalConfigPlugin.setTemplateName(template);
      portalConfigPlugin.setOwnerType(siteType);

      newPortalConfigListener_.createPortalConfig(portalConfigPlugin, siteName);
      newPortalConfigListener_.createPage(portalConfigPlugin, siteName);
      newPortalConfigListener_.createPageNavigation(portalConfigPlugin, siteName);
    }

    /**
     * This method removes the PortalConfig, Page and PageNavigation that belong to the portal in the database.
     *
     * @param portalName the portal name
     */
    public void removeUserPortalConfig(String portalName) {
        removeUserPortalConfig(PortalConfig.PORTAL_TYPE, portalName);
    }

    /**
     * This method removes the PortalConfig, Page and PageNavigation that belong to the portal in the database.
     *
     * @param ownerType the owner type
     * @param ownerId the portal name
     */
    public void removeUserPortalConfig(String ownerType, String ownerId) {
        PortalConfig config = layoutService.getPortalConfig(ownerType, ownerId);
        if (config != null) {
            layoutService.remove(config);
        }
    }

    /**
     * Load metadata of specify page
     *
     * @param pageRef the PageKey
     * @return the PageContext
     */
    public PageContext getPage(PageKey pageRef) {
        if (pageRef == null) {
            return null;
        }

        PageContext page = layoutService.getPageContext(pageRef);
        if (page == null || !userACL_.hasAccessPermission(page, getCurrentIdentity())) {
            return null;
        }
        return page;
    }

    /**
     * Creates a page from an existing template.
     *
     * @param temp the template name
     * @param ownerType the new owner type
     * @param ownerId the new owner id
     * @return the page
     * @throws Exception any exception
     */
    public Page createPageTemplate(String temp, String ownerType, String ownerId) throws Exception {
        Page page = newPortalConfigListener_.createPageFromTemplate(ownerType, ownerId, temp);
        updateOwnership(page, ownerType, ownerId);
        return page;
    }

    public String getGlobalPortal() {
      return globalPortal_;
    }

    public List<String> getSiteNames(SiteType siteType, int offset, int limit) {
      List<String> list = layoutService.getSiteNames(siteType, offset, limit);
      for (Iterator<String> i = list.iterator(); i.hasNext();) {
        String name = i.next();
        if (siteType == SiteType.PORTAL && StringUtils.equals(name, globalPortal_)) {
          i.remove();
          continue;
        }
        PortalConfig config = layoutService.getPortalConfig(siteType.getName(), name);
        if (config == null || !userACL_.hasAccessPermission(config, getCurrentIdentity())) {
          i.remove();
        }
      }
      return list;
    }

    public List<PortalConfig> getUserPortalSites() {
      List<PortalConfig> list = layoutService.getSites(siteFilter);
      return list.stream()
                 .filter(Objects::nonNull)
                 .filter(portalConfig -> userACL_.hasAccessPermission(portalConfig, getCurrentIdentity()))
                 .sorted((s1, s2) -> {
                   if (!s2.isDisplayed() && !s1.isDisplayed()) {
                     if (StringUtils.equals(s1.getName(), PUBLIC_SITE_NAME)) {
                       return -1;
                     } else if (StringUtils.equals(s2.getName(), PUBLIC_SITE_NAME)) {
                       return 1;
                     } else {
                       return s2.getName().compareTo(s1.getName());
                     }
                   } else if (!s2.isDisplayed()) {
                     return -Integer.MAX_VALUE;
                   } else if (!s1.isDisplayed()) {
                     return Integer.MAX_VALUE;
                   } else {
                     int order = s1.getDisplayOrder() - s2.getDisplayOrder();
                     return order == 0 ? s2.getName().compareTo(s1.getName()) : order;
                   }
                 })
                 .toList();
    }

    public String computePortalSitePath(String portalName, HttpServletRequest context) {
      PortalConfig portalConfig = layoutService.getPortalConfig(portalName);
      if (portalConfig == null) {
        return null;
      }
      Collection<UserNode> userNodes = getPortalSiteNavigations(portalName, SiteType.PORTAL.getName(), context);
      UserNode userNode = getFirstAllowedPageNode(userNodes);
      if (userNode == null) {
        return null;
      }
      return getDefaultUri(userNode, portalName);
    }

    public String computePortalPath(HttpServletRequest context) {
      List<PortalConfig> portalConfigList = getUserPortalSites();
      if (CollectionUtils.isEmpty(portalConfigList)) {
        return null;
      }
      return portalConfigList.stream()
                             .filter(PortalConfig::isDefaultSite)
                             .map(portalConfig -> computePortalSitePath(portalConfig.getName(), context))
                             .filter(Objects::nonNull)
                             .findFirst()
                             .orElse(null);
    }

    public Collection<UserNode> getPortalSiteNavigations(String siteName, String portalType, HttpServletRequest context) {
      UserPortalConfig userPortalConfig = getUserPortalConfig(siteName, context.getRemoteUser());
      if (userPortalConfig == null) {
        return Collections.emptyList();
      }
      UserPortal userPortal = userPortalConfig.getUserPortal();
      UserNavigation navigation = userPortal.getNavigation(new SiteKey(SiteType.valueOf(portalType.toUpperCase()), siteName));
      if (navigation == null) {
        return Collections.emptyList();
      }
      UserNodeFilterConfig builder = UserNodeFilterConfig.builder()
                                                         .withReadCheck()
                                                         .withVisibility(Visibility.DISPLAYED, Visibility.TEMPORAL)
                                                         .withTemporalCheck()
                                                         .build();
      UserNode rootNode = userPortal.getNode(navigation, org.exoplatform.portal.mop.navigation.Scope.ALL, builder, null);
      return rootNode != null ? rootNode.getChildren() : Collections.emptyList();
    }

    public UserNode getPortalSiteRootNode(String siteName,String siteType, HttpServletRequest context) {
      UserPortalConfig userPortalConfig = null;
      String username = context.getRemoteUser();
      if (StringUtils.equalsIgnoreCase(siteType, PortalConfig.PORTAL_TYPE)) {
        userPortalConfig = getUserPortalConfig(siteName, username);
      } else {
        PortalConfig defaultPortalConfig = getUserPortalSites().stream().findFirst().orElse(null);
        if (defaultPortalConfig != null) {
          userPortalConfig = getUserPortalConfig(defaultPortalConfig.getName(), username);
        }
      }
      if (userPortalConfig == null) {
        return null;
      }
      UserPortal userPortal = userPortalConfig.getUserPortal();
      UserNavigation navigation = userPortal.getNavigation(new SiteKey(SiteType.valueOf(siteType.toUpperCase()), siteName));
      if (navigation == null) {
        return null;
      }
      UserNodeFilterConfig builder = UserNodeFilterConfig.builder()
                                                         .withReadWriteCheck()
                                                         .withVisibility(Visibility.DISPLAYED, Visibility.TEMPORAL)
                                                         .withTemporalCheck()
                                                         .build();
      return userPortal.getNode(navigation, org.exoplatform.portal.mop.navigation.Scope.ALL, builder, null);
    }

    public UserNode getFirstAllowedPageNode(Collection<UserNode> userNodes) {
      UserNode userNode = null;
      for (UserNode node : userNodes) {
        if (node.getPageRef() != null && layoutService.getPage(node.getPageRef()) != null) {
          userNode = node;
          break;
        } else if (node.getChildren() != null && !node.getChildren().isEmpty()) {
          userNode = getFirstAllowedPageNode(node.getChildren());
        }
        if (userNode != null) {
          break;
        }
      }
      return userNode;
    }

    public String getFirstAllowedPageNode(String portalName, String portalType, String nodePath, HttpServletRequest context) {
      UserNode targetUserNode = getPortalSiteRootNode(portalName, portalType, context);
      if (targetUserNode == null) {
        return nodePath;
      }
      String[] pathNodesNames = nodePath.split("/");
      Iterator<String> iterator = Arrays.stream(pathNodesNames).iterator();
      while (iterator.hasNext() && targetUserNode != null) {
        targetUserNode = targetUserNode.getChild(iterator.next());
      }
      String newPath = null;
      while (newPath == null) {
        if (targetUserNode == null) {
          return nodePath;
        } else if (targetUserNode.getPageRef() != null) {
          newPath = targetUserNode.getURI();
        } else if (!targetUserNode.getChildren().isEmpty()) {
          targetUserNode = getFirstAllowedPageNode(targetUserNode.getChildren());
        } else {
          targetUserNode = targetUserNode.getParent();
        }
      }
      return newPath;
    }

    /**
     * Update the ownership recursively on the model graph.
     *
     * @param object the model object graph root
     * @param ownerType the new owner type
     * @param ownerId the new owner id
     */
    private void updateOwnership(ModelObject object, String ownerType, String ownerId) {
        if (object instanceof Container) {
            Container container = (Container) object;
            if (container instanceof Page) {
                Page page = (Page) container;
                page.setOwnerType(ownerType);
                page.setOwnerId(ownerId);
            }
            for (ModelObject child : container.getChildren()) {
                updateOwnership(child, ownerType, ownerId);
            }
        } else if (object instanceof Application) {
            Application application = (Application) object;
            TransientApplicationState applicationState = (TransientApplicationState) application.getState();
            if (applicationState != null && (applicationState.getOwnerType() == null || applicationState.getOwnerId() == null)) {
                applicationState.setOwnerType(ownerType);
                applicationState.setOwnerId(ownerId);
            }
        }
    }

    public void initListener(ComponentPlugin listener) {
        if (listener instanceof NewPortalConfigListener) {
            synchronized (this) {
                if (newPortalConfigListener_ == null) {
                    this.newPortalConfigListener_ = (NewPortalConfigListener) listener;
                } else {
                    newPortalConfigListener_.mergePlugin((NewPortalConfigListener) listener);
                }
            }
        }
    }

    public void deleteListenerElements(ComponentPlugin listener) {
        if (listener instanceof NewPortalConfigListener) {
            synchronized (this) {
                if (newPortalConfigListener_ != null) {
                    newPortalConfigListener_.deleteListenerElements((NewPortalConfigListener) listener);
                }
            }
        }
    }

    @ContainerTransactional
    public void start() {
        try {
            if (newPortalConfigListener_ == null) {
                return;
            }

            //
            newPortalConfigListener_.run();
        } catch (Exception e) {
            log.error("Could not import initial data", e);
        }

        loadMetaPortalConfig();
    }

    public void stop() {
    }

    /**
     * @return configured default portal
     * @deprecated notion of 'default' portal doesn't exist anymore
     */
    @Deprecated(forRemoval = true, since = "1.5.0")
    public String getDefaultPortal() {
      return newPortalConfigListener_.getDefaultPortal();
    }

    public String getMetaPortal() {
      return newPortalConfigListener_.getMetaPortal();
    }

    /**
     * @param username user name
     * @return User home page uri preference
     */
    public String getUserHomePage(String username) {
      if (StringUtils.isBlank(username)) {
        return null;
      } else {
        SettingValue<?> homePageSettingValue = settingService.get(Context.USER.id(username),
                                                                  HOME_PAGE_URI_PREFERENCE_SCOPE,
                                                                  HOME_PAGE_URI_PREFERENCE_KEY);
        if (homePageSettingValue != null && homePageSettingValue.getValue() != null) {
          return homePageSettingValue.getValue().toString();
        }
        return PropertyManager.getProperty("exo.portal.user.defaultHome");
      }
    }

    /**
     * Stores user default home page
     * 
     * @param username user name
     * @param uri URI to consider as default page of user
     */
    public void saveUserHomePage(String username, String uri) {
      if (StringUtils.isBlank(uri)) {
        settingService.remove(Context.USER.id(username),
                              HOME_PAGE_URI_PREFERENCE_SCOPE,
                              HOME_PAGE_URI_PREFERENCE_KEY);
      } else {
        settingService.set(Context.USER.id(username),
                           HOME_PAGE_URI_PREFERENCE_SCOPE,
                           HOME_PAGE_URI_PREFERENCE_KEY,
                           SettingValue.create(uri));
      }
    }

    /**
     * Returns the default portal template to be used when creating a site
     *
     * @return the default portal template name
     */
    public String getDefaultPortalTemplate() {
        return newPortalConfigListener_.getDefaultPortalTemplate();
    }

    public Set<String> getPortalTemplates() {
        return newPortalConfigListener_.getTemplateConfigs(PortalConfig.PORTAL_TYPE);
    }

    public <T> T getConfig(String portalType,
                           String portalName,
                           Class<T> objectType,
                           String parentLocation) {
      return newPortalConfigListener_.getConfig(portalType, portalName, objectType, parentLocation);
    }

    /**
     * Get the skin name of the default portal
     * @return Skin name of the default portal
     * @deprecated notion of 'default' portal doesn't exist anymore
     */
    @Deprecated(forRemoval = true, since = "1.5.0")
    public String getDefaultPortalSkinName() {
        return getMetaPortalSkinName();
    }

    /**
     * Get the skin name of the default portal
     * @return Skin name of the default portal
     */
    public String getMetaPortalSkinName() {
        return metaPortalConfig != null && StringUtils.isNotBlank(metaPortalConfig.getSkin()) ?
                metaPortalConfig.getSkin() : null;
    }

    /**
     * Get the PortalConfig object of the default portal
     * 
     * @return PortalConfig object of the default portal
     * @deprecated the default portal has been deprecated in favor of a meta
     *             site that aggregate other sites into it and which shares the
     *             same Shared Layout and Portal layout with aggregated sites.
     *             The notion of default portal doesn't exist anymore, since the
     *             default displayed portal/page will be the first portal +
     *             navigation determined switch 'display-order' of portal (portal.xml) and
     *             'node' order definition (navigation.xml)
     */
    @Deprecated(forRemoval = true, since = "1.5.0")
    public PortalConfig getDefaultPortalConfig() {
      return getMetaPortalConfig();
    }

    /**
     * Get the PortalConfig object of the default portal
     * @return PortalConfig object of the default portal
     */
    public PortalConfig getMetaPortalConfig() {
        return metaPortalConfig;
    }

    /**
     * Set the PortalConfig object of the default portal
     * @param metaPortalConfig PortalConfig object of the default portal
     */
    public void setMetaPortalConfig(PortalConfig metaPortalConfig) {
        this.metaPortalConfig = metaPortalConfig;
    }

    /**
     * Load the PortalConfig object of the default portal
     */
    private void loadMetaPortalConfig() {
        String metaPortal = this.getMetaPortal();
        if(metaPortal != null) {
            try {
                RequestLifeCycle.begin(ExoContainerContext.getCurrentContainer());
                metaPortalConfig = getDataStorage().getPortalConfig(metaPortal);
            } catch (Exception e) {
                log.error("Cannot retrieve data of portal " + metaPortal, e);
            } finally {
                RequestLifeCycle.end();
            }
        }
    }

    public void reloadConfig(String ownerType, String predefinedOwner, String location, String importMode, boolean overrideMode) {
      newPortalConfigListener_.reloadConfig(ownerType, predefinedOwner, location, importMode, overrideMode);
    }
    
    private String getDefaultUri(UserNode node, String site) {
      String uri = "/portal/" + site + "/";
      Page userNodePage = layoutService.getPage(node.getPageRef());
      if (PageType.LINK.equals(PageType.valueOf(userNodePage.getType()))) {
        uri = userNodePage.getLink();
      } else {
        uri = uri + node.getURI();
      }
      return uri;
    }

    private Identity getCurrentIdentity() {
      ConversationState conversationState = ConversationState.getCurrent();
      return conversationState == null ? null : conversationState.getIdentity();
    }

}
