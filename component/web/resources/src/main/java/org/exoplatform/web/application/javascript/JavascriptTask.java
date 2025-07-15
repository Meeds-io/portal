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
package org.exoplatform.web.application.javascript;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.collections.CollectionUtils;
import org.gatein.portal.controller.resource.ResourceId;
import org.gatein.portal.controller.resource.script.ScriptResource;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import jakarta.servlet.ServletContext;
import lombok.SneakyThrows;

public class JavascriptTask {

  private static final Log               LOG         = ExoLogger.getExoLogger(JavascriptTask.class);

  private static final boolean           DEVELOPPING = PropertyManager.isDevelopping();

  private List<ScriptResourceDescriptor> descriptors;

  public JavascriptTask() {
    descriptors = new ArrayList<>();
  }

  public void execute(JavascriptConfigService javascriptService, ServletContext scontext) {
    for (ScriptResourceDescriptor desc : descriptors) {
      String contextPath = scontext.getContextPath();
      if (CollectionUtils.isNotEmpty(desc.modules)) {
        contextPath = desc.modules.get(0).getContextPath();
      }

      ScriptResource resource = javascriptService.getScriptGraph()
                                                 .addResource(desc.id, desc.fetchMode, desc.alias, desc.group, contextPath);
      if (resource != null) {
        resource.setContextPath(contextPath);
        for (Javascript module : desc.modules) {
          module.addModuleTo(resource);
        }
        for (Locale locale : desc.getSupportedLocales()) {
          resource.addSupportedLocale(locale);
        }
        for (DependencyDescriptor dependency : desc.dependencies) {
          resource.addDependency(dependency.getResourceId(), dependency.getAlias(), dependency.getPluginResource());
        }
      }
    }
  }

  public void addDescriptor(ScriptResourceDescriptor desc) {
    descriptors.add(desc);
  }

  @SneakyThrows
  public void initJSModuleCache(JavascriptConfigService javascriptService, ResourceId resourceId) {
    ExoContainerContext.setCurrentContainer(PortalContainer.getInstance());
    try {
      javascriptService.getScriptContent(resourceId.getScope(), resourceId.getName(), !DEVELOPPING);
    } catch (Exception e) {
      LOG.debug("Error while initializing cache of JS with id {}. Will reattempt in first portal request", resourceId, e);
    } finally {
      ExoContainerContext.setCurrentContainer(null);
    }
  }

}
