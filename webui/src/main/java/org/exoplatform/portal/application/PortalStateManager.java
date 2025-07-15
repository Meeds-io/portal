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
package org.exoplatform.portal.application;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.SneakyThrows;

import org.exoplatform.commons.utils.Safe;
import org.exoplatform.portal.application.replication.ApplicationState;
import org.exoplatform.webui.application.ConfigurationManager;
import org.exoplatform.webui.application.StateManager;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;

public class PortalStateManager extends StateManager {

  /** . */
  private static final String APPLICATION_ATTRIBUTE_PREFIX = "psm.";

  /** . */
  private static final Log    log                          = ExoLogger.getLogger(PortalStateManager.class);

  @Override
  @SneakyThrows
  public UIApplication restoreUIRootComponent(WebuiRequestContext context) {
    context.setStateManager(this);

    //
    WebuiApplication app = (WebuiApplication) context.getApplication();

    //
    ApplicationState appState = null;
    HttpSession session = getSession();
    String key = getKey(context);
    if (session != null) {
      appState = (ApplicationState) session.getAttribute(APPLICATION_ATTRIBUTE_PREFIX + key);
    }

    UIApplication uiapp = null;
    if (appState != null) {
      if (log.isDebugEnabled()) {
        log.debug("Found application " + key + " :" + appState.getApplication());
      }
      if (Safe.equals(context.getRemoteUser(), appState.getUserName())) {
        uiapp = appState.getApplication();
      }
    } else {
      log.debug("Application " + key + " not found");
    }

    //
    if (uiapp == null) {
      ConfigurationManager cmanager = app.getConfigurationManager();
      String uirootClass = cmanager.getApplication().getUIRootComponent().trim();
      Class<? extends UIApplication> type = (Class<UIApplication>) Class.forName(uirootClass,
                                                                                 true,
                                                                                 Thread.currentThread().getContextClassLoader());
      uiapp = app.createUIComponent(type, null, null, context);
    }

    //
    return uiapp;
  }

  @Override
  public void storeUIRootComponent(final WebuiRequestContext context) {
    UIApplication uiapp = context.getUIApplication();

    //
    if (uiapp != null) {
      HttpSession session = getSession();

      // At this point if it returns null it means that it was not possible to
      // create a session
      // because the session might be invalidated and the response is already
      // commited to the client.
      // That situation happens during a logout that invalidates the HttpSession
      if (session != null) {
        String key = getKey(context);
        log.debug("Storing application " + key);
        session.setAttribute(APPLICATION_ATTRIBUTE_PREFIX + key, new ApplicationState(uiapp, context.getRemoteUser()));
      }
    }
  }

  private String getKey(WebuiRequestContext webuiRC) {
    if (webuiRC instanceof PortletRequestContext portletRC) {
      return portletRC.getApplication().getApplicationId() + "/" + portletRC.getWindowId();
    } else {
      return PortalApplication.PORTAL_APPLICATION_ID;
    }
  }

  private HttpSession getSession() {
    HttpServletRequest req = PortalRequestContext.getCurrentInstance().getRequest();
    return req.getSession(false);
  }

}
