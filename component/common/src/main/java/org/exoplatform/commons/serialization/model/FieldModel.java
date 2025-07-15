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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;


public final class FieldModel<O, V> {

    /** . */
    private final TypeModel<O> owner;

    /** . */
    private final Field field;

    /** . */
    private final TypeModel<V> type;

    /** . */
    private boolean _transient;

    FieldModel(TypeModel<O> owner, Field field, TypeModel<V> type) {
        this.owner = owner;
        this.field = field;
        this.type = type;
        this._transient = Modifier.isTransient(field.getModifiers());
    }

    public TypeModel<O> getOwner() {
        return owner;
    }

    public String getName() {
        return field.getName();
    }

    public boolean isTransient() {
        return _transient;
    }

    public TypeModel<V> getType() {
        return type;
    }

    public V get(Object o) {
        try {
            Object value = field.get(o);
            if (value == null) {
                return null;
            } else {
                Class<V> valueType = type.getJavaType();
                if (valueType.isInstance(value)) {
                    return valueType.cast(value);
                } else {
                    throw new ClassCastException("Cannot cast value " + value + " with type " + value.getClass().getName()
                            + " to type " + valueType.getName());
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public void castAndSet(Object o, Object value) {
        V v = type.getJavaType().cast(value);
        set(o, v);
    }

    public void set(Object o, V value) {
        try {
            field.set(o, value);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public String toString() {
        return "FieldModel[name=" + field.getName() + ",owner=" + owner + "]";
    }
}
