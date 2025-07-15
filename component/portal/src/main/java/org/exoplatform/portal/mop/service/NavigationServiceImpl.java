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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.State;
import org.exoplatform.portal.mop.navigation.GenericScope;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationData;
import org.exoplatform.portal.mop.navigation.NodeChangeListener;
import org.exoplatform.portal.mop.navigation.NodeChangeNotifier;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.navigation.NodeManager;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.storage.NavigationStorage;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class NavigationServiceImpl implements NavigationService {

  private static final Log  LOG = ExoLogger.getLogger(NavigationServiceImpl.class);

  private final NodeManager nodeManager;

  final DescriptionService  descriptionService;

  final ListenerService     listenerService;

  final NavigationStorage   navigationStorage;

  public NavigationServiceImpl(DescriptionService descriptionService,
                               ListenerService listenerService,
                               NavigationStorage navigationStorage) {
    this.descriptionService = descriptionService;
    this.listenerService = listenerService;
    this.navigationStorage = navigationStorage;
    this.nodeManager = new NodeManager(navigationStorage);
  }

  public NavigationContext loadNavigation(SiteKey key) {
    NavigationData navigationData = navigationStorage.loadNavigationData(key);
    return navigationData == null ? null : new NavigationContext(navigationData);
  }

  public void saveNavigation(NavigationContext navigation) {
    boolean created = loadNavigation(navigation.getKey()) == null;

    navigationStorage.saveNavigation(navigation.getKey(), navigation.getState());
    navigation.setData(navigationStorage.loadNavigationData(navigation.getKey()));
    navigation.setState(null);

    if (created) {
      notify(EventType.NAVIGATION_CREATED, navigation.getKey());
    } else {
      notify(EventType.NAVIGATION_UPDATED, navigation.getKey());
    }
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void saveNavigationFromTemplate(SiteKey sourceSiteTemplate,
                                         SiteKey targetSiteKey) throws ObjectNotFoundException {
    NavigationContext navigationTemplateContext = loadNavigation(sourceSiteTemplate);
    navigationStorage.saveNavigation(targetSiteKey, navigationTemplateContext.getState());
    NavigationContext navigationContext = loadNavigation(targetSiteKey);
    NodeContext rootTemplate = loadNode(NodeModel.SELF_MODEL,
                                        navigationTemplateContext,
                                        Scope.ALL,
                                        null);
    NodeContext root = loadNode(NodeModel.SELF_MODEL,
                                navigationContext,
                                Scope.ALL,
                                null);
    cloneNodes(rootTemplate, root, targetSiteKey);
    notify(EventType.NAVIGATION_CREATED, targetSiteKey);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void cloneNodes(NodeContext src, NodeContext dest, SiteKey siteKey) {
    if (src == null || src.getNodeCount() == 0) {
      return;
    }

    List<?> nodes = new ArrayList<>(src.getNodes());
    Long previousId = null;
    for (int i = 0; i < nodes.size(); i++) {
      NodeContext<NodeContext> sourceChild = (NodeContext<NodeContext>) nodes.get(i);
      NodeState state = sourceChild.getData().getState();
      NodeData[] nodeDatas = createNode(Long.parseLong(dest.getId()),
                                        previousId,
                                        sourceChild.getName(),
                                        new NodeState(state.getLabel(),
                                                      state.getIcon(),
                                                      state.getStartPublicationTime(),
                                                      state.getEndPublicationTime(),
                                                      state.getVisibility(),
                                                      state.getPageRef() == null ? null :
                                                                                 siteKey.page(state.getPageRef().getName()),
                                                      siteKey,
                                                      state.getTarget(),
                                                      System.currentTimeMillis()));
      NodeData nodeData = nodeDatas[1];
      Map<Locale, State> descriptions = descriptionService.getDescriptions(sourceChild.getId());
      descriptionService.setDescriptions(nodeData.getId(), descriptions);
      previousId = Long.parseLong(nodeData.getId());
      if (sourceChild.getNodeCount() > 0) {
        NodeContext destinationChild = loadNodeById(NodeModel.SELF_MODEL, nodeData.getId(), Scope.ALL, null);
        cloneNodes(sourceChild, destinationChild, siteKey);
      }
    }
  }

  public boolean destroyNavigation(NavigationContext navigation) {
    if (navigation == null) {
      throw new IllegalArgumentException("NavigationContext is mandatory");
    }
    if (navigation.getData() == null) {
      throw new IllegalArgumentException("NavigationContext.data removed");
    }

    notify(EventType.NAVIGATION_DESTROY, navigation.getKey());
    if (navigationStorage.destroyNavigation(navigation.getData())) {
      navigation.setData(null);
      notify(EventType.NAVIGATION_DESTROYED, navigation.getKey());
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean destroyNavigation(SiteKey siteKey) {
    NavigationContext navigationContext = loadNavigation(siteKey);
    if (navigationContext == null) {
      return false;
    } else {
      return destroyNavigation(navigationContext);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public NodeContext<NodeContext<?>> loadNode(SiteKey siteKey) {
    return loadNode(siteKey, null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public NodeContext<NodeContext<?>> loadNode(SiteKey siteKey, String navUri) {
    if (siteKey == null) {
      return null;
    }
    NavigationContext navigation = loadNavigation(siteKey);
    if (navigation == null) {
      return null;
    }

    if (StringUtils.isBlank(navUri)) {
      return loadNode(NodeModel.SELF_MODEL, navigation, Scope.ALL, null);
    } else {
      String[] pathTreeParts = StringUtils.trim(navUri).split("/");
      NodeContext<NodeContext<?>> node = loadNode(NodeModel.SELF_MODEL,
                                                  navigation,
                                                  GenericScope.branchShape(pathTreeParts, Scope.ALL),
                                                  null);
      Iterator<String> iterator = Arrays.asList(pathTreeParts).iterator();
      while (iterator.hasNext() && node != null) {
        String name = iterator.next();
        node = node.get(name);
      }
      return node;
    }
  }

  public <N> NodeContext<N> loadNode(NodeModel<N> model,
                                     NavigationContext navigation,
                                     Scope scope,
                                     NodeChangeListener<NodeContext<N>> listener) {
    if (model == null) {
      throw new NullPointerException("No null model accepted");
    }
    if (navigation == null) {
      throw new NullPointerException("No null navigation accepted");
    }
    if (scope == null) {
      throw new NullPointerException("No null scope accepted");
    }
    String nodeId = navigation.getData().getRootId();
    if (nodeId != null) {
      return nodeManager.loadNode(model, nodeId, scope, new NodeChangeNotifier<>(listener, this, listenerService));
    } else {
      return null;
    }
  }

  @Override
  public <N> NodeContext<N> loadNodeById(NodeModel<N> model,
                                         String nodeId,
                                         Scope scope,
                                         NodeChangeListener<NodeContext<N>> listener) {
    if (model == null) {
      throw new NullPointerException("No null model accepted");
    }
    if (nodeId == null) {
      throw new NullPointerException("No null node id accepted");
    }
    if (scope == null) {
      throw new NullPointerException("No null scope accepted");
    }
    return nodeManager.loadNode(model, nodeId, scope, new NodeChangeNotifier<>(listener, this, listenerService));
  }

  public <N> void updateNode(NodeContext<N> root, Scope scope, NodeChangeListener<NodeContext<N>> listener) {
    nodeManager.updateNode(root, scope, new NodeChangeNotifier<>(listener, this, listenerService));
  }

  public <N> void saveNode(NodeContext<N> context) {
    saveNode(context, null);
  }

  public <N> void saveNode(NodeContext<N> context, NodeChangeListener<NodeContext<N>> listener) {
    nodeManager.saveNode(context, new NodeChangeNotifier<>(listener, this, listenerService));
  }

  public <N> void rebaseNode(NodeContext<N> context, Scope scope, NodeChangeListener<NodeContext<N>> listener) {
    nodeManager.rebaseNode(context, scope, new NodeChangeNotifier<>(listener, this, listenerService));
  }

  @Override
  public NodeData getNodeById(Long nodeId) {
    return navigationStorage.loadNode(nodeId);
  }

  @Override
  public void moveNode(Long targetId, Long fromId, Long toId, Long previousId) {
    navigationStorage.moveNode(targetId, fromId, toId, previousId);
  }

  @Override
  public void deleteNode(Long nodeId) {
    navigationStorage.destroyNode(nodeId);
  }

  @Override
  public NodeData[] createNode(Long parentId, Long previousId, String name, NodeState state) {
    return navigationStorage.createNode(parentId, previousId, name, state);
  }

  public void updateNode(Long nodeId, NodeState state) {
    navigationStorage.updateNode(nodeId, state);
  }

  private void notify(String name, SiteKey key) {
    try {
      listenerService.broadcast(name, this, key);
    } catch (Exception e) {
      LOG.error("Error when delivering notification " + name + " for navigation " + key, e);
    }
  }
}
