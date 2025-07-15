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


/**
 * Interface for creating salted hashes from plaintext passwords and for validating passwords against stored salted hashes.
 *
 */
public interface SaltedHashService {
    /**
     * Computes a salted hash of given plaintext password suitable for storing in a database.
     *
     * @throws SaltedHashException
     */
    String getSaltedHash(String password) throws SaltedHashException;

    /**
     * Checks whether given plaintext {@code password} corresponds to the given {@code saltedHash}.
     *
     * @param password
     * @param saltedHash
     * @return {@code true} if the given {@code password} matches the given {@code saltedHash}; {@code false} otherwise.
     * @throws SaltedHashException if the {@code saltedHash} cannot be parsed or if the hashing algorithm is not available.
     */
    boolean validate(String password, String saltedHash) throws SaltedHashException;
}
