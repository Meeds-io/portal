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
package org.exoplatform.web.controller.regexp;

public enum GroupType {

    CAPTURING_GROUP("(", ")"),

    NON_CAPTURING_GROUP("(?:", ")"),

    POSITIVE_LOOKAHEAD("(?=", ")"),

    NEGATIVE_LOOKAHEAD("(?!", ")"),

    POSITIVE_LOOKBEHIND("(?<=", ")"),

    NEGATIVE_LOOKBEHIND("(?<!", ")");

    /** . */
    private static final GroupType[] ALL = values();

    public static GroupType forPrefix(String s) {
        if (s == null) {
            throw new NullPointerException("No null prefix accepted");
        }
        // No need for a fast lookup, iteration on array will do well
        for (GroupType type : ALL) {
            if (type.open.equals(s)) {
                return type;
            }
        }
        return null;
    }

    /** . */
    private final String open;

    /** . */
    private final String close;

    GroupType(String open, String close) {
        this.open = open;
        this.close = close;
    }

    public String getOpen() {
        return open;
    }

    public String getClose() {
        return close;
    }
}
