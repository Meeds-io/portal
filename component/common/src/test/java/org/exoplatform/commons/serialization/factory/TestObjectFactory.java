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
package org.exoplatform.commons.serialization.factory;

import java.io.InvalidClassException;

import org.exoplatform.commons.serialization.SerializationContext;
import org.exoplatform.commons.serialization.model.TypeDomain;
import org.exoplatform.component.test.AbstractGateInTest;

public class TestObjectFactory extends AbstractGateInTest {

    public void testCustomFactory() throws Exception {
        TypeDomain domain = new TypeDomain();
        domain.addTypeModel(A2.class);
        SerializationContext context = new SerializationContext(domain);
        context.addFactory(new A1());
        A2 a2 = new A2();
        assertSame(A1.instance, context.clone(a2));
    }

    public void testFactoryThrowsException() throws Exception {
        TypeDomain domain = new TypeDomain();
        domain.addTypeModel(B.class);
        SerializationContext context = new SerializationContext(domain);
        B b = new B(false);
        try {
            context.clone(b);
            fail();
        } catch (InvalidClassException e) {
        }
    }
}
