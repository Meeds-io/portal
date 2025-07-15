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
 * A {@link SaltedHashService} implementation which does not do any hashing at all. It simply returns the plaintext password
 * from {@link #getSaltedHash(String)} and tests the string equality of {@code password} and {@code saltedHash} in
 * {@link #validate(String, String)}.
 *
 * This class is intended to be used in tests and maybe also in some real life scenarios where backwards compatibility requires
 * storing of plaintext passwords.
 *
 */
public class NoSaltedHashService implements SaltedHashService {

        public NoSaltedHashService() {
    }

    /*
     * (non-Javadoc)
     *
     * @see org.exoplatform.web.security.hash.SaltedHashService#getSaltedHash(java.lang.String, java.security.SecureRandom)
     */
    @Override
    public String getSaltedHash(String password) throws SaltedHashException {
        return password;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.exoplatform.web.security.hash.SaltedHashService#validate(java.lang.String, java.lang.String)
     */
    @Override
    public boolean validate(String password, String saltedHash) throws SaltedHashException {
        return password == saltedHash || (password != null && password.equals(saltedHash));
    }

}
