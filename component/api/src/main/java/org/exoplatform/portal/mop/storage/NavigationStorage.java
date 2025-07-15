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
package org.exoplatform.portal.mop.storage;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NavigationData;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.navigation.NodeState;

public interface NavigationStorage {

  NodeData loadNode(Long nodeId);

  /**
   * Load all Navigation node which refer to a page
   * 
   * @param pageRef
   * @return
   */
  NodeData[] loadNodes(String pageRef);

  default NodeData[] createNode(Long parentId, Long previousId, String name, NodeState state) {
    return createNode(parentId, previousId, name, state, null);
  }

  NodeData destroyNode(Long targetId);

  NodeData updateNode(Long targetId, NodeState state);

  NodeData[] moveNode(Long targetId, Long fromId, Long toId, Long previousId);

  NodeData[] renameNode(Long targetId, Long parentId, String name);

  NavigationData loadNavigationData(SiteKey key);

  void saveNavigation(SiteKey key, NavigationState state);

  boolean destroyNavigation(NavigationData data);

  boolean destroyNavigation(SiteKey siteKey);

  NodeData[] createNode(Long parentId, Long previousId, String name, NodeState state, Integer index);

}
