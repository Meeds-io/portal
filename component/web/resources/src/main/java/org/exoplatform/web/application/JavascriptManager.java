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
package org.exoplatform.web.application;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.gatein.portal.controller.resource.ResourceId;
import org.gatein.portal.controller.resource.ResourceScope;
import org.gatein.portal.controller.resource.script.*;
import org.gatein.portal.controller.resource.script.Module;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.javascript.JavascriptConfigService;

/**
 * Created by The eXo Platform SAS Mar 27, 2007
 */
public class JavascriptManager {

    public static final boolean USE_WEBUI_RESOURCES = Boolean.parseBoolean(System.getProperty("io.meeds.useWebuiResources", "true"));

    public static final Log LOG = ExoLogger.getLogger("portal:JavascriptManager");

    /** . */
    private FetchMap<ResourceId> resourceIds = new FetchMap<ResourceId>();

    /** . */
    private Set<String> extendedScriptURLs = new LinkedHashSet<String>();

    private JavascriptConfigService javascriptConfigService;
    
    /** . */
    private StringBuilder scripts = new StringBuilder();

    /** . */
    private StringBuilder customizedOnloadJavascript = new StringBuilder();

    private RequireJS requireJS;

    public JavascriptManager() {
        requireJS = new RequireJS();
        if (USE_WEBUI_RESOURCES) {
          requireJS.require("SHARED/base", "base");
        }
    }

    public JavascriptManager(JavascriptConfigService javascriptConfigService) {
      this();
      this.javascriptConfigService = javascriptConfigService;
    }

    /**
     * Add a valid javascript code
     *
     * @param s a valid javascript code
     */
    public void addJavascript(CharSequence s) {
        if (s != null) {
            scripts.append("try {");
            scripts.append(s.toString().trim());
            scripts.append(";\n");
            scripts.append("} catch(unhandledError) { console.error(unhandledError); }");
        }
    }

    /**
     * Register a SHARE Javascript resource that will be loaded in Rendering phase Script FetchMode is ON_LOAD by default
     */
    public void loadScriptResource(String name) {
        loadScriptResource(ResourceScope.SHARED, name);
    }

    /**
     * Register a Javascript resource that will be loaded in Rendering phase If mode is null, script will be loaded with mode
     * defined in gatein-resources.xml
     */
    public void loadScriptResource(ResourceScope scope, String name) {
        if (scope == null) {
            throw new IllegalArgumentException("scope can't be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name can't be null");
        }
        ResourceId id = new ResourceId(scope, name);
        JavascriptConfigService service = getJavascriptConfigService();
        ScriptResource resource = service.getResource(id);
        if (resource != null) {
            if (FetchMode.IMMEDIATE.equals(resource.getFetchMode())) {
                resourceIds.add(id, null);
            } else {
                Map<ResourceId, FetchMode> tmp = new HashMap<>();
                tmp.put(id, null);
                for (ScriptResource res : service.resolveIds(tmp).keySet()) {
                    require(res.getId().toString());
                }
            }
        }
    }

    public FetchMap<ResourceId> getScriptResources() {
        return resourceIds;
    }

    public List<String> getExtendedScriptURLs() {
        return new LinkedList<>(extendedScriptURLs);
    }

    public void addExtendedScriptURLs(String url) {
        this.extendedScriptURLs.add(url);
    }

    public void addOnLoadJavascript(CharSequence s) {
        if (s != null) {
            String id = Integer.toString(Math.abs(s.hashCode()));
            StringBuilder script = new StringBuilder("base.Browser.addOnLoadCallback('mid");
            script.append(id);
            script.append("',");
            script.append(s instanceof String ? (String) s : s.toString());
            script.append(");");
            requireJS.addScripts(script.toString());
        }
    }

    public void addOnResizeJavascript(CharSequence s) {
        if (s != null) {
            String id = Integer.toString(Math.abs(s.hashCode()));
            StringBuilder script = new StringBuilder();
            script.append("base.Browser.addOnResizeCallback('mid");
            script.append(id);
            script.append("',");
            script.append(s instanceof String ? (String) s : s.toString());
            script.append(");");
            requireJS.addScripts(script.toString());
        }
    }

    public void addOnScrollJavascript(CharSequence s) {
        if (s != null) {
            String id = Integer.toString(Math.abs(s.hashCode()));
            StringBuilder script = new StringBuilder();
            script.append("base.Browser.addOnScrollCallback('mid");
            script.append(id);
            script.append("',");
            script.append(s instanceof String ? (String) s : s.toString());
            script.append(");");
            requireJS.addScripts(script.toString());
        }
    }

    public void addCustomizedOnLoadScript(CharSequence s) {
        if (s != null) {
            customizedOnloadJavascript.append("try {");
            customizedOnloadJavascript.append(s.toString().trim());
            customizedOnloadJavascript.append(";\n");
            customizedOnloadJavascript.append("} catch(unhandledError) { console.error(unhandledError); }");
        }
    }

