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
package org.exoplatform.upload;

import java.util.HashMap;
import java.util.Map;


class ParameterParser {

    private char[] chars_ = null;

    private int pos = 0;

    private int len = 0;

    private int i1 = 0, i2 = 0;

    private boolean lowerCaseNames = false;

    private boolean hasChar() {
        return this.pos < this.len;
    }

    private String getToken(boolean quoted) {
        while ((i1 < i2) && (Character.isWhitespace(chars_[i1])))
            i1++;

        while ((i2 > i1) && (Character.isWhitespace(chars_[i2 - 1])))
            i2--;

        if (quoted && ((i2 - i1) >= 2) && (chars_[i1] == '"') && (chars_[i2 - 1] == '"')) {
            i1++;
            i2--;
        }
        if (i2 > i1)
            return new String(chars_, i1, i2 - i1);

        return null;
    }

    private boolean isOneOf(char ch, final char[] charray) {
        for (int i = 0; i < charray.length; i++) {
            if (ch != charray[i])
                continue;
            return true;
        }
        return false;
    }

    private String parseToken(final char[] terminators) {
        char ch;
        i1 = pos;
        i2 = pos;
        while (hasChar()) {
            ch = chars_[pos];
            if (isOneOf(ch, terminators))
                break;
            i2++;
            pos++;
        }
        return getToken(false);
    }

    private String parseQuotedToken(final char[] terminators) {
        char ch;
        i1 = pos;
        i2 = pos;
        boolean quoted = false;
        boolean charEscaped = false;
        while (hasChar()) {
            ch = chars_[pos];
            if (!quoted && isOneOf(ch, terminators))
                break;
            if (!charEscaped && ch == '"')
                quoted = !quoted;
            charEscaped = (!charEscaped && ch == '\\');
            i2++;
            pos++;
        }
        return getToken(true);
    }

    public void setLowerCaseNames(boolean b) {
        this.lowerCaseNames = b;
    }

    public Map<String, String> parse(final String str, char separator) {
        if (str == null)
            return new HashMap<String, String>();

        chars_ = str.toCharArray();
        int offset = 0;
        int length = chars_.length;

        HashMap<String, String> params = new HashMap<String, String>();
        this.pos = offset;
        this.len = length;

        String paramName = null;
        String paramValue = null;
        while (hasChar()) {
            paramName = parseToken(new char[] { '=', separator });
            paramValue = null;

            if (hasChar() && (chars_[pos] == '=')) {
                pos++;
                paramValue = parseQuotedToken(new char[] { separator });
            }

            if (hasChar() && (chars_[pos] == separator))
                pos++;

            if ((paramName != null) && (paramName.length() > 0)) {
                if (this.lowerCaseNames)
                    paramName = paramName.toLowerCase();
                params.put(paramName, paramValue);
            }
        }
        return params;
    }
}
