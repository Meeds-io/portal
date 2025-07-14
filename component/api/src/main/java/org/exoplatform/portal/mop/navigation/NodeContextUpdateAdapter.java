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
package org.exoplatform.portal.mop.navigation;

import java.util.ArrayList;

import org.exoplatform.portal.mop.Utils;

class NodeContextUpdateAdapter<N> implements TreeUpdateAdapter<NodeContext<N>> {

  private static final NodeContextUpdateAdapter<?> INSTANCE = new NodeContextUpdateAdapter<>();

  @SuppressWarnings("unchecked")
  static <N> NodeContextUpdateAdapter<N> create() {
    return (NodeContextUpdateAdapter<N>) INSTANCE;
  }

  public String getHandle(NodeContext<N> node) {
    return node.handle;
  }

  public String[] getChildren(NodeContext<N> node) {
    if (node.getFirst() != null) {
      ArrayList<String> tmp = new ArrayList<>();
      for (NodeContext<N> current = node.getFirst(); current != null; current = current.getNext()) {
        tmp.add(current.handle);
      }
      return tmp.toArray(new String[tmp.size()]);
    } else {
      return Utils.EMPTY_STRING_ARRAY;
    }
  }

  public NodeContext<N> getDescendant(NodeContext<N> node, String handle) {
    return node.getDescendant(handle);
  }

  public NodeData getData(NodeContext<N> node) {
    return node.data;
  }

  public NodeState getState(NodeContext<N> node) {
    return node.state;
  }

  public String getName(NodeContext<N> node) {
    return node.name;
  }
}
