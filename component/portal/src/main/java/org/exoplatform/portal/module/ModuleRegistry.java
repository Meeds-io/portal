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
package org.exoplatform.portal.module;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.gatein.common.i18n.LocalizedString;
import org.gatein.pc.api.PortletInvoker;
import org.gatein.pc.api.PortletInvokerException;
import org.gatein.pc.api.info.MetaInfo;
import org.picocontainer.Startable;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Main registry to store and manage eXo Platform modules. Modules can be
 * registered and also activated.<br>
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Jun
 * 24, 2010
 */
public class ModuleRegistry implements Startable {

  private static final Log             LOG                           = ExoLogger.getExoLogger(ModuleRegistry.class);

  private Map<String, Boolean>         isPortletActiveCache          = new ConcurrentHashMap<>();

  private boolean                      isPortletDisplayNamesImported = false;

  /**
   * modules indexed by name
   * 
   * @see Module#getName()
   */
  private Map<String, Module>          modulesByName                 = new HashMap<>();

  /**
   * modules indexed by webapp name
   */
  private Map<String, Set<Module>>     modulesByWebapp               = new HashMap<>();

  /**
   * modules indexed by portlet name
   */
  private Map<String, Set<Module>>     modulesByPortlet              = new HashMap<>();

  /**
   * modules indexed by portlet name
   */
  private Map<String, LocalizedString> portletDisplayNames           = new HashMap<>();

  public ModuleRegistry(InitParams initParams) {
    if (initParams != null) {
      Iterator<Module> iterator = initParams.getObjectParamValues(Module.class).iterator();
      while (iterator.hasNext()) {
        Module module = iterator.next();
        modulesByName.put(module.getName(), module);
      }
    }
  }

  /**
   * Add a module by plugin injection
   * 
   * @param modulePlugin module component plugin
   */
  public void addModule(ModulePlugin modulePlugin) {
    if (modulePlugin != null && !modulePlugin.getModulesByName().isEmpty()) {
      modulesByName.putAll(modulePlugin.getModulesByName());
    }
  }

  @Override
  public void start() {
    // Compute Modules by webapp & by portletId (= webapp/portletName )
    Collection<Module> modules = getAvailableModules();
    for (Module module : modules) {
      for (String webappName : module.getWebapps()) {
        Set<Module> webappModules = modulesByWebapp.get(webappName);
        if (webappModules == null) {
          webappModules = new HashSet<>();
          modulesByWebapp.put(webappName, webappModules);
        }
        webappModules.add(module);
      }
    }
    for (Module module : modules) {
      if (module.getPortlets() != null && !module.getPortlets().isEmpty()) {
        for (String portletId : module.getPortlets()) {
          if (!portletId.contains("/")) {
            LOG.warn(portletId + " isn't a valid portlet ID, it have to be something like: {webappName}/{portletName}.");
            continue;
          }
          Set<Module> portletModules = modulesByPortlet.get(portletId);
          if (portletModules == null) {
            portletModules = new HashSet<>();
            modulesByPortlet.put(portletId, portletModules);

            // Add related webapp modules to this portletId modules
            String[] portletIdSplitted = portletId.split("/");
            String webappName = portletIdSplitted[0];
            Set<Module> webappModules = modulesByWebapp.get(webappName);
            if (webappModules != null && !webappModules.isEmpty()) {
              portletModules.addAll(webappModules);
            }
          }
          portletModules.add(module);
        }
      }
    }
  }

  public String getDisplayName(String portletName, Locale locale) {
    String portletDisplayName = portletName;
    if (portletDisplayNames.get(portletName) != null) {
      portletDisplayName = portletDisplayNames.get(portletName).getValue(locale, true).getString();
    } else if (!isPortletDisplayNamesImported) {
      PortletInvoker portletInvoker = (PortletInvoker) PortalContainer.getComponent(PortletInvoker.class);
      try {
        Set<org.gatein.pc.api.Portlet> portlets = portletInvoker.getPortlets();
        for (org.gatein.pc.api.Portlet portlet : portlets) {
          portletDisplayNames.put(portlet.getInfo().getName(), portlet.getInfo().getMeta().getMetaValue(MetaInfo.DISPLAY_NAME));
        }
        isPortletDisplayNamesImported = true;
      } catch (PortletInvokerException exception) {
        LOG.error("Error occurred when trying to import portlets", exception);
      }
      if (portletDisplayNames.get(portletName) != null) {
        portletDisplayName = portletDisplayNames.get(portletName).getValue(locale, true).getString();
      }
    }
    return portletDisplayName;
  }

  /**
   * @param webappName web archive application name
   * @return List of profiles/modules that activate a webapp
   */
  public Set<String> getModulesForWebapp(String webappName) {
    Set<String> profileNames = new HashSet<>();
    Set<Module> webappModules = modulesByWebapp.get(webappName);
    if (webappModules != null && !webappModules.isEmpty()) {
      for (Module module : webappModules) {
        profileNames.add(module.getName());
      }
    }
    return profileNames;
  }

  /**
   * @param portletId with form : applicationName/portletName
   * @return List of profiles/modules that activate a portlet
   */
  public Set<String> getModulesForPortlet(String portletId) {
    Set<String> profileNames = new HashSet<>();
    Set<Module> portletModules = modulesByPortlet.get(portletId);
    if (portletModules == null || portletModules.isEmpty()) {
      // Add related webapp modules to this portletId modules too
      String[] portletIdSplitted = portletId.split("/");
      String webappName = portletIdSplitted[0];
      profileNames = getModulesForWebapp(webappName);
    } else {
      for (Module module : portletModules) {
        profileNames.add(module.getName());
      }
    }
    return profileNames;
  }

  public boolean isPortletActive(String portletId) {
    // Read from cache
    Boolean isPortletActive = isPortletActiveCache.get(portletId);
    if (isPortletActive != null) {
      return isPortletActive;
    }
    // Read active profiles
    Set<String> portletActiveProfiles = getModulesForPortlet(portletId);
    if (portletActiveProfiles == null || portletActiveProfiles.isEmpty()) {
      isPortletActive = true;
    } else {
      isPortletActive = ExoContainer.getProfiles()
                                    .stream()
                                    .noneMatch(portletActiveProfiles::contains);
    }
    isPortletActiveCache.put(portletId, isPortletActive);
    return isPortletActive;
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  /**
   * Get all available modules
   * 
   * @return the list of all modules registered
   * @see #registerModule(Module)
   */
  private Collection<Module> getAvailableModules() {
    return modulesByName.values();
  }

}
