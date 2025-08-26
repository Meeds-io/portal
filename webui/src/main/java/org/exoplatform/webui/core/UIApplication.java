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
package org.exoplatform.webui.core;

import java.io.Serializable;
import java.io.Writer;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * This is abstract class for Root WebUI component of applications in GateIn.
 * <br>
 * Act as container of WebUI components, it also provide method to show a Popup
 * message
 */
@Serialized
public abstract class UIApplication extends UIContainer implements Serializable {

  private static final String DIV_BLOCK_TO_UPDATE_DATA = "<div class=\"BlockToUpdateData\">";

  private static final String DIV_BLOCK_TO_UPDATE_ID   = "<div class=\"BlockToUpdateId\">";

  private static final String DIV_BLOCK_TO_UPDATE      = "<div class=\"BlockToUpdate\">";

  private static final String END_DIV                  = "</div>";

  private static final long   serialVersionUID         = -8410121291107766699L;

  protected static final Log  LOG                      = ExoLogger.getLogger("portal:UIApplication");

  private static final String UI_APPLICATION           = "uiapplication";

  @Override
  public String getUIComponentName() {
    return UI_APPLICATION;
  }

  /**
   * Wrap the action processing by a try catch, if there is exceptions, add a
   * Log Trace only
   */
  @Override
  public void processAction(WebuiRequestContext context) throws Exception {
    try {
      super.processAction(context);
    } catch (Exception e) {
      LOG.error("Error during the processAction phase", e);
    }
  }

  /**
   * Triggered when there is Ajax request. <br>
   * This method add xml structure that help PortalHttpRequest.js parse the
   * response
   */
  public void renderBlockToUpdate(UIComponent uicomponent, WebuiRequestContext context, Writer w) throws Exception {
    w.write(DIV_BLOCK_TO_UPDATE);
    w.append(DIV_BLOCK_TO_UPDATE_ID).append(uicomponent.getId()).append(END_DIV);
    w.write(DIV_BLOCK_TO_UPDATE_DATA);
    uicomponent.processRender(context);
    w.write(END_DIV);
    w.write(END_DIV);
  }
}
