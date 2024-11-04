/*
 * Copyright (C) 2010 eXo Platform SAS.
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.ExpressionUtil;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.GenericScope;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationServiceException;
import org.exoplatform.portal.mop.navigation.NodeChangeListener;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.navigation.VisitMode;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.ResourceBundleManager;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;

import lombok.Getter;
import lombok.SneakyThrows;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class UserPortalImpl implements UserPortal {

  private static final String            SPACES_SITE_TYPE_PREFIX  = "/spaces/";

  private final UserNavigationComparator userNavigationComparator = new UserNavigationComparator();

  /** . */
  final UserPortalConfigService          service;

  private ResourceBundleManager          resourceBundleManager;

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

  public UserPortalImpl(UserPortalConfigService service,
                        String portalName,
                        PortalConfig portal,
                        String userName,
                        Locale locale) {
    this.service = service;
    this.portalName = portalName;
    this.portalConfig = portal;
    this.userName = userName;
    this.navigations = null;
    this.userNavigationComparator.setGlobalPortal(service.getGlobalPortal());
    this.userLocale = locale;
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
        List<String> userGroupIds = getUserGroupIds(ConversationState.getCurrent());
        if (CollectionUtils.isNotEmpty(userGroupIds)) {
          userGroupIds.forEach(groupId -> loadUserNavigation(SiteKey.group(groupId)));
        }
        if (this.refreshList) {
          // Delete navigations where user doesn't belong anymore
          this.navigations.removeIf(nav -> {
            SiteKey navSiteKey = nav.getKey();
            return navSiteKey.getType() == SiteType.GROUP && !userGroupIds.contains(navSiteKey.getName());
          });
        }
      }

      this.refreshList = false;
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
          || (!includeGlobal && siteKey.getName().equalsIgnoreCase(service.getGlobalPortal()))) {
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
    NodeContext<UserNode> nodeContext = service.getNavigationService()
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
    NodeContext<UserNode> nodeContext = service.getNavigationService()
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
    service.getNavigationService().updateNode(node.context, scope, new UserNodeListener(listener));
    node.filter();
  }

  @Override
  public void rebaseNode(UserNode node, Scope scope, NodeChangeListener<UserNode> listener) {
    if (node == null) {
      throw new IllegalArgumentException("No null node accepted");
    }
    service.getNavigationService().rebaseNode(node.context, scope, new UserNodeListener(listener));
    node.filter();
  }

  @Override
  public void saveNode(UserNode node, NodeChangeListener<UserNode> listener) throws NullPointerException,
                                                                             UserPortalException,
                                                                             NavigationServiceException {
    if (node == null) {
      throw new IllegalArgumentException("No null node accepted");
    }
    service.getNavigationService().saveNode(node.context, new UserNodeListener(listener));
    navigations = null;
    node.filter();
  }

  @Override
  public UserNode getDefaultPath(UserNodeFilterConfig filterConfig) throws UserPortalException, NavigationServiceException {
    for (UserNavigation userNavigation : getNavigations()) {
      UserNode node = getDefaultPath(userNavigation, filterConfig);
      if (node != null) {
        return node;
      }
    }

    //
    return null;
  }

  @Override
  public UserNode getDefaultPath(UserNavigation userNavigation, UserNodeFilterConfig filterConfig)
                                                                                                   throws UserPortalException,
                                                                                                   NavigationServiceException {
    NavigationContext navigation = userNavigation.navigation;
    if (navigation.getState() != null) {
      UserNodeContext userNodeContext = new UserNodeContext(userNavigation, null);
      NodeContext<UserNode> nodeContext = service.getNavigationService()
                                                 .loadNode(userNodeContext,
                                                           navigation,
                                                           Scope.CHILDREN,
                                                           null);
      if (nodeContext != null) {
        UserNode root = nodeContext.getNode();

        //
        if (filterConfig == null) {
          filterConfig = UserNodeFilterConfig.builder().build();
        }
        UserNodeFilter filter = new UserNodeFilter(userNavigation.portal, filterConfig);

        // Filter node by node
        for (UserNode node : root.getChildren()) {
          if (node.context.accept(filter)) {
            return node;
          }
        }
      }
    }
    return null;
  }

  @Override
  public UserNode resolvePath(UserNodeFilterConfig filterConfig, String path) { // NOSONAR
    if (path == null) {
      throw new IllegalArgumentException("No null path accepted");
    }

    // Parse path
    String[] segments = Utils.parsePath(path);

    // Find the first navigation available or return null
    if (segments == null) {
      return getDefaultPath(filterConfig);
    }

    // Create a filter as we need one for the path
    if (filterConfig == null) {
      filterConfig = UserNodeFilterConfig.builder().build();
    } else {
      filterConfig = UserNodeFilterConfig.builder(filterConfig).build();
    }

    // Restrict the filter with path
    filterConfig.path = segments;

    //
    MatchingScope best = null;
    for (UserNavigation navigation : getNavigations()) {
      MatchingScope scope = new MatchingScope(navigation, filterConfig, segments);
      scope.resolve();
      if (scope.score == segments.length) {
        best = scope;
        break;
      } else {
        if (best == null) {
          best = scope;
        } else {
          if (scope.score > best.score) {
            best = scope;
          }
        }
      }
    }

    //
    if (best != null && best.score > 0) {
      UserNode ret = best.userNode;
      if (ret != null) {
        ret.owner.filterConfig.path = null;
      }
      return ret;
    } else {
      return getDefaultPath(null);
    }
  }

  @Override
  public UserNode resolvePath(UserNavigation navigation, UserNodeFilterConfig filterConfig, String path)
                                                                                                         throws NullPointerException,
                                                                                                         UserPortalException,
                                                                                                         NavigationServiceException {
    if (navigation == null) {
      throw new IllegalArgumentException("No null navigation accepted");
    }
    if (path == null) {
      throw new IllegalArgumentException("No null path accepted");
    }

    //
    String[] segments = Utils.parsePath(path);

    //
    if (segments == null) {
      return null;
    }

    // Create a filter as we need one for the path
    if (filterConfig == null) {
      filterConfig = UserNodeFilterConfig.builder().build();
    } else {
      filterConfig = UserNodeFilterConfig.builder(filterConfig).build();
    }

    // Restrict the filter with the path
    filterConfig.path = segments;

    //
    MatchingScope scope = new MatchingScope(navigation, filterConfig, segments);
    scope.resolve();

    //
    UserNode globalUserNode = null;
    if (scope.score > 0) {
      UserNode ret = scope.userNode;
      if (ret != null && !StringUtils.equals(scope.userNode.getURI(), ret.getURI())) {
        UserNode globalNode = getGlobalUserNode(filterConfig, navigation.getKey(), segments);
        if (globalNode != null) {
          return globalNode;
        }
      }
      if (ret != null) {
        ret.owner.filterConfig.path = null;
        if (!segments[segments.length - 1].equals(ret.getName())) {
          globalUserNode = getGlobalUserNode(filterConfig, navigation.getKey(), segments);
          if (globalUserNode != null && segments[segments.length - 1].equals(globalUserNode.getName())) {
            ret = globalUserNode;
          }
        }
        return ret;
      }
    }
    return globalUserNode == null ? getGlobalUserNode(filterConfig, navigation.getKey(), segments) : globalUserNode;
  }

  @Override
  @SneakyThrows
  public String getPortalLabel(SiteKey siteKey) {
    return getPortalLabel(siteKey, userLocale);
  }

  @Override
  @SneakyThrows
  public String getPortalLabel(SiteKey siteKey, Locale locale) {
    PortalConfig site = service.getDataStorage().getPortalConfig(siteKey);
    String label = site == null ?
                                siteKey.getName() :
                                StringUtils.firstNonBlank(site.getLabel(),
                                                          site.getName(),
                                                          siteKey.getName());
    if (siteKey.getType() == SiteType.PORTAL) {
      return StringUtils.firstNonBlank(getLabel(siteKey, label, locale),
                                       siteKey.getName());
    } else if (siteKey.getType() == SiteType.GROUP) {
      Group siteGroup = service.getOrganizationService()
                               .getGroupHandler()
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
    PortalConfig site = service.getDataStorage().getPortalConfig(siteKey);
    String description = site == null ? null : site.getDescription();
    if (siteKey.getType() == SiteType.PORTAL && description != null) {
      return getLabel(siteKey, description, locale);
    } else if (siteKey.getType() == SiteType.GROUP) {
      Group siteGroup = service.getOrganizationService()
                               .getGroupHandler()
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
          && StringUtils.isNotBlank(service.getGlobalPortal())
          && !StringUtils.equals(service.getGlobalPortal(), siteKey.getName())) {
        // Add global navigation at the end
        loadUserNavigation(new SiteKey(SiteType.PORTAL, service.getGlobalPortal()));
      }
    } else {
      this.navigations.removeIf(nav -> nav.getKey().equals(siteKey));
    }
    NavigationContext navigationContext = service.getNavigationService()
                                                 .loadNavigation(siteKey);
    PortalConfig sitePortalConfig = service.getDataStorage().getPortalConfig(siteKey);
    if (navigationContext != null
        && navigationContext.getState() != null
        && service.getUserACL().hasAccessPermission(sitePortalConfig, getCurrentIdentity())) {
      UserACL userACL = service.getUserACL();
      UserNavigation userNavigation = new UserNavigation(this,
                                                         navigationContext,
                                                         userACL.hasEditPermission(sitePortalConfig, userACL.getUserIdentity(userName)));
      this.navigations.add(userNavigation);
      Collections.sort(this.navigations, userNavigationComparator);
      return userNavigation;
    } else {
      return null;
    }
  }

  protected List<String> getUserGroupIds(ConversationState conversationState) {
    Collection<?> groups = null;
    if (conversationState != null && conversationState.getIdentity() != null
        && !IdentityConstants.ANONIM.equals(conversationState.getIdentity().getUserId())
        && !IdentityConstants.SYSTEM.equals(conversationState.getIdentity().getUserId())) {
      groups = conversationState.getIdentity().getGroups();
    } else {
      try {
        groups = service.getOrganizationService().getGroupHandler().findGroupsOfUser(userName);
      } catch (Exception e) {
        throw new IllegalStateException("Could not retrieve groups", e);
      }
    }
    return getUserGroupIds(groups);
  }

  protected List<String> getUserGroupIds(Collection<?> groups) {
    String guestsGroupId = service.getUserACL().getGuestsGroup();
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

  protected UserNode getGlobalUserNode(UserNodeFilterConfig filterConfig, SiteKey siteKey, String[] segments) {
    UserNavigation globalNavigation = getNavigation(SiteKey.portal(this.service.getGlobalPortal()));
    if (globalNavigation != null) {
      MatchingScope globalScope = new MatchingScope(globalNavigation, filterConfig, segments);
      globalScope.resolve();
      if (globalScope.score > 0) {
        UserNode globalNode = globalScope.userNode;
        if (globalNode != null) {
          globalNode.owner.filterConfig.path = null;
        }
        if (globalNode != null && StringUtils.equals(globalScope.userNode.getURI(), globalNode.getURI())) {
          return globalNode;
        }
      }
    }
    return null;
  }

  protected ResourceBundle getNavigationBundle(SiteKey siteKey) {
    UserNavigation navigation = getNavigation(siteKey);
    return navigation == null ? null : navigation.getBundle();
  }

  /**
   * Note : the scope implementation is not stateless but we don't care in this
   * case.
   */
  protected class MatchingScope extends GenericScope.Branch.Visitor implements Scope {
    final UserNavigation       userNavigation;

    final UserNodeFilterConfig filterConfig;

    final String[]             match;

    int                        score;

    String                     id;

    UserNode                   userNode;

    MatchingScope(UserNavigation userNavigation, UserNodeFilterConfig filterConfig, String[] match) {
      this.userNavigation = userNavigation;
      this.filterConfig = filterConfig;
      this.match = match;
    }

    @Override
    public Visitor get() {
      return this;
    }

    @Override
    protected int getSize() {
      return match.length;
    }

    @Override
    protected String getName(int index) {
      return match[index];
    }

    @Override
    protected Visitor getFederated() {
      return Scope.CHILDREN.get();
    }

    @Override
    public VisitMode enter(int depth, String id, String name, NodeState state) {
      VisitMode vm = super.enter(depth, id, name, state);
      if (depth == 0) {
        score = 0;
        MatchingScope.this.id = null;
      } else {
        if (vm == VisitMode.ALL_CHILDREN) {
          MatchingScope.this.id = id;
          score++;
        }
      }
      return vm;
    }

    void resolve() {
      UserNodeContext userNodeContext = new UserNodeContext(userNavigation, filterConfig);
      NodeContext<UserNode> nodeContext = service.getNavigationService()
                                                 .loadNode(userNodeContext,
                                                           userNavigation.navigation,
                                                           this,
                                                           null);
      if (score > 0) {
        userNode = nodeContext.getNode().filter().find(id);
      }
    }
  }

  public ResourceBundle getBundle(UserNavigation navigation) {
    return getBundle(getSiteType(navigation), getSiteName(navigation), userLocale);
  }

  private ResourceBundle getBundle(String siteType, String siteName, Locale locale) {
    return getResourceBundleManager().getNavigationResourceBundle(LocaleContextInfo.getLocaleAsString(locale),
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

  private ResourceBundleManager getResourceBundleManager() {
    if (resourceBundleManager == null) {
      resourceBundleManager = ExoContainerContext.getService(ResourceBundleManager.class);
    }
    return resourceBundleManager;
  }

  private Identity getCurrentIdentity() {
    ConversationState conversationState = ConversationState.getCurrent();
    return conversationState == null ? null : conversationState.getIdentity();
  }

}
