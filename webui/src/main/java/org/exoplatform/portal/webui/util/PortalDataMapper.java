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
package org.exoplatform.portal.webui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.gatein.common.net.media.MediaType;
import org.gatein.pc.api.Portlet;
import org.gatein.pc.api.info.ModeInfo;
import org.gatein.pc.api.info.PortletInfo;
import org.gatein.pc.portlet.impl.info.ContainerPortletInfo;

import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PageBody;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.model.SiteBody;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.webui.application.PortletState;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.portal.webui.container.UIComponentFactory;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.page.UIPageBody;
import org.exoplatform.portal.webui.page.UISiteBody;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;

import lombok.SneakyThrows;

public class PortalDataMapper {

  public static final String SITE_LAYOUT_TEMPLATE = "system:/groovy/portal/webui/container/UISiteLayout.gtmpl";

  protected static final Log log                  = ExoLogger.getLogger("portal:PortalDataMapper");

  public static void toUIPortlet(UIPortlet uiPortlet, Application model) {
    PortletState portletState = new PortletState(model.getState());
    uiPortlet.setWidth(model.getWidth());
    uiPortlet.setHeight(model.getHeight());
    uiPortlet.setState(portletState);
    uiPortlet.setTitle(model.getTitle());
    uiPortlet.setIcon(model.getIcon());
    uiPortlet.setDescription(model.getDescription());
    uiPortlet.setShowInfoBar(model.getShowInfoBar());
    uiPortlet.setShowWindowState(model.getShowApplicationState());
    uiPortlet.setShowPortletMode(model.getShowApplicationMode());
    uiPortlet.setProperties(model.getProperties());
    uiPortlet.setTheme(model.getTheme());
    uiPortlet.setId(model.getId());
    if (model.getAccessPermissions() != null) {
      uiPortlet.setAccessPermissions(model.getAccessPermissions());
    }

    Portlet portlet = uiPortlet.getProducedOfferedPortlet();
    if (portlet == null || portlet.getInfo() == null)
      return;

    PortletInfo portletInfo = portlet.getInfo();

    /*
     * Define which portlet modes the portlet supports and hence should be shown
     * in the portlet info bar
     */
    Set<ModeInfo> modes = portletInfo.getCapabilities().getModes(MediaType.create("text/html"));
    uiPortlet.setSupportModes(modes.stream()
                                   .map(modeInfo -> modeInfo.getModeName().toLowerCase())
                                   .filter(mode -> modes.size() == 1 || !"view".equals(mode))
                                   .toList());
    if (portletInfo instanceof ContainerPortletInfo containerPortletInfo) {
      uiPortlet.setCssClass(containerPortletInfo.getInitParameter("layout-css-class"));
    }
  }

  public static void toUIContainer(UIContainer uiContainer, Container model) {
    uiContainer.setStorageId(model.getStorageId());
    uiContainer.setId(model.getId());
    uiContainer.setWidth(model.getWidth());
    uiContainer.setHeight(model.getHeight());
    uiContainer.setProfiles(model.getProfiles());
    uiContainer.setCssClass(model.getCssClass());
    uiContainer.setTitle(model.getTitle());
    uiContainer.setIcon(model.getIcon());
    uiContainer.setDescription(model.getDescription());
    uiContainer.setFactoryId(model.getFactoryId());
    uiContainer.setName(model.getName());
    uiContainer.setTemplate(model.getTemplate());
    if (model.getAccessPermissions() != null) {
      uiContainer.setAccessPermissions(model.getAccessPermissions());
    }
    List<ModelObject> children = model.getChildren();
    if (children == null)
      return;
    for (Object child : children) {
      buildUIContainer(uiContainer, child);
    }
  }

