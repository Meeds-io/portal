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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.portal.config.model.PageNode;

/**
 * A navigation node that the configurations of a site define, attach a
 * navigation fragment to, or name in the path of a fragment attachment point,
 * with the child nodes the configurations define under it.
 */
class DefinedNavigationNode {

  private final Map<String, DefinedNavigationNode> children = new HashMap<>();

  /**
   * True when the configurations define the node children: the node is
   * defined by a configuration, or is the attachment point of a fragment. The
   * children of a node that is only in the path of an attachment point are
   * not defined by the configurations.
   */
  private boolean                                  childrenDefined;

  /**
   * True when the node is the attachment point of a fragment, or a top-level
   * node of a fragment: a child node that no configuration defines under it is
   * a top-level section, not a node nested in a default node.
   */
  private boolean                                  topLevelParent;

  DefinedNavigationNode getChild(String name) {
    return children.get(name);
  }

  DefinedNavigationNode getOrAddPathNode(String name) {
    return children.computeIfAbsent(name, key -> new DefinedNavigationNode());
  }

  void setAttachmentPoint() {
    childrenDefined = true;
    topLevelParent = true;
  }

  void addTopLevelNode(PageNode node) {
    addNode(node).topLevelParent = true;
  }

  boolean isChildrenDefined() {
    return childrenDefined;
  }

  boolean isTopLevelParent() {
    return topLevelParent;
  }

  private DefinedNavigationNode addNode(PageNode node) {
    DefinedNavigationNode definedNode = getOrAddPathNode(node.getName());
    definedNode.childrenDefined = true;
    List<PageNode> nodes = node.getNodes();
    if (nodes != null) {
      nodes.forEach(definedNode::addNode);
    }
    return definedNode;
  }

}
