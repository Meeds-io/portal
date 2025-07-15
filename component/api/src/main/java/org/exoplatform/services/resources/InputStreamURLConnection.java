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
package org.exoplatform.services.resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;


/**
 * An URLConnection extension that use a local {@link java.io.InputStream} object.
 *
 */
public class InputStreamURLConnection extends URLConnection {

    private final InputStream in;

    /**
     * Creates a new connection.
     *
     * @param url the original URL
     * @param in the input stream
     * @throws IllegalArgumentException if the stream is null
     */
    public InputStreamURLConnection(URL url, InputStream in) throws IllegalArgumentException {
        super(url);

        //
        if (in == null) {
            throw new IllegalArgumentException("No null input stream accepted");
        }

        //
        this.in = in;
    }

    public void connect() throws IOException {
        // no op
    }

    public InputStream getInputStream() throws IOException {
        return in;
    }
}