  public static void toUIPage(UIPage uiPage, Page model) {
    toUIContainer(uiPage, model);
    uiPage.setSiteKey(new SiteKey(model.getOwnerType(), model.getOwnerId()));
    uiPage.setIcon(model.getIcon());
    uiPage.setAccessPermissions(model.getAccessPermissions());
    uiPage.setEditPermission(model.getEditPermission());
    uiPage.setFactoryId(model.getFactoryId());
    uiPage.setPageId(model.getPageId());
    uiPage.setTitle(model.getTitle());
    uiPage.setProfiles(model.getProfiles());
    uiPage.setShowMaxWindow(model.isShowMaxWindow());
    uiPage.setHideSharedLayout(model.isHideSharedLayout());
    uiPage.setShowSharedLayout(model.isShowSharedLayout());

    List<UIPortlet> portlets = new ArrayList<>();
    uiPage.findComponentOfType(portlets, UIPortlet.class);
  }

  public static void toUIPortal(UIPortal uiPortal, PortalConfig model) {
    buildUIPortal(uiPortal, model, false);
  }

  public static void toUIPortalWithMetaLayout(UIPortal uiPortal, PortalConfig model) {
    buildUIPortal(uiPortal, model, true);
  }

  private static void buildUIPortal(UIPortal uiPortal, PortalConfig model, boolean metaLayout) {
    uiPortal.setSiteKey(new SiteKey(model.getType(), model.getName()));
    uiPortal.setStorageId(model.getStorageId());
    uiPortal.setName(model.getName());
    uiPortal.setId("UIPortal");

    uiPortal.setLabel(model.getLabel());
    uiPortal.setDescription(model.getDescription());
    uiPortal.setLocale(model.getLocale());
    uiPortal.setSkin(model.getSkin());
    uiPortal.setAccessPermissions(model.getAccessPermissions());
    uiPortal.setEditPermission(model.getEditPermission());
    uiPortal.setProperties(model.getProperties());
    uiPortal.setUseDynamicLayout(model.isDefaultLayout());
    UserPortalConfigService userPortalConfigService = uiPortal.getApplicationComponent(UserPortalConfigService.class);
    PortalConfig metaSite = userPortalConfigService.getMetaPortalConfig();

    Container layout = metaLayout && model.isDisplayed() ? metaSite.getPortalLayout() : model.getPortalLayout();
    List<ModelObject> children = layout.getChildren();
    if (children != null) {
      if (StringUtils.equals(layout.getTemplate(), SITE_LAYOUT_TEMPLATE)) {
        buildUIContainer(uiPortal, layout);
      } else {
        for (ModelObject child : children) {
          buildUIContainer(uiPortal, child);
        }
      }
    }
  }

  @SneakyThrows
  private static void buildUIContainer(UIContainer uiContainer, Object model) {
    UIComponent uiComponent = null;
    WebuiRequestContext context = Util.getPortalRequestContext();

    if (model instanceof SiteBody) {
      uiComponent = uiContainer.createUIComponent(context, UISiteBody.class, null, null);
    } else if (model instanceof PageBody) {
      uiComponent = uiContainer.createUIComponent(context, UIPageBody.class, null, null);
    } else if (model instanceof Application application) {
      UIPortlet uiPortlet = uiContainer.createUIComponent(context, UIPortlet.class, null, null);
      uiPortlet.setStorageId(application.getStorageId());
      if (application.getStorageName() != null) {
        uiPortlet.setStorageName(application.getStorageName());
      } else {
        uiPortlet.setStorageName(application.getStorageId());
      }
      toUIPortlet(uiPortlet, application);
      uiComponent = uiPortlet;
    } else if (model instanceof Container container) {
      UIComponentFactory<? extends UIContainer> factory = UIComponentFactory.getInstance(UIContainer.class);
      UIContainer uiTempContainer = factory.createUIComponent(container.getFactoryId(), context);

      if (uiTempContainer == null) {
        log.warn("Can't find container factory for: {}. Default container is used", container.getFactoryId());
        uiTempContainer = uiContainer.createUIComponent(context, UIContainer.class, null, null);
      }

      toUIContainer(uiTempContainer, (Container) model);
      uiComponent = uiTempContainer;
    }
    uiContainer.addChild(uiComponent);
  }

}
