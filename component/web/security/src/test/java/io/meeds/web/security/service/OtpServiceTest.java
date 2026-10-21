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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

import io.meeds.web.security.plugin.OtpPlugin;

import lombok.SneakyThrows;

@RunWith(MockitoJUnitRunner.Silent.class)
public class OtpServiceTest {

  @Mock
  private CacheService           cacheService;

  @Mock
  private ExoCache<String, Long> otpTentativeCache;

  @Mock
  private OtpPlugin              otpPlugin;

  @InjectMocks
  private OtpService             otpService;

  @Before
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void setUp() {
    otpService.setOtpPlugins(Arrays.asList(otpPlugin));
    otpService.setOtpTentativesTtl(5); // minutes
    otpService.setOtpMaxTentatives(3);

    when(cacheService.getCacheInstance("otp.tentatives")).thenReturn((ExoCache) otpTentativeCache); // NOSONAR
    when(otpPlugin.getName()).thenReturn("sms");
  }

  @Test
  public void testSendOtpCodeSuccess() {
    otpService.sendOtpCode("john", "sms");
    verify(otpPlugin).generateOtpCode("john");
  }

  @Test(expected = IllegalAccessException.class)
  @SneakyThrows
  public void testSendOtpCodeNoPluginFoundThrowsException() {
    otpService.setOtpPlugins(Collections.emptyList());
    otpService.sendOtpCode("john", "email");
  }

  @Test(expected = IllegalAccessException.class)
  @SneakyThrows
  public void testValidateOtpBlankCodeThrowsException() {
    otpService.validateOtp("john", "sms", " ");
  }

  @Test(expected = IllegalAccessException.class)
  @SneakyThrows
  public void testValidateOtpTooManyAttemptsThrowsException() {
    when(otpTentativeCache.get("john")).thenReturn(3L);
    otpService.validateOtp("john", "sms", "1234");
  }

  @Test(expected = IllegalAccessException.class)
  @SneakyThrows
  public void testValidateOtpNoPluginFoundThrowsException() {
    otpService.setOtpPlugins(Collections.emptyList());
    when(otpTentativeCache.get("john")).thenReturn(0L);
    otpService.validateOtp("john", "sms", "1234");
  }

  @Test(expected = IllegalAccessException.class)
  @SneakyThrows
  public void testValidateOtpPluginInvalidatesCodeIncrementsCounter() {
    when(otpTentativeCache.get("john")).thenReturn(1L);
    when(otpPlugin.validateOtp("john", "1234")).thenReturn(false);

    otpService.validateOtp("john", "sms", "1234");

    verify(otpTentativeCache).put("john", 2L);
  }

  @Test
  @SneakyThrows
  public void testValidateOtpPluginValidatesCodeResetsCounter() {
    when(otpTentativeCache.get("john")).thenReturn(1L);
    when(otpPlugin.validateOtp("john", "1234")).thenReturn(true);

    otpService.validateOtp("john", "sms", "1234");

    verify(otpTentativeCache).remove("john");
  }

  @Test
  @SneakyThrows
  public void testValidateAndConsumeOtpConsumesTheCode() {
    when(otpTentativeCache.get("john")).thenReturn(0L);
    when(otpPlugin.validateOtp("john", "1234")).thenReturn(true);
    when(otpPlugin.consumeOtp("john", "1234")).thenReturn(true);

    otpService.validateAndConsumeOtp("john", "sms", "1234");

    verify(otpPlugin).consumeOtp("john", "1234");
  }

  /**
   * Two validations of one code: the first consumes it, the second finds it
   * gone and is refused like a wrong code, without counting a failed attempt
   */
  @Test
  @SneakyThrows
  public void testValidateAndConsumeOtpHasOneWinnerPerCode() {
    when(otpTentativeCache.get("john")).thenReturn(0L);
    when(otpPlugin.validateOtp("john", "1234")).thenReturn(true);
    when(otpPlugin.consumeOtp("john", "1234")).thenReturn(true, false);

    otpService.validateAndConsumeOtp("john", "sms", "1234");
    try {
      otpService.validateAndConsumeOtp("john", "sms", "1234");
      fail("A consumed code must not validate twice");
    } catch (IllegalAccessException e) {
      // Expected
    }
    verify(otpTentativeCache, never()).put(anyString(), anyLong());
  }

  @Test
  @SneakyThrows
  public void testValidateAndConsumeOtpKeepsTheCodeOnWrongCode() {
    when(otpTentativeCache.get("john")).thenReturn(0L);
    when(otpPlugin.validateOtp("john", "0000")).thenReturn(false);
    try {
      otpService.validateAndConsumeOtp("john", "sms", "0000");
      fail("A wrong code must be refused");
    } catch (IllegalAccessException e) {
      // Expected
    }
    verify(otpPlugin, never()).consumeOtp(anyString(), anyString());
    verify(otpTentativeCache, times(1)).put("john", 1L);
  }

  @Test
  @SneakyThrows
  public void testValidateAndConsumeOtpRefusesThrottledUserWithoutConsuming() {
    when(otpTentativeCache.get("john")).thenReturn(3L);
    try {
      otpService.validateAndConsumeOtp("john", "sms", "1234");
      fail("A throttled user must be refused");
    } catch (IllegalAccessException e) {
      // Expected
    }
    verify(otpPlugin, never()).validateOtp(anyString(), anyString());
    verify(otpPlugin, never()).consumeOtp(anyString(), anyString());
  }

  @Test
  public void testConsumeOtpOfUnknownMethodConsumesNothing() {
    assertFalse(otpService.consumeOtp("john", "email", "1234"));
    verify(otpPlugin, never()).consumeOtp(anyString(), anyString());
  }

  @Test
  public void testConsumeOtpDelegatesToThePlugin() {
    when(otpPlugin.consumeOtp("john", "1234")).thenReturn(true);
    assertTrue(otpService.consumeOtp("john", "sms", "1234"));
  }

  @Test
  public void testDefaultPluginConsumptionHasNothingToConsume() {
    OtpPlugin plugin = mock(OtpPlugin.class, org.mockito.Mockito.CALLS_REAL_METHODS);
    assertTrue(plugin.consumeOtp("john", "1234"));
    assertEquals(0, plugin.getCodeTtlMinutes());
    assertEquals(0, plugin.getSendIntervalSeconds());
  }
}
