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

import java.util.*;

import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.NavigationServiceException;
import org.exoplatform.portal.mop.navigation.NodeChangeListener;
import org.exoplatform.portal.mop.navigation.Scope;

/**
 * The user portal establish the relationship between a user and the portal.
 *
 */
public interface UserPortal {

  /**
   * Returns the portal locale.
   *
   * @return the portal locale
   */
  Locale getLocale();

  /**
   * @return username
   */
  String getUserName();

  /**
   * Returns the sorted list of current user navigations.
   *
   * @return the current user navigations
   * @throws UserPortalException any user portal exception
   * @throws NavigationServiceException any navigation service exception
   */
  List<UserNavigation> getNavigations() throws UserPortalException, NavigationServiceException;

  /**
   * Returns a user navigation for a specified site key, null is returned when
   * such navigation does not exist.
   *
   * @param key the site key
   * @return the corresponding user navigation
   * @throws NullPointerException when the provided key is null
   * @throws UserPortalException any user portal exception
   * @throws NavigationServiceException any navigation service exception
   */
  UserNavigation getNavigation(SiteKey key) throws NullPointerException, UserPortalException, NavigationServiceException;

  /**
   * Refresh the current user portal.
   */
  void refresh();

  /**
   * Load a user node from a specified user navigation with a custom scope. The
   * returned node is the root node of the navigation.
   *
   * @param navigation the user navigation
   * @param scope an optional scope
   * @param filterConfig an optional filter
   * @param listener an optional listener @return the user node
   * @return the user node
   * @throws NullPointerException if the navigation argument is null
   * @throws UserPortalException any user portal exception
   * @throws NavigationServiceException any navigation service exception
   */
  UserNode getNode(UserNavigation navigation,
                   Scope scope,
                   UserNodeFilterConfig filterConfig,
                   NodeChangeListener<UserNode> listener);

  /**
   * Load specific navigation node children
   *
   * @param userNodeId target userNode id
   * @param siteKey Site key
   * @param scope the scope
   * @param filterConfig an optional filter
   * @param listener an optional listener
   * @return {@link UserNode}
   * @throws NullPointerException
   * @throws UserPortalException
   * @throws NavigationServiceException
   */
  default UserNode getNodeById(String userNodeId,
                               SiteKey siteKey,
                               Scope scope,
                               UserNodeFilterConfig filterConfig,
                               NodeChangeListener<UserNode> listener) throws NullPointerException,
                                                                      UserPortalException,
                                                                      NavigationServiceException {
    throw new UnsupportedOperationException();
  }

  /**
   * Load the list of user nodes computed from the list of
   * {@link UserNavigation} of type siteType (PORTAL, GROUP or USER)
   * 
   * @param siteType site type: PORTAL, GROUP or USER
   * @param scope an optional scope
   * @param filterConfig an optional filter
   * @return a {@link Collection} of {@link UserNode}
   */
  Collection<UserNode> getNodes(SiteType siteType, Scope scope, UserNodeFilterConfig filterConfig);

  /**
   * Load the list of user nodes computed from the list of
   * {@link UserNavigation} of type siteType (PORTAL, GROUP or USER)
   * 
   * @param siteType site type: PORTAL, GROUP or USER
   * @param scope an optional scope
   * @param filterConfig an optional filter
   * @param includeGlobal to include global nodes
   * @return a {@link Collection} of {@link UserNode}
   */
  Collection<UserNode> getNodes(SiteType siteType, Scope scope, UserNodeFilterConfig filterConfig, boolean includeGlobal);

  /**
   * Load the list of user nodes computed from the list of
   * {@link UserNavigation} of type siteType (PORTAL, GROUP or USER)
   * 
   * @param siteType site type: PORTAL, GROUP or USER
   * @param excludedSiteName site name regex to exclude
   * @param scope an optional scope
   * @param filterConfig an optional filter
   * @return a {@link Collection} of {@link UserNode}
   * @deprecated use {@link #getNodes(SiteType, Scope, UserNodeFilterConfig)}
   *             instead since no need of filtering on Spaces nodes due to
   *             introduction of new {@link SiteType#SPACE} that allows to get
   *             space navigations
   */
  @Deprecated(forRemoval = true, since = "6.5")
  default Collection<UserNode> getNodes(SiteType siteType,
                                        String excludedSiteName,
                                        Scope scope,
                                        UserNodeFilterConfig filterConfig) {
    return getNodes(siteType, scope, filterConfig);
  }

  /**
   * Update the specified content with the most recent state.
   *
   * @param node the node to update
   * @param scope the optional scope
   * @param listener an optional listener
   * @throws NullPointerException if the context argument is null
   * @throws IllegalArgumentException if the node has pending changes
   * @throws UserPortalException any user portal exception
   * @throws NavigationServiceException anything that would prevent the
   *           operation to succeed
   */
  void updateNode(UserNode node, Scope scope, NodeChangeListener<UserNode> listener) throws NullPointerException,
                                                                                     IllegalArgumentException,
                                                                                     UserPortalException,
                                                                                     NavigationServiceException;

