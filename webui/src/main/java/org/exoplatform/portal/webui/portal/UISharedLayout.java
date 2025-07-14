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
package org.exoplatform.portal.webui.portal;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.page.UISiteBody;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;

@ComponentConfig
public class UISharedLayout extends UIContainer {

  private String metaPortal;

  @Override
  public List<UIComponent> getChildren() {
    PortalRequestContext portalRequestContext = PortalRequestContext.getCurrentInstance();
    if (isShowSharedLayout(portalRequestContext)) {
      return getSharedLayoutChildren();
    } else {
      return getSiteLayoutChildren();
    }
  }

  public boolean isShowSharedLayout(PortalRequestContext requestContext) {
    boolean showSharedLayout = !requestContext.isHideSharedLayout()
                               && !hidePageSharedLayout(requestContext.getUiPage());
    UserPortalConfig userPortalConfig = requestContext.getUserPortalConfig();
    if (userPortalConfig != null && userPortalConfig.getPortalConfig() != null) {
      showSharedLayout = showSharedLayout
                         && requestContext.getSiteType() != SiteType.GROUP_TEMPLATE
                         && requestContext.getSiteType() != SiteType.PORTAL_TEMPLATE
                         && requestContext.getSiteType() != SiteType.DRAFT
                         && (requestContext.getSiteType() != SiteType.PORTAL
                             || showSiteSharedLayout(userPortalConfig.getPortalConfig())
                             || showPageSharedLayout(requestContext.getUiPage()));
    }
    return showSharedLayout;
  }

  protected List<UIComponent> getSiteLayoutChildren() {
    UISiteBody uiSiteBody = findFirstComponentOfType(UISiteBody.class, getSharedLayoutChildren());
    return Collections.singletonList(uiSiteBody);
  }

  protected List<UIComponent> getSharedLayoutChildren() {
    return super.getChildren();
  }

  private boolean showSiteSharedLayout(PortalConfig site) {
    return StringUtils.equals(site.getName(), getMataPortal())
           || site.isDisplayed();
  }

  private boolean hidePageSharedLayout(UIPage uiPage) {
    return uiPage != null && uiPage.isHideSharedLayout();
  }

  private boolean showPageSharedLayout(UIPage uiPage) {
    return uiPage != null && !uiPage.isHideSharedLayout() && uiPage.isShowSharedLayout();
  }

  private String getMataPortal() {
    if (metaPortal == null) {
      metaPortal = PortalRequestContext.getCurrentInstance().getMetaPortal();
    }
    return metaPortal;
  }

}
