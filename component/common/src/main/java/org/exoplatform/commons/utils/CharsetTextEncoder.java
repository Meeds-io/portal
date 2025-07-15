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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;


/**
 * A stateless encoder that use a char encoder for performing the encoding task.
 *
 */
public final class CharsetTextEncoder implements TextEncoder {

    private static final CharsetTextEncoder UTF8 = new CharsetTextEncoder(CharsetCharEncoder.getUTF8());

    public static TextEncoder getUTF8() {
        return UTF8;
    }

    /** . */
    private final CharEncoder charEncoder;

    public CharsetTextEncoder(CharEncoder charEncoder) {
        this.charEncoder = charEncoder;
    }

    public CharsetTextEncoder(String encoding) {
        this(new TableCharEncoder(new CharsetCharEncoder(Charset.forName(encoding))));
    }

    public Charset getCharset() {
        return charEncoder.getCharset();
    }

    public void encode(char c, OutputStream out) throws IOException {
        if (c > -1 && c < 128) {
            out.write((int) c);
        } else {
            byte[] bytes = charEncoder.encode(c);
            switch (bytes.length) {
                case 0:
                    throw new AssertionError();
                case 1:
                    out.write(bytes[0]);
                    break;
                case 2:
                    out.write(bytes[0]);
                    out.write(bytes[1]);
                    break;
                case 3:
                    out.write(bytes[0]);
                    out.write(bytes[1]);
                    out.write(bytes[2]);
                    break;
                default:
                    out.write(bytes);
            }
        }
    }

    public void encode(char[] chars, int off, int len, OutputStream out) throws IOException {
        for (int i = off; i < len; i++) {
            char c = chars[i];
            encode(c, out);
        }
    }

    public void encode(String str, int off, int len, OutputStream out) throws IOException {
        for (int i = off; i < len; i++) {
            char c = str.charAt(i);
            encode(c, out);
        }
    }
}
