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
package org.gatein.common.xml.stax.navigator.builder;

import java.io.InputStream;
import java.io.Reader;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;

import org.staxnav.Naming;
import org.staxnav.StaxNavException;
import org.staxnav.StaxNavigator;

public interface StaxNavBuilder {
    StaxNavBuilder withProperty(String name, Object value);

    StaxNavBuilder withPropertyIfSupported(String name, Object value);

    StaxNavBuilder withInputStream(InputStream inputStream);

    StaxNavBuilder withInputStream(InputStream inputStream, String encoding);

    StaxNavBuilder withReader(Reader reader);

    StaxNavBuilder withSource(Source source);

    StaxNavBuilder withXmlStreamReader(XMLStreamReader reader);

    <N> StaxNavigator<N> build(Naming<N> naming) throws StaxNavException, IllegalStateException;
}
