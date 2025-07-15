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
package org.exoplatform.portal.mop.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.ExpressionUtil;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationServiceException;
import org.exoplatform.portal.mop.navigation.NodeChangeListener;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.ResourceBundleManager;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;

import lombok.Getter;
import lombok.SneakyThrows;

public class UserPortalImpl implements UserPortal {

  public static final String               SPACES_SITE_TYPE_PREFIX  = "/spaces/";

  protected static UserPortalConfigService portalConfigService;

  protected static LayoutService           layoutService;

  protected static ResourceBundleManager   resourceBundleManager;

  protected static OrganizationService     organizationService;

  private final UserNavigationComparator userNavigationComparator = new UserNavigationComparator();

  /** . */
  private final PortalConfig             portalConfig;

  /** . */
  @Getter
  private final String                   userName;

  /** . */
  private List<UserNavigation>           navigations;

  private boolean                        refreshList;

  /** . */
  private final String                   portalName;

  /** . */
  @Getter
  private final Locale                   userLocale;

  public UserPortalImpl(String portalName,
                        PortalConfig portal,
                        String userName,
                        Locale locale) {
    this.portalName = portalName;
    this.portalConfig = portal;
    this.userName = userName;
    this.navigations = null;
    this.userLocale = locale;
    if (portalConfigService == null) {
      portalConfigService = ExoContainerContext.getService(UserPortalConfigService.class); // NOSONAR
      layoutService = ExoContainerContext.getService(LayoutService.class); // NOSONAR
      resourceBundleManager = ExoContainerContext.getService(ResourceBundleManager.class); // NOSONAR
      organizationService = ExoContainerContext.getService(OrganizationService.class); // NOSONAR
    }
    this.userNavigationComparator.setGlobalPortal(portalConfigService.getGlobalPortal());
  }

  @Override
  public Locale getLocale() {
    return userLocale;
  }

  /**
   * Returns an immutable sorted list of the valid navigations related to the
   * user.
   *
   * @return the navigations
   */
  @Override
  public List<UserNavigation> getNavigations() {
    if (navigations == null || this.refreshList) {
      // Add designated site navigation
      loadUserNavigation(new SiteKey(SiteType.PORTAL, portalName));

      // Add group navigations
      if (StringUtils.isNotBlank(userName)) {
        Identity userIdentity = portalConfigService.getUserACL().getUserIdentity(userName);
        List<String> userGroupIds = getUserGroupIds(userIdentity);
        if (CollectionUtils.isNotEmpty(userGroupIds)) {
          userGroupIds.forEach(groupId -> loadUserNavigation(SiteKey.group(groupId)));
        }
        if (this.refreshList) {
          this.refreshList = false;
          // Delete navigations where user doesn't belong anymore
          this.navigations.removeIf(nav -> {
            PortalConfig site = layoutService.getPortalConfig(nav.getKey());
            return site == null || !portalConfigService.getUserACL().hasAccessPermission(site, userIdentity);
          });
        }
      }
    }
    return Collections.unmodifiableList(this.navigations);
  }

  @Override
  public Collection<UserNode> getNodes(SiteType siteType, Scope scope, UserNodeFilterConfig filterConfig) {
    return getNodes(siteType, scope, filterConfig, true);
  }

  @Override
  public Collection<UserNode> getNodes(SiteType siteType, Scope scope, UserNodeFilterConfig filterConfig, boolean includeGlobal) {

    Collection<UserNode> resultUserNodes = new ArrayList<>();
    Set<String> addedUserNodesURI = new HashSet<>();
    for (UserNavigation userNavigation : getNavigations()) {
      SiteKey siteKey = userNavigation.getKey();
      if (siteKey.getType() != siteType
          || (siteType == SiteType.GROUP && siteKey.getName().startsWith(SPACES_SITE_TYPE_PREFIX))
          || (siteType == SiteType.SPACE && !siteKey.getName().startsWith(SPACES_SITE_TYPE_PREFIX))
          || (!includeGlobal && siteKey.getName().equalsIgnoreCase(portalConfigService.getGlobalPortal()))) {
        continue;
      }

      UserNode rootNode = getNode(userNavigation, scope, filterConfig, null);
      Collection<UserNode> userNodes = rootNode.getChildren();
      for (UserNode userNode : userNodes) {
        if (addedUserNodesURI.contains(userNode.getURI())) {
          continue;
        }
        addedUserNodesURI.add(userNode.getURI());
        resultUserNodes.add(userNode);
      }
    }
    return resultUserNodes;
  }

