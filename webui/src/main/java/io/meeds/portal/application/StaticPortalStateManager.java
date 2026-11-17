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
package io.meeds.portal.application;

import java.util.concurrent.atomic.AtomicReference;

import org.exoplatform.portal.application.PortalStateManager;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIApplication;

import lombok.SneakyThrows;

public class StaticPortalStateManager extends PortalStateManager {

  /**
   * The application's only UI root, built by the first request
   */
  private final AtomicReference<UIApplication> uiapp = new AtomicReference<>();

  @Override
  @SneakyThrows
  public UIApplication restoreUIRootComponent(WebuiRequestContext context) {
    context.setStateManager(this);
    UIApplication application = uiapp.get();
    if (application == null) {
      // Concurrent first requests build the root once
      synchronized (this) {
        application = uiapp.get();
        if (application == null) {
          WebuiApplication app = (WebuiApplication) context.getApplication();
          application = createStaticUIApplication(context, app);
          uiapp.set(application);
        }
      }
    }
    return application;
  }

  @Override
  public void storeUIRootComponent(WebuiRequestContext context) {
    if (uiapp.get() == null) {
      synchronized (this) {
        uiapp.compareAndSet(null, context.getUIApplication());
      }
    }
  }

  @SneakyThrows
  UIApplication createStaticUIApplication(WebuiRequestContext context, WebuiApplication app) {
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

}
