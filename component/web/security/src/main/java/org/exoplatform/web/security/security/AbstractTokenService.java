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
package org.exoplatform.web.security.security;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.management.annotations.Impact;
import org.exoplatform.management.annotations.ImpactType;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.web.login.LoginUtils;
import org.exoplatform.web.security.Token;
import org.exoplatform.web.security.TokenStore;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.gatein.common.util.Base64;
import org.gatein.common.util.Base64.EncodingOption;
import org.picocontainer.Startable;

/**
 * @param <T> the token type
 * @param <K> the token key type
 */
@Managed
@ManagedDescription("Token Store Service")
@NameTemplate({ @Property(key = "service", value = "TokenStore"), @Property(key = "name", value = "{Name}") })
public abstract class AbstractTokenService<T extends Token, K> implements Startable, TokenStore {

  protected final Log              log                       = ExoLogger.getLogger(getClass());

  protected static final String    SERVICE_CONFIG            = "service.configuration";

  protected static final String    CLEANUP_PERIOD_TIME       = "cleanup.period.time";

  /**
   * See {@link #tokenByteLength}. 16 bytes (128 bits) would be enough
   * (Validator + Selector = 256bits), but we want to get padding-less Byte64
   * representation, so we take a greater number divisible by 3.<br>
   * (24 bytes is equal to 192 bits.)
   */
  public static final int          DEFAULT_TOKEN_BYTE_LENGTH = 24;

  /**
   * The number of random bits generared by {@link #nextRandom()}. Use values
   * divisible by 3 to produce random strings consisting only of {@code 0-9},
   * {@code a-z}, {@code A-Z}, {@code -} and {@code _}, i.e. URL safe Byte64
   * without padding. If a value not divisible by 3 is used the random strings
   * will contain {@code *} in addition to the named characters.
   */
  protected final int              tokenByteLength;

  protected String                 name;

  protected long                   validityMillis;

  protected int                    delayTime                 = 600;

  private ScheduledExecutorService executor;

  AbstractTokenService(InitParams initParams) {
    List<String> params = initParams.getValuesParam(SERVICE_CONFIG).getValues();
    this.name = params.get(0);
    long configValue = Long.parseLong(params.get(1));
    this.validityMillis = TimeoutEnum.valueOf(params.get(2)).toMilisecond(configValue);
    this.tokenByteLength = DEFAULT_TOKEN_BYTE_LENGTH;
    ValueParam delayParam = initParams.getValueParam(CLEANUP_PERIOD_TIME);
    if (delayParam != null) {
      delayTime = Integer.parseInt(delayParam.getValue());
    }
  }

  @Override
  public void start() {
    if (delayTime > 0) {
      // start a thread, garbage expired cookie token every [DELAY_TIME]
      executor = Executors.newSingleThreadScheduledExecutor();
      executor.scheduleWithFixedDelay(() -> {
        try {
          PortalContainer container = PortalContainer.getInstance();
          ExoContainerContext.setCurrentContainer(container);
          RequestLifeCycle.begin(container);
          try {
            AbstractTokenService.this.cleanExpiredTokens();
          } finally {
            RequestLifeCycle.end();
          }
        } catch (Throwable t) { // NOSONAR
          log.warn("Failed to clean expired tokens", t);
        }
      }, 0, delayTime, TimeUnit.SECONDS);
    }
  }

  @Override
  public void stop() {
    if (executor != null) {
      executor.shutdown();
    }
  }

  public static <T extends AbstractTokenService<?, ?>> T getInstance(Class<T> classType) {
    PortalContainer container = PortalContainer.getInstance();
    return classType.cast(container.getComponentInstanceOfType(classType));
  }

  public String validateToken(String stringKey, boolean remove) {
    if (stringKey == null) {
      throw new NullPointerException();
    }

    //
    K tokenKey = decodeKey(stringKey);

    T token;
    try {
      if (remove) {
        token = this.deleteToken(tokenKey);
      } else {
        token = this.getToken(tokenKey);
      }

      if (token != null) {
        boolean valid = token.getExpirationTimeMillis() > System.currentTimeMillis();
        if (valid) {
          return token.getUsername();
        } else if (!remove) {
          this.deleteToken(tokenKey);
        }
      }
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.warn("Error retrieving token", e);
      } else {
        log.warn("Error retrieving token. Cause: " + e.getMessage());
      }
    }
    return null;
  }

  @Managed
  @ManagedDescription("Clean all tokens are expired")
  @Impact(ImpactType.IDEMPOTENT_WRITE)
  public abstract void cleanExpiredTokens();

  @Managed
  @ManagedDescription("Get time for token expiration in seconds")
  public long getValidityTime() {
    return validityMillis / 1000;
  }

  @Managed
  @ManagedDescription("The expiration daemon period time in seconds")
  public long getPeriodTime() {
    return delayTime;
  }

  @Managed
  @ManagedDescription("The token service name")
  public String getName() {
    return name;
  }

  public abstract T getToken(K id);

  public abstract T getToken(K id, String tokenType);

  public abstract T deleteToken(K id);

  public abstract T deleteToken(K id, String tokenType);

  /**
   * Decode a key from its string representation.
   *
   * @param stringKey the key a s a string
   * @return the typed key
   */
  protected abstract K decodeKey(String stringKey);

  // We don't make it a property as retrieving the value can be an expensive
  // operation
  @Managed
  @ManagedDescription("The number of tokens")
  @Impact(ImpactType.READ)
  public abstract long size();

  private enum TimeoutEnum {
    SECOND(1000), MINUTE(1000l * 60), HOUR(1000l * 60 * 60), DAY(1000l * 60 * 60 * 24);

    private long multiply;

    private TimeoutEnum(long multiply) {
      this.multiply = multiply;
    }

    public long toMilisecond(long configValue) {
      return configValue * multiply;
    }
  }

  protected String nextTokenId() {
    return LoginUtils.COOKIE_NAME + nextRandom();
  }

  protected String nextRandom() {
    byte[] randomBytes = new byte[tokenByteLength];
    PortalContainer container = PortalContainer.getInstance();
    SecureRandom random = container.getComponentInstanceOfType(SecureRandomService.class).getSecureRandom();
    random.nextBytes(randomBytes);
    return Base64.encodeBytes(randomBytes, EncodingOption.USEURLSAFEENCODING);
  }

}
