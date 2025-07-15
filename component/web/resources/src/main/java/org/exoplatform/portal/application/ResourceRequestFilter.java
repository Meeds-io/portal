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
package org.exoplatform.portal.application;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.gatein.portal.controller.resource.ResourceRequestHandler;
import org.gatein.portal.controller.resource.ResourceScope;
import org.gatein.portal.controller.resource.ScriptContent;
import org.gatein.portal.controller.resource.ScriptKey;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.web.AbstractFilter;
import org.exoplatform.portal.resource.SimpleSkin;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.web.application.javascript.JavascriptConfigService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("deprecation")
public class ResourceRequestFilter extends AbstractFilter {

  private static final Log        LOG                       = ExoLogger.getLogger(ResourceRequestFilter.class);

  private static final String     CACHE_CONTROL_HEADER_NAME = "Cache-Control";

  public static final String      IF_MODIFIED_SINCE         = "If-Modified-Since";

  public static final String      LAST_MODIFIED             = "Last-Modified";

  public static final String      EXPIRES                   = "Expires";

  public static final long        DEFAULT_MAX_AGE           = 31536000l;

  /**
   * can't be final, the RootContainer sometimes is built after init
   */
  public static long              maxAge                    = getMaxAge();                                     // NOSONAR

  /**
   * can't be final, the RootContainer sometimes is built after init
   */
  public static String            version                   = getVersion();                                    // NOSONAR

  private JavascriptConfigService javascriptService;

  private SkinService             skinService;

  private ResourceBundleService   resourceBundleService;

