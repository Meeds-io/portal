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
package org.exoplatform.portal.config.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.gatein.common.io.IOTools;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.impl.UnmarshallingContext;

public class ModelUnmarshaller {

    public static <T> UnmarshalledObject<T> unmarshall(Class<T> type, InputStream in) throws Exception {
        return unmarshall(type, IOTools.getBytes(in));
    }

    public static <T> UnmarshalledObject<T> unmarshall(Class<T> type, byte[] bytes) throws Exception {
        ByteArrayInputStream baos = new ByteArrayInputStream(bytes);

        //
        IBindingFactory bfact = BindingDirectory.getFactory(type);
        UnmarshallingContext uctx = (UnmarshallingContext) bfact.createUnmarshallingContext();
        uctx.setDocument(baos, null, "UTF-8", false);
        T obj = type.cast(uctx.unmarshalElement());

        // Find out version
        XMLInputFactory factory = XMLInputFactory.newInstance();
        baos.reset();

        Version version = Version.UNKNOWN;
        XMLStreamReader reader = factory.createXMLStreamReader(baos);
        try {
          while (reader.hasNext()) {
            int next = reader.next();
            if (next == XMLStreamReader.START_ELEMENT) {
              QName name = reader.getName();
              String uri = name.getNamespaceURI();
              if (uri != null) {
                version = Version.forURI(uri);
              }
              break;
            }
          }
        } finally {
          reader.close();
        }

        //
        return new UnmarshalledObject<>(version, obj);
    }

}
