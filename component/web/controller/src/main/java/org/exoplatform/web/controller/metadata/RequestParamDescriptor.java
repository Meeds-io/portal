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
package org.exoplatform.web.controller.metadata;

import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.ControlMode;
import org.exoplatform.web.controller.router.ValueMapping;
import org.exoplatform.web.controller.router.ValueType;

public class RequestParamDescriptor extends ParamDescriptor {

    /** . */
    private String name;

    /** . */
    private String value;

    /** . */
    private ValueType valueType;

    /** . */
    private ControlMode controlMode;

    /** . */
    private ValueMapping valueMapping;

    public RequestParamDescriptor(QualifiedName qualifiedName) {
        super(qualifiedName);

        //
        this.value = null;
        this.controlMode = ControlMode.OPTIONAL;
        this.valueType = ValueType.LITERAL;
        this.valueMapping = ValueMapping.CANONICAL;
    }

    public RequestParamDescriptor(String qualifiedName) {
        super(qualifiedName);

        //
        this.value = null;
        this.controlMode = ControlMode.OPTIONAL;
        this.valueType = ValueType.LITERAL;
        this.valueMapping = ValueMapping.CANONICAL;
    }

    public RequestParamDescriptor named(String name) {
        this.name = name;
        return this;
    }

    public RequestParamDescriptor matchedByLiteral(String value) {
        this.value = value;
        this.valueType = ValueType.LITERAL;
        return this;
    }

    public RequestParamDescriptor matchedByPattern(String value) {
        this.value = value;
        this.valueType = ValueType.PATTERN;
        return this;
    }

    public RequestParamDescriptor required() {
        this.controlMode = ControlMode.REQUIRED;
        return this;
    }

    public RequestParamDescriptor optional() {
        this.controlMode = ControlMode.OPTIONAL;
        return this;
    }

    public RequestParamDescriptor neverEmpty() {
        this.valueMapping = ValueMapping.NEVER_EMPTY;
        return this;
    }

    public RequestParamDescriptor neverNull() {
        this.valueMapping = ValueMapping.NEVER_NULL;
        return this;
    }

    public RequestParamDescriptor canonical() {
        this.valueMapping = ValueMapping.CANONICAL;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    public ControlMode getControlMode() {
        return controlMode;
    }

    public void setControlMode(ControlMode controlMode) {
        this.controlMode = controlMode;
    }

    public ValueMapping getValueMapping() {
        return valueMapping;
    }

    public void setValueMapping(ValueMapping valueMapping) {
        this.valueMapping = valueMapping;
    }
}
