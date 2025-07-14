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
package org.exoplatform.management.data;

import java.lang.reflect.Method;

import org.exoplatform.management.invocation.GetterInvoker;
import org.exoplatform.management.invocation.MethodInvoker;
import org.exoplatform.management.invocation.NoSuchMethodInvoker;
import org.exoplatform.management.invocation.SetterInvoker;
import org.exoplatform.management.spi.ManagedPropertyMetaData;

public class RestResourceProperty {

    /** . */
    final ManagedPropertyMetaData metaData;

    /** . */
    private final MethodInvoker setterInvoker;

    /** . */
    private final MethodInvoker getterInvoker;

    public RestResourceProperty(ManagedPropertyMetaData metaData) {
        Method getter = metaData.getGetter();
        MethodInvoker getterInvoker = getter != null ? new GetterInvoker(getter) : new NoSuchMethodInvoker();

        //
        Method setter = metaData.getSetter();
        MethodInvoker setterInvoker = setter != null ? new SetterInvoker(setter) : new NoSuchMethodInvoker();

        //
        this.metaData = metaData;
        this.setterInvoker = setterInvoker;
        this.getterInvoker = getterInvoker;
    }

    public String getName() {
        return metaData.getName();
    }

    public String getDescription() {
        return metaData.getDescription();
    }

    // Internal *********************************************************************************************************

    MethodInvoker getSetterInvoker() {
        return setterInvoker;
    }

    MethodInvoker getGetterInvoker() {
        return getterInvoker;
    }
}
