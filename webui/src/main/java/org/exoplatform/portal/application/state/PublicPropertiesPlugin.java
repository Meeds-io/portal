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
package org.exoplatform.portal.application.state;

import java.util.Map;

import javax.xml.namespace.QName;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.portal.UIPortal;

/**
 * This plugin setup properties that are publicly supported, hence this is part of a public API and once published its contract
 * must not change whatsoever.
 *
 */
public class PublicPropertiesPlugin extends AbstractContextualPropertyProviderPlugin {

    /** . */
    private final QName navigationURIQName;

    /** . */
    private final QName pageNameQName;

    /** . */
    private final QName siteTypeQName;

    /** . */
    private final QName siteNameQName;

    /** . */
    private final QName windowShowInfoBarQName;

    /** . */
    private final QName windowHeight;

    /** . */
    private final QName windowWidth;

    public PublicPropertiesPlugin(InitParams params) {
        super(params);

        //
        this.navigationURIQName = new QName(namespaceURI, "navigation_uri");
        this.pageNameQName = new QName(namespaceURI, "page_name");
        this.siteTypeQName = new QName(namespaceURI, "site_type");
        this.siteNameQName = new QName(namespaceURI, "site_name");
        this.windowWidth = new QName(namespaceURI, "window_width");
        this.windowHeight = new QName(namespaceURI, "window_height");
        this.windowShowInfoBarQName = new QName(namespaceURI, "window_show_info_bar");
    }

    @Override
    public void getProperties(UIPortlet portletWindow, Map<QName, String[]> properties) {
        try {
            UIPortal currentSite = PortalRequestContext.getCurrentInstance().getUiPortal();
            UserNode currentNode = currentSite.getSelectedUserNode();

            // Navigation related properties
            addProperty(properties, navigationURIQName, currentNode.getURI());

            // Page related properties
            PageKey pageRef = currentNode.getPageRef();
            UIPage currentPage = currentSite.getUIPage(pageRef != null ? pageRef.format() : null);
            if (currentPage != null) {
                addProperty(properties, pageNameQName, currentPage.getTitle());
            }

            // Site related properties
            addProperty(properties, siteTypeQName, currentSite.getSiteType().getName());
            addProperty(properties, siteNameQName, currentSite.getName());

            // Window related properties
            addProperty(properties, windowShowInfoBarQName, Boolean.toString(portletWindow.getShowInfoBar()));
            addProperty(properties, windowWidth, portletWindow.getWidth());
            addProperty(properties, windowHeight, portletWindow.getHeight());
        } catch (Exception ex) {
            log.error("Could not obtain contextual properties for portlet " + portletWindow, ex);
        }
    }
}
