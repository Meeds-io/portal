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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
  private ExoCache<String, String> otpTentativeCache;

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
    when(otpTentativeCache.get("john")).thenReturn("3");
    otpService.validateOtp("john", "sms", "1234");
  }

  @Test(expected = IllegalAccessException.class)
  @SneakyThrows
  public void testValidateOtpNoPluginFoundThrowsException() {
    otpService.setOtpPlugins(Collections.emptyList());
    when(otpTentativeCache.get("john")).thenReturn("0");
    otpService.validateOtp("john", "sms", "1234");
  }

  @Test(expected = IllegalAccessException.class)
  @SneakyThrows
  public void testValidateOtpPluginInvalidatesCodeIncrementsCounter() {
    when(otpTentativeCache.get("john")).thenReturn("1");
    when(otpPlugin.validateOtp("john", "1234")).thenReturn(false);

    otpService.validateOtp("john", "sms", "1234");

    verify(otpTentativeCache).put(eq("john"), endsWith(":2"));
  }

  @Test
  @SneakyThrows
  public void testValidateOtpPluginValidatesCodeResetsCounter() {
    when(otpTentativeCache.get("john")).thenReturn("1");
    when(otpPlugin.validateOtp("john", "1234")).thenReturn(true);

    otpService.validateOtp("john", "sms", "1234");

    verify(otpTentativeCache).remove("john");
  }

  /**
   * Wrong codes sent in parallel each count as an attempt: once the allowed
   * attempts are taken, the others are refused before their code is checked.
   * Counted after the check, every request would read the same count and all
   * of them would reach the check.
   */
  @Test
  public void testConcurrentWrongCodesEachCountAsAnAttempt() throws Exception {
    int requests = 8;
    Map<String, String> counts = backTheCounterByAMap();
    CountDownLatch allSent = new CountDownLatch(requests);
    AtomicInteger checkedCodes = new AtomicInteger();
    when(otpPlugin.validateOtp("john", "0000")).thenAnswer(invocation -> {
      checkedCodes.incrementAndGet();
      allSent.await(2, TimeUnit.SECONDS);
      return false;
    });

    ExecutorService executor = Executors.newFixedThreadPool(requests);
    try {
      for (int i = 0; i < requests; i++) {
        executor.execute(() -> {
          try {
            allSent.countDown();
            otpService.validateOtp("john", "sms", "0000");
          } catch (IllegalAccessException e) {
            // refused, as every wrong code is
          }
        });
      }
    } finally {
      executor.shutdown();
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    assertEquals(3, checkedCodes.get());
    assertTrue(counts.get("john").endsWith(":3"));
  }


  /**
   * The attempts window is the service's property while the cache keeps the
   * entry at least as long as that window
   */
  @Test
  public void testAttemptsPastTheirWindowNoLongerCount() throws Exception {
    when(otpTentativeCache.get("john")).thenReturn((System.currentTimeMillis() - 1000) + ":3");
    when(otpPlugin.validateOtp("john", "1234")).thenReturn(true);

    otpService.validateOtp("john", "sms", "1234");

    verify(otpPlugin).validateOtp("john", "1234");
  }

  @Test
  public void testAttemptsInsideTheirWindowStillLock() {
    when(otpTentativeCache.get("john")).thenReturn((System.currentTimeMillis() + 60000) + ":3");

    try {
      otpService.validateOtp("john", "sms", "1234");
      fail("The attempts limit must hold inside its window");
    } catch (IllegalAccessException e) {
      // Expected
    }
    verify(otpPlugin, never()).validateOtp(anyString(), anyString());
  }

  private Map<String, String> backTheCounterByAMap() {
    Map<String, String> counts = new ConcurrentHashMap<>();
    when(otpTentativeCache.get(anyString())).thenAnswer(invocation -> counts.get(invocation.getArgument(0, String.class)));
    doAnswer(invocation -> counts.put(invocation.getArgument(0), invocation.getArgument(1))).when(otpTentativeCache)
                                                                                         .put(anyString(), any());
    doAnswer(invocation -> counts.remove(invocation.getArgument(0, String.class))).when(otpTentativeCache)
                                                                                  .remove(anyString());
    return counts;
  }
}
