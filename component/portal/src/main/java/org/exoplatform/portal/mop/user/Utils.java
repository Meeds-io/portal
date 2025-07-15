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
package org.exoplatform.portal.mop.user;

/**
 * A set of utils for this package.
 *
 */
public class Utils {

    /**
     * Parse the path with the following algorithm:
     *
     * &lt;ul&gt;
     * &lt;li&gt;The one char &lt;code&gt;/&lt;/code&gt; string returns the null array&lt;/li&gt;
     * &lt;li&gt;Any leading &lt;code&gt;/&lt;code&gt; char is ommited&lt;/li&gt;
     * &lt;li&gt;Any trailing &lt;/code&gt;/&lt;/code&gt; chars are ommited&lt;/li&gt;
     * &lt;li&gt;All the substrings obtained by slicing the remaining string by the &lt;code&gt;/&lt;/code&gt; char are returned as an array, even
     * the empty strings&lt;/li&gt;
     * &lt;/ul&gt;
     *
     * &lt;p&gt;
     * Note that this is a reimplementation of a previous method that was using regex splitting, this reimplementation was done
     * in order to minimize the created object count in mind and attempt to create the minimum required.
     * &lt;/p&gt;
     *
     * @param path the path
     * @return the parse result
     * @throws NullPointerException if the path argument is null
     */
    public static String[] parsePath(String path) throws NullPointerException {
        // Where we start
        final int start = 0 < path.length() && path.charAt(0) == '/' ? 1 : 0;

        //
        if (start == path.length()) {
            return null;
        }

        // Where we end
        int end = path.length();
        while (end > start && path.charAt(end - 1) == '/') {
            end--;
        }

        // Count the number of slash
        int count = 0;
        int i = start;
        while (true) {
            int pos = path.indexOf('/', i);
            if (pos == -1) {
                pos = end;
            }
            if (pos == end) {
                if (pos > i) {
                    count++;
                }
                break;
            } else {
                count++;
                i = pos + 1;
            }
        }

        // Now fill the array
        String[] names = new String[count];
        i = start;
        int index = 0;
        while (true) {
            int pos = path.indexOf('/', i);
            if (pos == -1) {
                pos = end;
            }
            if (pos == end) {
                if (pos > i) {
                    names[index] = path.substring(i, end);
                }
                break;
            } else {
                if (i < pos) {
                    names[index++] = path.substring(i, pos);
                } else {
                    names[index++] = "";
                }
                i = pos + 1;
            }
        }

        //
        return names;
    }
}
