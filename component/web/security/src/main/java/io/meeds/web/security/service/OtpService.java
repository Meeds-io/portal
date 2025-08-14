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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

import io.meeds.web.security.plugin.OtpPlugin;

import lombok.SneakyThrows;

@Service
public class OtpService {

  private static final String    OTP_TENTATIVES_CACHE_NAME = "otp.tentatives";

  @Autowired
  private CacheService           cacheService;

  @Autowired(required = false)
  private List<OtpPlugin>        otpPlugins;

  /**
   * OTP Tentatives Cache in minutes
   */
  @Value("${meeds.apiKey.otp.maxTentatives.ttl:5}")
  private long                   otpTentativesTtl;

  @Value("${meeds.apiKey.otp.maxTentatives.count:5}")
  private long                   otpMaxTentatives;

  private ExoCache<String, Long> otpTentativeCache;

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
    long tentativeCount = getOtpTentativeCount(userName);
    if (tentativeCount >= otpMaxTentatives) {
      throw new IllegalAccessException();
    } else if (otpPlugin == null || !otpPlugin.validateOtp(userName, otpCode)) {
      getOtpTentativeCache().put(userName, tentativeCount + 1);
      throw new IllegalAccessException();
    } else {
      getOtpTentativeCache().remove(userName);
    }
  }

  private long getOtpTentativeCount(String userName) {
    Long tentativeCount = getOtpTentativeCache().get(userName);
    if (tentativeCount == null) {
      return 0l;
    } else {
      return tentativeCount;
    }
  }

  private OtpPlugin getOtpPlugin(String otpMethod) {
    return otpPlugins.stream()
                     .filter(p -> p.getName().equals(otpMethod))
                     .findFirst()
                     .orElse(null);
  }

  private ExoCache<String, Long> getOtpTentativeCache() {
    if (otpTentativeCache == null) {
      otpTentativeCache = cacheService.getCacheInstance(OTP_TENTATIVES_CACHE_NAME);
      otpTentativeCache.setLiveTime(otpTentativesTtl * 60);
    }
    return otpTentativeCache;
  }

}
