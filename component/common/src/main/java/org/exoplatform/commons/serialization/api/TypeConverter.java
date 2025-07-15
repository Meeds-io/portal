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
package org.exoplatform.commons.serialization.api;

/**
 * A type converter that performs a bidirectional conversion between an external type and an internal type. The converter only
 * assure conversion of non null values.
 *
 */
public abstract class TypeConverter<E, I> {

    /**
     * Converts an external value to an internal value.
     *
     * @param external the external value
     * @return the the internal value
     * @throws Exception any conversion exception
     */
    public abstract I write(E external) throws Exception;

    /**
     * Converts an internal value to an external value.
     *
     * @param internal the internal value
     * @return the external value
     * @throws Exception any conversion exception
     */
    public abstract E read(I internal) throws Exception;

}
