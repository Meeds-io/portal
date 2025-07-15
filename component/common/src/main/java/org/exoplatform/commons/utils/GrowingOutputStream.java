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
package org.exoplatform.commons.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class GrowingOutputStream extends OutputStream {

    private final OutputStream out;

    private ByteArrayOutputStream buffer;

    private boolean open;

    public GrowingOutputStream(OutputStream out, int initialBufferSize) {
        if (out == null) {
            throw new NullPointerException("No null output stream");
        }
        if (initialBufferSize < 0) {
            throw new IllegalArgumentException("No initial buffer size under 0");
        }

        this.out = out;
        this.buffer = new ByteArrayOutputStream(initialBufferSize);
        this.open = true;
    }

    @Override
    public void write(int b) throws IOException {
        if (!open) {
            throw new IOException("closed");
        }
        buffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (!open) {
            throw new IOException("closed");
        }
        buffer.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        if (!open) {
            throw new IOException("closed");
        }

        //
        out.write(buffer.toByteArray());

        //
        out.flush();
    }

    @Override
    public void close() throws IOException {
        if (!open) {
            throw new IOException("closed");
        }

        //
        out.write(buffer.toByteArray());

        //
        open = false;
        out.close();
    }

}
