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
package org.exoplatform.portal.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.portal.mop.PageType;
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeContext;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig.Builder;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.mop.user.Utils;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import io.meeds.common.ContainerTransactional;
import io.meeds.portal.navigation.constant.SidebarItemType;
import io.meeds.portal.navigation.model.SidebarConfiguration;
import io.meeds.portal.navigation.model.SidebarItem;
import io.meeds.portal.navigation.service.NavigationConfigurationService;

import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;

/**
 * Created by The eXo Platform SAS Apr 19, 2007 This service is used to load the
 * PortalConfig, Page config and Navigation config for a given user.
 */
public class UserPortalConfigService implements Startable {

  public static final String             SITE_TEMPLATE_INSTANTIATED     = "site.template.instantiated";

  public static final String             PUBLIC_SITE_NAME               = "public";

  public static final Scope              HOME_PAGE_URI_PREFERENCE_SCOPE = Scope.PORTAL.id("HOME");

  public static final String             HOME_PAGE_URI_PREFERENCE_KEY   = "HOME_PAGE_URI";

  public static final String             DEFAULT_GLOBAL_PORTAL          = "global";

  public static final String             DEFAULT_GROUP_SITE_TEMPLATE    = "group";

  public static final String             DEFAULT_USER_SITE_TEMPLATE     = "user";

  public static final String             SITE_NAME_PROP_NAME            = "siteName";

  public static final String             SITE_TYPE_PROP_NAME            = "siteType";

  private static final Log               LOG                            = ExoLogger.getLogger("Portal:UserPortalConfigService");

  private LayoutService                  layoutService;

  private UserACL                        userAcl;

  private SettingService                 settingService;

  private ListenerService                listenerService;

  private NavigationService              navigationService;

  private NavigationConfigurationService navigationConfigurationService;

  private NewPortalConfigListener        newPortalConfigListener;

  @Getter
  @Setter
  private String                         globalPortal;

  private final ImportMode               defaultImportMode;

  private PortalConfig                   metaPortalConfig;

  protected final SiteFilter             portalSiteFilter               = new SiteFilter(SiteType.PORTAL,
                                                                                         null,
                                                                                         null,
                                                                                         true,
                                                                                         true,
                                                                                         false,
                                                                                         false,
                                                                                         0,
                                                                                         0);

  public UserPortalConfigService(LayoutService layoutService,
                                 NavigationService navigationService,
                                 SettingService settingService,
                                 ListenerService listenerService,
                                 UserACL userAcl,
                                 InitParams params) {
    this.layoutService = layoutService;
    this.settingService = settingService;
    this.listenerService = listenerService;
    this.userAcl = userAcl;
    this.navigationService = navigationService;
    this.defaultImportMode =
                           ImportMode.valueOf(getParam(params, "default.import.mode", ImportMode.CONSERVE.name()).toUpperCase());
    this.globalPortal = getParam(params, "global.portal", DEFAULT_GLOBAL_PORTAL);
    this.portalSiteFilter.setExcludedSiteName(globalPortal);
  }

  public boolean canRestore(String type, String name) {
    if (newPortalConfigListener == null) {
      return false;
    }
    return newPortalConfigListener.getPortalConfig(type, name) != null;
  }

