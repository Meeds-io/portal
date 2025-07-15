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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * {@link ClassLoader}, which delegates work to list of delegates (Delegating classloaders), which are provided from
 * constructor. Order of delegates is important (First has biggest priority)
 *
 */
public class DelegatingClassLoader extends ClassLoader {
    private final List<ClassLoader> delegates;

    private static final Log      log = ExoLogger.getLogger(DelegatingClassLoader.class);

    public DelegatingClassLoader(ClassLoader... delegates) {
        super(Thread.currentThread().getContextClassLoader());

        if (delegates == null || delegates.length == 0) {
            throw new IllegalArgumentException("Some delegating classloaders needs to be provided");
        }

        this.delegates = Arrays.asList(delegates);
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class cl;

        for (ClassLoader delegate : delegates) {
            try {
                cl = delegate.loadClass(name);
                if (cl != null) {
                    return cl;
                }
            } catch (ClassNotFoundException ignore) {
            }

            if (log.isTraceEnabled()) {
                log.trace("Class " + name + " not found with classloader: " + delegate + ". Trying other delegates");
            }
        }

        throw new ClassNotFoundException("Class " + name + " not found with any of delegates " + delegates);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        for (ClassLoader delegate : delegates) {
            InputStream is = delegate.getResourceAsStream(name);
            if (is != null) {
                return is;
            }

            if (log.isTraceEnabled()) {
                log.trace("Resource " + name + " not found with classloader: " + delegate + ". Trying other delegates");
            }
        }

        return null;
    }

    @Override
    public URL getResource(String name) {
        for (ClassLoader delegate : delegates) {
            URL url = delegate.getResource(name);
            if (url != null) {
                return url;
            }

            if (log.isTraceEnabled()) {
                log.trace("URL " + name + " not found with classloader: " + delegate + ". Trying other delegates");
            }
        }

        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        DelegateEnumeration<URL> ret = new DelegateEnumeration<URL>();
        for (ClassLoader delegate : delegates) {
            Enumeration<URL> e = delegate.getResources(name);
            if (e != null) {
                if (e.hasMoreElements()) {
                    ret.addEnumeration(e);
                    continue;
                }
            }
            if (log.isTraceEnabled()) {
                log.trace("URL " + name + " not found with classloader: " + delegate + ". Trying other delegates");
            }
        }

        return ret;
    }

    private static class DelegateEnumeration<E> implements Enumeration<E> {

        private ArrayList<Enumeration<E>> delegates = new ArrayList<Enumeration<E>>();
        private int current = 0;

        private void addEnumeration(Enumeration<E> enumeration) {
            delegates.add(enumeration);
        }

        @Override
        public boolean hasMoreElements() {
            Enumeration<E> e = getCurrent();
            return e != null && e.hasMoreElements();
        }

        @Override
        public E nextElement() {
            Enumeration<E> e = getCurrent();
            if (e != null)
                return e.nextElement();
            throw new NoSuchElementException();
        }

        private Enumeration<E> getCurrent() {
            if (delegates.size() == 0)
                return null;

            if (current >= delegates.size())
                return null;

            Enumeration<E> e = delegates.get(current);
            if (!e.hasMoreElements()) {
                current += 1;
                if (current < delegates.size()) {
                    return delegates.get(current);
                } else {
                    return null;
                }
            }
            return e;
        }
    }
}
