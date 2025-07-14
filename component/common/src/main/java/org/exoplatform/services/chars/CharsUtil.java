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
package org.exoplatform.services.chars;

/**
 * Created by The eXo Platform SARL Author : Nhu Dinh Thuan thuan.nhu@exoplatform.com Sep 13, 2006
 */
public final class CharsUtil {

    public static int indexOf(char[] value, char[] c, int start) {
        boolean is = false;
        for (int i = start; i < value.length; i++) {
            is = true;
            for (int j = 0; j < c.length; j++) {
                if (i + j < value.length && c[j] == value[i + j])
                    continue;
                is = false;
                break;
            }
            if (is)
                return i;
        }
        return -1;
    }

    public static char[] cutAndTrim(char[] data, int start, int end) {
        int s = start;
        int e = end - 1;
        while (s < end) {
            if (!Character.isWhitespace(data[s]))
                break;
            s++;
        }
        while (e > start) {
            if (!Character.isWhitespace(data[e]))
                break;
            e--;
        }
        e++;
        if (e <= s)
            return new char[0];
        char[] newChar = new char[e - s];
        System.arraycopy(data, s, newChar, 0, newChar.length);
        return newChar;
    }

    public static char[] cutBySpace(char[] data, int start) {
        int e = start;
        while (e < data.length) {
            if (Character.isWhitespace(data[e]))
                break;
            e++;
        }
        if (e <= start)
            return new char[0];
        char[] newChar = new char[e - start];
        System.arraycopy(data, start, newChar, 0, newChar.length);
        return newChar;
    }

    public static int indexOfIgnoreCase(char[] value, char[] c, int start) {
        boolean is = false;
        for (int i = start; i < value.length; i++) {
            is = true;
            for (int j = 0; j < c.length; j++) {
                if (Character.toLowerCase(c[j]) == Character.toLowerCase(value[i + j]))
                    continue;
                is = false;
                break;
            }
            if (is)
                return i;
        }
        return -1;
    }

    public static int indexOf(char[] value, char c, int start) {
        for (int i = start; i < value.length; i++) {
            if (c == value[i])
                return i;
        }
        return -1;
    }

}
