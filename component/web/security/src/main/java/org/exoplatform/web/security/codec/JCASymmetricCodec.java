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
package org.exoplatform.web.security.codec;

import org.gatein.common.util.Base64;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Symmetric codec bases on Java Cryptography Library
 *
 */
public class JCASymmetricCodec extends AbstractCodec {

    private final Cipher encrypter;

    private final Cipher decrypter;

    JCASymmetricCodec(String transformation, SecretKey key) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException {
        this.encrypter = Cipher.getInstance(transformation);
        this.decrypter = Cipher.getInstance(transformation);

        this.encrypter.init(Cipher.ENCRYPT_MODE, key);
        this.decrypter.init(Cipher.DECRYPT_MODE, key);
    }

    @Override
    public String encode(String plainInput) {
        try {
            byte[] bytes;
            synchronized (encrypter) {
                bytes = encrypter.doFinal(plainInput.getBytes());
            }
            return Base64.encodeBytes(bytes);
        } catch (Exception ex) {
            throw new UndeclaredThrowableException(ex);
        }
    }

    @Override
    public String decode(String encodedInput) {
        try {
            byte[] bytes = Base64.decode(encodedInput);
            synchronized (decrypter) {
                bytes = decrypter.doFinal(bytes);
            }
            return new String(bytes);
        } catch (Exception ex) {
            throw new UndeclaredThrowableException(ex);
        }
    }
}
