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
package org.exoplatform.web.security.security;

import org.exoplatform.container.xml.InitParams;

import io.meeds.web.security.storage.PortalTokenStorage;

/**
 * The issuer of the onboarding tokens, the links from which an invited user
 * sets their own password: its validity is configured on its own
 * (gatein.portal.onboarding.expiretime, in days), apart from the password
 * reminder's. It shares the password reminder's storage, the type of the
 * token telling them apart.
 */
public class OnboardingTokenService extends CookieTokenService {

  public OnboardingTokenService(InitParams initParams, PortalTokenStorage tokenStore) throws TokenServiceInitializationException {
    super(initParams, tokenStore);
  }

  /**
   * @return the validity of a token this service issues, in milliseconds
   */
  public long getValidityMillis() {
    return validityMillis;
  }

  @Override
  protected String nextTokenId() {
    return nextRandom();
  }
}
