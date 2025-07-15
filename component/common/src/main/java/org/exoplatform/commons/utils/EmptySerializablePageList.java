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
package org.exoplatform.commons.utils;

import java.io.Serializable;


public class EmptySerializablePageList<E> extends AbstractSerializablePageList<E> implements Serializable {

    /** . */
    private static final EmptySerializablePageList instance = new EmptySerializablePageList();

    /** . */
    private final transient ListAccess<E> listAccess = new ListAccess<E>() {
        public E[] load(int index, int length) throws Exception {
            throw new UnsupportedOperationException("Should not be called");
        }

        public int getSize() throws Exception {
            return 0;
        }
    };

    public EmptySerializablePageList() {
        super(10);
    }

    @Override
    protected ListAccess<E> connect() throws Exception {
        return listAccess;
    }

    public static <E> PageList<E> get() {
        // Cast OK
        return (PageList<E>) instance;
    }

    private Object readResolve() {
        return instance;
    }
}
