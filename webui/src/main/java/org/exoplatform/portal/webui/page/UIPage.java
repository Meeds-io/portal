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

import java.util.ArrayList;
import java.util.List;

import javax.portlet.WindowState;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;

import lombok.Getter;
import lombok.Setter;

/**
 * May 19, 2006
 */
@ComponentConfig(lifecycle = UIPageLifecycle.class, template = "system:/groovy/portal/webui/page/UIPage.gtmpl")
public class UIPage extends UIContainer {

  /** . */
  @Getter
  @Setter
  private String  pageId;

  @Getter
  @Setter
  private SiteKey siteKey;

  @Getter
  @Setter
  private String  editPermission;

  @Getter
  @Setter
  private boolean showMaxWindow    = false;

  @Getter
  @Setter
  private boolean hideSharedLayout = false;

  @Getter
  @Setter
  private boolean showSharedLayout = false;

  public void normalizePortletWindowStates() {
    for (UIPortlet childUIPortlet : recursivelyFindUIPortlets(this)) {
      if (!WindowState.MINIMIZED.equals(childUIPortlet.getCurrentWindowState())) {
        childUIPortlet.setCurrentWindowState(WindowState.NORMAL);
      }
    }
  }

  /**
   * @deprecated use getSiteKey() instead
   * @return
   */
  @Deprecated
  public String getOwnerType() {
    return getSiteKey().getTypeName();
  }

  /**
   * @deprecated use getSiteKey() instead
   * @return
   */
  @Deprecated
  public String getOwnerId() {
    return getSiteKey().getName();
  }

  @Override
  public boolean hasAccessPermission() {
    return ExoContainerContext.getService(UserACL.class)
                              .hasAccessPermission(PortalRequestContext.getCurrentInstance().getPage(),
                                                   ConversationState.getCurrent().getIdentity());
  }

  private List<UIPortlet> recursivelyFindUIPortlets(org.exoplatform.webui.core.UIContainer uiContainer) {
    List<UIPortlet> uiPortletList = new ArrayList<>();
    for (UIComponent uiComponent : uiContainer.getChildren()) {
      if (org.exoplatform.webui.core.UIContainer.class.isAssignableFrom(uiComponent.getClass())) {
        org.exoplatform.webui.core.UIContainer childUIContainer = (org.exoplatform.webui.core.UIContainer) uiComponent;
        if (UIPortlet.class.isAssignableFrom(childUIContainer.getClass())) {
          uiPortletList.add((UIPortlet) childUIContainer);
        } else {
          uiPortletList.addAll(recursivelyFindUIPortlets(childUIContainer));
        }
      }
    }
    return uiPortletList;
  }
}
