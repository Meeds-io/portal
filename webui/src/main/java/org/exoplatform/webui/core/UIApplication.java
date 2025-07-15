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
import org.exoplatform.web.application.AbstractApplicationMessage;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.exception.CSRFException;
import org.exoplatform.webui.exception.MessageException;

/**
 * This is abstract class for Root WebUI component of applications in GateIn.
 * <br>
 * Act as container of WebUI components, it also provide method to show a Popup
 * message
 */
@Serialized
public abstract class UIApplication extends UIContainer implements Serializable {

  protected static final Log  LOG            = ExoLogger.getLogger("portal:UIApplication");

  private long                lastAccessApplication_;

  private UIPopupMessages     uiPopupMessages_;

  private static final String UI_APPLICATION = "uiapplication";

  /**
   * Return the common UIPopupMessages
   *
   * @return UIPopupMessages
   */
  public UIPopupMessages getUIPopupMessages() {
    if (USE_WEBUI_RESOURCES && uiPopupMessages_ == null) {
      try {
        uiPopupMessages_ = createUIComponent(UIPopupMessages.class, null, null);
        uiPopupMessages_.setId("_" + uiPopupMessages_.hashCode());
      } catch (Exception e) {
        LOG.error(e.getMessage(), e);
      }
    }
    return uiPopupMessages_;
  }

  public void addMessage(AbstractApplicationMessage message) {
    if (USE_WEBUI_RESOURCES) {
      getUIPopupMessages().addMessage(message);
    }
  }

  public void addMessage(ApplicationMessage message) {
    if (USE_WEBUI_RESOURCES) {
      addMessage((AbstractApplicationMessage) message);
    }
  }

  public void clearMessages() {
    if (USE_WEBUI_RESOURCES) {
      getUIPopupMessages().clearMessages();
    }
  }

  public long getLastAccessApplication() {
    return lastAccessApplication_;
  }

  public void setLastAccessApplication(long time) {
    lastAccessApplication_ = time;
  }

  @Override
  public String getUIComponentName() {
    return UI_APPLICATION;
  }

  @SuppressWarnings("unchecked")
  public <T extends UIComponent> T findComponentById(String lookupId) {
    if (USE_WEBUI_RESOURCES && getUIPopupMessages().getId().equals(lookupId))
      return (T) getUIPopupMessages();
    return (T) super.findComponentById(lookupId);
  }

  public void renderChildren() throws Exception {
    super.renderChildren();
    if (!USE_WEBUI_RESOURCES || getUIPopupMessages() == null) {
      return;
    }
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    getUIPopupMessages().processRender(context);
  }

  /**
   * Wrap the action processing by a try catch, if there is exceptions, show a
   * popup message if no exception the processAction will be delegate to the
   * target of the action request
   */
  public void processAction(WebuiRequestContext context) throws Exception {
    try {
      super.processAction(context);
    } catch (MessageException ex) {
      addMessage(ex.getDetailMessage());
    } catch (CSRFException e) {
      context.getJavascriptManager().getRequireJS().addScripts("location.reload();");
    } catch (Throwable t) {
      ApplicationMessage msg = new ApplicationMessage("UIApplication.msg.unknown-error", null, ApplicationMessage.ERROR);
      addMessage(msg);
      LOG.error("Error during the processAction phase", t);
    }
  }

  /**
   * Triggered when there is Ajax request. <br>
   * This method add xml structure that help PortalHttpRequest.js parse the
   * response
   */
  public void renderBlockToUpdate(UIComponent uicomponent, WebuiRequestContext context, Writer w) throws Exception {
    w.write("<div class=\"BlockToUpdate\">");
    w.append("<div class=\"BlockToUpdateId\">").append(uicomponent.getId()).append("</div>");
    w.write("<div class=\"BlockToUpdateData\">");
    uicomponent.processRender(context);
    w.write("</div>");
    w.write("</div>");
  }
}
