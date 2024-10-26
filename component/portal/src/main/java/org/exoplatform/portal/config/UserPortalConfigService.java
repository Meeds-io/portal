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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import io.meeds.common.ContainerTransactional;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;

/**
 * Created by The eXo Platform SAS Apr 19, 2007 This service is used to load the
 * PortalConfig, Page config and Navigation config for a given user.
 */
public class UserPortalConfigService implements Startable {

  private static final Log        LOG                            = ExoLogger.getLogger("Portal:UserPortalConfigService");

  private static final String     PUBLIC_SITE_NAME               = "public";

  private static final Scope      HOME_PAGE_URI_PREFERENCE_SCOPE = Scope.PORTAL.id("HOME");

  private static final String     HOME_PAGE_URI_PREFERENCE_KEY   = "HOME_PAGE_URI";

  public static final String      DEFAULT_GLOBAL_PORTAL          = "global";

  public static final String      DEFAULT_GROUP_SITE_TEMPLATE    = "group";

  public static final String      DEFAULT_USER_SITE_TEMPLATE     = "user";

  private LayoutService           layoutService;

  private UserACL                 userAcl;

  private SettingService          settingService;

  private NavigationService       navigationService;

  private NewPortalConfigListener newPortalConfigListener;

  @Getter
  @Setter
  private String                  globalPortal;

  private final ImportMode        defaultImportMode;

  private PortalConfig            metaPortalConfig;

  protected final SiteFilter      siteFilter                     = new SiteFilter(SiteType.PORTAL,
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
                                 UserACL userAcl,
                                 InitParams params) {
    this.layoutService = layoutService;
    this.settingService = settingService;
    this.userAcl = userAcl;
    this.navigationService = navigationService;
    this.defaultImportMode =
                           ImportMode.valueOf(getParam(params, "default.import.mode", ImportMode.CONSERVE.name()).toUpperCase());
    this.globalPortal = getParam(params, "global.portal", DEFAULT_GLOBAL_PORTAL);
    this.siteFilter.setExcludedSiteName(globalPortal);
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
    PortalConfig portal = layoutService.getPortalConfig(portalName);
    if (portal == null || !userAcl.hasAccessPermission(portal, userAcl.getUserIdentity(accessUser))) {
      return null;
    }
    return new UserPortalConfig(portal, this, portalName, accessUser, locale);
  }

  public void createSiteFromTemplate(SiteKey siteKey,
                                     SiteKey siteTemplateKey,
                                     String permission) throws ObjectNotFoundException {
    layoutService.savePortalFromTemplate(siteKey, siteTemplateKey, permission);
    layoutService.savePagesFromTemplate(siteKey, siteTemplateKey, permission);
    navigationService.saveNavigationFromTemplate(siteKey, siteTemplateKey);
  }

  /**
   * This method should create a the portal config, pages and navigation
   * according to the template name.
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
      String templatePath = newPortalConfigListener.getTemplateConfig(siteType, template);
      portalConfig = new NewPortalConfig(templatePath);
      portalConfig.setTemplateName(template);
    }

    portalConfig.setOwnerType(siteType);
    newPortalConfigListener.createPortalConfig(portalConfig, siteName);
    newPortalConfigListener.createPage(portalConfig, siteName);
    newPortalConfigListener.createPageNavigation(portalConfig, siteName);
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

    newPortalConfigListener.createPortalConfig(portalConfigPlugin, siteName);
    newPortalConfigListener.createPage(portalConfigPlugin, siteName);
    newPortalConfigListener.createPageNavigation(portalConfigPlugin, siteName);
  }

  /**
   * This method removes the PortalConfig, Page and PageNavigation that belong
   * to the portal in the database.
   *
   * @param portalName the portal name
   */
  public void removeUserPortalConfig(String portalName) {
    removeUserPortalConfig(PortalConfig.PORTAL_TYPE, portalName);
  }

  /**
   * This method removes the PortalConfig, Page and PageNavigation that belong
   * to the portal in the database.
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

  public List<PortalConfig> getUserPortalSites(String username) {
    Identity userAclIdentity = userAcl.getUserIdentity(username);
    List<PortalConfig> list = layoutService.getSites(siteFilter);
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

  public String computePortalPath(HttpServletRequest request) {
    List<PortalConfig> portalConfigList = getUserPortalSites(request.getRemoteUser());
    if (CollectionUtils.isEmpty(portalConfigList)) {
      return null;
    }
    return portalConfigList.stream()
                           .filter(PortalConfig::isDefaultSite)
                           .map(portalConfig -> computePortalSitePath(portalConfig.getName(), request))
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

  public UserNode getPortalSiteRootNode(String siteName, String siteType, HttpServletRequest request) {
    UserPortalConfig userPortalConfig = null;
    if (StringUtils.equalsIgnoreCase(siteType, PortalConfig.PORTAL_TYPE)) {
      userPortalConfig = getUserPortalConfig(siteName, request.getRemoteUser());
    } else {
      PortalConfig defaultPortalConfig = getUserPortalSites(request.getRemoteUser()).stream().findFirst().orElse(null);
      if (defaultPortalConfig != null) {
        userPortalConfig = getUserPortalConfig(defaultPortalConfig.getName(), request.getRemoteUser());
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
    return userNodes.stream()
                    .map(node -> {
                      if (node.getPageRef() != null && layoutService.getPage(node.getPageRef()) != null) {
                        return node;
                      } else if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                        return getFirstAllowedPageNode(node.getChildren());
                      }
                      return null;
                    })
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
  }

  public String getFirstAllowedPageNode(String portalName, String portalType, String nodePath, HttpServletRequest request) {
    UserNode targetUserNode = getPortalSiteRootNode(portalName, portalType, request);
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
   * Returns the default portal template to be used when creating a site
   *
   * @return the default portal template name
   */
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
    }
    return page;
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

  public void reloadConfig(String ownerType, String predefinedOwner, String location, String importMode, boolean overrideMode) {
    newPortalConfigListener.reloadConfig(ownerType, predefinedOwner, location, importMode, overrideMode);
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

}
