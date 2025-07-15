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
package org.exoplatform.webui.application.portlet;

import org.exoplatform.webui.application.StateManager;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIApplication;

public class ParentAppStateManager extends StateManager {

  /**
   * This method simply delegate the call to the same method of the parent
   * WebuiRequestContext
   */
  public UIApplication restoreUIRootComponent(WebuiRequestContext context) {
    WebuiRequestContext pcontext = (WebuiRequestContext) context.getParentAppRequestContext();
    return pcontext.getStateManager().restoreUIRootComponent(context);
  }

  public void storeUIRootComponent(WebuiRequestContext context) {
    WebuiRequestContext pcontext = context;
    while (pcontext.getParentAppRequestContext() != null) {
      pcontext = (WebuiRequestContext) context.getParentAppRequestContext();
    }
    pcontext.getStateManager().storeUIRootComponent(context);
  }

}
