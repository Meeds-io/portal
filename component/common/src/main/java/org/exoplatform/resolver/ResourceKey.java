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
package org.exoplatform.resolver;

import java.io.Serializable;

import lombok.ToString;


@ToString
public class ResourceKey implements Serializable {

    private static final long serialVersionUID = -51562811492603221L;

    /** . */
    private final int resolverId;

    /** . */
    private final String url;

    public ResourceKey(int resolverId, String url) {
        if (url == null) {
            throw new NullPointerException("no null URL accepted");
        }
        this.resolverId = resolverId;
        this.url = url;
    }

    public String getURL() {
        return url;
    }

    @Override
    public int hashCode() {
        return resolverId ^ url.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof ResourceKey) {
            ResourceKey that = (ResourceKey) o;
            return resolverId == that.resolverId && url.equals(that.url);
        }
        return false;
    }

}
