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
package org.exoplatform.web.security.security;

public class TokenServiceInitializationException extends Exception {

    /** . */
    private static final long serialVersionUID = -372883737885959179L;

    /**
     * @see Exception#Exception()
     */
    public TokenServiceInitializationException() {
        super();
    }

    /**
     * @see Exception#Exception(String, Throwable)
     *
     * @param message
     * @param cause
     */
    public TokenServiceInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @see Exception#Exception(String)
     *
     * @param message
     */
    public TokenServiceInitializationException(String message) {
        super(message);
    }

    /**
     * @see Exception#Exception(Throwable)
     *
     * @param cause
     */
    public TokenServiceInitializationException(Throwable cause) {
        super(cause);
    }

}
