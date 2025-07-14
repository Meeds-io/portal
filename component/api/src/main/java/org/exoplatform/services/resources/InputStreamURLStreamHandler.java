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
import java.net.URLStreamHandler;


/**
 * An URLStreamHandler extension that use a local {@link java.io.InputStream} object. This object will always use the stream
 * provided and nothing else. So the life time of an instance is very limited.
 *
 */
public class InputStreamURLStreamHandler extends URLStreamHandler {

    private final InputStream in;

    /**
     * Creates a new handler.
     *
     * @param in the input stream
     * @throws IllegalArgumentException if the stream is null
     */
    public InputStreamURLStreamHandler(InputStream in) throws IllegalArgumentException {
        if (in == null) {
            throw new IllegalArgumentException("No null input stream accepted");
        }

        //
        this.in = in;
    }

    protected URLConnection openConnection(URL u) throws IOException {
        return new InputStreamURLConnection(u, in);
    }

}
