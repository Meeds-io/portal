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
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletContext;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS Mar 15, 2006
 */
public class ServletResourceResolver extends ResourceResolver {

    protected static Log log = ExoLogger.getLogger("portal:ServletResourceResolver");

    private ServletContext scontext_;

    private String scheme_;

    public ServletResourceResolver(ServletContext context, String scheme) {
        scontext_ = context;
        scheme_ = scheme;
    }

    public URL getResource(String url) throws Exception {
        String path = removeScheme(url);
        return scontext_.getResource(path);
    }

    public InputStream getInputStream(String url) throws Exception {
        String path = removeScheme(url);
        return scontext_.getResourceAsStream(path);
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
        return scontext_.getRealPath(path);
    }

    public boolean isModified(String url, long lastAccess) {
        try {
            URL uri = getResource(url);
            URLConnection con = uri.openConnection();
            if (log.isDebugEnabled())
                log.debug(url + ": " + con.getLastModified() + " " + lastAccess);
            if (con.getLastModified() > lastAccess) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public String getWebAccessPath(String url) {
        if (log.isDebugEnabled())
            log.debug("GET WEB ACCESS " + url);
        return "/" + scontext_.getServletContextName() + removeScheme(url);
    }

    public String getResourceScheme() {
        return scheme_;
    }

    @Override
    public ResourceKey createResourceKey(String url) {
        return new ResourceKey(scontext_.getContextPath().hashCode(), url);
    }
}
