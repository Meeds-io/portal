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
package org.exoplatform.commons.cache.future;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class FutureMap<K extends Serializable, V, C> extends FutureCache<K, V, C> {

    /** . */
    final Map<K, V> data;

    public FutureMap(Loader<K, V, C> loader) {
        super(loader);

        //
        this.data = new ConcurrentHashMap<K, V>();
    }

    public Set<K> getKeys() {
      return data.keySet();
    }

    public void clear() {
        data.clear();
    }

    public void remove(K key) {
        data.remove(key);
    }

    @Override
    protected V get(K key) {
        return data.get(key);
    }

    @Override
    protected void put(K key, V value) {
        data.put(key, value);
    }

    @Override
    protected void putOnly(K key, V value)
    {
        put(key, value);
    }
}
