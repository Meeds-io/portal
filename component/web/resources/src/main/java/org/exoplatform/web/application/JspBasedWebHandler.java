/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.web.application;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.gatein.portal.controller.resource.ResourceId;
import org.gatein.portal.controller.resource.ResourceScope;
import org.gatein.portal.controller.resource.script.*;
import org.gatein.portal.controller.resource.script.Module;
import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.*;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.web.application.javascript.JavascriptConfigService;

public abstract class JspBasedWebHandler extends WebRequestHandler {

  private static final Log          LOG            = ExoLogger.getLogger(JspBasedWebHandler.class);

  protected static final String     TEXT_HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

  protected static final String     JS_PATHS_PARAM         = "paths";

  protected LocaleConfigService     localeConfigService;

  protected BrandingService         brandingService;

  protected JavascriptConfigService javascriptConfigService;

  protected SkinService             skinService;

  protected JspBasedWebHandler(LocaleConfigService localeConfigService,
                               BrandingService brandingService,
                               JavascriptConfigService javascriptConfigService,
                               SkinService skinService) {
    this.localeConfigService = localeConfigService;
    this.brandingService = brandingService;
    this.javascriptConfigService = javascriptConfigService;
    this.skinService = skinService;
  }

  @Override
  public String getHandlerName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean execute(ControllerContext context) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  protected boolean getRequiresLifeCycle() {
    throw new UnsupportedOperationException();
  }

  public void prepareDispatch(ControllerContext context,
                              String applicationModule,
                              List<String> additionalJSModules,
                              List<String> additionalCSSModules,
                              Consumer<JSONObject> extendUIParameters) throws Exception {
    HttpServletRequest request = context.getRequest();
    HttpServletResponse response = context.getResponse();
    response.setContentType(TEXT_HTML_CONTENT_TYPE);

    LocaleConfig localeConfig = request.getLocale() == null ? localeConfigService.getDefaultLocaleConfig()
                                                            : localeConfigService.getLocaleConfig(request.getLocale()
                                                                                                         .getLanguage());
    if (localeConfig == null) {
      localeConfig = localeConfigService.getDefaultLocaleConfig();
    }
    request.setAttribute("localeConfig", localeConfig);

    Locale locale = localeConfig.getLocale();

    JavascriptManager javascriptManager = new JavascriptManager(javascriptConfigService);
    javascriptManager.loadScriptResource(ResourceScope.SHARED, "bootstrap");
    javascriptManager.loadScriptResource(ResourceScope.PORTLET, "social/Login");

    JSONObject params = new JSONObject();

    String companyName = brandingService.getCompanyName();
    params.put("companyName", companyName);
    String brandingLogo = "/" + PortalContainer.getCurrentPortalContainerName() + "/"
        + PortalContainer.getCurrentRestContextName() + "/v1/platform/branding/logo?v=" + brandingService.getLastUpdatedTime();
    params.put("brandingLogo", brandingLogo);
    params.put("authenticationBackground", brandingService.getLoginBackgroundPath());
    params.put("authenticationTextColor", brandingService.getLoginBackgroundTextColor());
    params.put("authenticationTitle", brandingService.getLoginTitle(locale));
    params.put("authenticationSubtitle", brandingService.getLoginSubtitle(locale));

    if (extendUIParameters != null) {
      extendUIParameters.accept(params);
    }

    if (StringUtils.isNotBlank(applicationModule)) {
      javascriptManager.require(applicationModule, "app").addScripts("app.init(" + params.toString() + ");");
    }

    JSONObject jsConfig = javascriptConfigService.getJSConfig();
    request.setAttribute("jsConfig", jsConfig.toString());

    if (jsConfig.has(JS_PATHS_PARAM)) {
      JSONObject jsConfigPaths = jsConfig.getJSONObject(JS_PATHS_PARAM);

      LinkedList<String> headerScripts = getHeaderScripts(javascriptManager, jsConfigPaths);
      request.setAttribute("headerScripts", headerScripts);

      Set<String> pageScripts = getPageScripts(javascriptManager, additionalJSModules, jsConfigPaths);
      request.setAttribute("pageScripts", pageScripts);
    }
    request.setAttribute("inlineScripts", javascriptManager.getJavaScripts());

    String brandingPrimaryColor = brandingService.getThemeStyle().get("primaryColor");
    String brandingThemeUrl = "/" + PortalContainer.getCurrentPortalContainerName() + "/"
        + PortalContainer.getCurrentRestContextName() + "/v1/platform/branding/css?v=" + brandingService.getLastUpdatedTime();

    request.setAttribute("brandingPrimaryColor", brandingPrimaryColor);
    request.setAttribute("brandingThemeUrl", brandingThemeUrl);
    request.setAttribute("brandingFavicon", brandingService.getFaviconPath());

    List<String> skinUrls = getPageSkins(additionalCSSModules, localeConfig.getOrientation());
    request.setAttribute("skinUrls", skinUrls);
  }

