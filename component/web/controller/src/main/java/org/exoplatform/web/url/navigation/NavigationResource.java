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
package org.exoplatform.web.url.navigation;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.user.UserNode;

/**
 * A class that contains combination of a portal name and a page node to determine the target URL
 *
 */
public class NavigationResource {

    /** . */
    private final SiteType siteType;

    /** . */
    private final String siteName;

    /** . */
    private final String nodeURI;

    public NavigationResource(UserNode node) {
        this(node.getNavigation().getKey().getType(), node.getNavigation().getKey().getName(), node.getURI());
    }

    public NavigationResource(SiteKey siteKey, String nodeURI) {
        this(siteKey.getType(), siteKey.getName(), nodeURI);
    }

    public NavigationResource(SiteType siteType, String portalName, String nodeURI) {
        this.siteType = siteType;
        this.siteName = portalName;
        this.nodeURI = nodeURI;
    }

    public SiteType getSiteType() {
        return siteType;
    }

    public String getSiteName() {
        return siteName;
    }

    public String getNodeURI() {
        return nodeURI;
    }
}
