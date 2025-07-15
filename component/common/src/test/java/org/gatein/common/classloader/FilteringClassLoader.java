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
package org.gatein.common.classloader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;

/**
 * A URLClassLoader that skips delegation of specified resources, and classes to parent.
 */
public class FilteringClassLoader extends URLClassLoader {

    private HashSet<String> filtered = new HashSet<String>();

    public FilteringClassLoader(ClassLoader parent, URL[] urls, String [] filteredNames) {
        super(urls, parent);
        for (String name: filteredNames) {
            filtered.add(name);
        }
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        if (filtered.contains(name)) {
            Class localClass = findLoadedClass(name);
            if (localClass == null) {
                localClass = findClass(name);
            }
            if (resolve) {
                resolveClass(localClass);
            }
            return localClass;
        }

        return super.loadClass(name, resolve);
    }
}