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
package org.exoplatform.web.security.hash;

public class SaltedHashException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 2581616573889279142L;

    /**
     *
     */
    public SaltedHashException() {
    }

    /**
     * @param message
     */
    public SaltedHashException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public SaltedHashException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public SaltedHashException(String message, Throwable cause) {
        super(message, cause);
    }

}
