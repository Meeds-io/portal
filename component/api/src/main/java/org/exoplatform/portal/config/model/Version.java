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
package org.exoplatform.portal.config.model;

public enum Version {

    UNKNOWN(null),

    V_1_0("http://www.gatein.org/xml/ns/gatein_objects_1_0"),

    V_1_1("http://www.gatein.org/xml/ns/gatein_objects_1_1"),

    V_1_2("http://www.gatein.org/xml/ns/gatein_objects_1_2"),

    V_1_3("http://www.gatein.org/xml/ns/gatein_objects_1_3"),

    V_1_4("http://www.gatein.org/xml/ns/gatein_objects_1_4"),

    V_1_5("http://www.gatein.org/xml/ns/gatein_objects_1_5"),

    V_1_6("http://www.gatein.org/xml/ns/gatein_objects_1_6");

    /** . */
    private final String uri;

    Version(String uri) {
        this.uri = uri;
    }

    public String getURI() {
        return uri;
    }

    public static Version forURI(String uri) {
        if (uri == null) {
            throw new NullPointerException();
        }
        for (Version version : values()) {
            if (uri.equals(version.uri)) {
                return version;
            }
        }
        return null;
    }
}
