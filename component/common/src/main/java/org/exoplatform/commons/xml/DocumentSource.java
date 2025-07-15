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
package org.exoplatform.commons.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


public abstract class DocumentSource {

    /** Some kind of identifier for error reporting. */
    private String identifier;

    private DocumentSource(String identifier) {
        if (identifier == null) {
            throw new NullPointerException("An identifier must be provided");
        }
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public abstract InputStream getStream() throws IOException;

    public static DocumentSource create(String identifier, final InputStream in) {
        return new DocumentSource(identifier) {
            @Override
            public InputStream getStream() throws IOException {
                return in;
            }
        };
    }

    public static DocumentSource create(final URL url) {
        return new DocumentSource(url.toString()) {
            @Override
            public InputStream getStream() throws IOException {
                return url.openStream();
            }
        };
    }

}