  public boolean restoreSite(String type,
                             String name,
                             ImportMode importMode,
                             boolean restoreSiteLayout,
                             boolean restorePages,
                             boolean restoreNavigationTree) {
    if (!canRestore(type, name)) {
      return false;
    }
    List<NewPortalConfig> configs = newPortalConfigListener.getPortalConfigs(type, name);
    for(NewPortalConfig config : configs) {
      config = config.clone();
      config.setImportMode(importMode.name());
      config.setOverrideMode(true);
      HashSet<String> ownerName = new HashSet<>();
      ownerName.add(name);
      config.setPredefinedOwner(ownerName);
      if (restoreSiteLayout) {
        PortalConfig previousSite = layoutService.getPortalConfig(type, name);
        newPortalConfigListener.initPortalConfigDB(config, true);
        PortalConfig site = layoutService.getPortalConfig(type, name);
        if (previousSite != null) {
          // Preserve previous version or properties
          site.setLabel(previousSite.getLabel());
          site.setDescription(previousSite.getDescription());
          site.setAccessPermissions(previousSite.getAccessPermissions());
          site.setEditPermission(previousSite.getEditPermission());
          site.setDisplayed(previousSite.isDisplayed());
          site.setDisplayOrder(previousSite.getDisplayOrder());
          site.setIcon(previousSite.getIcon());
          site.setProperties(previousSite.getProperties());
          layoutService.save(site);
        }
      }
      if (restorePages) {
        newPortalConfigListener.initPageDB(config, true);
      }
      if (restoreNavigationTree) {
        newPortalConfigListener.initPageNavigationDB(config, true);
      }
    }
    return true;
  }

  public LayoutService getDataStorage() {
    return layoutService;
  }

  public ImportMode getDefaultImportMode() {
    return defaultImportMode;
  }

  /**
   * Returns the navigation service associated with this service.
   *
   * @return the navigation service;
   */
  public NavigationService getNavigationService() {
    return navigationService;
  }

  public UserACL getUserACL() {
    return userAcl;
  }

  /**
   * <p>
   * Build and returns an instance of <code>UserPortalConfig</code>.
   * </p>
   * <br>
   * <p>
   * To return a valid config, the current thread must be associated with an
   * identity that will grant him access to the portal as returned by the
   * {@link UserACL#hasAccessPermission(org.exoplatform.portal.config.model.PortalConfig, Identity)}
   * method.
   * </p>
   * <br>
   * The navigation loaded on the <code>UserPortalConfig</code> object are
   * obtained according to the specified user argument. The portal navigation is
   * always loaded. If the specified user is null then the navigation of the
   * guest group as configured by UserACL#getGuestsGroup is also loaded,
   * otherwise the navigations are loaded according to the following rules: <br>
   * <ul>
   * <li>The navigation corresponding to the user is loaded.</li>
   * <li>When the user is root according to the value returned by
   * UserACL#getSuperUser then the navigation of all groups are loaded.</li>
   * <li>When the user is not root, then all its groups are added except the
   * guest group as configued per UserACL#getGuestsGroup.</li>
   * </ul>
   * <br>
   * All the navigations are sorted using the value returned by
   * {@link org.exoplatform.portal.config.model.PageNavigation#getPriority()}.
   *
   * @param portalName the portal name
   * @param username the user name
   * @return the config
   */
  public UserPortalConfig getUserPortalConfig(String portalName, String username) {
    return getUserPortalConfig(portalName, username, LocaleContextInfoUtils.getUserLocale(username));
  }

  public UserPortalConfig getUserPortalConfig(String portalName, String accessUser, Locale locale) {
    PortalConfig site = layoutService.getPortalConfig(portalName);
    if (site == null
        || !userAcl.hasAccessPermission(site, userAcl.getUserIdentity(accessUser))) {
      return null;
    }
    return new UserPortalConfig(site, portalName, accessUser, locale);
  }

  public void createSiteFromTemplate(SiteKey sourceSiteKey,
                                     SiteKey targetSiteKey) throws ObjectNotFoundException {
    createSiteFromTemplate(sourceSiteKey, targetSiteKey, null);
  }

  public void createSiteFromTemplate(SiteKey sourceSiteKey,
                                     SiteKey targetSiteKey,
                                     String permission) throws ObjectNotFoundException {
    layoutService.savePortalFromTemplate(sourceSiteKey, targetSiteKey, permission);
    layoutService.savePagesFromTemplate(sourceSiteKey, targetSiteKey, permission);
    navigationService.saveNavigationFromTemplate(sourceSiteKey, targetSiteKey);
    listenerService.broadcast(SITE_TEMPLATE_INSTANTIATED, sourceSiteKey, targetSiteKey);
  }

