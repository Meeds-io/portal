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

import org.exoplatform.component.test.AbstractKernelTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.picketlink.idm.api.Attribute;
import org.picketlink.idm.api.AttributesManager;
import org.picketlink.idm.api.IdentitySession;
import org.picketlink.idm.common.exception.IdentityException;

import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Argon2IdPasswordEncoderTest extends AbstractKernelTest {

  private Argon2IdPasswordEncoder argon2IdPasswordEncoder;

  @Before
  public void setUp() throws Exception {
    System.setProperty("credentialEncoder.secureRandomAlgorithm", "SHA1PRNG");
    argon2IdPasswordEncoder = new Argon2IdPasswordEncoder();
    argon2IdPasswordEncoder.initialize(new HashMap<>(), null);
  }

  @Test
  public void testEncodeCredential() throws IdentityException {
    IdentitySession identitySession = mock(IdentitySession.class);
    AttributesManager attributesManager = mock(AttributesManager.class);
    Attribute saltAttribute = mock(Attribute.class);
    when(attributesManager.getAttribute("user", "passwordSalt128")).thenReturn(null, saltAttribute);
    when(identitySession.getAttributesManager()).thenReturn(attributesManager);
    argon2IdPasswordEncoder.setIdentitySession(identitySession);
    String hash = argon2IdPasswordEncoder.encodeCredential("user", "Password1234");
    assertNotNull(hash);
    when(saltAttribute.getValue()).thenReturn("7f33a8dddac20d3b2b8e058be59d1a36");
    String storedHash = argon2IdPasswordEncoder.encodeCredential("user", "Password1234");
    assertNotNull(storedHash);
  }
}
