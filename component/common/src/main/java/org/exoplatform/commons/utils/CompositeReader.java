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
import java.io.Reader;
import java.util.*;

import org.apache.commons.collections.IteratorUtils;
import org.gatein.common.io.IOTools;
import org.gatein.common.util.Tools;

public class CompositeReader extends Reader {

    /** . */
    private Iterator<Reader> compounds;

    /** . */
    private Reader current;

    public CompositeReader(Reader... compounds) throws NullPointerException {
        this(Tools.iterator(compounds));
    }

    public CompositeReader(Iterable<Reader> compounds) throws NullPointerException {
        this(compounds.iterator());
    }

    public CompositeReader(Iterator<Reader> compounds) throws NullPointerException {
        this.compounds = compounds;
        this.current = null;
    }

    public List<Reader> getCompounds() {
        return IteratorUtils.toList(compounds);
    }

    public static boolean isMinify(Reader reader) {
        if (reader != null && reader instanceof MinifiableReader) {
            return ((MinifiableReader)reader).isMinify();
        } else {
            return false;
        }
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int read = 0;
        while (len > 0) {
            if (current == null) {
                if (compounds.hasNext()) {
                    current = compounds.next();
                } else {
                    if (read == 0) {
                        // Otherwise it would loop for ever
                        read = -1;
                    }
                    break;
                }
            } else {
                int tmp = current.read(cbuf, off, len);
                if (tmp == -1) {
                    Reader reader = current;
                    current = null;
                    reader.close();
                } else {
                    off += tmp;
                    len -= tmp;
                    read += tmp;
                }
            }
        }
        return read;
    }

    @Override
    public void close() throws IOException {
        if (current != null) {
            IOTools.safeClose(current);
            current = null;
        }
        while (compounds.hasNext()) {
            IOTools.safeClose(compounds.next());
        }
        compounds = null;
    }

    public static class MinifiableReader extends CompositeReader {
        private boolean minify;

        public MinifiableReader(Iterable<Reader> compounds, boolean isMinify) {
            super(compounds);
            this.minify = isMinify;
        }

        public boolean isMinify() {
            return minify;
        }
    }
}
