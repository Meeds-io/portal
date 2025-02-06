/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.webui.portal;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.page.UISiteBody;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;

@ComponentConfig(

)
public class UISharedLayout extends UIContainer {

  private String metaPortal;

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    PortalRequestContext portalRequestContext = PortalRequestContext.getCurrentInstance();
    portalRequestContext.startServerTime("UISharedLayout");
    try {
      if (isShowSharedLayout(portalRequestContext)) {
        processContainerRender(context);
      } else {
        processSiteBodyRender(context);
      }
    } finally {
      portalRequestContext.endServerTime("UISharedLayout");
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

  protected void processSiteBodyRender(WebuiRequestContext context) throws Exception {
    UISiteBody uiSiteBody = findFirstComponentOfType(UISiteBody.class);
    uiSiteBody.processRender(context);
  }

  protected void processContainerRender(WebuiRequestContext context) throws Exception {
    super.processRender(context);
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
