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
package org.exoplatform.groovyscript;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.charset.Charset;

import org.exoplatform.commons.utils.BinaryOutput;
import org.exoplatform.commons.utils.Text;

public class GroovyText extends Text {

    /** . */
    private final String s;

    /** . */
    private final byte[] bytes;

    /** . */
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    public GroovyText(String s) {
        try {
            this.s = s;
            this.bytes = s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    @Override
    public void writeTo(Writer writer) throws IOException {
        if (writer instanceof BinaryOutput) {
            BinaryOutput osw = (BinaryOutput) writer;
            if (UTF_8.equals(osw.getCharset())) {
                osw.write(bytes);
                return;
            }
        }
        writer.append(s);
    }
}
