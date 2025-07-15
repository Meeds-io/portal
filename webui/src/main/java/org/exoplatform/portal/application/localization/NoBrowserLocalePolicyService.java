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
package org.exoplatform.portal.application.localization;

import java.util.Locale;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.LocaleContextInfo;

/**
 * This implementation of {@link org.exoplatform.services.resources.LocalePolicy} disregards client browser language preference.
 * Localization will therefore not be affected by different OS or browser language settings.
 *
 */
public class NoBrowserLocalePolicyService extends DefaultLocalePolicyService {

    public NoBrowserLocalePolicyService(LocaleConfigService localeConfigService, InitParams params) {
      super(localeConfigService, params);
    }

    /**
     * Override super method with no-op.
     *
     * @param context locale context info available to implementations in order to determine appropriate Locale
     * @return null
     */
    @Override
    protected Locale getLocaleConfigFromBrowser(LocaleContextInfo context) {
        return null;
    }
}
