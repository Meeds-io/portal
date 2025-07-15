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
package org.exoplatform.portal.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;

import org.exoplatform.commons.utils.BinaryOutput;
import org.exoplatform.commons.utils.CharsetCharEncoder;
import org.exoplatform.commons.utils.CharsetTextEncoder;
import org.exoplatform.commons.utils.TableCharEncoder;
import org.exoplatform.commons.utils.TextEncoder;

public class CachedStylesheet {

    /** The optimized encoder. */
    private static final TextEncoder encoder = new CharsetTextEncoder(new TableCharEncoder(CharsetCharEncoder.getUTF8()));

    /** . */
    private final String text;

    /** . */
    private final byte[] bytes;

    private long lastModified;

    public CachedStylesheet(String text) {
        // Compute encoded bytes
        byte[] bytes;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(text.length() * 2);
            encoder.encode(text, 0, text.length(), baos);
            baos.flush();
            bytes = baos.toByteArray();
        } catch (IOException e) {
            throw new UndeclaredThrowableException(e, "That should not happen");
        }

        //
        this.text = text;
        this.bytes = bytes;
        // Remove miliseconds because string of date retrieve from Http header doesn't have miliseconds
        lastModified = (System.currentTimeMillis() / 1000) * 1000;
    }

    public String getText() {
        return text;
    }

    public int getLength() {
      return bytes.length;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void writeTo(BinaryOutput output) throws IOException {
        output.write(bytes);
    }
}