  /**
   * Rebase the specified context with the most recent state.
   *
   * @param node the user node that will be rebased
   * @param scope the optional scope
   * @param listener the optional node change listener
   * @throws NullPointerException if the context argument is null
   * @throws UserPortalException any user portal exception
   * @throws NavigationServiceException anything that would prevent the
   *           operation to succeed
   */
  void rebaseNode(UserNode node, Scope scope, NodeChangeListener<UserNode> listener) throws NullPointerException,
                                                                                     UserPortalException,
                                                                                     NavigationServiceException;

  /**
   * Save the specified context to the persistent storage.
   *
   * @param node the user node that will be rebased
   * @param listener the optional node change listener
   * @throws NullPointerException if the node argument is null
   * @throws UserPortalException any user portal exception
   * @throws NavigationServiceException anything that would prevent the
   *           operation to succeed
   */
  void saveNode(UserNode node, NodeChangeListener<UserNode> listener) throws NullPointerException,
                                                                      UserPortalException,
                                                                      NavigationServiceException;

  /**
   * Returns the user node for the default path.
   *
   * @param filterConfig an optional filter
   * @return the default navigation path
   * @throws UserPortalException any user portal exception
   * @throws NavigationServiceException any navigation service exception
   * @deprecated use {@link UserPortalConfigService#getDefaultPath(String)} instead
   */
  @Deprecated(forRemoval = true, since = "7.0")
  default UserNode getDefaultPath(UserNodeFilterConfig filterConfig) throws UserPortalException, NavigationServiceException {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the user node for the default path in specified navigation
   *
   * @param navigation the navigation
   * @param filterConfig an optional filter
   * @return the default navigation path
   * @throws UserPortalException any user portal exception
   * @throws NavigationServiceException any navigation service exception
   * @deprecated use {@link UserPortalConfigService#getDefaultSitePath(String, String)} instead
   */
  @Deprecated(forRemoval = true, since = "7.0")
  default UserNode getDefaultPath(UserNavigation navigation, UserNodeFilterConfig filterConfig) throws UserPortalException,
                                                                                                NavigationServiceException {
    throw new UnsupportedOperationException();
  }

  /**
   * Resolves and returns a node among all user navigations for a specified
   * path.
   *
   * @param filterConfig an optional filter
   * @param path the path
   * @return the navigation path
   * @throws NullPointerException if the navigation or path argument is null
   * @throws UserPortalException any user portal exception
   * @throws NavigationServiceException any navigation service exception
   * @deprecated use {@link UserPortalConfigService#getSiteNodeOrGlobalNode(String, String, String, String)} instead
   */
  @Deprecated(forRemoval = true, since = "7.0")
  default UserNode resolvePath(UserNodeFilterConfig filterConfig, String path) throws NullPointerException,
                                                                               UserPortalException,
                                                                               NavigationServiceException {
    throw new UnsupportedOperationException();
  }

  /**
   * Resolves and returns a node for the specified navigation and for a
   * specified path.
   *
   * @param navigation the navigation
   * @param filterConfig an optional filter
   * @param path the path @return the navigation path
   * @return the navigation path
   * @throws NullPointerException if the navigation or path argument is null
   * @throws UserPortalException any user portal exception
   * @throws NavigationServiceException any navigation service exception
   * @deprecated use {@link UserPortalConfigService#getSiteNodeOrGlobalNode(String, String, String, String)} instead
   */
  @Deprecated(forRemoval = true, since = "7.0")
  default UserNode resolvePath(UserNavigation navigation, UserNodeFilterConfig filterConfig, String path)
                                                                                                          throws NullPointerException,
                                                                                                          UserPortalException,
                                                                                                          NavigationServiceException {
    throw new UnsupportedOperationException();
  }

  /**
   * @param siteKey Site Key
   * @return the {@link PortalConfig} label
   */
  default String getPortalLabel(SiteKey siteKey) {
    return null;
  }

  /**
   * @param siteKey Site Key
   * @param locale {@link Locale}
   * @return the {@link PortalConfig} label
   */
  default String getPortalLabel(SiteKey siteKey, Locale locale) {
    return null;
  }

  /**
   * @param siteKey Site Key
   * @return the {@link PortalConfig} description
   */
  default String getPortalDescription(SiteKey siteKey) {
    return null;
  }

  /**
   * @param siteKey Site Key
   * @param locale {@link Locale}
   * @return the {@link PortalConfig} description
   */
  default String getPortalDescription(SiteKey siteKey, Locale locale) {
    return null;
  }
}
