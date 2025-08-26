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

import java.io.Writer;
import java.util.Set;

import javax.portlet.ResourceRequest;
import javax.portlet.WindowState;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;

/**
 * Root UI component of portlets written using GateIn WebUI framework should extends this class <br>
 * There are 3 WebUI lifecycle methods, that are called during JSR 286 portlet lifecycle : <br>
 * - processDecode - decode and bind parameters from the request to WebUI component<br>
 * - processAction - triggered when there is a request for portlet ActionURL, or a WebUI event<br>
 * - processRender - mapped to JSR 286 render method<br>
 * - serveResource - mapped to JSR 286 serveResource method<br>
 *
 * Portlet request go through this class and then be delegated to child WebUI components with the help of UIApplicationLifecycle.
 * Use ComponentConfig annotation to config lifecycle class for WebUI component. <br>
 *
 * This class also provides machanism to show a WebUI popup message
 */
@Serialized
public abstract class UIPortletApplication extends UIApplication {

  private static final long serialVersionUID = -2343595321472731526L;

    public static String VIEW_MODE = "ViewMode";

    public static String EDIT_MODE = "EditMode";

    public static String HELP_MODE = "HelpMode";

    public static String CONFIG_MODE = "ConfigMode";

    //
    @Override
    public void renderChildren() throws Exception {
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
        super.renderChildren(context);
    }

    /**
     * The default processRender for an UIPortletApplication does nothing if the current WindowState in the render request is
     * MINIMIZED. Otherwise, it handles two cases:
     *
     * A. Ajax is used --------------- If Ajax is used and that the entire portal should not be re rendered, then an AJAX
     * fragment is generated with information such as the portlet id, the portlet title, the portlet modes, the window states as
     * well as the HTML for the block to render
     *
     * B. A full render is made ------------------------ a simple call to the method super.processRender(context) which will
     * delegate the call to all the Lifecycle components
     *
     */
    public void processRender(WebuiApplication app, WebuiRequestContext context) throws Exception {
        // Do nothing if WindowState in the render request is MINIMIZED
        WindowState currentWindowState = ((PortletRequestContext) context).getRequest().getWindowState();
        if (currentWindowState == WindowState.MINIMIZED) {
            return;
        }

        WebuiRequestContext pContext = (WebuiRequestContext) context.getParentAppRequestContext();

        if (context.useAjax() && !pContext.getFullRender()) {
            Writer w = context.getWriter();

            Set<UIComponent> list = context.getUIComponentToUpdateByAjax();
            if (list != null) {
                for (UIComponent uicomponent : list) {
                    renderBlockToUpdate(uicomponent, context, w);
                }
                return;
            }
        }
        super.processRender(context);
    }

    /**
     * Root uicomponent of a portlet should override this method to leverage serveResource that JSR286 offers
     *
     * @param context - WebUI context
     */
    public void serveResource(WebuiRequestContext context) {
        if (!(context.getRequest() instanceof ResourceRequest)) {
            throw new IllegalStateException("serveSource can only be called in portlet context");
        }
    }
}
