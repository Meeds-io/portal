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
package org.exoplatform.commons.utils;

import org.apache.commons.lang3.LocaleUtils;

import java.util.Locale;


/**
 * Various I18N utility methods.
 *
 */
public class I18N {

    /**
     * Provide a string representation of the locale argument to the {@link java.util.Locale#toString()} method.
     *
     * @param locale the locale
     * @return the java representation
     * @throws NullPointerException if the locale argument is null
     */
    public static String toJavaIdentifier(Locale locale) throws NullPointerException {
        if (locale == null) {
            throw new NullPointerException("No null locale accepted");
        }
        return locale.toString();
    }

    /**
     * Parse the java string representation and returns a locale. See {@link #toJavaIdentifier(java.util.Locale)} method for
     * more details.
     *
     * @param s the java representation to parse
     * @return the corresponding locale
     * @throws NullPointerException if the locale argument is null
     * @throws IllegalArgumentException if the string cannot be parsed to a locale
     */
    public static Locale parseJavaIdentifier(String s) throws NullPointerException, IllegalArgumentException {
        return LocaleUtils.toLocale(s);
    }

    /**
     * Provide a string representation of the locale argument according to the http://www.ietf.org/rfc/rfc1766.txt :
     * <ul>
     * <li>locale with a language only will return the language string</li>
     * <li>otherwise it returns the language and country separated by an hyphen '-'</li>
     * </ul>
     *
     * @param locale the locale
     * @return the RFC1766 representation
     * @throws NullPointerException if the locale argument is null
     */
    public static String toTagIdentifier(Locale locale) throws NullPointerException {
        if (locale == null) {
            throw new NullPointerException("No null locale accepted");
        }
        String country = locale.getCountry();
        String lang = locale.getLanguage();
        if (country != null && country.length() > 0) {
            return lang + "-" + country;
        } else {
            return lang;
        }
    }

    /**
     * Parse the http://www.ietf.org/rfc/rfc1766.txt string representation and returns a locale. See
     * {@link #toTagIdentifier(java.util.Locale)} method for more details.
     *
     * @param s the RFC1766 representation to parse
     * @return the corresponding locale
     * @throws NullPointerException if the locale argument is null
     * @throws IllegalArgumentException if the string cannot be parsed to a locale
     */
    public static Locale parseTagIdentifier(String s) throws NullPointerException, IllegalArgumentException {
        if (s == null) {
            throw new NullPointerException("No null string accepted");
        }
        if (s.length() == 2 || s.length() == 3) {
            return new Locale(s);
        } else if (s.length() == 5 && s.charAt(2) == '-') {
            String lang = s.substring(0, 2);
            String country = s.substring(3, 5);
            return new Locale(lang, country);
        } else if (s.length() == 6 && s.charAt(3) == '-') {
            String lang = s.substring(0, 3);
            String country = s.substring(4, 6);
            return new Locale(lang, country);
        } else {
            throw new IllegalArgumentException("Locale " + s + " cannot be parsed");
        }
    }

    /**
     * Returns the parent locale of a locale or null when it does not have one.
     *
     * @param locale the locale
     * @return the parent locale
     * @throws NullPointerException if the specified locale is null
     */
    public static Locale getParent(Locale locale) throws NullPointerException {
        if (locale == null) {
            throw new NullPointerException("No null locale accepted");
        }

        //
        if (locale.getVariant() != null && locale.getVariant().length() > 0) {
            return new Locale(locale.getLanguage(), locale.getCountry());
        } else {
            if (locale.getCountry() != null && locale.getCountry().length() > 0) {
                return new Locale(locale.getLanguage());
            } else {
                return null;
            }
        }
    }
}
