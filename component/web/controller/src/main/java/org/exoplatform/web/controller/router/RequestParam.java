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
package org.exoplatform.web.controller.router;

import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.metadata.RequestParamDescriptor;

class RequestParam extends Param {

    static RequestParam create(RequestParamDescriptor descriptor, Router router) {
        if (descriptor == null) {
            throw new NullPointerException("No null descriptor accepted");
        }

        //
        Regex matchValue = null;
        if (descriptor.getValue() != null) {
            PatternBuilder matchValueBuilder = new PatternBuilder();
            matchValueBuilder.expr("^");
            if (descriptor.getValueType() == ValueType.PATTERN) {
                matchValueBuilder.expr(descriptor.getValue());
            } else {
                matchValueBuilder.literal(descriptor.getValue());
            }
            matchValueBuilder.expr("$");
            matchValue = router.compile(matchValueBuilder.build());
        }

        //
        return new RequestParam(descriptor.getQualifiedName(), descriptor.getName(), matchValue, descriptor.getControlMode(),
                descriptor.getValueMapping());
    }

    /** . */
    final QualifiedName name;

    /** . */
    final String matchName;

    /** . */
    final Regex matchPattern;

    /** . */
    final ControlMode controlMode;

    /** . */
    final ValueMapping valueMapping;

    RequestParam(QualifiedName name, String matchName, Regex matchPattern, ControlMode controlMode, ValueMapping valueMapping) {
        super(name);

        //
        if (matchName == null) {
            throw new NullPointerException("No null match name accepted");
        }
        if (controlMode == null) {
            throw new NullPointerException("No null control mode accepted");
        }
        if (valueMapping == null) {
            throw new NullPointerException("No null value mapping accepted");
        }

        //
        this.name = name;
        this.matchName = matchName;
        this.matchPattern = matchPattern;
        this.controlMode = controlMode;
        this.valueMapping = valueMapping;
    }

    boolean matchValue(String value) {
        return matchPattern == null || matchPattern.matcher().matches(value);
    }
}
