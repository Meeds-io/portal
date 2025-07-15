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
package org.gatein.common.xml.stax.writer.builder;

import java.io.OutputStream;
import java.io.Writer;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;

import org.gatein.common.xml.stax.writer.StaxWriter;
import org.gatein.common.xml.stax.writer.formatting.XmlStreamingFormatter;
import org.staxnav.EnumElement;
import org.staxnav.Naming;
import org.staxnav.StaxNavException;

public interface StaxWriterBuilder {
    StaxWriterBuilder withProperty(String name, Object value);

    StaxWriterBuilder withPropertyIfSupported(String name, Object value);

    StaxWriterBuilder withEncoding(String encoding);

    StaxWriterBuilder withVersion(String version);

    StaxWriterBuilder withFormatting(XmlStreamingFormatter formatter);

    StaxWriterBuilder withOutputStream(OutputStream outputStream);

    StaxWriterBuilder withOutputStream(OutputStream outputStream, String encoding);

    StaxWriterBuilder withWriter(Writer writer);

    StaxWriterBuilder withResult(Result result);

    StaxWriterBuilder withXmlStreamWriter(XMLStreamWriter writer);

    <N> StaxWriter<N> build(Naming<N> naming) throws StaxNavException, IllegalStateException;

    <E extends Enum<E> & EnumElement<E>> StaxWriter<E> build(Class<E> enumeratedType) throws StaxNavException,
            IllegalStateException;
}
