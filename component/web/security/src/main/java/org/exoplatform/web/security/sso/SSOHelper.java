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
package org.exoplatform.web.security.sso;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;

/**
 * Helper for SSO related things
 *
 */
public class SSOHelper {
    private final boolean ssoEnabled;
    private final boolean skipJSPRedirection;
    private final String ssoRedirectURLSuffix;

    private static final Log log = ExoLogger.getLogger(SSOHelper.class);

    public SSOHelper(InitParams params) {
        String ssoEnabledParam = params.getValueParam("isSSOEnabled").getValue();
        this.ssoEnabled = Boolean.parseBoolean(ssoEnabledParam);

        // Needs to be explicitly specified as "false", otherwise will have same value like ssoEnabled
        String ssoJSPRedirectionEnabledParam = params.getValueParam("skipJSPRedirection").getValue();
        if ("false".equals(ssoJSPRedirectionEnabledParam)) {
            this.skipJSPRedirection = false;
        } else {
            this.skipJSPRedirection = this.ssoEnabled;
        }

        this.ssoRedirectURLSuffix = params.getValueParam("SSORedirectURLSuffix").getValue();
        log.debug("SSOHelper initialized. ssoEnabled: " + ssoEnabled + ", skipJSPRedirection: " + skipJSPRedirection
                + ", ssoRedirectURLSuffix: " + ssoRedirectURLSuffix);
    }

    public boolean isSSOEnabled() {
        return ssoEnabled;
    }

    public boolean skipJSPRedirection() {
        return skipJSPRedirection;
    }

    public String getSSORedirectURLSuffix() {
        return ssoRedirectURLSuffix;
    }

}
