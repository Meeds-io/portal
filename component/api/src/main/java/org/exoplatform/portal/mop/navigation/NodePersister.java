/*
 * Copyright (C) 2011 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.mop.navigation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.exoplatform.commons.utils.Safe;
import org.exoplatform.portal.mop.storage.NavigationStorage;

/**
* @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
*/
class NodePersister<N> extends NodeChangeListener.Base<NodeContext<N>> {

    /** The persisted handles to assign. */
    final Map<String, String> toPersist;

    /** The handles to update. */
    final Set<String> toUpdate;

    /** . */
    private final NavigationStorage persistence;

    NodePersister(NavigationStorage persistence) {
        this.persistence = persistence;
        this.toPersist = new HashMap<>();
        this.toUpdate = new HashSet<>();
    }

    @Override
    public void onCreate(NodeContext<N> target, NodeContext<N> parent, NodeContext<N> previous, String name)
            throws HierarchyException {

        //
        NodeData[] result = persistence.createNode(Safe.parseLong(parent.data.id), previous != null ? Safe.parseLong(previous.data.id) : null, name, target.state);

        //
        parent.data = result[0];

        // Save the handle
        toPersist.put(target.handle, result[1].id);

        //
        target.data = result[1];
        target.handle = target.data.id;
        target.name = null;
        target.state = null;

        //
        toUpdate.add(parent.handle);
        toUpdate.add(target.handle);
    }

    @Override
    public void onDestroy(NodeContext<N> target, NodeContext<N> parent) {

        // Recurse on children
        if (target.isExpanded()) {
            for (NodeContext<N> child = target.getFirst(); child != null; child = child.getNext()) {
                onDestroy(child, target);
            }
        }

        //
        parent.data = persistence.destroyNode(Safe.parseLong(target.data.id));

        //
        toUpdate.add(parent.handle);
        toPersist.values().remove(target.data.id);
        toUpdate.remove(target.data.id);
    }

    @Override
    public void onUpdate(NodeContext<N> source, NodeState state) throws HierarchyException {

        //
        source.data = persistence.updateNode(Safe.parseLong(source.data.id), state);
        source.state = null;


        //
        toUpdate.add(source.handle);
    }

    @Override
    public void onMove(NodeContext<N> target, NodeContext<N> from, NodeContext<N> to, NodeContext<N> previous)
            throws HierarchyException {

        NodeData[] result = persistence.moveNode(Safe.parseLong(target.data.id), Safe.parseLong(from.data.id), Safe.parseLong(to.data.id), previous != null ? Safe.parseLong(previous.data.id) : null);


        //
        from.data = result[1];
        to.data = result[2];
        target.data = result[0];

        //
        toUpdate.add(target.handle);
        toUpdate.add(from.handle);
        toUpdate.add(to.handle);
    }

    @Override
    public void onRename(NodeContext<N> target, NodeContext<N> parent, String name) throws HierarchyException {

        NodeData[] result = persistence.renameNode(Safe.parseLong(target.data.id), Safe.parseLong(parent.data.id), name);

        //
        target.data = result[0];
        target.name = null;
        parent.data = result[1];

        //
        toUpdate.add(parent.handle);
        toUpdate.add(target.handle);
    }
}
