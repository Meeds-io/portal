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

import java.util.Collection;
import java.util.LinkedList;

/**
 * A queuing implementation of the {@link NodeChangeListener} interface.
 *
 */
public class NodeChangeQueue<N> extends LinkedList<NodeChange<N>> implements NodeChangeListener<N> {

  private static final long serialVersionUID = -4635111145546093584L;

  public NodeChangeQueue() {
  }

  public NodeChangeQueue(Collection<? extends NodeChange<N>> c) {
    super(c);
  }

  public void broadcast(NodeChangeListener<N> listener) {
    for (NodeChange<N> change : this) {
      change.dispatch(listener);
    }
  }

  public void onAdd(N target, N parent, N previous) {
    add(new NodeChange.Added<N>(parent, previous, target));
  }

  public void onCreate(N target, N parent, N previous, String name) {
    add(new NodeChange.Created<N>(parent, previous, target, name));
  }

  public void onRemove(N target, N parent) {
    add(new NodeChange.Removed<N>(parent, target));
  }

  public void onDestroy(N target, N parent) {
    add(new NodeChange.Destroyed<N>(parent, target));
  }

  public void onRename(N target, N parent, String name) {
    add(new NodeChange.Renamed<N>(parent, target, name));
  }

  public void onUpdate(N target, NodeState state) {
    add(new NodeChange.Updated<N>(target, state));
  }

  public void onMove(N target, N from, N to, N previous) {
    add(new NodeChange.Moved<N>(from, to, previous, target));
  }
}