  @Override
  protected void afterInit(FilterConfig config) throws ServletException {
    super.afterInit(config);
    if (StringUtils.isBlank(version)) {
      version = getVersion(); // NOSONAR can't be final, the RootContainer
      // sometimes is built after init
      maxAge = getMaxAge(); // NOSONAR can't be final, the RootContainer sometimes
      // is built after init
      ResourceRequestHandler.MAX_AGE = maxAge; // NOSONAR
      ResourceRequestHandler.VERSION = version; // NOSONAR
    }
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(PortalContainer.getInstance().getPortalClassLoader());
    try {
      String servletPath = httpRequest.getServletPath();
      if (servletPath.endsWith(".css")) {
        String fileContentHash = httpRequest.getParameter(SimpleSkin.HASH_QUERY_PARAM);
        String orientation = httpRequest.getParameter(SimpleSkin.ORIENTATION_QUERY_PARAM);
        String compress = httpRequest.getParameter(SimpleSkin.MINIFY_QUERY_PARAM);
        if (fileContentHash != null && orientation != null && compress != null) {
          // CSS Module resource
          String fileContent = getSkinModuleContent(httpRequest, fileContentHash, orientation, compress);
          if (fileContent != null) {
            byte[] bytes = fileContent.getBytes(StandardCharsets.UTF_8);
            writeStaticResourceContent(httpResponse, bytes, "text/css");
            return; // NOSONAR not redundant jump
          }
        }
      } else if (servletPath.endsWith(".js")) {
        String scope = httpRequest.getParameter(ScriptKey.SCOPE_QUERY_PARAM);
        String compress = httpRequest.getParameter(ScriptKey.MINIFY_QUERY_PARAM);
        if (scope != null && compress != null) {
          // JS Module resource
          ScriptContent script = getScriptContent(httpRequest, scope, compress);
          if (script != null) {
            byte[] bytes = script.getContentAsBytes();
            writeStaticResourceContent(httpResponse, bytes, "text/javascript");
            return; // NOSONAR not redundant jump
          }
        }
      } else if (servletPath.startsWith("/i18n/")) {
        String lang = httpRequest.getParameter("lang");
        String resourceBundleName = servletPath.replace("/i18n/", "");
        String i18N = getResourceBundleContent(resourceBundleName, lang);
        if (i18N != null) {
          writeStaticResourceContent(httpResponse, i18N.getBytes(StandardCharsets.UTF_8), "application/json");
          return; // NOSONAR not redundant jump
        }
      }
    } finally {
      Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
    // All other static resources caching basic HTTP Headers
    httpResponse.setHeader(CACHE_CONTROL_HEADER_NAME, "public, " + maxAge);
    httpResponse.setDateHeader(ResourceRequestFilter.LAST_MODIFIED, System.currentTimeMillis());
    httpResponse.setDateHeader(ResourceRequestFilter.EXPIRES,
                               System.currentTimeMillis() + maxAge * 1000);
    chain.doFilter(request, response);
  }

  private void writeStaticResourceContent(HttpServletResponse httpResponse, byte[] bytes, String mimeType) throws IOException {
    httpResponse.setContentType(mimeType);
    httpResponse.setContentLength(bytes.length);
    httpResponse.setHeader(CACHE_CONTROL_HEADER_NAME, "public, " + maxAge);
    httpResponse.setDateHeader(ResourceRequestFilter.LAST_MODIFIED, System.currentTimeMillis());
    httpResponse.setDateHeader(ResourceRequestFilter.EXPIRES,
                               System.currentTimeMillis() + maxAge * 1000);
    PrintWriter writer = httpResponse.getWriter();
    IOUtils.write(bytes, writer, StandardCharsets.UTF_8);
    writer.close();
  }

  private ScriptContent getScriptContent(HttpServletRequest httpRequest, String scope, String compress) {
    String jsPath = httpRequest.getServletPath();
    String module = jsPath.substring(jsPath.lastIndexOf("/") + 1).replace(".js", "");
    ResourceScope resourceScope = ResourceScope.valueOf(scope);
    if (resourceScope == ResourceScope.PORTLET) {
      module = httpRequest.getServletContext().getContextPath().replace("/", "") + "/" + module;
    }

    return getJavascriptService().getScriptContent(resourceScope, module, "true".equals(compress));
  }

  private String getSkinModuleContent(HttpServletRequest httpRequest,
                                      String fileContentHash,
                                      String orientation,
                                      String compress) throws IOException {
    return getSkinService().getSkinModuleContent(httpRequest.getContextPath() + httpRequest.getServletPath(),
                                                 Integer.parseInt(fileContentHash),
                                                 Orientation.valueOf(orientation),
                                                 StringUtils.equals("true", compress));
  }

  private String getResourceBundleContent(String resourceBundleName, String lang) {
    return getResourceBundleService().getResourceBundleContent(resourceBundleName, Locale.forLanguageTag(lang));
  }

  private JavascriptConfigService getJavascriptService() {
    if (javascriptService == null) {
      javascriptService = ExoContainerContext.getService(JavascriptConfigService.class);
    }
    return javascriptService;
  }

  private SkinService getSkinService() {
    if (skinService == null) {
      skinService = ExoContainerContext.getService(SkinService.class);
    }
    return skinService;
  }

  public ResourceBundleService getResourceBundleService() {
    if (resourceBundleService == null) {
      resourceBundleService = ExoContainerContext.getService(ResourceBundleService.class);
    }
    return resourceBundleService;
  }

  private static String getVersion() {
    String version = PropertyManager.getProperty("gatein.assets.version");
    if (StringUtils.isNotBlank(version)) {
      if (StringUtils.contains(version, "SNAPSHOT") || PropertyManager.isDevelopping()) {
        version += "-" + new Random().nextLong(); // NOSONAR
      }
      LOG.info("Use version \"{}\" for resource serving", version);
    }
    return version;
  }

  private static long getMaxAge() {
    String maxAge = PropertyManager.getProperty("gatein.assets.script.max-age");
    if (StringUtils.isBlank(maxAge)) {
      maxAge = String.valueOf(DEFAULT_MAX_AGE);
    }
    LOG.info("Use maxAge \"{}\" seconds for resource serving", maxAge);
    return Long.parseLong(maxAge);
  }

}
