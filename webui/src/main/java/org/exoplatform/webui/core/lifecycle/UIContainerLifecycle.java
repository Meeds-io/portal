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
package org.exoplatform.webui.core.lifecycle;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIContainer;

/**
 * Jul 10, 2006
 */
public class UIContainerLifecycle extends Lifecycle<UIContainer> {

    public void processRender(UIContainer uicomponent, WebuiRequestContext context) throws Exception {
        context.getWriter().append("<div class=\"").append(uicomponent.getId()).append("\" id=\"").append(uicomponent.getId())
                .append("\">");
        uicomponent.renderChildren(context);
        context.getWriter().append("</div>");
    }
}
