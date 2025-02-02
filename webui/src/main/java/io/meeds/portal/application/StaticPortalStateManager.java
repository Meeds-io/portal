/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.portal.application;

import org.exoplatform.commons.api.settings.ExoFeatureService;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalApplication;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.application.PortalStateManager;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.core.UIApplication;

import io.meeds.portal.application.model.StaticApplicationState;

import jakarta.servlet.ServletContext;
import lombok.SneakyThrows;

public class StaticPortalStateManager extends PortalStateManager {

  private static final String PORTAL_STATIC_WEB_UI_FEATURE = "portal.staticWebUIComponents";

  private static final String APPLICATION_ATTRIBUTE_PREFIX = "portal.";

  private ExoFeatureService   featureService;

  @Override
  @SneakyThrows
  public UIApplication restoreUIRootComponent(WebuiRequestContext context) {
    if (getFeatureService().isActiveFeature(PORTAL_STATIC_WEB_UI_FEATURE)) {
      context.setStateManager(this);
      WebuiApplication app = (WebuiApplication) context.getApplication();

      ServletContext servletContext = getServletContext();
      String key = getStaticKey(context);

      StaticApplicationState appState = null;
      if (servletContext != null) {
        appState = (StaticApplicationState) servletContext.getAttribute(APPLICATION_ATTRIBUTE_PREFIX + key);
      }
      return appState == null ? createStaticUIApplication(context, app) : appState.getApplication();
    } else {
      return super.restoreUIRootComponent(context);
    }
  }

  @Override
  public void storeUIRootComponent(WebuiRequestContext context) {
    if (getFeatureService().isActiveFeature(PORTAL_STATIC_WEB_UI_FEATURE)) {
      UIApplication uiapp = context.getUIApplication();
      if (uiapp != null) {
        getServletContext().setAttribute(APPLICATION_ATTRIBUTE_PREFIX + getStaticKey(context),
                                         new StaticApplicationState(uiapp));
      }
    } else {
      super.storeUIRootComponent(context);
    }
  }

  @SneakyThrows
  private UIApplication createStaticUIApplication(WebuiRequestContext context, WebuiApplication app) {
    String uiRootClass = app.getConfigurationManager()
                            .getApplication()
                            .getUIRootComponent()
                            .trim();
    @SuppressWarnings("unchecked")
    Class<? extends UIApplication> type = (Class<UIApplication>) Class.forName(uiRootClass,
                                                                               true,
                                                                               Thread.currentThread().getContextClassLoader());
    return app.createUIComponent(type, null, null, context);
  }

  private String getStaticKey(WebuiRequestContext webuiRC) {
    if (webuiRC instanceof PortletRequestContext portletRC) {
      return portletRC.getApplication().getApplicationId() + "/" + portletRC.getWindowId();
    } else {
      return PortalApplication.PORTAL_APPLICATION_ID;
    }
  }

  private ServletContext getServletContext() {
    return PortalRequestContext.getCurrentInstance().getRequest().getServletContext();
  }

  public ExoFeatureService getFeatureService() {
    if (featureService == null) {
      featureService = ExoContainerContext.getService(ExoFeatureService.class);
    }
    return featureService;
  }

}
