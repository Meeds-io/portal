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
package org.exoplatform.commons.serialization.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;


public final class ClassTypeModel<O> extends TypeModel<O> {

    /** . */
    private final ClassTypeModel<? super O> superType;

    /** . */
    private final Map<String, FieldModel<O, ?>> fields;

    /** . */
    private final Map<String, FieldModel<O, ?>> immutableFields;

    /** . */
    private final SerializationMode serializationMode;

    ClassTypeModel(Class<O> type, ClassTypeModel<? super O> superType, Map<String, FieldModel<O, ?>> fields,
            SerializationMode serializationMode) {
        super(type, superType);

        //
        this.superType = superType;
        this.fields = fields;
        this.immutableFields = Collections.unmodifiableMap(fields);
        this.serializationMode = serializationMode;
    }

    @Override
    public ClassTypeModel<? super O> getSuperType() {
        return superType;
    }

    public SerializationMode getSerializationMode() {
        return serializationMode;
    }

    public Collection<FieldModel<O, ?>> getFields() {
        return immutableFields.values();
    }

    public Map<String, FieldModel<O, ?>> getFieldMap() {
        return immutableFields;
    }

}
