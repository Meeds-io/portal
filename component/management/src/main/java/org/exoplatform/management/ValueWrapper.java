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
package org.exoplatform.management;

/**
 * Temporary class until eXo JSON marshaller supports non java bean objects.
 *
 */
public class ValueWrapper<T> {

    public static <T> ValueWrapper<T> wrap(T value) {
        return new ValueWrapper<T>(value);
    }

    /** . */
    private final T value;

    public ValueWrapper(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }
}
