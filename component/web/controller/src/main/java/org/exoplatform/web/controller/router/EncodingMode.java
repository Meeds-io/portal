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
package org.exoplatform.web.controller.router;

/**
 * Specifies how a string value should be encoded in an URL.
 *
 */
public enum EncodingMode {

    /**
     * FORM encodes the whole string with the <code>x-www-form-urlencoded</code> also known as <i>default form encoding</i>. For
     * instance the string "/a/b" is encoded to "%2Fa%2Fb".
     */
    FORM,

    /**
     * PRESERVE_PATH encodes the whole string like the {@link #FORM} but preserve the path separators. For instance the string
     * "/a b" is enocded to "/a+b".
     */
    PRESERVE_PATH

}