  /**
   * This method should create a the portal config, pages and navigation
   * according to the template name.
   *
   * @param siteType the site type
   * @param siteName the Site name
   * @param template the template to use
   * @return created {@link PortalConfig}
   * @deprecated the Site creation process has changed to use Site Template
   *             Definition as deifned in layout addon instead of creating a
   *             site from a static template from sources. You may replace the
   *             usage of this method by
   *             {@link #createSiteFromTemplate(SiteKey, SiteKey)}
   */
  @Deprecated(forRemoval = true, since = "7.0")
  public PortalConfig createUserPortalConfig(String siteType, String siteName, String template) {
    NewPortalConfig portalConfig = null;
    if (StringUtils.isBlank(template)) {
      portalConfig = new NewPortalConfig();
      portalConfig.setUseMetaPortalLayout(true);
    } else {
      String templatePath = newPortalConfigListener.getTemplateConfig(siteType, template);
      portalConfig = new NewPortalConfig(templatePath);
      portalConfig.setTemplateName(template);
    }

    portalConfig.setOwnerType(siteType);
    newPortalConfigListener.createPortalConfig(portalConfig, siteName);
    newPortalConfigListener.createPage(portalConfig, siteName);
    newPortalConfigListener.createPageNavigation(portalConfig, siteName);
    return layoutService.getPortalConfig(new SiteKey(siteType, siteName));
  }

  /**
   * This method should create a the portal config, pages and navigation
   * according to the template name.
   *
   * @param siteType the site type
   * @param siteName the Site name
   * @param template the template to use
   * @param templatePath the template path to use
   * @deprecated the Site creation process has changed to use Site Template
   *             Definition as deifned in layout addon instead of creating a
   *             site from a static template from sources. You may replace the
   *             usage of this method by
   *             {@link #createSiteFromTemplate(SiteKey, SiteKey)}
   */
  @Deprecated(forRemoval = true, since = "7.0")
  public void createUserPortalConfig(String siteType, String siteName, String template, String templatePath) {
    NewPortalConfig portalConfigPlugin = new NewPortalConfig(templatePath);
    portalConfigPlugin.setTemplateName(template);
    portalConfigPlugin.setOwnerType(siteType);

    newPortalConfigListener.createPortalConfig(portalConfigPlugin, siteName);
    newPortalConfigListener.createPage(portalConfigPlugin, siteName);
    newPortalConfigListener.createPageNavigation(portalConfigPlugin, siteName);
  }

  /**
   * Load metadata of specify page
   *
   * @param pageRef the PageKey
   * @return the PageContext
   * @deprecated shouldn't use ConversationState.getCurrent implicitely in
   *             Service layer, thus use {@link #getPage(PageKey, String)}
   *             instead
   */
  @Deprecated(forRemoval = true, since = "7.0")
  public PageContext getPage(PageKey pageRef) {
    ConversationState conversationState = ConversationState.getCurrent();
    return getPage(pageRef, conversationState == null ? null : conversationState.getIdentity());
  }

  public PageContext getPage(PageKey pageRef, String username) {
    Identity userAclIdentity = userAcl.getUserIdentity(username);
    return getPage(pageRef, userAclIdentity);
  }

  public String getDefaultPath(String username) {
    return getDefaultPath(username, true);
  }

