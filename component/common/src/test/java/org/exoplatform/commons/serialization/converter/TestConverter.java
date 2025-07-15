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
package org.exoplatform.commons.serialization.converter;

import java.io.InvalidObjectException;

import junit.framework.AssertionFailedError;

import org.exoplatform.commons.serialization.SerializationContext;
import org.exoplatform.commons.serialization.api.TypeConverter;
import org.exoplatform.commons.serialization.model.TypeDomain;
import org.exoplatform.component.test.AbstractGateInTest;

public class TestConverter extends AbstractGateInTest {

    public void testConvertSerializedType() throws Exception {
        TypeDomain domain = new TypeDomain();
        domain.addTypeModel(A1.class);
        A1 a = new A1("foo");

        //
        A3.delegate = new TypeConverter<A1, A2>() {
            @Override
            public A2 write(A1 input) throws Exception {
                return new A2(input.state);
            }

            @Override
            public A1 read(A2 output) throws Exception {
                return new A1(output.state);
            }
        };
        SerializationContext context = new SerializationContext(domain);
        a = context.clone(a);
        assertEquals("foo", a.state);
    }

    public void testConvertSerializableType() throws Exception {
        TypeDomain domain = new TypeDomain();
        domain.addTypeModel(B1.class);
        B1 b = new B1("foo");

        //
        SerializationContext context = new SerializationContext(domain);
        b = context.clone(b);
        assertEquals("foo", b.state);
    }

    public void testConverterWriteThrowsException() throws Exception {
        TypeDomain domain = new TypeDomain();
        domain.addTypeModel(A1.class);
        A1 a = new A1("foo");

        //
        final Exception e = new Exception();
        A3.delegate = new TypeConverter<A1, A2>() {
            @Override
            public A2 write(A1 input) throws Exception {
                throw e;
            }

            @Override
            public A1 read(A2 output) throws Exception {
                throw new AssertionFailedError();
            }
        };
        SerializationContext context = new SerializationContext(domain);
        try {
            a = context.clone(a);
            fail();
        } catch (InvalidObjectException ioe) {
            assertSame(e, ioe.getCause());
        }
    }

    public void testConverterWriteReturnsNull() throws Exception {
        TypeDomain domain = new TypeDomain();
        domain.addTypeModel(A1.class);
        A1 a = new A1("foo");

        //
        A3.delegate = new TypeConverter<A1, A2>() {
            @Override
            public A2 write(A1 input) throws Exception {
                return null;
            }

            @Override
            public A1 read(A2 output) throws Exception {
                throw new AssertionFailedError();
            }
        };
        SerializationContext context = new SerializationContext(domain);
        try {
            a = context.clone(a);
            fail();
        } catch (InvalidObjectException e) {
        }
    }

    public void testConverterReadReturnsNull() throws Exception {
        TypeDomain domain = new TypeDomain();
        domain.addTypeModel(A1.class);
        A1 a = new A1("foo");

        //
        A3.delegate = new TypeConverter<A1, A2>() {
            @Override
            public A2 write(A1 input) throws Exception {
                return new A2(input.state);
            }

            @Override
            public A1 read(A2 output) throws Exception {
                return null;
            }
        };
        SerializationContext context = new SerializationContext(domain);
        try {
            a = context.clone(a);
            fail();
        } catch (InvalidObjectException e) {
        }
    }

    public void testConverterReadThrowsException() throws Exception {
        TypeDomain domain = new TypeDomain();
        domain.addTypeModel(A1.class);
        A1 a = new A1("foo");

        //
        final Exception e = new Exception();
        A3.delegate = new TypeConverter<A1, A2>() {
            @Override
            public A2 write(A1 input) throws Exception {
                return new A2(input.state);
            }

            @Override
            public A1 read(A2 output) throws Exception {
                throw e;
            }
        };
        SerializationContext context = new SerializationContext(domain);
        try {
            a = context.clone(a);
            fail();
        } catch (InvalidObjectException ioe) {
            assertSame(e, ioe.getCause());
        }
    }
}
