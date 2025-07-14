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
import java.io.Writer;
import java.nio.charset.Charset;

import org.exoplatform.commons.utils.BinaryOutput;
import org.exoplatform.commons.utils.OutputStreamPrinter;
import org.exoplatform.commons.utils.Text;

public class OutputStreamWriterGroovyPrinter extends GroovyPrinter implements BinaryOutput {

    /** . */
    private final OutputStreamPrinter out;

    public OutputStreamWriterGroovyPrinter(OutputStreamPrinter out) {
        if (out == null) {
            throw new NullPointerException();
        }
        this.out = out;
    }

    @Override
    protected Writer getWriter() {
        return out;
    }

    @Override
    protected void write(char c) throws IOException {
        out.write(c);
    }

    @Override
    protected void write(String s) throws IOException {
        out.write(s);
    }

    @Override
    protected void write(Text text) throws IOException {
        text.writeTo(out);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    public Charset getCharset() {
        return out.getCharset();
    }

    public void write(byte[] bytes) throws IOException {
        out.write(bytes);
    }

    public void write(byte[] bytes, int off, int len) throws IOException {
        out.write(bytes, off, len);
    }

    public void write(byte b) throws IOException {
        out.write(b);
    }
}
