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
package org.exoplatform.portal.webui.workspace;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIComponentDecorator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;

/** Created by The eXo Platform SAS Mar 13, 2007 */
@ComponentConfig(id = "UIMaskWorkspace", template = "system:/groovy/portal/webui/workspace/UIMaskWorkspace.gtmpl", events = @EventConfig(phase = Phase.DECODE, listeners = UIMaskWorkspace.CloseActionListener.class, csrfCheck = false))
public class UIMaskWorkspace extends UIComponentDecorator {

    private int width_ = -1;

    private int height_ = -1;

    private boolean isShow = false;

    private String cssClasses = "";

    // TODO: Seems the isUpdated is never true
    private boolean isUpdated = false;

    public int getWindowWidth() {
        return width_;
    }

    public int getWindowHeight() {
        return height_;
    }

    public void setWindowSize(int w, int h) {
        width_ = w;
        height_ = h;
    }

    public boolean isShow() {
        return isShow;
    }

    public void setShow(boolean bln) {
        this.isShow = bln;
        if (bln == false) {
            isUpdated = false;
        }
    }

    public boolean isUpdated() {
        return isUpdated;
    }

    public void setUpdated(boolean bln) {
        // this.isUpdated = bln;
    }

    public String getCssClasses() {
        return cssClasses;
    }

    public void setCssClasses(String cssClasses) {
        this.cssClasses = cssClasses;
    }

    public void reset() {
        cssClasses = "";
        setWindowSize(-1, -1);
    }

    public <T extends UIComponent> T createUIComponent(Class<T> clazz, String configId, String id) throws Exception {
        T uicomponent = super.createUIComponent(clazz, configId, id);
        setUIComponent(uicomponent);
        return uicomponent;
    }

    public <T extends UIComponent> T createUIComponent(Class<T> clazz) throws Exception {
        return createUIComponent(clazz, null, null);
    }

    public UIComponent setUIComponent(UIComponent uicomponent) {
        UIComponent oldOne = super.setUIComponent(uicomponent);
        setShow(uicomponent != null);
        return oldOne;
    }

    public static class CloseActionListener extends EventListener<UIComponent> {
        public void execute(Event<UIComponent> event) throws Exception {
            UIMaskWorkspace uiMaskWorkspace = null;
            UIComponent uiSource = event.getSource();
            if (uiSource instanceof UIMaskWorkspace) {
                uiMaskWorkspace = (UIMaskWorkspace) uiSource;
            } else {
                uiMaskWorkspace = uiSource.getAncestorOfType(UIMaskWorkspace.class);
            }
            if (uiMaskWorkspace == null || !uiMaskWorkspace.isShow()) {
                return;
            }
            uiMaskWorkspace.setUIComponent(null);
            uiMaskWorkspace.reset();
            WebuiRequestContext rContext = event.getRequestContext();
            rContext.getJavascriptManager().require("SHARED/uiMaskWorkspace", "maskWS")
                    .addScripts("maskWS.hide('" + uiMaskWorkspace.getId() + "');");
            rContext.addUIComponentToUpdateByAjax(uiMaskWorkspace);
        }
    }
}
