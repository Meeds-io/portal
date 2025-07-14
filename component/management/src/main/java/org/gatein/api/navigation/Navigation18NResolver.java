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
package org.gatein.api.navigation;

import java.util.Locale;
import java.util.ResourceBundle;

import org.gatein.api.ApiException;
import org.gatein.api.PortalRequest;
import org.gatein.api.Util;
import org.gatein.api.site.SiteId;

import org.exoplatform.commons.utils.ExpressionUtil;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.storage.DescriptionStorage;
import org.exoplatform.services.resources.ResourceBundleManager;

public class Navigation18NResolver {
    private final DescriptionStorage descriptionStorage;
    private final ResourceBundleManager bundleManager;
    private final Locale siteLocale;
    private final SiteId siteId;

    public Navigation18NResolver(DescriptionStorage descriptionStorage, ResourceBundleManager bundleManager, Locale siteLocale,
            SiteId siteId) {
        this.descriptionStorage = descriptionStorage;
        this.bundleManager = bundleManager;
        this.siteLocale = siteLocale;
        this.siteId = siteId;
    }

    private ResourceBundle getResourceBundle() {
        Locale userLocale = getUserLocale();
        // Use site locale
        if (userLocale == null) {
            userLocale = siteLocale;
        }
        // If site locale is null for some reason, just use default system locale
        if (userLocale == null) {
            userLocale = Locale.getDefault();
        }
        SiteKey siteKey = Util.from(siteId);

        return bundleManager.getNavigationResourceBundle(userLocale.getLanguage(), siteKey.getTypeName(), siteKey.getName());
    }

    private Locale getUserLocale() {
        return PortalRequest.getInstance().getLocale();
    }

    public String resolveName(String string, String descriptionId, String defaultValue) {
        return resolve(string, descriptionId, defaultValue, true);
    }

    private String resolve(String string, String descriptionId, String defaultValue, boolean nameFlag) {
        String resolved = null;

        if (string != null) {
            if (ExpressionUtil.isResourceBindingExpression(string)) {
                resolved = ExpressionUtil.getExpressionValue(getResourceBundle(), string);
            } else {
                resolved = string;
            }
        } else if (descriptionId != null) {
            Locale userLocale = getUserLocale();
            org.exoplatform.portal.mop.State described;
            try {
                if (userLocale != null) {
                    described = descriptionStorage.resolveDescription(descriptionId, siteLocale, userLocale);
                } else {
                    described = descriptionStorage.resolveDescription(descriptionId, siteLocale);
                }
            } catch (Throwable t) {
                throw new ApiException("Failed to resolve description", t);
            }
            if (described != null) {
                resolved = (nameFlag) ? described.getName() : described.getDescription();
            }
        }

        return (resolved == null) ? defaultValue : resolved;
    }
}
