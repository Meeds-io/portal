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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import jakarta.servlet.ServletContext;


class SimpleResourceContext {

    private final String contextPath;

    private final ServletContext context;

    public SimpleResourceContext(String contextPath, ServletContext context) {
        this.contextPath = contextPath;
        this.context = context;
    }

    public Resource getResource(String path) {
        int i2 = path.lastIndexOf("/") + 1;
        String targetedParentPath = path.substring(0, i2);
        String targetedFileName = path.substring(i2);
        final InputStream inputStream = context.getResourceAsStream(path);
        if (inputStream != null) {
            return new Resource(contextPath, targetedParentPath, targetedFileName) {
                @Override
                public Reader read() {
                    return new InputStreamReader(inputStream);
                }
            };
        }
        return null;
    }

    public String getContextPath() {
        return contextPath;
    }
}
