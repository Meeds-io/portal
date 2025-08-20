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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.exoplatform.web.security.codec.CodecInitializer;
import org.exoplatform.web.security.security.SecureRandomService;

import io.meeds.web.security.storage.ApiKeyStorage;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

@Service
public class ApiKeyService {

  private static final String UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  private static final String LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";

  private static final String DIGIT_CHARS     = "0123456789";

  private static final String SPECIAL_CHARS   = "!@#$%&*?";

  @Autowired
  private ApiKeyStorage       apiKeyStorage;

  @Autowired
  private OtpService          otpService;

  @Autowired
  private CodecInitializer    encrypter;

  @Autowired
  private SecureRandomService secureRandomService;

  @Value("${meeds.apiKey.length:20}")
  @Getter
  @Setter
  private long                apiKeyLength;

  private SecureRandom        secureRandom;

  @SneakyThrows
  public String getPassword(String userName, String otpMethod, String otpCode, boolean renew) throws IllegalAccessException {
    otpService.validateOtp(userName, otpMethod, otpCode);
    String password = renew ? null : getPassword(userName);
    if (password == null) {
      password = generatePassword();
      String encryptedPassword = encrypter.getCodec().encode(password);
      apiKeyStorage.saveKey(userName, encryptedPassword);
    }
    return password;
  }

  @SneakyThrows
  public String getPassword(String userName) {
    String encryptedPassword = apiKeyStorage.getKey(userName);
    return encryptedPassword == null ? null : encrypter.getCodec().decode(encryptedPassword);
  }

  private String generatePassword() {
    List<Character> charPool = new ArrayList<>();
    String allChars = UPPERCASE_CHARS + LOWERCASE_CHARS + DIGIT_CHARS + SPECIAL_CHARS;
    SecureRandom random = getSecureRandom();
    for (int i = 0; i < apiKeyLength - 4; i++) {
      charPool.add(getRandomChar(allChars, random));
    }
    charPool.add(getRandomChar(UPPERCASE_CHARS, random));
    charPool.add(getRandomChar(LOWERCASE_CHARS, random));
    charPool.add(getRandomChar(DIGIT_CHARS, random));
    charPool.add(getRandomChar(SPECIAL_CHARS, random));
    Collections.shuffle(charPool, random);
    StringBuilder finalPassword = new StringBuilder();
    charPool.forEach(finalPassword::append);
    return finalPassword.toString();
  }

  private char getRandomChar(String charSet, SecureRandom random) {
    int index = random.nextInt(charSet.length());
    return charSet.charAt(index);
  }

  private SecureRandom getSecureRandom() {
    if (secureRandom == null) {
      secureRandom = secureRandomService.getSecureRandom();
    }
    return secureRandom;
  }

}
