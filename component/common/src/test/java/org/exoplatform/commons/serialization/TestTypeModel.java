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
package org.exoplatform.commons.serialization;

import java.util.Map;

import org.exoplatform.commons.serialization.model.ClassTypeModel;
import org.exoplatform.commons.serialization.model.FieldModel;
import org.exoplatform.commons.serialization.model.TypeDomain;
import org.exoplatform.commons.serialization.model.TypeModel;
import org.exoplatform.component.test.AbstractGateInTest;

public class TestTypeModel extends AbstractGateInTest {

    public void testBar() {
        TypeDomain domain = new TypeDomain();
        assertEquals(0, domain.getSize());
    }

    public void testFoo() {
        TypeDomain domain = new TypeDomain();
        assertType(String.class, domain.addTypeModel(String.class));
        assertEquals(7, domain.getSize());
        assertType(String.class, domain.getTypeModel(String.class));
        assertType(Object.class, domain.getTypeModel(Object.class));
        assertType(Integer.class, domain.getTypeModel(Integer.class));
        assertType(byte[].class, domain.getTypeModel(byte[].class));
        assertType(Number.class, domain.getTypeModel(Number.class));
        assertType(Byte.class, domain.getTypeModel(Byte.class));
        assertType(Boolean.class, domain.getTypeModel(Boolean.class));
    }

    public void testJuu() {
        TypeDomain domain = new TypeDomain();
        ClassTypeModel<A> aTM = (ClassTypeModel<A>) domain.addTypeModel(A.class);
        assertEquals(A.class.getName(), aTM.getName());
        /*
         * assertEquals(SetBuilder. create(domain.getTypeModel(Object.class)). with(domain.getTypeModel(Integer.class)).
         * with(domain.getTypeModel(Number.class)). with(domain.getTypeModel(char[].class)). with(aTM).
         * with(domain.getTypeModel(Boolean.class)). build(domain.getTypeModel(String.class)) , domain.getTypeModels());
         */
        Map<String, FieldModel<A, ?>> fieldMap = aTM.getFieldMap();
        assertEquals(3, fieldMap.size());
        FieldModel aFM = fieldMap.get("a");
        assertEquals("a", aFM.getName());
        assertEquals(domain.getTypeModel(String.class), aFM.getType());
        FieldModel bFM = fieldMap.get("b");
        assertEquals("b", bFM.getName());
        assertEquals(domain.getTypeModel(Integer.class), bFM.getType());
        FieldModel cFM = fieldMap.get("c");
        assertEquals("c", cFM.getName());
        assertEquals(domain.getTypeModel(Boolean.class), cFM.getType());
    }

    public void testDoubleAdd() {
        TypeDomain domain = new TypeDomain();
        ClassTypeModel<A> aTM1 = (ClassTypeModel<A>) domain.addTypeModel(A.class);
        ClassTypeModel<A> aTM2 = (ClassTypeModel<A>) domain.addTypeModel(A.class);
        assertSame(aTM2, aTM1);
    }

    private void assertType(Class<?> javaType, TypeModel typeModel) {
        assertTrue(typeModel instanceof ClassTypeModel);
        ClassTypeModel serializableTypeModel = (ClassTypeModel) typeModel;
        assertEquals(javaType, serializableTypeModel.getJavaType());
    }
}