  public String getDefaultPath(String username, boolean useUserHome) {
    String userHomePage = useUserHome && isAllowUserHome() && StringUtils.isNotBlank(username) ? getUserHomePage(username) : null;
    if (StringUtils.isNotBlank(userHomePage)) {
      return userHomePage;
    } else {
      if (getNavigationConfigurationService() != null && StringUtils.isNotBlank(username)) {
        SidebarConfiguration sidebarConfiguration = getNavigationConfigurationService().getSidebarConfiguration(username,
                                                                                                                Locale.ENGLISH);
          String userDefaultPath = sidebarConfiguration.getItems()
                                                       .stream()
                                                       .flatMap(item -> {
                                                         if (CollectionUtils.isNotEmpty(item.getItems())) {
                                                           return Stream.concat(Stream.of(item), item.getItems().stream());
                                                         } else {
                                                           return Stream.of(item);
                                                         }
                                                       })
                                                       .filter(SidebarItem::isDefaultPath)
                                                       .map(SidebarItem::getUrl)
                                                       .filter(StringUtils::isNotBlank)
                                                       .findFirst()
                                                       .orElse(null);
        if (StringUtils.isNotBlank(userDefaultPath)) {
          return userDefaultPath;
        }
      }
      PortalConfig portalConfig = getDefaultSite(username);
      return portalConfig == null ? null : getDefaultSitePath(portalConfig.getName(), username);
    }
  }

  public String getDefaultSitePath(String portalName, String username) {
    UserNode userNode = getDefaultSiteNode(portalName, username);
    return userNode == null ? null : getDefaultUri(userNode, portalName);
  }

  public UserNode getDefaultSiteNode(String portalName, String username) {
    return getDefaultSiteNode(SiteType.PORTAL.getName(), portalName, username);
  }

  public UserNode getDefaultSiteNode(String portalType, String portalName, String username) {
    UserNode targetUserNode = getSiteRootNode(portalType, portalName, username, true);
    if (targetUserNode == null) {
      return null;
    } else {
      do {
        if (isAccessiblePageNoDraft(targetUserNode, username)) {
          return targetUserNode;
        } else if (CollectionUtils.isNotEmpty(targetUserNode.getChildren())) {
          UserNode childUserNode = getNodeOrFirstChildWithPage(targetUserNode.getChildren(), username);
          if (isAccessiblePageNoDraft(childUserNode, username)) {
            return childUserNode;
          }
        }
        targetUserNode = targetUserNode.getParent();
      } while (targetUserNode != null && targetUserNode != targetUserNode.getParent());
      return null;
    }
  }

  public UserNode getSiteRootNode(String siteType, String siteName, String username, boolean withVisibility) {
    UserPortalConfig userPortalConfig = null;
    if (StringUtils.equalsIgnoreCase(siteType, PortalConfig.PORTAL_TYPE)) {
      userPortalConfig = getUserPortalConfig(siteName, username);
    } else {
      PortalConfig defaultPortalConfig = getDefaultSite(username);
      if (defaultPortalConfig != null) {
        userPortalConfig = getUserPortalConfig(defaultPortalConfig.getName(), username);
      }
    }
    if (userPortalConfig == null) {
      return null;
    }
    SiteKey siteKey = new SiteKey(SiteType.valueOf(siteType.toUpperCase()), siteName);

    UserPortal userPortal = userPortalConfig.getUserPortal();
    UserNavigation navigation = userPortal.getNavigation(siteKey);
    NavigationContext navigationContext = getNavigationService().loadNavigation(siteKey);
    if (navigationContext == null) {
      return null;
    }
    Builder builder = UserNodeFilterConfig.builder().withReadCheckNoPage().withTemporalCheck();
    if (withVisibility) {
      builder.withVisibility(Visibility.DISPLAYED, Visibility.TEMPORAL);
    } else {
      builder.withoutVisibility();
    }
    UserNodeContext userNodeContext = new UserNodeContext(navigation, builder.build());
    NodeContext<UserNode> nodeContext = getNavigationService().loadNode(userNodeContext,
                                                                        navigationContext,
                                                                        org.exoplatform.portal.mop.navigation.Scope.ALL,
                                                                        null);
    if (nodeContext != null) {
      return nodeContext.getNode().filter();
    } else {
      return null;
    }
  }

