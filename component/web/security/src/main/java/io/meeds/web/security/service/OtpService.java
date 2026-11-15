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

import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

import io.meeds.web.security.plugin.OtpPlugin;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

@Service
public class OtpService {

  private static final String    OTP_TENTATIVES_CACHE_NAME = "otp.tentatives";

  private static final char      EXPIRY_SEPARATOR          = ':';

  @Autowired
  private CacheService           cacheService;

  @Autowired(required = false)
  @Getter
  @Setter
  private List<OtpPlugin>        otpPlugins;

  /**
   * OTP Tentatives Cache in minutes
   */
  @Value("${meeds.apiKey.otp.maxTentatives.ttl:5}")
  @Getter
  @Setter
  private long                   otpTentativesTtl;

  @Value("${meeds.apiKey.otp.maxTentatives.count:5}")
  @Getter
  @Setter
  private long                   otpMaxTentatives;

  private ExoCache<String, String> otpTentativeCache;

  /**
   * Striped locks under which an attempt is counted, so that wrong codes sent
   * in parallel for one user each take an attempt
   */
  private final Object[]         attemptLocks              = IntStream.range(0, 64).mapToObj(i -> new Object()).toArray();

  @SneakyThrows
  public void sendOtpCode(String userName, String otpMethod) {
    OtpPlugin otpPlugin = getOtpPlugin(otpMethod);
    if (otpPlugin == null) {
      throw new IllegalAccessException();
    }
    otpPlugin.generateOtpCode(userName);
  }

  public void validateOtp(String userName, String otpMethod, String otpCode) throws IllegalAccessException {
    if (StringUtils.isBlank(otpCode)) {
      throw new IllegalAccessException();
    }
    OtpPlugin otpPlugin = getOtpPlugin(otpMethod);
    // The attempt is counted before the code is checked, and the count is read
    // and written under the user's lock: concurrent attempts each count once
    synchronized (getAttemptLock(userName)) {
      long tentativeCount = getOtpTentativeCount(userName);
      if (tentativeCount >= otpMaxTentatives) {
        throw new IllegalAccessException();
      }
      getOtpTentativeCache().put(userName,
                                 String.valueOf(System.currentTimeMillis() + otpTentativesTtl * 60000) + EXPIRY_SEPARATOR
                                     + (tentativeCount + 1));
    }
    if (otpPlugin == null || !otpPlugin.validateOtp(userName, otpCode)) {
      throw new IllegalAccessException();
    } else {
      getOtpTentativeCache().remove(userName);
    }
  }

  /**
   * Validates the code as {@link #validateOtp(String, String, String)} does,
   * then consumes it, so that the same code never validates a second time.
   * Two concurrent validations of one code see one winner: the loser is
   * refused like a wrong code, without counting as a failed attempt.
   *
   * @param userName user login
   * @param otpMethod OTP method name
   * @param otpCode code to validate
   * @throws IllegalAccessException when the code is blank, wrong, expired,
   *           already consumed, or when the user exceeded the allowed attempts
   */
  public void validateAndConsumeOtp(String userName, String otpMethod, String otpCode) throws IllegalAccessException {
    validateOtp(userName, otpMethod, otpCode);
    if (!consumeOtp(userName, otpMethod, otpCode)) {
      throw new IllegalAccessException();
    }
  }

  /**
   * Consumes a code already validated, for a flow that writes between its
   * validation and the consumption
   *
   * @param userName user login
   * @param otpMethod OTP method name
   * @param otpCode validated code
   * @return true when this call consumed the code, false when it was no longer
   *         there or the method is unknown
   */
  public boolean consumeOtp(String userName, String otpMethod, String otpCode) {
    OtpPlugin otpPlugin = getOtpPlugin(otpMethod);
    return otpPlugin != null && otpPlugin.consumeOtp(userName, otpCode);
  }

  /**
   * The count carries its expiry, so the attempts window is this service's
   * property as long as the cache keeps the entry at least
   * meeds.apiKey.otp.maxTentatives.ttl: an entry the cache drops sooner
   * counts no attempt
   */
  private long getOtpTentativeCount(String userName) {
    String storedCount = getOtpTentativeCache().get(userName);
    if (storedCount == null) {
      return 0;
    }
    int separator = storedCount.indexOf(EXPIRY_SEPARATOR);
    if (separator < 0) {
      return Long.parseLong(storedCount);
    }
    long expiry = Long.parseLong(storedCount.substring(0, separator));
    return expiry > System.currentTimeMillis() ? Long.parseLong(storedCount.substring(separator + 1)) : 0;
  }

  private Object getAttemptLock(String userName) {
    return attemptLocks[Math.floorMod(userName.hashCode(), attemptLocks.length)];
  }

  private OtpPlugin getOtpPlugin(String otpMethod) {
    return otpPlugins.stream()
                     .filter(p -> p.getName().equals(otpMethod))
                     .findFirst()
                     .orElse(null);
  }

  private ExoCache<String, String> getOtpTentativeCache() {
    if (otpTentativeCache == null) {
      otpTentativeCache = cacheService.getCacheInstance(OTP_TENTATIVES_CACHE_NAME);
    }
    return otpTentativeCache;
  }

}
