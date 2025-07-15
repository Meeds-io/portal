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
package org.gatein.common.xml.stax.writer;

import javax.xml.namespace.QName;

import org.staxnav.StaxNavException;

public interface StaxWriter<N> {
    /**
     * Writes the start tag of an xml element. Requires that an element has been started first.
     *
     * @param element element to start
     * @return StaxWriter
     * @throws org.staxnav.StaxNavException if an exception occurs
     */
    StaxWriter<N> writeStartElement(N element) throws StaxNavException;

    /**
     * Writes an attribute for an xml element. Requires that an element has been started first.
     *
     * @param name the name of the attribute
     * @param value the value of the attribute
     * @return StaxWriter
     * @throws org.staxnav.StaxNavException if an exception occurs
     */
    StaxWriter<N> writeAttribute(String name, String value) throws StaxNavException;

    /**
     * Writes an attribute for an xml element. Requires that an element has been started first.
     *
     * @param name QName object representing the name of the attribute
     * @param value the value of the attribute
     * @return StaxWriter
     * @throws org.staxnav.StaxNavException if an exception occurs
     */
    StaxWriter<N> writeAttribute(QName name, String value) throws StaxNavException;

    /**
     * Writes xml content. Requires an xml element has been started first.
     *
     * @param content content to be written
     * @return StaxWriter
     * @throws org.staxnav.StaxNavException if an exception occurs
     */
    StaxWriter<N> writeContent(String content) throws StaxNavException;

    /**
     * Writes xml content based on the ValueType responsible for converting the content to string. Requires an xml element has
     * been started first.
     *
     * @param valueType object responsible for writing content to string
     * @param content content to be written
     * @return StaxWriter
     * @throws org.staxnav.StaxNavException if an exception occurs
     */
    <V> StaxWriter<N> writeContent(WritableValueType<V> valueType, V content) throws StaxNavException;

    /**
     * Writes an end tag for the previously started element. Requires that an element has been started first.
     *
     * @return StaxWriter
     * @throws org.staxnav.StaxNavException if an exception occurs
     */
    StaxWriter<N> writeEndElement() throws StaxNavException;

    /**
     * Convenience method for calling <code>writeStartElement</code>, <code>writeContent</code>, <code>writeEndElement</code>
     *
     * @param element element to write
     * @param content content to be written
     * @return StaxWriter
     * @throws org.staxnav.StaxNavException if an exception occurs
     */
    <V> StaxWriter<N> writeElement(N element, String content) throws StaxNavException;

    /**
     * Convenience method for calling <code>writeStartElement</code>, <code>writeContent</code>, <code>writeEndElement</code>
     *
     * @param element element to write
     * @param valueType object responsible for writing content to string
     * @param content content to be written
     * @return StaxWriter
     * @throws org.staxnav.StaxNavException if an exception occurs
     */
    <V> StaxWriter<N> writeElement(N element, WritableValueType<V> valueType, V content) throws StaxNavException;

    /**
     * Writes the namespace. If prefix is an empty string, "xmlns", or null this will delegate to writeDefaultNamespace
     *
     * @param prefix the prefix to bind the namespace to
     * @param uri the uri to bind the prefix to
     * @return StaxWriter
     * @throws org.staxnav.StaxNavException if an exception occurs
     */
    StaxWriter<N> writeNamespace(String prefix, String uri) throws StaxNavException;

    /**
     * Writes the default namespace
     *
     * @param uri the uri to bind the default namespace to
     * @return StaxWriter
     * @throws org.staxnav.StaxNavException if an exception occurs
     */
    StaxWriter<N> writeDefaultNamespace(String uri) throws StaxNavException;

    /**
     * Writes an xml comment
     *
     * @param comment the comment to write
     * @return StaxWriter
     * @throws org.staxnav.StaxNavException if an exception occurs
     */
    StaxWriter<N> writeComment(String comment) throws StaxNavException;

    /**
     * Writes a cdata section
     *
     * @param cdata content of the cdata
     * @return StaxWriter
     * @throws org.staxnav.StaxNavException if an exception occurs
     */
    StaxWriter<N> writeCData(String cdata) throws StaxNavException;

    /**
     * Calling finish will flush and close the underlying stream. It will also call any endElements for you if they were never
     * explicitly called.
     *
     * @throws org.staxnav.StaxNavException if an exception occurs
     */
    void finish() throws StaxNavException;
}
