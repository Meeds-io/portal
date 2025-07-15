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
package org.gatein.api.navigation;

import java.util.Locale;
import java.util.Map;

import org.gatein.api.ApiException;
import org.gatein.api.PortalRequest;
import org.gatein.api.internal.Parameters;
import org.gatein.api.site.Site;
import org.gatein.api.site.SiteId;

import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.navigation.NodeChangeListener;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.storage.DescriptionStorage;
import org.exoplatform.services.resources.ResourceBundleManager;

@Deprecated(forRemoval = true, since = "7.0")
public class NavigationImpl implements Navigation {
    private final NavigationService navigationService;
    private final NavigationContext navCtx;
    private final DescriptionStorage descriptionStorage;
    private final ResourceBundleManager bundleManager;

    private final SiteId siteId;
    private final ApiNodeModel model;

    private Navigation18NResolver i18nResolver;

    public NavigationImpl(SiteId siteId, NavigationService navigationService, NavigationContext navCtx, DescriptionStorage descriptionService,
            ResourceBundleManager bundleManager) {
        this.siteId = siteId;
        this.navigationService = navigationService;
        this.navCtx = navCtx;
        this.descriptionStorage = descriptionService;
        this.bundleManager = bundleManager;
        this.model = new ApiNodeModel(this);
    }

    // Used for unit testing
    NavigationImpl(SiteId siteId) {
        this.siteId = siteId;
        this.navigationService = null;
        this.navCtx = null;
        this.descriptionStorage = null;
        this.bundleManager = null;
        this.model = null;
    }

    @Override
    public boolean removeNode(NodePath path) {
        Parameters.requireNonNull(path, "path");

        Node parent = getNode(path.parent(), Nodes.visitChildren());
        if (parent == null || !parent.removeChild(path.getLastSegment())) {
            return false;
        }

        saveNode(parent);
        return true;
    }

    @Override
    public Node getNode(String... nodePath) {
        return getNode(NodePath.path(nodePath));
    }

    @Override
    public Node getNode(NodePath nodePath) {
        return getNode(nodePath, Nodes.visitNone());
    }

    @Override
    public Node getNode(NodePath nodePath, NodeVisitor visitor) {
        Parameters.requireNonNull(nodePath, "nodePath");
        Parameters.requireNonNull(visitor, "visitor");

        NodeContext<ApiNode> ctx = getNodeContext(nodePath, visitor);
        return (ctx == null) ? null : ctx.getNode();
    }

    @Override
    public int getPriority() {
        return navCtx.getState().getPriority();
    }

    @Override
    public SiteId getSiteId() {
        return siteId;
    }

    @Override
    public Node getRootNode(NodeVisitor visitor) {
        NodeContext<ApiNode> ctx = loadNodeContext(visitor);
        return (ctx == null) ? null : ctx.getNode();
    }

    @Override
    public void refreshNode(Node node) {
        refreshNode(node, Nodes.visitNone());
    }

    @Override
    public void refreshNode(Node node, NodeVisitor visitor) {
        Parameters.requireNonNull(node, "node");
        Parameters.requireNonNull(visitor, "visitor");

        NodeContext<ApiNode> ctx = ((ApiNode) node).getContext();
        rebaseNodeContext(ctx, new NodeVisitorScope(visitor), null);

        Node r = node;
        while (!r.isRoot())
            r = r.getParent();
        clearCached(r);
    }

    private void clearCached(Node node) {
        ((ApiNode) node).clearCached();
        if (node.isChildrenLoaded()) {
            for (Node c : node) {
                clearCached(c);
            }
        }
    }

    @Override
    public void saveNode(Node node) {
        Parameters.requireNonNull(node, "node");

        NodeContext<ApiNode> ctx = ((ApiNode) node).getContext();
        saveNodeContext(ctx, null);
        saveDisplayNames(ctx);
    }

    @Override
    public void setPriority(int priority) {
        navCtx.setState(new NavigationState(priority));
        save(navCtx);
    }

    Map<Locale, org.exoplatform.portal.mop.State> loadDescriptions(String id) {
        try {
            return descriptionStorage.getDescriptions(id);
        } catch (Throwable t) {
            throw new ApiException("Failed to retrieve descriptions", t);
        }
    }

    String resolve(NodeContext<ApiNode> ctx) {
        if (i18nResolver == null) {
            PortalRequest request = PortalRequest.getInstance();
            Site site;
            if (request.getSiteId().equals(siteId)) {
                site = request.getSite();
            } else { // look it up
                site = request.getPortal().getSite(siteId);
            }

            if (site == null) {
                throw new ApiException("Could not resolve display name because site " + siteId + " could not be found.");
            }

            i18nResolver = new Navigation18NResolver(descriptionStorage, bundleManager, site.getLocale(), siteId);
        }

        return i18nResolver.resolveName(ctx.getState().getLabel(), ctx.getId(), ctx.getName());
    }

    NodeContext<ApiNode> getNodeContext(NodePath nodePath, NodeVisitor visitor) {
        NodeContext<ApiNode> ctx = loadNodeContext(Nodes.visitNodes(nodePath, visitor));
        for (String name : nodePath) {
            ctx = ctx.get(name);
            if (ctx == null)
                return null;
        }

        return ctx;
    }

    private NodeContext<ApiNode> loadNodeContext(NodeVisitor visitor) {
        return loadNodeContext(new NodeVisitorScope(visitor), null);
    }

    private NodeContext<ApiNode> loadNodeContext(Scope scope, NodeChangeListener<NodeContext<ApiNode>> listener) {
        try {
            return navigationService.loadNode(model, navCtx, scope, listener);
        } catch (Throwable e) {
            throw new ApiException("Failed to load node", e);
        }
    }

    void rebaseNodeContext(NodeContext<ApiNode> ctx, Scope scope, NodeChangeListener<NodeContext<ApiNode>> listener) {
        try {
            navigationService.rebaseNode(ctx, scope, listener);
        } catch (Throwable e) {
            throw new ApiException("Failed to refresh node", e);
        }
    }

    private void saveNodeContext(NodeContext<ApiNode> ctx, NodeChangeListener<NodeContext<ApiNode>> listener) {
        try {
            navigationService.saveNode(ctx, listener);
        } catch (Throwable e) {
            throw new ApiException("Failed to save node", e);
        }
    }

    private void save(NavigationContext ctx) {
        try {
            navigationService.saveNavigation(ctx);
        } catch (Throwable e) {
            throw new ApiException("Failed to save navigation", e);
        }
    }

    private void saveDisplayNames(NodeContext<ApiNode> ctx) {
        ApiNode node = ctx.getNode();
        if (node != null && node.isDisplayNameChanged()) {
            if (!node.getDisplayNames().isLocalized()) {
                Map<Locale, org.exoplatform.portal.mop.State> descriptions = loadDescriptions(ctx.getId());
                if (descriptions != null) {
                    setDescriptions(ctx.getId(), null);
                }
            } else {
                Map<Locale, org.exoplatform.portal.mop.State> descriptions = ObjectFactory.createDescriptions(node.getDisplayNames());
                setDescriptions(ctx.getId(), descriptions);
            }
        }

        for (NodeContext<ApiNode> c = ctx.getFirst(); c != null; c = c.getNext()) {
            saveDisplayNames(c);
        }
    }

    private void setDescriptions(String id, Map<Locale, org.exoplatform.portal.mop.State> descriptions) {
        try {
            descriptionStorage.setDescriptions(id, descriptions);
        } catch (Throwable t) {
            throw new ApiException("Failed to set descriptions", t);
        }
    }
}
