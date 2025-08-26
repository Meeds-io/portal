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
package org.exoplatform.portal.resource;

import java.net.URL;
import java.util.Set;

import jakarta.servlet.ServletContext;

import org.exoplatform.commons.xml.DocumentSource;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer.PortalContainerPostInitTask;
import org.exoplatform.portal.resource.config.xml.SkinConfigParser;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.gatein.wci.WebApp;

public class GateInSkinConfigDeployer extends AbstractResourceDeployer {

  /** . */
  private static final Log  log = ExoLogger.getLogger(GateInSkinConfigDeployer.class);

  /** . */
  private final SkinService skinService;

  /**
   * The name of the portal container
   */
  private final String      portalContainerName;

  public GateInSkinConfigDeployer(String portalContainerName, SkinService skinService) {
    this.skinService = skinService;
    this.portalContainerName = portalContainerName;
  }

  /**
   * @see org.exoplatform.portal.resource.AbstractResourceDeployer#add(WebApp,
   *      URL)
   */
  @Override
  protected void add(final WebApp webApp, final URL url) {
    ServletContext scontext = null;
    try {
      scontext = webApp.getServletContext();
      final PortalContainerPostInitTask task = new PortalContainerPostInitTask() {
        public void execute(ServletContext scontext, PortalContainer portalContainer) {
          skinService.registerContext(webApp);
          try {
            SkinConfigParser.processConfigResource(DocumentSource.create(url), skinService, scontext);
          } catch (Exception ex) {
            log.error("An error occurs while registering '" + GATEIN_CONFIG_RESOURCE + "' from the context '" +
                (scontext == null ? "unknown" : scontext.getServletContextName()) + "'", ex);
          }
        }
      };
      PortalContainer.addInitTask(scontext, task, portalContainerName);
    } catch (Exception ex) {
      log.error("An error occurs while registering '" + GATEIN_CONFIG_RESOURCE + "' from the context '" +
          (scontext == null ? "unknown" : scontext.getServletContextName()) + "'", ex);
    }
  }

  /**
   * @see org.exoplatform.portal.resource.AbstractResourceDeployer#remove(org.gatein.wci.WebApp)
   */
  @Override
  protected void remove(WebApp webApp) {
    String contextPath = webApp.getServletContext().getContextPath();
    try {
      skinService.removeSkins(SkinDependentManager.getPortalSkins(contextPath));
      skinService.removeSkins(SkinDependentManager.getPortletSkins(contextPath));

      /*
       * Remove skinName defined by the webApp, if no other webApps supports the
       * skinName
       */
      Set<String> supportedSkins = SkinDependentManager.getSkinNames(contextPath);
      if (supportedSkins != null) {
        for (String skin : supportedSkins) {
          if (SkinDependentManager.skinNameIsRemovable(skin, contextPath)) {
            skinService.removeSupportedSkin(skin);
          }
        }
      }

      // Update the 'skinDependentManager'
      SkinDependentManager.clearAssociatedSkins(contextPath);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    skinService.unregisterServletContext(webApp);
  }

}
