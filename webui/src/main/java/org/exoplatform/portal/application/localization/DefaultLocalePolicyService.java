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

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.LocalePolicy;

/**
 * This service represents a default policy for determining LocaleConfig to be used for user's session. This service is
 * registered through portal services configuration file: conf/portal/configuration.xml Custom locale determination policy can
 * be implemented by overriding or completely replacing this class, and registering an alternative implementation.
 *
 * To gracefully fallback from more specific locales (lang_COUNTRY) to more generic (lang) without loss of information about
 * user's language selection use {@link LocaleContextInfo#getLocaleIfLangSupported(java.util.Locale)} and return a more specific
 * Locale. The Locale will be appropriately narrowed by LocalePolicy caller.
 *
 * Special care needs to be taken to assure Locale consistency between portal requests and non-portal requests - like login
 * redirect upon failed authentication attempt. To keep consistency at least one of {@link LocaleContextInfo#cookieLocales} and
 * {@link LocaleContextInfo#sessionLocale} needs to be enabled.
 *
 * @see NoBrowserLocalePolicyService
 * @see LocalizationFilter
 *
 */
public class DefaultLocalePolicyService implements LocalePolicy {

    private static final String USE_DEFAULT_SITE_LANGUAGE_PARAM = "useDefaultSiteLanguage";

    private LocaleConfigService localeConfigService;

    private boolean             useDefaultSiteLanguage;

    public DefaultLocalePolicyService(LocaleConfigService localeConfigService, InitParams params) {
      this.localeConfigService = localeConfigService;
      if (params != null && params.containsKey(USE_DEFAULT_SITE_LANGUAGE_PARAM)) {
        this.useDefaultSiteLanguage = Boolean.parseBoolean(params.getValueParam(USE_DEFAULT_SITE_LANGUAGE_PARAM).getValue());
      }
    }

    /**
     * @see LocalePolicy#determineLocale(LocaleContextInfo)
     */
    public Locale determineLocale(LocaleContextInfo context) {
      if (context.getRequestLocale() != null) {
        return context.getRequestLocale();
      }

      //
      Locale locale = null;
      if (context.getRemoteUser() == null) {
        locale = getLocaleConfigForAnonymous(context);
      } else {
        locale = getLocaleConfigForRegistered(context);
      }

      if (locale == null) {
        if (useDefaultSiteLanguage) {
          locale = context.getPortalLocale();
        } else {
          locale = localeConfigService.getDefaultLocaleConfig().getLocale();
        }
      } else {
        Set<Locale> supportedLocales = context.getSupportedLocales();
        if (supportedLocales == null) {
          supportedLocales = localeConfigService.getLocalConfigs()
                                                .stream()
                                                .map(LocaleConfig::getLocale)
                                                .collect(Collectors.toSet());
        }
        if (!supportedLocales.contains(locale) && StringUtils.isNotBlank(locale.getVariant())) {
          // Try without variant
          locale = new Locale(locale.getLanguage(), locale.getCountry());
        }
        if (!supportedLocales.contains(locale) && StringUtils.isNotBlank(locale.getCountry())) {
          // Try without variant and country
          locale = new Locale(locale.getLanguage());
        }
        if (!supportedLocales.contains(locale)) {
          // Use default language
          locale = localeConfigService.getDefaultLocaleConfig()
                                      .getLocale();
        }
      }
      return locale;
    }

    /**
     * Override this method to change the LocaleConfig determination for registered users. Default is: use user's profile
     * language, if not available fall back to LOCALE cookie, and finally if that is not available either fall back to browser
     * language preference.
     *
     * @param context locale context info available to implementations in order to determine appropriate Locale
     * @return Locale representing a language to use, or null
     */
    protected Locale getLocaleConfigForRegistered(LocaleContextInfo context) {
        Locale locale = context.getLocaleIfLangSupported(context.getUserProfileLocale());
        if (locale == null)
            locale = getLocaleConfigFromCookie(context);
        if (locale == null)
            locale = getLocaleConfigFromSession(context);
        if (locale == null)
            locale = getLocaleConfigFromBrowser(context);

        return locale;
    }

    /**
     * Override this method to change the Locale determination based on browser language preferences. If you want to disable the
     * use of browser language preferences simply return null.
     *
     * @param context locale context info available to implementations in order to determine appropriate Locale
     * @return Locale representing a language to use, or null
     */
    protected Locale getLocaleConfigFromBrowser(LocaleContextInfo context) {
        List<Locale> locales = context.getBrowserLocales();
        if (locales != null && !locales.isEmpty()) {
          return context.getLocaleIfLangSupported(locales.get(0));
        }
        return null;
    }

    /**
     * Override this method to change Locale determination for users that aren't logged in. By default the request's LOCALE
     * cookie is used, if that is not available the browser language preferences are used.
     *
     * @param context locale context info available to implementations in order to determine appropriate Locale
     * @return Locale representing a language to use, or null
     */
    protected Locale getLocaleConfigForAnonymous(LocaleContextInfo context) {
        Locale locale = getLocaleConfigFromCookie(context);
        if (locale == null)
            locale = getLocaleConfigFromSession(context);
        if (locale == null)
            locale = getLocaleConfigFromBrowser(context);

        return locale;
    }

    /**
     * Override this method to change the Locale determination based on session attribute. Note: this is mostly a backup for
     * cookie, as either one usually has to be enabled for locale to remain synchronized between portal and non-portal pages.
     *
     * @param context locale context info available to implementations in order to determine appropriate Locale
     * @return Locale representing a language to use, or null
     */
    protected Locale getLocaleConfigFromSession(LocaleContextInfo context) {
        return context.getSessionLocale();
    }

    /**
     * Override this method to change the Locale determination based on browser cookie. If you want to disable the use of
     * browser cookies simply return null.
     *
     * @param context locale context info available to implementations in order to determine appropriate Locale
     * @return Locale representing a language to use, or null
     */
    protected Locale getLocaleConfigFromCookie(LocaleContextInfo context) {
        List<Locale> locales = context.getCookieLocales();
        if (locales != null && !locales.isEmpty()) {
          return context.getLocaleIfLangSupported(locales.get(0));
        }
        return null;
    }

}