    /**
     * Returns javascripts which were added by {@link #addJavascript(CharSequence)}, {@link #addOnLoadJavascript(CharSequence)},
     * {@link #addOnResizeJavascript(CharSequence)}, {@link #addOnScrollJavascript(CharSequence)},
     * {@link #addCustomizedOnLoadScript(CharSequence)}, {@link #requireJS}
     *
     * @return
     */
    public String getJavaScripts() {
        StringBuilder callback = new StringBuilder();
        callback.append(scripts);
        callback.append(requireJS.addScripts("typeof base !== 'undefined' && base?.Browser && base.Browser.onLoad();").addScripts(customizedOnloadJavascript.toString())
                .toString());
        return callback.toString();
    }

    /**
     * Return a map of JS resource ids (required to be load for current page) and boolean:
     * true if that script should be push on the header before html.
     * false if that script should be load lazily after html has been loaded <br>
     *
     * JS resources always contains SHARED/bootstrap required to be loaded eagerly
     * and optionally (by configuration) contains: portal js, portlet js, and resouces registered to be load
     * through JavascriptManager
     *
     * @return
     */
     public Map<String, Boolean> getPageScripts() {
        JavascriptConfigService service = getJavascriptConfigService();
        FetchMap<ResourceId> pageResourceIds = new FetchMap<>();

        Set<ResourceId> permanentResources = resourceIds.keySet();
        for (ResourceId resourceId : permanentResources) {
          pageResourceIds.add(resourceId);
        }

        Map<String, Boolean> result = new LinkedHashMap<>();

        Set<String> noAlias = requireJS.getNoAlias();
        for (String moduleId : noAlias) {
          String[] moduleParts = StringUtils.split(moduleId, "/");
          ResourceId resourceId = new ResourceId(ResourceScope.valueOf(moduleParts[0]),
                                                 StringUtils.join(moduleParts, "/", 1, moduleParts.length));
          pageResourceIds.add(resourceId);
        }

        Set<String> modulesWithAlias = requireJS.getDepends().keySet();
        for (String moduleAlias : modulesWithAlias) {
          String moduleId = requireJS.getDepends().get(moduleAlias);
          String[] moduleParts = StringUtils.split(moduleId, "/");
          ResourceId resourceId = new ResourceId(ResourceScope.valueOf(moduleParts[0]),
                                                 StringUtils.join(moduleParts, "/", 1, moduleParts.length));
          pageResourceIds.add(resourceId);
        }

        Map<ScriptResource, FetchMode> resolvedPageResources = service.resolveIds(pageResourceIds);

        for (ScriptResource rs : resolvedPageResources.keySet()) {
          ScriptGroup group = rs.getGroup();
          if (group != null) {
            Set<ResourceId> dependencies = group.getDependencies();
            for (ResourceId moduleId : dependencies) {
              ScriptResource moduleScriptResource = service.getResource(moduleId);
              if (moduleScriptResource != null && moduleScriptResource.getClosure() != null) {
                addDependencies(service, result, moduleScriptResource.getClosure());
              }
            }
          }
          addResourceWithDependencies(service, result, rs);
        }
        for (String url : getExtendedScriptURLs()) {
          result.put(url, true);
        }
        return result;
    }

    public RequireJS require(String moduleId) {
        return require(moduleId, null);
    }

    public RequireJS require(String moduleId, String alias) {
        return requireJS.require(moduleId, alias);
    }

    public RequireJS getRequireJS() {
        return requireJS;
    }

    public String generateUUID() {
        return "uniq-" + UUID.randomUUID().toString();
    }

    public JavascriptConfigService getJavascriptConfigService() {
      if (javascriptConfigService == null) {
        javascriptConfigService = ExoContainerContext.getService(JavascriptConfigService.class);
      }
      return javascriptConfigService;
    }

    private void addResourceWithDependencies(JavascriptConfigService service,
                                             Map<String, Boolean> result,
                                             ScriptResource scriptResource) {
      Set<ResourceId> dependencies = addResource(service, result, scriptResource);

      addDependencies(service, result, dependencies);
    }

    private void addDependencies(JavascriptConfigService service,
                                 Map<String, Boolean> result,
                                 Set<ResourceId> dependencies) {
      for (ResourceId dependencyId : dependencies) {
        ScriptResource dependencyResource = service.getResource(dependencyId);
        if (dependencyResource != null) {
          addResourceWithDependencies(service, result, dependencyResource);
        } else if (PropertyManager.isDevelopping()) {
          LOG.warn("Can't find dependent resource {}", dependencyId);
        }
      }
    }

    private Set<ResourceId> addResource(JavascriptConfigService service,
                                        Map<String, Boolean> result,
                                        ScriptResource scriptResource) {
      ResourceId id = scriptResource.getId();
      Set<ResourceId> dependencies = service.getResource(id).getClosure();

      boolean isRemote = !scriptResource.isEmpty() && scriptResource.getModules().get(0) instanceof Module.Remote;
      result.put(id.toString(), isRemote);
      return dependencies;
    }

}
