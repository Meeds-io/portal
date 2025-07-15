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

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;


public class MockResourceResolver extends ResourceResolver {

    private Map<String, URL> resources_;

    public MockResourceResolver(Map<String, URL> mapResources) {
        resources_ = mapResources;
    }

    public URL getResource(String url) throws Exception {
        return resources_.get(url);
    }

    public InputStream getInputStream(String url) throws Exception {
        URL result = resources_.get(url);
        if (result != null)
            return result.openStream();
        return null;
    }

    @SuppressWarnings("unused")
    public List<URL> getResources(String url) throws Exception {
        return null;
    }

    @SuppressWarnings("unused")
    public List<InputStream> getInputStreams(String url) throws Exception {
        return null;
    }

    @SuppressWarnings("unused")
    public boolean isModified(String url, long lastAccess) {
        return false;
    }

    public String getResourceScheme() {
        return null;
    }

}
