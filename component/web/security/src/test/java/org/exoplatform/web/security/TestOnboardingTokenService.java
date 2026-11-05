/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package org.exoplatform.web.security;

import java.util.concurrent.TimeUnit;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.web.security.security.OnboardingTokenService;

/**
 * The onboarding token issuer as configured: the entry of the test
 * configuration is the one web/portal ships in remindpwd-configuration.xml,
 * so the figure the administrator sees (7 days by default) is the
 * configured one
 */
@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.security-configuration-local.xml") })
public class TestOnboardingTokenService extends AbstractKernelTest {

  private OnboardingTokenService onboardingTokenService;

  @Override
  public void setUp() throws Exception {
    PortalContainer container = getContainer();
    RequestLifeCycle.begin(container);
    onboardingTokenService = container.getComponentInstanceOfType(OnboardingTokenService.class);
  }

  @Override
  public void tearDown() throws Exception {
    // The token store is shared with the other token services' tests, which
    // count its rows: nothing is left behind
    onboardingTokenService.deleteTokensByUsernameAndType("jdoe", CookieTokenService.ONBOARD_TOKEN);
    RequestLifeCycle.end();
  }

  public void testConfiguredValidityIsSevenDays() {
    assertEquals(TimeUnit.DAYS.toMillis(7), onboardingTokenService.getValidityMillis());
    assertEquals(TimeUnit.DAYS.toSeconds(7), onboardingTokenService.getValidityTime());
  }

  public void testOnboardTokenIsIssuedWithTheConfiguredValidity() {
    long before = System.currentTimeMillis();
    String tokenId = onboardingTokenService.createToken("jdoe", CookieTokenService.ONBOARD_TOKEN);
    PortalToken token = onboardingTokenService.getToken(tokenId, CookieTokenService.ONBOARD_TOKEN);
    assertNotNull(token);
    assertEquals("jdoe", token.getUsername());
    assertFalse(token.isExpired());
    long drift = Math.abs(token.getExpirationTimeMillis() - before - TimeUnit.DAYS.toMillis(7));
    assertTrue(String.valueOf(drift), drift < TimeUnit.MINUTES.toMillis(1));
    // A token of another type is not this issuer's
    assertNull(onboardingTokenService.getToken(tokenId, CookieTokenService.FORGOT_PASSWORD_TOKEN));
  }
}
