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
package org.exoplatform.portal.application;

import org.exoplatform.web.controller.QualifiedName;

public class RequestNavigationData {
    public static final QualifiedName REQUEST_PATH = QualifiedName.create("gtn", "path");

    public static final QualifiedName REQUEST_SITE_TYPE = QualifiedName.create("gtn", "sitetype");

    public static final QualifiedName REQUEST_SITE_NAME = QualifiedName.create("gtn", "sitename");

    protected final String siteType;

    protected final String siteName;

    protected final String path;

    public RequestNavigationData(String siteType, String siteName, String path) {
        this.siteType = siteType != null ? siteType : "";
        this.siteName = siteName != null ? siteName : "";
        this.path = path != null ? path : "";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof RequestNavigationData)) {
            return false;
        } else {
            RequestNavigationData data = (RequestNavigationData) obj;
            return siteType.equals(data.siteType) && siteName.equals(data.siteName) && path.equals(data.path);
        }
    }
}