  @Override
  public UserNavigation getNavigation(SiteKey key) {
    if (key == null) {
      throw new IllegalArgumentException("SiteKey is mandatory");
    }
    return filterUserNavigation(key);
  }

  @Override
  public void refresh() {
    refreshList = true;
  }

  @Override
  public UserNode getNode(UserNavigation userNavigation,
                          Scope scope,
                          UserNodeFilterConfig filterConfig,
                          NodeChangeListener<UserNode> listener) throws NullPointerException,
                                                                 UserPortalException,
                                                                 NavigationServiceException {
    if (userNavigation == null || userNavigation.navigation == null) {
      return null;
    }
    UserNodeContext userNodeContext = new UserNodeContext(userNavigation, filterConfig);
    NodeContext<UserNode> nodeContext = portalConfigService.getNavigationService()
                                                           .loadNode(userNodeContext,
                                                                     userNavigation.navigation,
                                                                     scope,
                                                                     new UserNodeListener(listener));
    if (nodeContext != null) {
      return nodeContext.getNode().filter();
    } else {
      return null;
    }
  }

  @Override
  public UserNode getNodeById(String userNodeId,
                              SiteKey siteKey,
                              Scope scope,
                              UserNodeFilterConfig filterConfig,
                              NodeChangeListener<UserNode> listener) {
    UserNavigation userNavigation = getNavigation(siteKey);
    UserNodeContext userNodeContext = new UserNodeContext(userNavigation, filterConfig);
    NodeContext<UserNode> nodeContext = portalConfigService.getNavigationService()
                                                           .loadNodeById(userNodeContext,
                                                                         userNodeId,
                                                                         scope,
                                                                         new UserNodeListener(listener));
    if (nodeContext != null) {
      return nodeContext.getNode().filter();
    } else {
      return null;
    }
  }

  @Override
  public void updateNode(UserNode node, Scope scope, NodeChangeListener<UserNode> listener) {
    if (node == null) {
      throw new IllegalArgumentException("UserNode is mandatory");
    }
    portalConfigService.getNavigationService().updateNode(node.context, scope, new UserNodeListener(listener));
    node.filter();
  }

  @Override
  public void rebaseNode(UserNode node, Scope scope, NodeChangeListener<UserNode> listener) {
    if (node == null) {
      throw new IllegalArgumentException("No null node accepted");
    }
    portalConfigService.getNavigationService().rebaseNode(node.context, scope, new UserNodeListener(listener));
    node.filter();
  }

  @Override
  public void saveNode(UserNode node, NodeChangeListener<UserNode> listener) throws NullPointerException,
                                                                             UserPortalException,
                                                                             NavigationServiceException {
    if (node == null) {
      throw new IllegalArgumentException("No null node accepted");
    }
    portalConfigService.getNavigationService().saveNode(node.context, new UserNodeListener(listener));
    navigations = null;
    node.filter();
  }

  @Override
  @SneakyThrows
  public String getPortalLabel(SiteKey siteKey) {
    return getPortalLabel(siteKey, userLocale);
  }

  @Override
  @SneakyThrows
  public String getPortalLabel(SiteKey siteKey, Locale locale) {
    PortalConfig site = portalConfigService.getDataStorage().getPortalConfig(siteKey);
    String label = site == null ?
                                siteKey.getName() :
                                StringUtils.firstNonBlank(site.getLabel(),
                                                          site.getName(),
                                                          siteKey.getName());
    if (siteKey.getType() == SiteType.PORTAL) {
      return StringUtils.firstNonBlank(getLabel(siteKey, label, locale),
                                       siteKey.getName());
    } else if (siteKey.getType() == SiteType.GROUP) {
      Group siteGroup = organizationService.getGroupHandler()
                                           .findGroupById(siteKey.getName());
      if (siteGroup != null) {
        return siteGroup.getLabel();
      }
    }
    return label;
  }

  @Override
  @SneakyThrows
  public String getPortalDescription(SiteKey siteKey) {
    return getPortalDescription(siteKey, userLocale);
  }

  @Override
  @SneakyThrows
  public String getPortalDescription(SiteKey siteKey, Locale locale) {
    PortalConfig site = portalConfigService.getDataStorage().getPortalConfig(siteKey);
    String description = site == null ? null : site.getDescription();
    if (siteKey.getType() == SiteType.PORTAL && description != null) {
      return getLabel(siteKey, description, locale);
    } else if (siteKey.getType() == SiteType.GROUP) {
      Group siteGroup = organizationService.getGroupHandler()
                                           .findGroupById(siteKey.getName());
      if (siteGroup != null) {
        return siteGroup.getLabel();
      }
    }
    return description;
  }