  public PortalConfig getDefaultSite(String username) {
    if (StringUtils.isBlank(username)) {
      PortalConfig publicSitePortalConfig = layoutService.getPortalConfig(SiteKey.portal(PUBLIC_SITE_NAME));
      if (publicSitePortalConfig == null || !userAcl.hasAccessPermission(publicSitePortalConfig, null)) {
        return null;
      } else {
        return publicSitePortalConfig;
      }
    } else {
      List<PortalConfig> portalConfigList = getAccessiblePortalSites(username);
      if (CollectionUtils.isEmpty(portalConfigList)) {
        return null;
      } else if (getNavigationConfigurationService() == null) {
        return portalConfigList.stream()
                               .filter(PortalConfig::isDefaultSite)
                               .filter(p -> PortalConfig.PORTAL_TYPE.equalsIgnoreCase(p.getType()))
                               .filter(p -> p.isDisplayed() || StringUtils.equals(p.getName(), getMetaPortal()))
                               .filter(p -> navigationService.loadNode(new SiteKey(p.getType(), p.getName())) != null)
                               .filter(Objects::nonNull)
                               .findFirst()
                               .orElse(null);
      } else {
        SidebarConfiguration sidebarConfiguration = getNavigationConfigurationService().getSidebarConfiguration(username,
                                                                                                                Locale.ENGLISH);
        SiteKey defaultSiteKey = sidebarConfiguration.getItems()
                                                     .stream()
                                                     .filter(s -> SidebarItemType.SITE == s.getType())
                                                     .filter(SidebarItem::isDefaultPath)
                                                     .map(this::getSiteKey)
                                                     .findFirst()
                                                     .orElseGet(() -> SiteKey.portal(getMetaPortal()));
        return layoutService.getPortalConfig(defaultSiteKey);
      }
    }
  }

  private SiteKey getSiteKey(SidebarItem item) {
    return new SiteKey(item.getProperties().get(SITE_TYPE_PROP_NAME),
                       item.getProperties().get(SITE_NAME_PROP_NAME));
  }

  public UserNode getSiteNodeOrGlobalNode(String portalType, // NOSONAR
                                          String portalName,
                                          String nodePath,
                                          String username) {
    if (StringUtils.isBlank(nodePath)) {
      return getDefaultSiteNode(portalType, portalName, username);
    } else {
      UserNode rootUserNode = getSiteRootNode(portalType,
                                              portalName,
                                              username,
                                              false);
      if (rootUserNode == null) {
        return null;
      } else {
        UserNode exactUserNode = getExactUserNode(rootUserNode, nodePath, username);
        boolean isDefaultPath = exactUserNode == null
                                || exactUserNode.getParent() == null
                                || exactUserNode.getParent().getId().equals(rootUserNode.getId());
        if (isDefaultPath) {
          String[] nodePathParts = nodePath.split("/");
          return Arrays.asList(nodePathParts)
                       .reversed()
                       .stream()
                       .map(nodeName -> getChildNodeWithName(rootUserNode,
                                                             nodeName,
                                                             username))
                       .filter(Objects::nonNull)
                       .findFirst()
                       .orElse(exactUserNode);
        }
        return exactUserNode;
      }
    }
  }

  @Synchronized
  public void initListener(ComponentPlugin listener) {
    if (listener instanceof NewPortalConfigListener portalConfigListener) {
      if (newPortalConfigListener == null) {
        this.newPortalConfigListener = portalConfigListener;
      } else {
        newPortalConfigListener.mergePlugin(portalConfigListener);
      }
    }
  }

  @Override
  @ContainerTransactional
  public void start() {
    try {
      if (newPortalConfigListener == null) {
        return;
      }

      //
      newPortalConfigListener.run();
    } catch (Exception e) {
      LOG.error("Could not import initial data", e);
    }

    loadMetaPortalConfig();
  }

