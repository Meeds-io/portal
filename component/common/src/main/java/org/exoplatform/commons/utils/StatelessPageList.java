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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


public abstract class StatelessPageList<E> extends AbstractSerializablePageList<E> implements Serializable {
    protected StatelessPageList(int pageSize) {
        super(pageSize);
    }

    protected abstract ListAccess<E> connect() throws Exception;

    // Serialization

    private void writeObject(ObjectOutputStream out) throws IOException {
        super.writeState(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
    }
}
