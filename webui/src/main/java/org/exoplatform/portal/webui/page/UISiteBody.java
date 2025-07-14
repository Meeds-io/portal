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
package org.exoplatform.portal.webui.page;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIComponentDecorator;

@ComponentConfig(template = "system:/groovy/portal/webui/page/UISiteBody.gtmpl")
public class UISiteBody extends UIComponentDecorator {

  @Override
  public UIComponent getUIComponent() {
    if (isShowSiteBody()) {
      return getSiteComponent();
    } else {
      if (PortalRequestContext.getCurrentInstance().isMaximizePortlet()) {
        return getMaximizedPortlet();
      } else {
        return getPageComponent();
      }
    }
  }

  /**
   * @return Site CSS class. Used in gtmpl in order to allow specifying a
   *         specific CSS selector for current Site
   */
  public String getSiteClass() {
    String portalOwner = PortalRequestContext.getCurrentInstance().getPortalOwner();
    if (StringUtils.isBlank(portalOwner)) {
      return "";
    } else {
      return portalOwner.toUpperCase() + "Site";
    }
  }

  protected UIComponent getSiteComponent() {
    return PortalRequestContext.getCurrentInstance().getUiPortal();
  }

  protected UIComponent getPageComponent() {
    return getSiteComponent().findFirstComponentOfType(UIPageBody.class);
  }

  protected UIComponent getMaximizedPortlet() {
    return PortalRequestContext.getCurrentInstance().getMaximizedUIPortlet();
  }

  protected boolean isShowSiteBody() {
    PortalRequestContext requestContext = PortalRequestContext.getCurrentInstance();
    return !requestContext.isShowMaxWindow()
           && (requestContext.getUiPage() == null || !requestContext.getUiPage().isShowMaxWindow());
  }

}
