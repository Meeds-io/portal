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

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.exoplatform.portal.resource.SkinDependentManager;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.resource.config.xml.SkinConfigParser;

import jakarta.servlet.ServletContext;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by eXoPlatform SAS Author: Minh Hoang TO - hoang281283@gmail.com Sep
 * 16, 2009
 */
public class PortalSkinTask extends AbstractSkinModule implements SkinConfigTask {

  @Getter
  @Setter
  private String    moduleName;

  @Getter
  @Setter
  protected boolean filtered;

  public PortalSkinTask() {
    super(null);
    this.overwrite = true;
  }

  @Override
  public void execute(SkinService skinService, ServletContext scontext) {
    if (moduleName == null || cssPath == null) {
      return;
    }
    if (skinName == null) {
      skinName = skinService.getDefaultSkin();
    }
    String contextPath = scontext.getContextPath();
    String fullCSSPath = contextPath + cssPath;
    int priority;
    try {
      priority = Integer.valueOf(cssPriority);
    } catch (Exception e) {
      priority = Integer.MAX_VALUE;
    }
    skinService.addPortalSkin(moduleName, skinName, fullCSSPath, priority, overwrite, filtered);
    updateSkinDependentManager(contextPath, moduleName, skinName);
  }

  @Override
  public void binding(Element elemt) {
    bindingCSSPath(elemt);
    bindingSkinName(elemt);
    bindingModuleName(elemt);
    bindingOverwrite(elemt);
    bindingCSSPriority(elemt);
    bindingFiltered(elemt);
  }

  private void bindingModuleName(Element element) {
    NodeList nodes = element.getElementsByTagName(SkinConfigParser.SKIN_MODULE_TAG);
    if (nodes == null || nodes.getLength() < 1) {
      return;
    }
    moduleName = nodes.item(0).getFirstChild().getNodeValue();
  }

  private void bindingFiltered(Element element) {
    NodeList nodes = element.getElementsByTagName(SkinConfigParser.FILTERED);
    if (nodes == null || nodes.getLength() < 1) {
      return;
    }
    String isFiltered = nodes.item(0).getFirstChild().getNodeValue();
    setFiltered("true".equals(isFiltered));
  }

  /** Update skinDependentManager as it is needed to undeploy skin at runtime */
  private void updateSkinDependentManager(String webApp, String moduleName, String skinName) {
    SkinDependentManager.addPortalSkin(webApp, moduleName, skinName);
    SkinDependentManager.addSkinDeployedInApp(webApp, skinName);

    // Remark: Invoked only in PortalSkinTask
    SkinDependentManager.addDependentAppToSkinName(skinName, webApp);
  }
}
