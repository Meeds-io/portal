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
package org.exoplatform.web.application;

import java.util.List;

import org.exoplatform.container.PortalContainer;

/**
 * This is a single application lifecycle that is added in
 * webui-configuration.xml as point of extension to trigger real listeners
 * injected via Kernel configuration on service
 * {@link ApplicationLifecycleExtension}
 */
public class ExtensiblePortalApplicationLifecycle implements ApplicationLifecycle<RequestContext> {

  private ApplicationLifecycleExtension applicationLifecycleExtension;

  public void onInit(Application app) throws Exception {
    for (ApplicationLifecycle<RequestContext> applicationLifecycle : getApplicationLifecycles()) {
      applicationLifecycle.onInit(app);
    }
  }

  public void onStartRequest(final Application app, final RequestContext context) throws Exception {
    for (ApplicationLifecycle<RequestContext> applicationLifecycle : getApplicationLifecycles()) {
      applicationLifecycle.onStartRequest(app, context);
    }
  }

  public void onFailRequest(Application app, RequestContext context, RequestFailure failureType) {
    for (ApplicationLifecycle<RequestContext> applicationLifecycle : getApplicationLifecycles()) {
      applicationLifecycle.onFailRequest(app, context, failureType);
    }
  }

  public void onEndRequest(Application app, RequestContext context) throws Exception {
    for (ApplicationLifecycle<RequestContext> applicationLifecycle : getApplicationLifecycles()) {
      applicationLifecycle.onEndRequest(app, context);
    }
  }

  public void onDestroy(Application app) throws Exception {
    for (ApplicationLifecycle<RequestContext> applicationLifecycle : getApplicationLifecycles()) {
      applicationLifecycle.onDestroy(app);
    }
  }

  private List<ApplicationLifecycle<RequestContext>> getApplicationLifecycles() {
    if (applicationLifecycleExtension == null) {
      applicationLifecycleExtension =
                                    PortalContainer.getInstance().getComponentInstanceOfType(ApplicationLifecycleExtension.class);
    }
    return applicationLifecycleExtension.getPortalApplicationLifecycles();
  }
}
