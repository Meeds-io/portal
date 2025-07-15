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

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.lifecycle.Lifecycle;

import javax.portlet.WindowState;

/**
 * Created by The eXo Platform SARL Author : Tuan Nguyen
 * tuan08@users.sourceforge.net May 8, 2006
 */
public class UIPageLifecycle extends Lifecycle<UIPage> {

  @Override
  public void processRender(UIPage uiPage, WebuiRequestContext context) throws Exception {
    uiPage.normalizePortletWindowStates();
    UIPortlet maximizedUIPortlet = PortalRequestContext.getCurrentInstance().getMaximizedUIPortlet();
    if (maximizedUIPortlet != null) {
      maximizedUIPortlet.setCurrentWindowState(WindowState.MAXIMIZED);
      maximizedUIPortlet.processRender(context);
      return;
    }
    super.processRender(uiPage, context);
  }

}
