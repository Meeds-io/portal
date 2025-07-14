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
package org.gatein.common.xml.stax;

import java.util.Collection;

import org.gatein.common.xml.stax.writer.StaxWriter;
import org.staxnav.StaxNavigator;

import static org.gatein.common.xml.stax.navigator.Exceptions.*;

public abstract class CollectionXmlHandler<T, N> implements XmlHandler<Collection<T>, N> {

    private final N collectionName;
    private final N itemName;

    public CollectionXmlHandler(N collectionName, N itemName) {
        this.collectionName = collectionName;
        this.itemName = itemName;
    }

    @Override
    public Collection<T> read(StaxNavigator<N> navigator) {
        if (!navigator.getName().equals(collectionName)) {
            throw expectedElement(navigator, collectionName);
        }
        Collection<T> collection = createCollection();

        N element = navigator.child();
        while (element != null) {
            if (!element.equals(itemName)) {
                throw expectedElement(navigator, itemName);
            }
            collection.add(readElement(navigator.fork()));
            element = navigator.sibling();
        }

        return collection;
    }

    @Override
    public void write(StaxWriter<N> writer, Collection<T> collection) {
        if (collection == null || collection.isEmpty()) return;

        writer.writeStartElement(collectionName);
        for (T element : collection) {
            writeElement(writer, element);
        }
        writer.writeEndElement();
    }

    protected abstract T readElement(StaxNavigator<N> navigator);

    protected abstract void writeElement(StaxWriter<N> writer, T object);

    protected abstract Collection<T> createCollection();
}
