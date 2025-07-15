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
package org.exoplatform.portal.pom.spi.portlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public final class Preference implements Serializable {

    /** . */
    private final String name;

    /** . */
    private final List<String> values;

    /** . */
    private final boolean readOnly;

    public Preference(String name, List<String> values, boolean readOnly) {
        if (name == null) {
            throw new NullPointerException();
        }
        if (values == null) {
            throw new NullPointerException();
        }

        // Clone and check state
        values = Collections.unmodifiableList(new ArrayList<String>(values));
        if (values.size() == 0) {
            throw new IllegalArgumentException();
        }

        //
        this.name = name;
        this.values = values;
        this.readOnly = readOnly;
    }

    public Preference(String name, String value, boolean readOnly) {
        if (name == null) {
            throw new NullPointerException();
        }

        //
        this.name = name;
        this.values = Collections.singletonList(value);
        this.readOnly = readOnly;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return values.get(0);
    }

    public List<String> getValues() {
        return values;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Preference) {
            Preference that = (Preference) obj;
            return this.name.equals(that.name) && this.getValues().equals(that.getValues());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode() ^ values.hashCode() ^ (readOnly ? -1 : 0);
    }

    @Override
    public String toString() {
        return "Preference[name=" + name + ",values=" + values + ",readOnly=" + readOnly + "]";
    }
}
