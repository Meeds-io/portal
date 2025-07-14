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
package org.exoplatform.portal.resource.config.tasks;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.exoplatform.portal.resource.SkinDependentManager;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.resource.config.xml.SkinConfigParser;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import jakarta.servlet.ServletContext;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by eXoPlatform SAS Author: Minh Hoang TO - hoang281283@gmail.com Sep
 * 16, 2009
 */
public class PortletSkinTask extends AbstractSkinModule implements SkinConfigTask {

  protected static final Log LOG = ExoLogger.getLogger(PortletSkinTask.class);

  @Getter
  @Setter
  private String             applicationName;

  @Getter
  @Setter
  private String             portletName;

  @Getter
  @Setter
  private List<String>       additionalModules;

  public PortletSkinTask() {
    super(null);
    this.overwrite = true;
  }

  @Override
  public void execute(SkinService skinService, ServletContext scontext) {
    if (portletName == null) {
      return;
    }
    if (skinName == null) {
      skinName = skinService.getDefaultSkin();
    }
    if (applicationName == null) {
      applicationName = scontext.getContextPath();
    }
    String moduleName = applicationName + "/" + portletName;
    String fullCSSPath = cssPath == null ? null : scontext.getContextPath() + cssPath; // NOSONAR
    int priority;
    try {
      priority = Integer.valueOf(cssPriority);
    } catch (Exception e) {
      priority = Integer.MAX_VALUE;
    }
    skinService.addSkin(moduleName, skinName, fullCSSPath, priority, overwrite, additionalModules);
    updateSkinDependentManager("/" + applicationName, moduleName, skinName);
  }

  @Override
  public void binding(Element elemt) {
    bindingApplicationName(elemt);
    bindingPortletName(elemt);
    bindingCSSPath(elemt);
    bindingSkinName(elemt);
    bindingOverwrite(elemt);
    bindingCSSPriority(elemt);
    bindingAdditionalModules(elemt);
  }

  private void bindingApplicationName(Element element) {
    NodeList nodes = element.getElementsByTagName(SkinConfigParser.APPLICATION_NAME_TAG);
    if (nodes == null || nodes.getLength() < 1) {
      return;
    }
    this.applicationName = nodes.item(0).getFirstChild().getNodeValue();
  }

  private void bindingPortletName(Element element) {
    NodeList nodes = element.getElementsByTagName(SkinConfigParser.PORTLET_NAME_TAG);
    if (nodes == null || nodes.getLength() < 1) {
      return;
    }
    this.portletName = nodes.item(0).getFirstChild().getNodeValue();
  }

  private void bindingAdditionalModules(Element element) {
    NodeList nodes = element.getElementsByTagName(SkinConfigParser.ADDITIONAL_MODULE);
    if (nodes == null || nodes.getLength() == 0) {
      return;
    }
    int length = nodes.getLength();
    List<String> filteredPortalModuleNames = new ArrayList<>();
    for (int i = 0; i < length; i++) {
      filteredPortalModuleNames.add(nodes.item(i).getFirstChild().getNodeValue());
    }
    this.additionalModules = filteredPortalModuleNames;
  }

  private void updateSkinDependentManager(String webApp, String moduleName, String skinName) {
    SkinDependentManager.addPortletSkin(webApp, moduleName, skinName);
    SkinDependentManager.addSkinDeployedInApp(webApp, skinName);
  }

}
