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
package org.exoplatform.services.resources;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.ResourceBundle;


/**
 * May 7, 2004
 *
 */
@SuppressWarnings("serial")
public class ExoResourceBundle extends ListResourceBundle implements Serializable {
    public static final ExoResourceBundle NULL_OBJECT = new ExoResourceBundle((String) null);

    /** . */
    private final ResourceBundleData data;

    public ExoResourceBundle(String data) {
        this.data = new ResourceBundleData(data);
    }

    public ExoResourceBundle(ResourceBundleData data) {
        if (data == null) {
            throw new NullPointerException();
        }

        //
        this.data = data;
    }

    public ExoResourceBundle(ResourceBundleData data, ResourceBundle parent) {
        this(data);
        setParent(parent);
    }

    public Object[][] getContents() {
        return data.contents;
    }

    public void putAll(Map<? super Object, ? super Object> map) {
        Enumeration<String> keys = getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (key != null) {
                map.put(key.trim(), getString(key));
            }
        }
    }

}