  private List<String> getPageSkins(List<String> additionalCSSModules,
                                    Orientation orientation) {
    String skinName = skinService.getDefaultSkin();

    List<SkinConfig> skins = new ArrayList<>();

    Collection<SkinConfig> portalSkins = skinService.getPortalSkins(skinName);
    skins.addAll(portalSkins);

    if (CollectionUtils.isNotEmpty(additionalCSSModules)) {
      additionalCSSModules.forEach(module -> {
        SkinConfig loginSkin = skinService.getSkin(module, skinName);
        if (loginSkin != null) {
          skins.add(loginSkin);
        }
      });
    }

    Collection<SkinConfig> customSkins = skinService.getCustomPortalSkins(skinName);
    skins.addAll(customSkins);
    return skins.stream().map(skin -> {
      SkinURL url = skin.createURL();
      if (url == null) {
        return null;
      }
      url.setOrientation(orientation);
      return url.toString();
    }).filter(Objects::nonNull).toList();
  }

  private Set<String> getPageScripts(JavascriptManager javascriptManager,
                                     List<String> additionalJSModules,
                                     JSONObject jsConfigPaths) throws JSONException {
    if (CollectionUtils.isNotEmpty(additionalJSModules)) {
      additionalJSModules.forEach(javascriptManager::require);
    }

    Set<String> pageScripts = new HashSet<>();
    Map<String, Boolean> scriptsIdsMap = javascriptManager.getPageScripts();
    for (Entry<String, Boolean> scriptEntry : scriptsIdsMap.entrySet()) {
      boolean isRemote = scriptEntry.getValue().booleanValue();
      String scriptId = scriptEntry.getKey();
      if (!isRemote && jsConfigPaths.has(scriptId)) {
        String scriptPath = jsConfigPaths.getString(scriptId);
        pageScripts.add(scriptPath);
      }
    }
    return pageScripts;
  }

  private LinkedList<String> getHeaderScripts(JavascriptManager javascriptManager,
                                              JSONObject jsConfigPaths) throws JSONException {
    Map<String, Boolean> scriptsURLs = getScripts(javascriptManager);
    LinkedList<String> headerScripts = new LinkedList<>();
    for (Entry<String, Boolean> moduleEntry : scriptsURLs.entrySet()) {
      String module = moduleEntry.getKey();
      String url = jsConfigPaths.has(module) ? jsConfigPaths.getString(module) : null;
      headerScripts.add(url != null ? url : module);
    }
    return headerScripts;
  }

  private Map<String, Boolean> getScripts(JavascriptManager javascriptManager) {
    FetchMap<ResourceId> requiredResources = javascriptManager.getScriptResources();
    Map<ScriptResource, FetchMode> resolved = javascriptConfigService.resolveIds(requiredResources);

    Map<String, Boolean> ret = new LinkedHashMap<>();
    Map<String, Boolean> tmp = new LinkedHashMap<>();
    for (ScriptResource rs : resolved.keySet()) {
      ResourceId id = rs.getId();
      // SHARED/bootstrap should be loaded first
      if (ResourceScope.SHARED.equals(id.getScope()) && "bootstrap".equals(id.getName())) {
        ret.put(id.toString(), false);
      } else {
        boolean isRemote = !rs.isEmpty() && rs.getModules().get(0) instanceof Module.Remote;
        tmp.put(id.toString(), isRemote);
      }
    }
    ret.putAll(tmp);
    for (String url : javascriptManager.getExtendedScriptURLs()) {
      ret.put(url, true);
    }

    //
    LOG.debug("Resolved resources for page: " + ret);
    return ret;
  }

}
