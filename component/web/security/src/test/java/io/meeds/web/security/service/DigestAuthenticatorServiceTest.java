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
package io.meeds.web.security.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.apache.tomcat.util.buf.HexUtils;
import org.apache.tomcat.util.security.ConcurrentMessageDigest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.Identity;

import lombok.SneakyThrows;

@RunWith(MockitoJUnitRunner.class)
public class DigestAuthenticatorServiceTest {

  private static final String        PATH_EXAMPLE = "/path/to/resource";

  private static final String        ALGORITHM    = "MD5";

  private static final String        DIGEST       = "digestA2";

  private static final String        REALM        = "realm";

  private static final String        PASSWORD     = "secret";

  @Mock
  private ApiKeyService              apiKeyService;

  @Mock
  private Authenticator              authenticator;

  @Mock
  private Identity                   identity;

  private DigestAuthenticatorService service;

  @Before
  public void setUp() {
    service = new DigestAuthenticatorService(apiKeyService, authenticator);
  }

  @Test
  @SneakyThrows
  public void testCreateIdentity() {
    when(authenticator.createIdentity("john")).thenReturn(identity);

    Identity result = service.createIdentity("john");

    assertSame(identity, result);
    verify(authenticator).createIdentity("john");
  }

  @Test
  @SneakyThrows
  public void testValidateUserMatchingDigestNoQop() {
    String userName = "john";
    String realm = "myRealm";
    String algorithm = ALGORITHM;

    when(apiKeyService.getPassword(userName)).thenReturn(PASSWORD);

    String digestA1 = HexUtils.toHexString(
                                           ConcurrentMessageDigest.digest(algorithm,
                                                                          (userName + ":" + realm + ":" +
                                                                              PASSWORD).getBytes(StandardCharsets.UTF_8)))
                              .toLowerCase();
    String digestA2 = PATH_EXAMPLE;
    String nonce = "abc123";

    String serverDigestValue = digestA1 + ":" + nonce + ":" + digestA2;
    String clientDigest = HexUtils.toHexString(
                                               ConcurrentMessageDigest.digest(algorithm,
                                                                              serverDigestValue.getBytes(StandardCharsets.UTF_8)));

    String result = service.validateUser(userName, clientDigest, nonce, null, null, null, realm, digestA2, algorithm);

    assertEquals(userName, result);
  }

  @Test
  @SneakyThrows
  public void testValidateUserMatchingDigestWithQop() {
    String userName = "john";
    String realm = "myRealm";
    String algorithm = ALGORITHM;
    String password = PASSWORD;

    when(apiKeyService.getPassword(userName)).thenReturn(password);

    String digestA1 = HexUtils.toHexString(
                                           ConcurrentMessageDigest.digest(algorithm,
                                                                          (userName + ":" + realm + ":" +
                                                                              password).getBytes(StandardCharsets.UTF_8)))
                              .toLowerCase();
    String digestA2 = PATH_EXAMPLE;
    String nonce = "abc123";
    String nc = "0001";
    String cnonce = "xyz789";
    String qop = "auth";

    String serverDigestValue = digestA1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + digestA2;
    String clientDigest = HexUtils.toHexString(
                                               ConcurrentMessageDigest.digest(algorithm,
                                                                              serverDigestValue.getBytes(StandardCharsets.UTF_8)));

    String result = service.validateUser(userName, clientDigest, nonce, nc, cnonce, qop, realm, digestA2, algorithm);

    assertEquals(userName, result);
  }

  @Test
  @SneakyThrows
  public void testValidateUserNonMatchingDigest() {
    when(apiKeyService.getPassword("john")).thenReturn(PASSWORD);

    String result = service.validateUser("john", "wrongDigest", "nonce", null, null, null, REALM, DIGEST, ALGORITHM);

    assertNull(result);
  }

  @Test
  @SneakyThrows
  public void testGetDigestReturnsHash() {
    String userName = "john";
    String realm = REALM;
    String algorithm = ALGORITHM;
    String password = PASSWORD;

    when(apiKeyService.getPassword(userName)).thenReturn(password);

    String expected = HexUtils.toHexString(
                                           ConcurrentMessageDigest.digest(algorithm,
                                                                          (userName + ":" + realm + ":" +
                                                                              password).getBytes(StandardCharsets.UTF_8)));

    String actual = service.getDigest(userName, realm, algorithm);

    assertEquals(expected, actual);
  }

  @Test(expected = IllegalStateException.class)
  @SneakyThrows
  public void testGetDigestNoPasswordThrowsException() {
    when(apiKeyService.getPassword("john")).thenReturn(null);
    service.getDigest("john", REALM, ALGORITHM);
  }
}
