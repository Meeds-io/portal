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
package org.exoplatform.portal.config.serialize;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class JibxArraySerialize {
  
    /**
     * {@code "Nobody"} is equivalent to empty list of permissions.
     */
    public static final String NOBODY = "Nobody";

    public static String serializeStringArray(String[] values) {
        if (values == null || values.length == 0) {
            return "";
        } else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    String value = values[i].trim();
                    if (value.length() > 0) {
                        if (builder.length() > 0) {
                            builder.append(';');
                        }
                        builder.append(value);
                    }
                }
            }
            return builder.toString();
        }
    }

    public static String[] deserializeStringArray(String text) {
        if (text == null) {
            return new String[0];
        } else {
            text = text.trim();
            if (text.length() == 0) {
                return new String[0];
            } else {
                List<String> list = new ArrayList<>(5);
                StringTokenizer st = new StringTokenizer(text, ";");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken().trim();
                    if (token.length() > 0) {
                        list.add(token);
                    }
                }
                return list.toArray(new String[list.size()]);
            }
        }
    }

    public static String serializePermissions(String[] values) {
        if (values == null || values.length == 0) {
            return NOBODY;
        } else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    String value = values[i].trim();
                    if (value.length() > 0) {
                        if (builder.length() > 0) {
                            builder.append(';');
                        }
                        builder.append(value);
                    }
                }
            }
            return builder.length() == 0 ? NOBODY : builder.toString();
        }
    }

    public static String[] deserializePermissions(String text) {
        if (text == null) {
            return new String[0];
        } else {
            text = text.trim();
            if (text.length() == 0 || NOBODY.equals(text)) {
                return new String[0];
            } else {
                List<String> list = new ArrayList<String>(5);
                StringTokenizer st = new StringTokenizer(text, ";");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken().trim();
                    if (token.length() > 0) {
                        list.add(token);
                    }
                }
                return list.toArray(new String[list.size()]);
            }
        }
    }

}
