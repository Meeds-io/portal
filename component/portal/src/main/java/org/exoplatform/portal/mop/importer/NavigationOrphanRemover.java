/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package org.exoplatform.portal.mop.importer;

import java.util.List;

import org.exoplatform.portal.config.model.NavigationFragment;
import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.service.NavigationService;

/**
 * Removes the nodes of a site navigation that none of the navigation
 * configurations of the site defines, as the import mode removes orphan nodes:
 * every node with {@link ImportMode#OVERWRITE}, the nodes nested in a default
 * node with {@link ImportMode#RESTORE_DEFAULTS}. The configurations are
 * imported first without removing any node, so that a configuration doesn't
 * remove the nodes another configuration of the site defines. As the import
 * of a fragment, the removal keeps the child nodes of a node that is only in
 * the path of a fragment attachment point.
 */
public class NavigationOrphanRemover {

  private final NavigationService    navigationService;

  private final List<PageNavigation> navigations;

  private final ImportMode           importMode;

  /**
   * @param navigationService the navigation service
   * @param navigations the navigation configurations of one site
   * @param importMode the import mode requested for the site
   */
  public NavigationOrphanRemover(NavigationService navigationService, List<PageNavigation> navigations, ImportMode importMode) {
    this.navigationService = navigationService;
    this.navigations = navigations;
    this.importMode = importMode;
  }

  /**
   * @param importMode an import mode
   * @return true when the import mode removes the orphan nodes of a navigation
   */
  public static boolean removesOrphans(ImportMode importMode) {
    return importMode.config.destroyOrphan || importMode.config.destroyNestedOrphan;
  }

  public void perform() {
    if (navigations.isEmpty() || !removesOrphans(importMode)) {
      return;
    }
    PageNavigation siteNavigation = navigations.get(0);
    NavigationContext navigation = navigationService.loadNavigation(new SiteKey(siteNavigation.getOwnerType(),
                                                                                siteNavigation.getOwnerId()));
    if (navigation == null) {
      return;
    }
    NodeContext<?> rootNode = navigationService.loadNode(NodeModel.SELF_MODEL, navigation, Scope.ALL, null);
    if (removeOrphans(rootNode, getDefinedRootNode())) {
      navigationService.saveNode(rootNode, null);
    }
  }

  private DefinedNavigationNode getDefinedRootNode() {
    DefinedNavigationNode rootNode = new DefinedNavigationNode();
    for (PageNavigation navigation : navigations) {
      if (navigation.getFragments() != null) {
        for (NavigationFragment fragment : navigation.getFragments()) {
          DefinedNavigationNode attachmentNode = getAttachmentNode(rootNode, fragment.getParentURI());
          attachmentNode.setAttachmentPoint();
          if (fragment.getNodes() != null) {
            fragment.getNodes().forEach(attachmentNode::addTopLevelNode);
          }
        }
      }
    }
    return rootNode;
  }

  private DefinedNavigationNode getAttachmentNode(DefinedNavigationNode rootNode, String parentURI) {
    DefinedNavigationNode attachmentNode = rootNode;
    if (parentURI != null) {
      for (String name : parentURI.split("/")) {
        if (!name.isEmpty()) {
          attachmentNode = attachmentNode.getOrAddPathNode(name);
        }
      }
    }
    return attachmentNode;
  }

  private boolean removeOrphans(NodeContext<?> node, DefinedNavigationNode definedNode) {
    boolean removed = false;
    NodeContext<?> child = node.getFirst();
    while (child != null) {
      NodeContext<?> nextChild = child.getNext();
      DefinedNavigationNode definedChild = definedNode.getChild(child.getName());
      if (definedChild != null) {
        removed |= removeOrphans(child, definedChild);
      } else if (definedNode.isChildrenDefined()
          && (importMode.config.destroyOrphan
              || (importMode.config.destroyNestedOrphan && !definedNode.isTopLevelParent()))) {
        child.removeNode();
        removed = true;
      }
      child = nextChild;
    }
    return removed;
  }

}
