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

import java.util.Locale;
import java.util.ResourceBundle;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/**
 * May 3, 2004
 *
 */
public interface LocaleConfig {

    String getDescription();

    void setDescription(String desc);

    String getOutputEncoding();

    void setOutputEncoding(String enc);

    String getInputEncoding();

    void setInputEncoding(String enc);

    Locale getLocale();

    void setLocale(Locale locale);

    void setLocale(String localeName);

    String getLanguage();

    String getLocaleName();

    String getTagIdentifier();

    ResourceBundle getResourceBundle(String name);

    ResourceBundle getMergeResourceBundle(String[] names);

    ResourceBundle getNavigationResourceBundle(String ownerType, String ownerId);

    void setInput(HttpServletRequest req) throws java.io.UnsupportedEncodingException;

    void setOutput(HttpServletResponse res);

    /**
     * Returns the orientation of the locale config.
     *
     * @return the orientation
     */
    Orientation getOrientation();

    /**
     * Updates the orientation of the locale config.
     *
     * @param orientation the new orientation
     */
    void setOrientation(Orientation orientation);

}