  public PortalConfig getPortalConfig() {
    return portalConfig;
  }

  protected String getLabel(SiteKey siteKey, String label, Locale locale) {
    if (ExpressionUtil.isResourceBindingExpression(label)) {
      return Stream.of(locale, ResourceBundleService.DEFAULT_CROWDIN_LOCALE)
                   .map(l -> getBundle(siteKey.getTypeName(), siteKey.getName(), locale))
                   .filter(Objects::nonNull)
                   .map(b -> ExpressionUtil.getExpressionValue(b, label))
                   .filter(StringUtils::isNotBlank)
                   .findFirst()
                   .orElse(null);
    } else {
      return label;
    }
  }

  protected UserNavigation filterUserNavigation(SiteKey key) {
    UserNavigation userNavigation = this.navigations == null ? null :
                                                             this.navigations.stream()
                                                                             .filter(nav -> nav.getKey().equals(key))
                                                                             .findFirst()
                                                                             .orElse(null);
    if (userNavigation == null) {
      this.refreshList = true;
      return loadUserNavigation(key);
    } else {
      return userNavigation;
    }
  }

  protected UserNavigation loadUserNavigation(SiteKey siteKey) {
    if (this.navigations == null) {
      this.navigations = new ArrayList<>();
      if (siteKey.getType() == SiteType.PORTAL
          && StringUtils.isNotBlank(portalConfigService.getGlobalPortal())
          && !StringUtils.equals(portalConfigService.getGlobalPortal(), siteKey.getName())) {
        // Add global navigation at the end
        loadUserNavigation(new SiteKey(SiteType.PORTAL, portalConfigService.getGlobalPortal()));
      }
    } else {
      this.navigations.removeIf(nav -> nav.getKey().equals(siteKey));
    }
    NavigationContext navigationContext = portalConfigService.getNavigationService()
                                                             .loadNavigation(siteKey);
    PortalConfig sitePortalConfig = portalConfigService.getDataStorage().getPortalConfig(siteKey);
    UserACL userAcl = portalConfigService.getUserACL();
    if (navigationContext != null
        && navigationContext.getState() != null
        && userAcl.hasAccessPermission(sitePortalConfig, userAcl.getUserIdentity(userName))) {
      UserNavigation userNavigation = new UserNavigation(this,
                                                         navigationContext,
                                                         userAcl.hasEditPermission(sitePortalConfig,
                                                                                   userAcl.getUserIdentity(userName)));
      this.navigations.add(userNavigation);
      Collections.sort(this.navigations, userNavigationComparator);
      return userNavigation;
    } else {
      return null;
    }
  }

  @SneakyThrows
  protected List<String> getUserGroupIds(Identity identity) {
    Collection<?> groups = null;
    if (StringUtils.isBlank(userName)
        || IdentityConstants.ANONIM.equals(userName)
        || IdentityConstants.SYSTEM.equals(userName)
        || identity == null) {
      return Collections.emptyList();
    } else {
      groups = identity.getGroups();
    }
    return getUserGroupIds(groups);
  }

  private List<String> getUserGroupIds(Collection<?> groups) {
    String guestsGroupId = portalConfigService.getUserACL().getGuestsGroup();
    return groups.stream()
                 .map(groupObj -> {
                   if (groupObj instanceof Group group) {
                     return group.getId().trim();
                   } else {
                     return groupObj.toString().trim();
                   }
                 })
                 .filter(groupId -> !StringUtils.equals(groupId, guestsGroupId))
                 .toList();
  }

  public ResourceBundle getBundle(UserNavigation navigation) {
    return getBundle(getSiteType(navigation), getSiteName(navigation), userLocale);
  }

  private ResourceBundle getBundle(String siteType, String siteName, Locale locale) {
    return resourceBundleManager.getNavigationResourceBundle(LocaleContextInfo.getLocaleAsString(locale),
                                                             siteType,
                                                             siteName);
  }

  private String getSiteName(UserNavigation navigation) {
    return navigation.getKey()
                     .getName();
  }

  private String getSiteType(UserNavigation navigation) {
    return navigation.getKey()
                     .getTypeName();
  }

}
