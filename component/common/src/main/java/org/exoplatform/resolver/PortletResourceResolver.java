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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.portlet.PortletContext;
import jakarta.servlet.ServletContext;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class PortletResourceResolver extends ResourceResolver {

  protected static Log   log = ExoLogger.getLogger(PortletResourceResolver.class);

  private ServletContext portalContext;

  private PortletContext portletContext;

  private String         scheme;

  public PortletResourceResolver(PortletContext context, String scheme) {
    this.portletContext = context;
    this.scheme = scheme;
  }

  @Override
  public URL getResource(String url) throws Exception {
    ServletContext portalContextResolver = getPortalContext();
    if (portalContextResolver != null) {
      String relativePath = removeScheme(url);
      URL res = portalContextResolver.getResource(relativePath);
      if (res != null) {
        return res;
      }
    }
    return getPortletResource(url);
  }

  @Override
  public InputStream getInputStream(String url) throws Exception {
    ServletContext portalContextResolver = getPortalContext();
    if (portalContextResolver != null) {
      String relativePath = removeScheme(url);
      InputStream inputStream = portalContextResolver.getResourceAsStream(relativePath);
      if (inputStream != null) {
        return inputStream;
      }
    }
    return getPortletInputStream(url);
  }

  @Override
  public String getRealPath(String url) {
    ServletContext portalContextResolver = getPortalContext();
    if (portalContextResolver != null) {
      String relativePath = removeScheme(url);
      String path = portalContextResolver.getRealPath(relativePath);
      if (path != null) {
        return path;
      }
    }
    return getPortletRealPath(url);
  }

  @Override
  public List<URL> getResources(String url) throws Exception {
    ArrayList<URL> urlList = new ArrayList<>();
    urlList.add(getResource(url));
    return urlList;
  }

  @Override
  public List<InputStream> getInputStreams(String url) throws Exception {
    ArrayList<InputStream> inputStreams = new ArrayList<>();
    inputStreams.add(getInputStream(url));
    return inputStreams;
  }

  @Override
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

  @Override
  public String getWebAccessPath(String url) {
    return "/" + portletContext.getPortletContextName() + removeScheme(url);
  }

  @Override
  public String getResourceScheme() {
    return scheme;
  }

  @Override
  public ResourceKey createResourceKey(String url) {
    String portletContextName = portletContext.getPortletContextName();
    int resolverId = portletContextName != null ? portletContextName.hashCode() : hashCode();
    return new ResourceKey(resolverId, url);
  }

  private URL getPortletResource(String url) throws MalformedURLException {
    String path = removeScheme(url);
    return portletContext.getResource(path);
  }

  private InputStream getPortletInputStream(String url) {
    String path = removeScheme(url);
    return portletContext.getResourceAsStream(path);
  }

  private String getPortletRealPath(String url) {
    String path = removeScheme(url);
    return portletContext.getRealPath(path);
  }

  private ServletContext getPortalContext() {
    if (portalContext == null) {
      PortalContainer container = PortalContainer.getInstanceIfPresent();
      if (container != null) {
        portalContext = container.getPortalContext();
      }
    }
    return portalContext;
  }
}