  public String getMetaPortal() {
    return newPortalConfigListener.getMetaPortal();
  }

  /**
   * @param username user name
   * @return User home page uri preference
   */
  public String getUserHomePage(String username) {
    if (StringUtils.isBlank(username) || !isAllowUserHome()) {
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
   * Sets the user default Path
   * 
   * @param username user name
   * @param path Page path
   */
  public void setUserHomePage(String username, String path) {
    if (StringUtils.isBlank(path)) {
      settingService.remove(Context.USER.id(username),
                            HOME_PAGE_URI_PREFERENCE_SCOPE,
                            HOME_PAGE_URI_PREFERENCE_KEY);
    } else {
      settingService.set(Context.USER.id(username),
                         HOME_PAGE_URI_PREFERENCE_SCOPE,
                         HOME_PAGE_URI_PREFERENCE_KEY,
                         SettingValue.create(path));
    }
  }

  /**
   * Returns the default portal template to be used when creating a site
   *
   * @return the default portal template name
   * @deprecated Site Templates has been changed to be stored in database to
   *             make it dynamically managed by UI rather than statis pages and
   *             navigation from source files
   */
  @Deprecated(forRemoval = true, since = "7.0")
  public String getDefaultPortalTemplate() {
    return newPortalConfigListener.getDefaultPortalTemplate();
  }

  public Set<String> getPortalTemplates() {
    return newPortalConfigListener.getTemplateConfigs(PortalConfig.PORTAL_TYPE);
  }

  public <T> T getConfig(String portalType,
                         String portalName,
                         Class<T> objectType,
                         String parentLocation) {
    return newPortalConfigListener.getConfig(portalType, portalName, objectType, parentLocation);
  }

  /**
   * Get the skin name of the default portal
   * 
   * @return Skin name of the default portal
   */
  public String getMetaPortalSkinName() {
    return metaPortalConfig != null && StringUtils.isNotBlank(metaPortalConfig.getSkin()) ?
                                                                                          metaPortalConfig.getSkin() :
                                                                                          null;
  }

  /**
   * Get the PortalConfig object of the default portal
   * 
   * @return PortalConfig object of the default portal
   */
  public PortalConfig getMetaPortalConfig() {
    return metaPortalConfig;
  }

  /**
   * Set the PortalConfig object of the default portal
   * 
   * @param metaPortalConfig PortalConfig object of the default portal
   */
  public void setMetaPortalConfig(PortalConfig metaPortalConfig) {
    this.metaPortalConfig = metaPortalConfig;
  }

  private PageContext getPage(PageKey pageRef, Identity userAclIdentity) {
    if (pageRef == null) {
      return null;
    }

    PageContext page = layoutService.getPageContext(pageRef);
    if (page == null || !userAcl.hasAccessPermission(page, userAclIdentity)) {
      return null;
    } else {
      return page;
    }
  }

  /**
   * Load the PortalConfig object of the default portal
   */
  private void loadMetaPortalConfig() {
    String metaPortal = this.getMetaPortal();
    if (metaPortal != null) {
      try {
        RequestLifeCycle.begin(ExoContainerContext.getCurrentContainer());
        metaPortalConfig = getDataStorage().getPortalConfig(metaPortal);
      } catch (Exception e) {
        LOG.error("Cannot retrieve data of portal " + metaPortal, e);
      } finally {
        RequestLifeCycle.end();
      }
    }
  }

  private UserNode getExactUserNode(UserNode rootUserNode, String nodePath, String username) { // NOSONAR
    UserNode globalRootUserNode = getSiteRootNode(PortalConfig.PORTAL_TYPE,
                                                  globalPortal,
                                                  username,
                                                  false);
    UserNode siteUserNode = rootUserNode;
    UserNode globalUserNode = globalRootUserNode;
    int validGlobalDepth = 0;
    int validSiteDepth = 0;
    int currentDepth = 0;

    String[] nodeNames = Utils.parsePath(nodePath);
    Iterator<String> iterator = Arrays.stream(nodeNames).iterator();
    while (iterator.hasNext()
           && (validSiteDepth == currentDepth
               || validGlobalDepth == currentDepth)) {
      String path = iterator.next();
      if (validSiteDepth == currentDepth) {
        UserNode childUserNode = siteUserNode.getChild(path);
        if (childUserNode != null) {
          siteUserNode = childUserNode;
          validSiteDepth++;
        }
      }
      if (globalUserNode != null && validGlobalDepth == currentDepth) {
        UserNode childGlobalUserNode = globalUserNode.getChild(path);
        if (childGlobalUserNode != null) {
          globalUserNode = childGlobalUserNode;
          validGlobalDepth++;
        }
      }
      currentDepth++;
    }
    if (globalUserNode != null) {
      if (validSiteDepth >= validGlobalDepth) {
        UserNode result = getNodeOrFirstChildWithPage(siteUserNode, username);
        if (result == null) {
          result = getNodeOrFirstChildWithPage(rootUserNode, username);
        }
        return result;
      } else {
        if (globalUserNode.getPageRef() == null
            || getPage(globalUserNode.getPageRef(), username) == null) {
          return null;
        } else {
          return globalUserNode;
        }
      }
    } else {
      return getNodeOrFirstChildWithPage(siteUserNode, username);
    }
  }

  private UserNode getChildNodeWithName(UserNode userNode, String nodeName, String username) {
    if (StringUtils.equals(userNode.getName(), nodeName)) {
      return userNode;
    } else if (userNode.getChildrenCount() > 0) {
      return userNode.getChildren()
                     .stream()
                     .map(n -> getChildNodeWithName(n, nodeName, username))
                     .filter(Objects::nonNull)
                     .findFirst()
                     .orElse(null);
    } else {
      return null;
    }
  }

  private UserNode getNodeOrFirstChildWithPage(UserNode userNode, String username) {
    return getNodeOrFirstChildWithPage(Collections.singleton(userNode), username);
  }

  private UserNode getNodeOrFirstChildWithPage(Collection<UserNode> userNodes, String username) {
    return userNodes.stream()
                    .map(node -> {
                      if (isAccessiblePage(node, username)) {
                        return node;
                      } else if (CollectionUtils.isNotEmpty(node.getChildren())) {
                        return getNodeOrFirstChildWithPage(node.getChildren(), username);
                      }
                      return null;
                    })
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
  }

  private List<PortalConfig> getAccessiblePortalSites(String username) {
    Identity userAclIdentity = userAcl.getUserIdentity(username);
    List<PortalConfig> list = layoutService.getSites(portalSiteFilter);
    return list.stream()
               .filter(Objects::nonNull)
               .filter(portalConfig -> userAcl.hasAccessPermission(portalConfig, userAclIdentity))
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

  private boolean isAccessiblePageNoDraft(UserNode userNode, String username) {
    return isAccessiblePage(userNode, username) && userNode.getVisibility() != Visibility.DRAFT;
  }

  private boolean isAccessiblePage(UserNode userNode, String username) {
    return userNode != null
           && userNode.getPageRef() != null
           && getPage(userNode.getPageRef(), username) != null;
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

  private String getParam(InitParams params, String name, String defaultValue) {
    ValueParam valueParam = params == null ? null : params.getValueParam(name);
    return valueParam == null ? defaultValue : valueParam.getValue();
  }

  private boolean isAllowUserHome() {
    return getNavigationConfigurationService() == null || getNavigationConfigurationService().isAllowUserHome();
  }

  private NavigationConfigurationService getNavigationConfigurationService() {
    if (navigationConfigurationService == null) {
      navigationConfigurationService = ExoContainerContext.getService(NavigationConfigurationService.class);
    }
    return navigationConfigurationService;
  }

}
