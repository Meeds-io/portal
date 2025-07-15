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
package org.exoplatform.services.resources;

import java.util.List;
import java.util.Locale;
import java.util.Set;


/**
 * Data structure that holds the inputs for {@link LocalePolicy} pluggable policies mechanism.
 *
 */
public class LocaleContextInfo {
    private Set<Locale> supportedLocales;
    private List<Locale> browserLocales;
    private List<Locale> cookieLocales;
    private Locale userProfileLocale;
    private String remoteUser;
    private Locale portalLocale;
    private Locale sessionLocale;
    private Locale requestLocale;

    /**
     * Setter for supportedLocales
     *
     * @param supportedLocales locales supported by portal
     */
    public void setSupportedLocales(Set<Locale> supportedLocales) {
        this.supportedLocales = supportedLocales;
    }

    /**
     * Getter for supportedLocales
     *
     * @return supportedLocales
     */
    public Set<Locale> getSupportedLocales() {
        return supportedLocales;
    }

    /**
     * Setter for browserLocales
     *
     * @param browserLocales list of locales as preferred by client's browser
     */
    public void setBrowserLocales(List<Locale> browserLocales) {
        this.browserLocales = browserLocales;
    }

    /**
     * Getter for browserLocales
     *
     * @return browserLocales
     */
    public List<Locale> getBrowserLocales() {
        return browserLocales;
    }

    /**
     * Setter for cookieLocales
     *
     * @param cookieLocales locales stored in user's browser cookie
     */
    public void setCookieLocales(List<Locale> cookieLocales) {
        this.cookieLocales = cookieLocales;
    }

    /**
     * Getter for cookieLocales
     *
     * @return cookieLocales
     */
    public List<Locale> getCookieLocales() {
        return cookieLocales;
    }

    /**
     * Setter for userProfileLocale
     *
     * @param userProfileLocale locale loaded from user's profile
     */
    public void setUserProfileLocale(Locale userProfileLocale) {
        this.userProfileLocale = userProfileLocale;
    }

    /**
     * Getter for userProfileLocale
     *
     * @return userProfileLocale
     */
    public Locale getUserProfileLocale() {
        return userProfileLocale;
    }

    /**
     * Setter for remoteUser
     *
     * @param remoteUser username of the currently logged in user. Null for anonymous users.
     */
    public void setRemoteUser(String remoteUser) {
        this.remoteUser = remoteUser;
    }

    /**
     * Getter for remoteUser
     *
     * @return remoteUser
     */
    public String getRemoteUser() {
        return remoteUser;
    }

    /**
     * Setter for portalLocale
     *
     * @param portalLocale default locale configured for the portal
     */
    public void setPortalLocale(Locale portalLocale) {
        this.portalLocale = portalLocale;
    }

    /**
     * Getter for portalLocale
     *
     * @return portalLocale
     */
    public Locale getPortalLocale() {
        return portalLocale;
    }

    /**
     * Setter for sessionLocale
     *
     * @param locale Locale stored in current session
     */
    public void setSessionLocale(Locale locale) {
        this.sessionLocale = locale;
    }

    /**
     * Getter for sessionLocale
     *
     * @return sessionLocale
     */
    public Locale getSessionLocale() {
        return sessionLocale;
    }

    /**
     * Getter for requestLocale
     *
     * @return requestLocale
     */
    public Locale getRequestLocale() {
        return requestLocale;
    }

    /**
     * Setter for requestLocale
     *
     * @param requestLocale default locale configured for the request
     */
    public void setRequestLocale(Locale requestLocale) {
        this.requestLocale = requestLocale;
    }

    /**
     * Helper method that returns the locale only if it's supported by portal. Otherwise it returns null.
     *
     * @param locale locale to check
     * @return original locale if supported, null otherwise
     */
    public Locale getLocaleIfSupported(Locale locale) {
        if (locale == null)
            return null;
        if (supportedLocales.contains(locale))
            return locale;
        return null;
    }

    /**
     * Helper method that returns the locale only if it's language is supported by portal. Otherwise it returns null.
     *
     * @param locale locale to check
     * @return original locale if language is supported, null otherwise
     */
    public Locale getLocaleIfLangSupported(Locale locale) {
        if (locale == null)
            return null;
        if (supportedLocales.contains(locale))
            return locale;

        if ("".equals(locale.getCountry()) == false) {
            Locale loc = new Locale(locale.getLanguage());
            if (supportedLocales.contains(loc)) {
                // return original locale
                return locale;
            }
        }
        return null;
    }

    /**
     * Helper method to convert String representation of Locale into Locale object.
     *
     * @param portalLocaleName String representation of Locale
     * @return locale
     */
    public static Locale getLocale(String portalLocaleName) {
        int pos = portalLocaleName.indexOf("_");
        if (pos < 0)
            return new Locale(portalLocaleName);

        return new Locale(portalLocaleName.substring(0, pos), portalLocaleName.substring(pos + 1));
    }

    /**
     * Helper method to get a String representation of the Locale
     *
     * @param locale
     * @return String representation of the locale
     */
    public static String getLocaleAsString(Locale locale) {
        if (locale.getCountry().length() == 0)
            return locale.getLanguage();

        return locale.getLanguage() + "-" + locale.getCountry();
    }
}
