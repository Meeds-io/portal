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

import java.util.HashMap;
import java.util.Map;


/**
 * Created by The eXo Platform SARL Author : Nhu Dinh Thuan nhudinhthuan@exoplatform.com Jun 2, 2007
 */
@SuppressWarnings("serial")
public class Properties extends HashMap<String, String> {

    public Properties(Map<String, String> m) {
        super(m);
    }

    public Properties() {
        super();
    }

    public Properties(int size) {
        super(size);
    }

    public int getIntValue(String key) {
        String value = super.get(key);
        if (value == null || value.trim().length() < 1)
            return -1;
        return Integer.valueOf(value.trim());
    }

    public double getDoubleValue(String key) {
        String value = super.get(key);
        if (value == null || value.trim().length() < 1)
            return -1.0;
        return Double.valueOf(value.trim());
    }

    public void put(String key, int value) {
        super.put(key, String.valueOf(value));
    }

    public void put(String key, double value) {
        super.put(key, String.valueOf(value));
    }

    public String setProperty(String key, String value) {
        return put(key, value);
    }

}
