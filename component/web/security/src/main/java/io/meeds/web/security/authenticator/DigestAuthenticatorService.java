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
package io.meeds.web.security.authenticator;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.tomcat.util.buf.HexUtils;
import org.apache.tomcat.util.security.ConcurrentMessageDigest;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.Identity;
import org.exoplatform.web.security.codec.CodecInitializer;
import org.exoplatform.web.security.security.SecureRandomService;

import lombok.SneakyThrows;

public class DigestAuthenticatorService {

  private static final String      CACHE_NAME      = "digest.encryptedTemporaryPassword";

  private static final String      UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  private static final String      LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";

  private static final String      DIGIT_CHARS     = "0123456789";

  private static final String      SPECIAL_CHARS   = "!@#$%&*?";

  private CodecInitializer         encrypter;

  private Authenticator            authenticator;

  private SecureRandom             secureRandom;

  private ExoCache<String, String> cache;

  public DigestAuthenticatorService(SecureRandomService secureRandomService,
                                    CacheService cacheService,
                                    Authenticator authenticator,
                                    CodecInitializer encrypter,
                                    InitParams initParams) {
    this.authenticator = authenticator;
    this.encrypter = encrypter;
    this.secureRandom = secureRandomService.getSecureRandom();

    String ttl = initParams.getValueParam("password.ttl").getValue();
    String cacheSize = initParams.getValueParam("password.cacheSize").getValue();

    this.cache = cacheService.getCacheInstance(CACHE_NAME);
    this.cache.setLiveTime(Long.parseLong(ttl));
    this.cache.setMaxSize(Integer.parseInt(cacheSize));
  }

  @SneakyThrows
  public Identity createIdentity(String userName) {
    return authenticator.createIdentity(userName);
  }

  @SneakyThrows
  public String generatePassword(String userName) {
    String password = String.valueOf(generateSecurePassword(20));
    cache.put(userName, encrypter.getCodec().encode(password));
    return password;
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
    String clearTextPassword = getPassword(userName);
    if (clearTextPassword == null) {
      throw new IllegalStateException(String.format("No generated password for user '%s'", userName));
    }
    String digestValue = userName + ":" + realmName + ":" + clearTextPassword;
    byte[] valueBytes = digestValue.getBytes(StandardCharsets.UTF_8);
    return HexUtils.toHexString(ConcurrentMessageDigest.digest(algorithm, valueBytes));
  }

  @SneakyThrows
  private String getPassword(String userName) {
    String encryptedPassword = cache.get(userName);
    return encryptedPassword == null ? null : encrypter.getCodec().decode(encryptedPassword);
  }

  private String generateSecurePassword(int length) {
    List<Character> charPool = new ArrayList<>();
    String allChars = UPPERCASE_CHARS + LOWERCASE_CHARS + DIGIT_CHARS + SPECIAL_CHARS;
    for (int i = 0; i < length - 4; i++) {
      charPool.add(getRandomChar(allChars, secureRandom));
    }
    charPool.add(getRandomChar(UPPERCASE_CHARS, secureRandom));
    charPool.add(getRandomChar(LOWERCASE_CHARS, secureRandom));
    charPool.add(getRandomChar(DIGIT_CHARS, secureRandom));
    charPool.add(getRandomChar(SPECIAL_CHARS, secureRandom));
    Collections.shuffle(charPool, secureRandom);
    StringBuilder finalPassword = new StringBuilder();
    charPool.forEach(finalPassword::append);
    return finalPassword.toString();
  }

  private char getRandomChar(String charSet, SecureRandom random) {
    int index = random.nextInt(charSet.length());
    return charSet.charAt(index);
  }

}
