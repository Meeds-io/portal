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

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.tomcat.util.buf.HexUtils;
import org.apache.tomcat.util.security.ConcurrentMessageDigest;
import org.springframework.stereotype.Service;

import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.Identity;

import lombok.SneakyThrows;

@Service
public class DigestAuthenticatorService {

  private ApiKeyService apiKeyService;

  private Authenticator authenticator;

  public DigestAuthenticatorService(ApiKeyService apiKeyService,
                                    Authenticator authenticator) {
    this.authenticator = authenticator;
    this.apiKeyService = apiKeyService;
  }

  @SneakyThrows
  public Identity createIdentity(String userName) {
    return authenticator.createIdentity(userName);
  }

  @SneakyThrows
  public String validateUser(String userName, // NOSONAR
                             String clientDigest,
                             String nonce,
                             String nc,
                             String cnonce,
                             String qop,
                             String realm,
                             String digestA2,
                             String algorithm) {
    // In digest auth, digests are always lower case
    String digestA1 = getDigest(userName, realm, algorithm);
    digestA1 = digestA1.toLowerCase(Locale.ENGLISH);
    String serverDigestValue;
    if (qop == null) {
      serverDigestValue = digestA1 + ":" + nonce + ":" + digestA2;
    } else {
      serverDigestValue = digestA1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + digestA2;
    }
    byte[] valueBytes = serverDigestValue.getBytes(StandardCharsets.UTF_8);
    String serverDigest = HexUtils.toHexString(ConcurrentMessageDigest.digest(algorithm, valueBytes));
    if (serverDigest.equals(clientDigest)) {
      return userName;
    }
    return null;
  }

  protected String getDigest(String userName, String realmName, String algorithm) {
    String clearTextPassword = apiKeyService.getPassword(userName);
    if (clearTextPassword == null) {
      throw new IllegalStateException(String.format("No generated password for user '%s'", userName));
    }
    String digestValue = userName + ":" + realmName + ":" + clearTextPassword;
    byte[] valueBytes = digestValue.getBytes(StandardCharsets.UTF_8);
    return HexUtils.toHexString(ConcurrentMessageDigest.digest(algorithm, valueBytes));
  }

}
