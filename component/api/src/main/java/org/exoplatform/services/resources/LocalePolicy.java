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


/**
 * This interface represents a pluggable mechanism for different locale determining algorithms
 *
 */
public interface LocalePolicy {
    /**
     * Determine the Locale to be used for current request
     *
     * @param localeContext locale context info available to implementations as inputs to use when determining appropriate
     *        Locale
     * @return Locale to be used for current user's request
     */
    Locale determineLocale(LocaleContextInfo localeContext);
}
