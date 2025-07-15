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
package org.gatein.portal.controller.resource.script;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * <p>
 * Extends an {@link HashMap} to add convenient method for safely adding a fetch mode to a map. The method
 * {@link #add(Object, FetchMode)} will add the mode only if the new mode implies the previous mode in the map.
 * </p>
 *
 */
public class FetchMap<E> extends LinkedHashMap<E, FetchMode> {

    public FetchMap() {
    }

    public FetchMap(Map<? extends E, ? extends FetchMode> m) {
        super(m);
    }

    public boolean add(E element, FetchMode mode) throws NullPointerException {
        if (element == null) {
            throw new NullPointerException("No null element accepted");
        }

        //
        FetchMode prev = get(element);
        if (prev == null) {
            put(element, mode);
            return true;
        } else if (mode != null && mode.compareTo(prev) >= 0) {
            put(element, mode);
            return true;
        } else {
            return false;
        }
    }

    public boolean add(E element) throws NullPointerException {
        return add(element, null);
    }

    public void addAll(Map<E, FetchMode> m) {
        for (E elem : m.keySet()) {
            add(elem, m.get(elem));
        }
    }
}
