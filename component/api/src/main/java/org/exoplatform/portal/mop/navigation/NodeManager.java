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

import java.util.Map;

import org.exoplatform.commons.utils.Safe;
import org.exoplatform.portal.mop.storage.NavigationStorage;

public class NodeManager {

    /** . */
    private final NavigationStorage store;

    public NodeManager(NavigationStorage store) {
        this.store = store;
    }

    public NavigationStorage getStore() {
        return store;
    }

    public <N> NodeContext<N> loadNode(
            NodeModel<N> model,
            String nodeId,
            Scope scope,
            NodeChangeListener<NodeContext<N>> listener) {
      NodeData data = store.loadNode(Safe.parseLong(nodeId));
      if (data != null) {
        NodeContext<N> context = new NodeContext<>(model, data);
        updateNode(context, scope, listener);
        return context;
      } else {
        return null;
      }
    }

    public <N> void diff(ModelAdapter<N> adapter, N node, NodeContext<N> context) {
        diff(new NodeAdapterImpl<N>(adapter), node, context);
    }

    public <L, N> void diff(NodeAdapter<L, N> adapter, N node, NodeContext<N> context) {
        TreeDiff<L, N> diff = new TreeDiff<>(node, context, adapter);
        diff.perform();
    }

    public <N> void updateNode(
            NodeContext<N> root,
            Scope scope,
            NodeChangeListener<NodeContext<N>> listener)
            throws NullPointerException, IllegalArgumentException, HierarchyException {
        Scope.Visitor visitor;
        if (scope != null) {
            visitor = new FederatingVisitor<N>(root.tree, root, scope);
        } else {
            visitor = root.tree;
        }
        updateTree(root.tree, visitor, listener);
    }

    public <N> void saveNode(NodeContext<N> context, NodeChangeListener<NodeContext<N>> listener) throws NullPointerException,
            HierarchyException {
        saveTree(context.tree, listener);
    }

    public <N> void rebaseNode(
            NodeContext<N> context,
            Scope scope,
            NodeChangeListener<NodeContext<N>> listener)
            throws HierarchyException {
        Scope.Visitor visitor;
        if (scope != null) {
            visitor = new FederatingVisitor<N>(context.tree.origin(), context, scope);
        } else {
            visitor = context.tree.origin();
        }
        rebaseTree(context.tree, visitor, listener);
    }

    private <N> void updateTree(
            TreeContext<N> tree,
            Scope.Visitor visitor,
            NodeChangeListener<NodeContext<N>> listener)
            throws NullPointerException, IllegalArgumentException, HierarchyException {
        if (tree.hasChanges()) {
            throw new IllegalArgumentException("For now we don't accept to update a context that has pending changes");
        }
        NodeData data = store.loadNode(Safe.parseLong(tree.root.data.id));
        if (data == null) {
            throw new HierarchyException(HierarchyError.UPDATE_CONCURRENTLY_REMOVED_NODE);
        }

        // Switch to edit mode
        tree.editMode = true;

        // Apply diff changes to the model
        try {

            TreeUpdate.perform(tree, NodeContextUpdateAdapter.<N> create(), data,
                    NodeDataUpdateAdapter.create(store), listener, visitor);
        } finally {
            // Disable edit mode
            tree.editMode = false;
        }
    }

    private <N> void rebaseTree(
            TreeContext<N> tree,
            Scope.Visitor visitor,
            NodeChangeListener<NodeContext<N>> listener)
            throws HierarchyException {
        if (!tree.hasChanges()) {
            updateTree(tree, visitor, listener);
        } else {
            TreeContext<N> rebased = rebase(tree, visitor);
            TreeUpdate.perform(tree, NodeContextUpdateAdapter.<N> create(), rebased.root,
                    NodeContextUpdateAdapter.<N> create(), listener, rebased);
        }
    }

    private <N> TreeContext<N> rebase(
            TreeContext<N> tree,
            Scope.Visitor visitor) throws HierarchyException {
        NodeData data = store.loadNode(Safe.parseLong(tree.root.getId()));
        if (data == null) {
            throw new HierarchyException(HierarchyError.UPDATE_CONCURRENTLY_REMOVED_NODE);
        }

        //
        TreeContext<N> rebased = new NodeContext<N>(tree.model, data).tree;

        //
        TreeUpdate.perform(rebased, NodeContextUpdateAdapter.<N> create(), data,
                NodeDataUpdateAdapter.create(store), null, visitor);

        //
        NodeChangeQueue<NodeContext<N>> changes = tree.getChanges();

        //
        NodeChangeListener<NodeContext<N>> merger = new TreeMerge<>(rebased, rebased);

        //
        if (changes != null) {
            changes.broadcast(merger);
        }

        //
        return rebased;
    }

    private <N> void saveTree(TreeContext<N> tree, NodeChangeListener<NodeContext<N>> listener) throws NullPointerException,
            HierarchyException {

        NodeData data = store.loadNode(Safe.parseLong(tree.root.data.id));
        if (data == null) {
            throw new HierarchyException(HierarchyError.UPDATE_CONCURRENTLY_REMOVED_NODE);
        }

        // Attempt to rebase
        TreeContext<N> rebased = rebase(tree, tree.origin());

        //
        NodePersister<N> persister = new NodePersister<>(store);

        //
        NodeChangeQueue<NodeContext<N>> changes = rebased.getChanges();
        if (changes != null) {
            changes.broadcast(persister);
            if (listener != null) {
              changes.broadcast(listener);
            }

            // Update the tree handles to the persistent values
            for (Map.Entry<String, String> entry : persister.toPersist.entrySet()) {
                NodeContext<N> a = tree.getNode(entry.getKey());
                a.handle = entry.getValue();
            }

            // Update data
            for (String ddd : persister.toUpdate) {
                NodeContext<N> a = tree.getNode(ddd);
                a.data = new NodeData(a);
                a.name = null;
                a.state = null;
            }

            // Clear changes
            changes.clear();
            tree.getChanges().clear();
        }

        // Update
        TreeUpdate.perform(tree, NodeContextUpdateAdapter.<N> create(), rebased.root, NodeContextUpdateAdapter.<N> create(),
                null, rebased);
    }
}
