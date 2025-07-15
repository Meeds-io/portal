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

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;


class Retrieval<K, V, C> implements Callable<V> {

    /** . */
    private final C context;

    /** . */
    private final K key;

    /** . */
    private final FutureCache<K, V, C> cache;

    /** . */
    final FutureTask<V> future;

    /** Avoid reentrancy. */
    transient Thread current;

    public Retrieval(C context, K key, FutureCache<K, V, C> cache) {
        this.key = key;
        this.context = context;
        this.future = new FutureTask<V>(this);
        this.cache = cache;
        this.current = null;
    }

    public V call() throws Exception {
        // Retrieve the value from the loader
        V value = cache.loader.retrieve(context, key);

        //
        if (value != null) {
            // Cache it, it is made available to other threads (unless someone removes it)
            cache.putOnly(key, value);

            // Return value
            return value;
        } else {
            return null;
        }
    }
}
