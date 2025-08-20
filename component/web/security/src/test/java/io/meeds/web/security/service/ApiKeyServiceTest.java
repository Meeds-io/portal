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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.web.security.codec.AbstractCodec;
import org.exoplatform.web.security.codec.CodecInitializer;
import org.exoplatform.web.security.security.SecureRandomService;

import io.meeds.web.security.storage.ApiKeyStorage;

import lombok.SneakyThrows;

@RunWith(MockitoJUnitRunner.class)
public class ApiKeyServiceTest {

  private static final String USER2    = "user2";

  private static final String USER1    = "user1";

  private static final String PASSWORD = "existingPass";

  @Mock
  private ApiKeyStorage       apiKeyStorage;

  @Mock
  private OtpService          otpService;

  @Mock
  private CodecInitializer    encrypter;

  @Mock
  private AbstractCodec       codec;

  @Mock
  private SecureRandomService secureRandomService;

  @Mock
  private SecureRandom        secureRandom;

  @InjectMocks
  private ApiKeyService       service;

  @Before
  @SneakyThrows
  public void setUp() {
    when(encrypter.getCodec()).thenReturn(codec);
    when(secureRandom.nextInt(anyInt())).thenAnswer(invocation -> 0);
    service.setApiKeyLength(20);
  }

  @Test
  @SneakyThrows
  public void testGetPasswordKeyAlreadyExistsNoRenew() {
    String encrypted = Base64.getEncoder().encodeToString(PASSWORD.getBytes());
    when(apiKeyStorage.getKey(USER1)).thenReturn(encrypted);
    when(codec.decode(encrypted)).thenReturn(PASSWORD);

    String result = service.getPassword(USER1, "email", "otp123", false);

    verify(otpService).validateOtp(USER1, "email", "otp123");
    assertEquals(PASSWORD, result);
    verify(apiKeyStorage, never()).saveKey(anyString(), anyString());
  }

  @Test
  @SneakyThrows
  public void testGetPasswordKeyNotExistsOrRenew() {
    when(codec.encode(anyString())).thenAnswer(invocation -> {
      String plain = invocation.getArgument(0);
      return "ENC(" + plain + ")";
    });

    String result = service.getPassword(USER2, "sms", "otp456", true);

    verify(otpService).validateOtp(USER2, "sms", "otp456");
    assertNotNull(result);
    assertEquals(20, result.length());
    verify(apiKeyStorage).saveKey(eq(USER2), startsWith("ENC("));
  }

  @Test
  @SneakyThrows
  public void testGetPasswordSimple() {
    String encrypted = "ENCODED";
    when(apiKeyStorage.getKey("user3")).thenReturn(encrypted);
    when(codec.decode(encrypted)).thenReturn("decodedPass");

    String result = service.getPassword("user3");

    assertEquals("decodedPass", result);
  }

  @Test
  @SneakyThrows
  public void testGeneratePasswordContainsAllCharTypes() {
    when(secureRandom.nextInt(anyInt())).thenAnswer(invocation -> {
      int bound = invocation.getArgument(0);
      return bound / 2;
    });

    String password = service.getPassword("user4", "app", "otp789", true);

    assertEquals(service.getApiKeyLength(), password.length());
    assertTrue(password.chars().anyMatch(ch -> "ABCDEFGHIJKLMNOPQRSTUVWXYZ".indexOf(ch) >= 0));
    assertTrue(password.chars().anyMatch(ch -> "abcdefghijklmnopqrstuvwxyz".indexOf(ch) >= 0));
    assertTrue(password.chars().anyMatch(ch -> "0123456789".indexOf(ch) >= 0));
    assertTrue(password.chars().anyMatch(ch -> "!@#$%&*?".indexOf(ch) >= 0));
  }
}
