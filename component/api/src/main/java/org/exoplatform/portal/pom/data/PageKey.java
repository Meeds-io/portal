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
package org.exoplatform.portal.pom.data;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.pom.config.Utils;

public class PageKey extends OwnerKey {

    private static final long serialVersionUID = -7843836004063037149L;

    /** . */
    private final String name;

    public PageKey(String type, String id, String name) {
        super(type, id);

        //
        if (name == null) {
            throw new NullPointerException();
        }

        //
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof PageKey that) {
            return super.equals(that) && name.equals(that.name);
        }
        return false;
    }

    public static PageKey create(String compositeId) {
        if (compositeId == null) {
            throw new NullPointerException();
        }
        String[] components = Utils.split("::", compositeId);
        if (components.length != 3) {
            throw new IllegalArgumentException("Wrong page id key format " + compositeId);
        }
        return new PageKey(components[0], components[1], components[2]);
    }

    public org.exoplatform.portal.mop.page.PageKey toMopPageKey() {
      return new SiteKey(getType(), getId()).page(name);
    }

    @Override
    public String toString() {
      return "PageKey[type=" + getType() + ",id=" + getId() + ",name=" + name + "]";
    }
}
