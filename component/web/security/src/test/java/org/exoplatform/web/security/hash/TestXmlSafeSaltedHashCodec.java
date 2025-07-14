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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestXmlSafeSaltedHashCodec {

    @Test
    public void testSaltedHashRoundTrip() throws SaltedHashEncodingException {
        SaltedHashCodec subject = new XmlSafeSaltedHashCodec();
        SaltedHash saltedHash = new SaltedHash("dummy", 1024, new byte[] {-36, 63, 52, 127, -88, -117, 8, -75, -98, 47, 52, 10}, new byte[] {-72, 10, 11, -128, 78, 125, -3, -52, 37, 15, 14, -35});
        String encodedSaltedHash = subject.encode(saltedHash);
        assertEquals(saltedHash, subject.decode(encodedSaltedHash));
    }

}
