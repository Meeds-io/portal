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
package org.exoplatform.portal.webui.container;

import java.util.List;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * May 19, 2006
 */
@ComponentConfig(template = "system:/groovy/portal/webui/container/UITabContainer.gtmpl", events = {
  @EventConfig(listeners = UITabContainer.SelectTabActionListener.class)
})
public class UITabContainer extends UIContainer {

    public static final String TAB_CONTAINER = "TabContainer";
    public static final String FIRST_TAB = "FirstTab";
    public static final String LAST_TAB = "LastTab";

    public UITabContainer() {
        super();
    }

    public String getTabState(UIComponent uiChild, UITabContainer uiContainer) {
        String tabState = "";
        List<UIComponent> children = uiContainer.getChildren();
        int tabIndex = children.indexOf(uiChild);
        if (tabIndex == 0) {
            tabState = FIRST_TAB;
        } else if (tabIndex == children.size() - 1) {
            tabState = LAST_TAB;
        }
        return tabState;
    }

    public static class SelectTabActionListener extends EventListener<UITabContainer> {
        public void execute(Event<UITabContainer> event) throws Exception {
            String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
            UITabContainer container = event.getSource();
            UIComponent goal = container.findComponentById(objectId);
            if (goal == null) {
                return;
            }
            UITabContainer parent = goal.getParent();
            List<UIComponent> children = parent.getChildren();
            for (UIComponent child : children) {
                if (child.getId().equals(objectId)) {
                    child.setRendered(true);
                    continue;
                }
                child.setRendered(false);
            }
        }
    }
}
