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
package org.exoplatform.portal.webui.portal;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;

/**
 * Jun 5, 2006
 */
public class PageNodeEvent<T extends UIComponent> extends Event<T> {
    public static final String CHANGE_NODE = "ChangeNode";

    private String targetNodeUri;

    private SiteKey siteKey;

    public PageNodeEvent(T source, String name, SiteKey siteKey, String targetNodeUri) {
        super(source, name, null);
        this.targetNodeUri = targetNodeUri;
        this.siteKey = siteKey;
    }

    public String getTargetNodeUri() {
        return targetNodeUri;
    }

    public SiteKey getSiteKey() {
        return siteKey;
    }
}
