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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.BaseContainerLifecyclePlugin;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer.PortalContainerPostCreateTask;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.web.application.javascript.JavascriptConfigService;

import jakarta.servlet.ServletContext;

public class ResourceInitContainerLifecyclePlugin extends BaseContainerLifecyclePlugin {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceInitContainerLifecyclePlugin.class);

  @Override
  public void startContainer(ExoContainer container) throws Exception {
    if (container instanceof PortalContainer portalContainer && !PropertyManager.isDevelopping()) {
      PortalContainer.addInitTask(portalContainer.getPortalContext(), new PortalContainerPostCreateTask() {
        @Override
        public void execute(ServletContext context, PortalContainer portalContainer) {
          LOG.info("Proceed on resources caching initialization");
          container.getComponentInstanceOfType(JavascriptConfigService.class).initData();
          container.getComponentInstanceOfType(SkinService.class).initData();
        }
      });
    }
  }

}
