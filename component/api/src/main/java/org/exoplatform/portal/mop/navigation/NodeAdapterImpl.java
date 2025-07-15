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

class NodeAdapterImpl<N> implements NodeAdapter<N, N> {

    /** . */
    private final ModelAdapter<N> delegate;

    /** . */
    private final IdentityHashMap<N, String> handles = new IdentityHashMap<N, String>();

    NodeAdapterImpl(ModelAdapter<N> delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName(N node) {
        return delegate.getName(node);
    }

    @Override
    public NodeState getState(N node) {
        return delegate.getState(node);
    }

    @Override
    public N getParent(N node) {
        return delegate.getParent(node);
    }

    @Override
    public N getPrevious(N parent, N node) {
        return delegate.getPrevious(parent, node);
    }

    @Override
    public void setHandle(N node, String handle) {
        handles.put(node, handle);
    }

    @Override
    public String getHandle(N node) {
        String handle = delegate.getId(node);
        if (handle == null) {
            handle = handles.get(node);
            if (handle == null) {
                handles.put(node, handle = UUID.randomUUID().toString());
            }
        }
        return handle;
    }

    @Override
    public N getChildren(N node) {
        return node;
    }

    @Override
    public N getDescendant(N node, String handle) {
        String h = getHandle(node);
        if (h.equals(handle)) {
            return node;
        } else {
            Iterator<N> children = delegate.getChildren(node, false);
            while (children.hasNext()) {
                N child = children.next();
                N descendant = getDescendant(child, handle);
                if (descendant != null) {
                    return descendant;
                }
            }
            return null;
        }
    }

    @Override
    public int size(N list) {
        return delegate.size(list);
    }

    @Override
    public Iterator<String> iterator(N list, boolean reverse) {
        final Iterator<N> iterator = delegate.getChildren(list, reverse);
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }
            @Override
            public String next() {
                N node = iterator.next();
                return getHandle(node);
            }
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
