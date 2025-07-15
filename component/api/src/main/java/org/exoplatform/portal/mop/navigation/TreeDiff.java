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

import java.util.*;

import org.exoplatform.portal.tree.diff.*;

class TreeDiff<L, N> {

    /** . */
    private static final Comparator<String> COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.compareTo(o2);
        }
    };

    /** . */
    private final N node;

    /** . */
    private final NodeAdapter<L, N> adapter;

    /** . */
    private final NodeContext<N> context;

    TreeDiff(N node, NodeContext<N> context, NodeAdapter<L, N> adapter) {
        this.node = node;
        this.context = context;
        this.adapter = adapter;
    }

    public void perform() {
        NodeContextHierarchyAdapter nc = new NodeContextHierarchyAdapter();
        HierarchyDiff<List<String>, NodeContext<N>, L, N, String> diff = HierarchyDiff.create(nc, nc, adapter, adapter, COMPARATOR);
        LinkedList<NodeContext<N>> previousStack = new LinkedList<NodeContext<N>>();
        LinkedList<NodeContext<N>> parentStack = new LinkedList<NodeContext<N>>();
        HierarchyChangeIterator<List<String>, NodeContext<N>, L, N, String> i = diff.iterator(context, node);
        while (i.hasNext()) {
            HierarchyChangeType type = i.next();
            switch (type) {
                case ADDED: {
                    NodeState state = adapter.getState(i.getDestination());
                    String name = adapter.getName(i.getDestination());
                    NodeContext<N> parent = parentStack.peekLast();
                    NodeContext<N> previous = previousStack.peekLast();
                    NodeContext<N> added;
                    if (parent.get(name) != null) {
                        throw new HierarchyException(HierarchyError.ADD_CONCURRENTLY_ADDED_NODE);
                    } else {
                        if (previous != null) {
                            added = parent.add(previous.getIndex() + 1, name);
                        } else {
                            added = parent.add(0, name);
                        }
                        adapter.setHandle(i.getDestination(), added.handle);
                        previousStack.set(previousStack.size() - 1, added);
                    }
                    break;
                }
                case REMOVED:
                    i.getSource().removeNode();
                    break;
                case MOVED_OUT:
                    break;
                case MOVED_IN: {
                    NodeContext<N> moved = i.getSource();
                    N cd = i.getDestination();
                    N parent = adapter.getParent(cd);
                    String handle = adapter.getHandle(parent);
                    NodeContext<N> parent2 = context.getDescendant(handle);
                    N pre = adapter.getPrevious(parent, cd);
                    if (pre != null) {
                        String preHandle = adapter.getHandle(pre);
                        NodeContext<N> foo = context.getDescendant(preHandle);
                        parent2.add(foo.getIndex() + 1, moved);
                    } else {
                        parent2.add(0, moved);
                    }
                    previousStack.set(previousStack.size() - 1, moved);
                    break;
                }
                case KEEP:
                    NodeState s = adapter.getState(i.getDestination());
                    i.getSource().setState(s);
                    previousStack.set(previousStack.size() - 1, i.getSource());
                    break;
                case ENTER:
                    NodeContext<N> parent = i.getSource();
                    if (parent == null) {
                        // This is a trick : if the parent is null -> a node was added
                        // and this node should/must be the previous node
                        parentStack.addLast(previousStack.peekLast());
                    } else {
                        parentStack.addLast(parent);
                    }
                    previousStack.addLast(null);
                    break;
                case LEAVE:
                    parentStack.removeLast();
                    previousStack.removeLast();
                    break;
            }
        }
    }

    class NodeContextHierarchyAdapter implements HierarchyAdapter<List<String>, NodeContext<N>, String>, ListAdapter<List<String>, String> {

        @Override
        public String getHandle(NodeContext<N> node) {
            return node.getId();
        }

        @Override
        public List<String> getChildren(NodeContext<N> node) {
            ArrayList<String> ret = new ArrayList<String>(node.getSize());
            
            ListIterator<NodeContext<N>> iter = node.listIterator();
            while (iter.hasNext()) {
              ret.add(iter.next().getId());
            }
            return ret;
        }

        @Override
        public NodeContext<N> getDescendant(NodeContext<N> node, String handle) {
            return node.getDescendant(handle);
        }

        @Override
        public int size(List<String> list) {
            return list.size();
        }

        @Override
        public Iterator<String> iterator(List<String> list, boolean reverse) {
            if (reverse) {
                final ListIterator<String> i = list.listIterator(list.size());
                return new Iterator<String>() {
                    @Override
                    public boolean hasNext() {
                        return i.hasPrevious();
                    }
                    @Override
                    public String next() {
                        return i.previous();
                    }
                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            } else {
                return list.iterator();
            }
        }
    }
}
