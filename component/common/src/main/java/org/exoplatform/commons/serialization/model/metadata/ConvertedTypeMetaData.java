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
package org.exoplatform.commons.serialization.model.metadata;

import org.exoplatform.commons.serialization.api.TypeConverter;

public class ConvertedTypeMetaData extends TypeMetaData {

    /** . */
    private final Class<? extends TypeConverter<?, ?>> converterClass;

    public ConvertedTypeMetaData(String name, Class<? extends TypeConverter<?, ?>> converterClass) {
        super(name);

        //
        if (converterClass == null) {
            throw new NullPointerException();
        }

        //
        this.converterClass = converterClass;
    }

    public Class<? extends TypeConverter<?, ?>> getConverterClass() {
        return converterClass;
    }

    @Override
    public String toString() {
        return "ConvertedTypeMetaData[name=" + name + ",converterClass=" + converterClass + "]";
    }
}
