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

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NodeChangeListener;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.navigation.Scope;

public interface NavigationService {

  /**
   * Find and returns a navigation, if no such site exist, null is returned
   * instead.
   *
   * @param key the navigation key
   * @return the matching navigation
   */
  NavigationContext loadNavigation(SiteKey key);

  /**
   * Create, update a navigation. When the navigation state is not null, the
   * navigation will be created or updated depending on whether or not the
   * navigation already exists.
   *
   * @param navigation the navigation
   */
  void saveNavigation(NavigationContext navigation);

  /**
   * Delete a navigation for a given site
   *
   * @param navigation the navigation
   * @return true if the navigation was destroyed
   */
  boolean destroyNavigation(NavigationContext navigation);

  /**
   * Delete a navigation for a given site
   *
   * @param siteKey {@link SiteKey}
   * @return true if the navigation was destroyed
   */
  boolean destroyNavigation(SiteKey siteKey);

  /**
   * Load a navigation node from a specified navigation. The returned context
   * will be the root node of the navigation.
   *
   * @param model the node model
   * @param navigation the navigation
   * @param scope the scope
   * @param listener the optional listener
   * @param <N> the node generic type
   * @return the loaded node
   */
  <N> NodeContext<N> loadNode(NodeModel<N> model,
                              NavigationContext navigation,
                              Scope scope,
                              NodeChangeListener<NodeContext<N>> listener);

  /**
   * Loads root node of a designated Site
   * 
   * @param siteKey {@link SiteKey}
   * @param <N> the node generic type
   * @return the loaded node
   */
  <N> NodeContext<NodeContext<N>> loadNode(SiteKey siteKey);

  /**
   * Loads a specific node of a Site identitifed by its relative URI (from root
   * node until node)
   * 
   * @param siteKey
   * @param navUri
   * @param <N> the node generic type
   * @return the loaded node
   */
  <N> NodeContext<NodeContext<N>> loadNode(SiteKey siteKey, String navUri);

  /**
   * Load a navigation node from a specified navigation by its id
   *
   * @param model the node model
   * @param nodeId the node id
   * @param scope the scope
   * @param listener the optional listener
   * @param <N> the node generic type
   * @return the loaded node
   */
  default <N> NodeContext<N> loadNodeById(NodeModel<N> model, // NOSONAR
                                          String nodeId,
                                          Scope scope,
                                          NodeChangeListener<NodeContext<N>> listener) {
    throw new UnsupportedOperationException();
  }

  /**
   * <p>
   * Save the specified context state to the persistent storage. The operation
   * takes the pending changes done to the tree and attempt to save them to the
   * persistent storage. When conflicts happens, a merge will be attempted
   * however it can lead to a failure.
   * </p>
   *
   * @param context the context to save
   * @param listener the optional listener
   */
  <N> void saveNode(NodeContext<N> context, NodeChangeListener<NodeContext<N>> listener);

  /**
   * <p>
   * Update the specified <code>context</code> argument with the most recent
   * state. The update operation will affect the entire tree even if the
   * <code>context</code> argument is not the root of the tree. The
   * <code>context</code> argument determines the root from which the
   * <code>scope</code> argument applies to.
   * </p>
   * <p>
   * The update operation compares the actual tree and the most recent version
   * of the same tree. When the <code>scope</code> argument is not null, it will
   * be used to augment the tree with new nodes. During the operation, any
   * modification done to the tree wil be reported as a change to the optional
   * <code>listener</code> argument.
   * </p>
   * <p>
   * The update operates recursively by doing a comparison of the node intrisic
   * state (name or state) and its structural state (the children). The
   * comparison between the children of two nodes is done thanks to the Longest
   * Common Subsequence algorithm to minimize the number of changes to perform.
   * The operation assumes that no changes have been performed on the actual
   * tree.
   * </p>
   *
   * @param context the context to update
   * @param scope the optional scope
   * @param listener the optional node change listener
   * @param <N> the node generic type
   */
  <N> void updateNode(NodeContext<N> context, Scope scope, NodeChangeListener<NodeContext<N>> listener);

  /**
   * <p>
   * Rebase the specified <code>context</code> argument with the most recent
   * state. The rebase operation will affect the entire tree even if the
   * <code>context</code> argument is not the root of the tree. The
   * <code>context</code> argument determines the root from which the
   * <code>scope</code> argument applies to.
   * </p>
   * <p>
   * The rebase operation compares the actual tree and the most recent version
   * of the same tree. When the <code>scope</code> argument is not null, it will
   * be used to augment the tree with new nodes. During the operation, any
   * modification done to the tree wil be reported as a change to the optional
   * <code>listener</code> argument.
   * </p>
   * <p>
   * The rebase operates in a similar way of the update operation, however it
   * assumes that it can have pending changes done to the tree (i.e changes that
   * have not been saved). Actually a rebase operation with no changes will do
   * the same than an update operation. The rebase operation attempts to bring
   * the most recent changes to the tree, by doing a rebase of the pending
   * operations on the actual tree. When conflicting changes exist, a merge will
   * be attempted, however it could fail and lead to a non resolvable situation.
   * </p>
   *
   * @param context the context to rebase
   * @param scope the optional scope
   * @param listener the option node change listener @throws
   *          NullPointerException if the context argument is null
   * @param <N> the node generic type
   */
  <N> void rebaseNode(NodeContext<N> context, Scope scope, NodeChangeListener<NodeContext<N>> listener);

  /**
   * Delete a navigation node with a given node id
   *
   * @param nodeId the node id to be deleted
   */
  public void deleteNode(Long nodeId);

  /**
   * Get a navigation node with a given node id
   *
   * @param nodeId the node id
   */
  public NodeData getNodeById(Long nodeId);

  /**
   * Move the given navigation node
   *
   * @param targetId the node id
   * @param fromId the parent node id
   * @param toId destination parent node id
   * @param previousId the previous node id
   */
  public void moveNode(Long targetId, Long fromId, Long toId, Long previousId);

  /**
   * Create a navigation node
   *
   * @param parentId the parent node id
   * @param previousId the previous node id
   * @param name node name
   * @param state node state
   */
  public NodeData[] createNode(Long parentId, Long previousId, String name, NodeState state);

  /**
   * Update a navigation node
   *
   * @param nodeId the node id
   * @param state node state
   */
  public void updateNode(Long nodeId, NodeState state);

  /**
   * @param sourceSiteTemplate {@link SiteKey} of template
   * @param targetSiteKey {@link SiteKey} to create
   * @throws ObjectNotFoundException when Site Template isn't found
   */
  void saveNavigationFromTemplate(SiteKey sourceSiteTemplate,
                                  SiteKey targetSiteKey) throws ObjectNotFoundException;

}
