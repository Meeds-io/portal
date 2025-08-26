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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class FileResourceResolver extends ResourceResolver {

    static String FILE_PREFIX = "file:";

    public URL getResource(String url) throws Exception {
        String path = removeScheme(url);
        File file = new File(path);
        if (file.exists() && file.isFile())
            return file.toURL();
        return null;
    }

    public InputStream getInputStream(String url) throws Exception {
        String path = removeScheme(url);
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            FileInputStream is = new FileInputStream(file);
            return is;
        }
        return null;
    }

    public List<URL> getResources(String url) throws Exception {
        ArrayList<URL> urlList = new ArrayList<URL>();
        urlList.add(getResource(url));
        return urlList;
    }

    public List<InputStream> getInputStreams(String url) throws Exception {
        ArrayList<InputStream> inputStreams = new ArrayList<InputStream>();
        inputStreams.add(getInputStream(url));
        return inputStreams;
    }

    public String getRealPath(String url) {
        String path = removeScheme(url);
        return path;
    }

    public boolean isModified(String url, long lastAccess) {
        File file = new File(getRealPath(url));
        if (file.exists() && file.lastModified() > lastAccess)
            return true;
        return false;
    }

    public String getResourceScheme() {
        return "file:";
    }

    @Override
    public ResourceKey createResourceKey(String url) {
        return new ResourceKey(this.getResourceScheme().hashCode(), url);
    }
}
