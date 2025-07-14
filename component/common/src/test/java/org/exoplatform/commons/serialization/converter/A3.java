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
package org.exoplatform.commons.serialization.converter;

import junit.framework.AssertionFailedError;

import org.exoplatform.commons.serialization.api.TypeConverter;

public class A3 extends TypeConverter<A1, A2> {

    /** . */
    public static TypeConverter<A1, A2> delegate;

    @Override
    public A2 write(A1 input) throws Exception {
        if (delegate == null) {
            throw new AssertionFailedError();
        }
        return delegate.write(input);
    }

    @Override
    public A1 read(A2 output) throws Exception {
        if (delegate == null) {
            throw new AssertionFailedError();
        }
        return delegate.read(output);
    }
}
