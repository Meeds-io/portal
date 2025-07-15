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
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;

import org.gatein.common.xml.stax.writer.StaxWriter;
import org.gatein.common.xml.stax.writer.StaxWriterImpl;
import org.gatein.common.xml.stax.writer.formatting.NoOpFormatter;
import org.gatein.common.xml.stax.writer.formatting.XmlStreamingFormatter;
import org.staxnav.EnumElement;
import org.staxnav.Naming;
import org.staxnav.StaxNavException;

public class StaxWriterBuilderImpl implements StaxWriterBuilder {
    private XMLStreamWriter writer;
    private Object output;
    private String outputEncoding;
    private String version;
    private String encoding;

    private XmlStreamingFormatter formatter;

    private Map<String, Object> properties = new HashMap<String, Object>();
    private Map<String, Object> supportedProperties = new HashMap<String, Object>();

    public StaxWriterBuilder withProperty(String name, Object value) {
        if (name == null)
            throw new IllegalArgumentException("name is null");
        if (value == null)
            throw new IllegalArgumentException("value is null");

        properties.put(name, value);
        return this;
    }

    public StaxWriterBuilder withPropertyIfSupported(String name, Object value) {
        if (name == null)
            throw new IllegalArgumentException("name is null");
        if (value == null)
            throw new IllegalArgumentException("value is null");

        supportedProperties.put(name, value);
        return this;
    }

    public StaxWriterBuilder withOutputStream(OutputStream outputStream) {
        if (outputStream == null)
            throw new IllegalArgumentException("outputStream is null");

        output = outputStream;
        return this;
    }

    public StaxWriterBuilder withOutputStream(OutputStream outputStream, String encoding) {
        if (outputStream == null)
            throw new IllegalArgumentException("outputStream is null");
        if (encoding == null)
            throw new IllegalArgumentException("encoding is null");

        output = outputStream;
        outputEncoding = encoding;
        return this;
    }

    public StaxWriterBuilder withWriter(Writer writer) {
        if (writer == null)
            throw new IllegalArgumentException("writer is null");

        this.output = writer;
        return this;
    }

    public StaxWriterBuilder withResult(Result result) {
        if (result == null)
            throw new IllegalArgumentException("result is null");

        output = result;
        return this;
    }

    public StaxWriterBuilder withXmlStreamWriter(XMLStreamWriter writer) {
        if (writer == null)
            throw new IllegalArgumentException("writer is null");

        this.writer = writer;
        return this;
    }

    public StaxWriterBuilder withEncoding(String encoding) {
        if (encoding == null)
            throw new IllegalArgumentException("encoding is null");

        this.encoding = encoding;
        return this;
    }

    public StaxWriterBuilder withVersion(String version) {
        if (version == null)
            throw new IllegalArgumentException("version is null");

        this.version = version;
        return this;
    }

    public StaxWriterBuilder withFormatting(XmlStreamingFormatter formatter) {
        this.formatter = formatter;
        return this;
    }

    public <E extends Enum<E> & EnumElement<E>> StaxWriter<E> build(Class<E> enumeratedClass) {
        Naming<E> naming;
        if (EnumElement.class.isAssignableFrom(enumeratedClass)) {
            naming = new Naming.Enumerated.Mapped<E>(enumeratedClass, null);
        } else {
            naming = new Naming.Enumerated.Simple<E>(enumeratedClass, null);
        }

        return build(naming);
    }

    public <N> StaxWriter<N> build(Naming<N> naming) throws StaxNavException, IllegalStateException {
        if (naming == null)
            throw new IllegalArgumentException("naming is null");

        if (writer == null && output == null)
            throw new IllegalStateException(
                    "Cannot build stax writer. Try calling withOutputStream/withWriter or pass in own XMLStreamWriter.");

        if (writer == null) {
            // TODO: Create solution to properly cache XMLOutputFactory
            XMLOutputFactory factory = XMLOutputFactory.newInstance();

            // Set properties
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                factory.setProperty(entry.getKey(), entry.getValue());
            }

            // Set properties if supported
            for (Map.Entry<String, Object> entry : supportedProperties.entrySet()) {
                String name = entry.getKey();
                if (factory.isPropertySupported(name)) {
                    factory.setProperty(name, entry.getValue());
                }
            }

            if (output instanceof OutputStream) {
                if (outputEncoding != null) {
                    try {
                        writer = factory.createXMLStreamWriter((OutputStream) output, outputEncoding);
                    } catch (XMLStreamException e) {
                        throw new StaxNavException(e);
                    }
                } else {
                    try {
                        writer = factory.createXMLStreamWriter((OutputStream) output);
                    } catch (XMLStreamException e) {
                      throw new StaxNavException(e);
                    }
                }
            } else if (output instanceof Writer) {
                try {
                    writer = factory.createXMLStreamWriter((Writer) output);
                } catch (XMLStreamException e) {
                  throw new StaxNavException(e);
                }
            } else if (output instanceof Result) {
                try {
                    writer = factory.createXMLStreamWriter((Result) output);
                } catch (XMLStreamException e) {
                  throw new StaxNavException(e);
                }
            } else {
                throw new IllegalStateException("Unknown output: " + output); // should never happen...
            }
        }

        return new StaxWriterImpl<N>(naming, writer, formatter, encoding, version);
    }
}
