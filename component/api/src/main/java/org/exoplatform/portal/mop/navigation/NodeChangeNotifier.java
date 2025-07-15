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

import org.exoplatform.portal.mop.EventType;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class NodeChangeNotifier<N> implements NodeChangeListener<NodeContext<N>> {

  private static final Log                   LOG = ExoLogger.getLogger(NodeChangeNotifier.class);

  private NodeChangeListener<NodeContext<N>> listener;

  private ListenerService                    listenerService;

  private org.exoplatform.portal.mop.service.NavigationService                  navigationService;

  public NodeChangeNotifier(NodeChangeListener<NodeContext<N>> listener,
                            org.exoplatform.portal.mop.service.NavigationService navigationService,
                            ListenerService listenerService) {
    this.listener = listener;
    this.listenerService = listenerService;
    this.navigationService = navigationService;
  }

  @Override
  public void onAdd(NodeContext<N> target, NodeContext<N> parent, NodeContext<N> previous) {
    notifyNodeChange(EventType.NAVIGATION_NODE_ADD, target);
    if (listener != null) {
      listener.onAdd(target, parent, previous);
    }
  }

  @Override
  public void onCreate(NodeContext<N> target, NodeContext<N> parent, NodeContext<N> previous, String name) {
    notifyNodeChange(EventType.NAVIGATION_NODE_CREATE, target);
    if (listener != null) {
      listener.onCreate(target, parent, previous, name);
    }
  }

  @Override
  public void onRemove(NodeContext<N> target, NodeContext<N> parent) {
    notifyNodeChange(EventType.NAVIGATION_NODE_REMOVE, target);
    if (listener != null) {
      listener.onRemove(target, parent);
    }
  }

  @Override
  public void onDestroy(NodeContext<N> target, NodeContext<N> parent) {
    notifyNodeChange(EventType.NAVIGATION_NODE_DESTROY, target);
    if (listener != null) {
      listener.onDestroy(target, parent);
    }
  }

  @Override
  public void onRename(NodeContext<N> target, NodeContext<N> parent, String name) {
    notifyNodeChange(EventType.NAVIGATION_NODE_RENAME, target);
    if (listener != null) {
      listener.onRename(target, parent, name);
    }
  }

  @Override
  public void onUpdate(NodeContext<N> target, NodeState state) {
    notifyNodeChange(EventType.NAVIGATION_NODE_UPDATE, target);
    if (listener != null) {
      listener.onUpdate(target, state);
    }
  }

  @Override
  public void onMove(NodeContext<N> target, NodeContext<N> from, NodeContext<N> to, NodeContext<N> previous) {
    notifyNodeChange(EventType.NAVIGATION_NODE_MOVE, target);
    if (listener != null) {
      listener.onMove(target, from, to, previous);
    }
  }

  private void notifyNodeChange(String eventName, NodeContext<N> target) {
    try {
      LOG.debug("Broadcasting change type " + eventName + " notification for node " + target.getId()
          + " name " + target.getName());
      listenerService.broadcast(eventName, navigationService, target);
    } catch (Exception e) {
      LOG.error("Error when delivering notification " + eventName + " for node " + target.getId(), e);
    }
  }

}
