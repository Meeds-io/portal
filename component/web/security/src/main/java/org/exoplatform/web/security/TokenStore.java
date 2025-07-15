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
package org.exoplatform.web.security;

/**
 * The token store is a place where temporary tokens are held.
 *
 */
public interface TokenStore {
    /**
     * Create a token and returns it. The store state is modified as it retains the token until it is removed either explicitely
     * or because the token validity is expired.
     *
     * @param username the username related to the token
     * @return the token key
     * @throws IllegalArgumentException if the validity is not greater than zero
     * @throws NullPointerException if the payload is null
     */
    String createToken(String username) throws IllegalArgumentException, NullPointerException;

    /**
     * Validates a token. If the token is valid it returns the attached credentials. The store state may be modified by the
     * removal of the token. The token is removed either if the remove argument is set to true of if the token is not anymore
     * valid.
     *
     * @param tokenKey the token key
     * @param remove true if the token must be removed regardless its validity
     * @return the attached credentials or null
     * @throws NullPointerException if the token key argument is null
     */
    String validateToken(String tokenKey, boolean remove) throws NullPointerException;

}
