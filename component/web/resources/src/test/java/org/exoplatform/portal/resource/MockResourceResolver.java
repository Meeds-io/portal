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
package org.exoplatform.portal.resource;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;

class MockResourceResolver implements ResourceResolver {
    /** . */
    private final Log         log = ExoLogger.getLogger(MockResourceResolver.class);

    private Map<String, String> map = new HashMap<String, String>();

    public MockResourceResolver() {
        addResource("/path/to/MockResourceResolver.css", this.getClass().getName());
    }

    public void addResource(String path, String value) {
        map.put(path, value);
    }

    public String removeResource(String path) {
        return map.remove(path);
    }

    @Override
    public Resource resolve(String path) throws NullPointerException {
        if (path == null) {
            throw new NullPointerException("No null path is accepted");
        }

        log.info("path to resolve : " + path);

        final String css = map.get(path);
        if (css != null) {
            return new Resource(path) {
                @Override
                public Reader read() {
                    return new StringReader(css);
                }
            };
        }
        return null;
    }

    public void reset() {
        map.clear();
    }
}
