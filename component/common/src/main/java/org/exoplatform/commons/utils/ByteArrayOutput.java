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
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;


public class ByteArrayOutput implements BinaryOutput {

    /** . */
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    /** . */
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public Charset getCharset() {
        return UTF_8;
    }

    public void write(byte b) throws IOException {
        baos.write(b);
    }

    public void write(byte[] bytes) throws IOException {
        baos.write(bytes);
    }

    public void write(byte[] bytes, int off, int len) throws IOException {
        baos.write(bytes, off, len);
    }

    public byte[] getBytes() {
        return baos.toByteArray();
    }

    public String getString() throws UnsupportedEncodingException {
        return baos.toString(UTF_8.name());
    }
}
