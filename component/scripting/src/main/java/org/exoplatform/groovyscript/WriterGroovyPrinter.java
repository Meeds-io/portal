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

import org.exoplatform.commons.utils.Text;

public class WriterGroovyPrinter extends GroovyPrinter {

    /** . */
    private Writer writer;

    public WriterGroovyPrinter(Writer writer) {
        if (writer == null) {
            throw new NullPointerException();
        }
        this.writer = writer;
    }

    @Override
    protected Writer getWriter() {
        return writer;
    }

    @Override
    protected void write(char c) throws IOException {
        writer.write(c);
    }

    @Override
    protected void write(String s) throws IOException {
        writer.write(s);
    }

    @Override
    protected void write(Text text) throws IOException {
        text.writeTo(writer);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
