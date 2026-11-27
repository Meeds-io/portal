/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package org.exoplatform.web.login.recovery;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.web.security.security.OnboardingTokenService;

/**
 * Ends the pending onboarding links of an account on each sign-in the
 * conversation registry announces
 */
public class OnboardingLinkSignInListener extends Listener<ConversationRegistry, ConversationState> {

  private static final Log             LOG = ExoLogger.getLogger(OnboardingLinkSignInListener.class);

  private final OnboardingTokenService onboardingTokenService;

  private final OrganizationService    organizationService;

  public OnboardingLinkSignInListener(OnboardingTokenService onboardingTokenService, OrganizationService organizationService) {
    this.onboardingTokenService = onboardingTokenService;
    this.organizationService = organizationService;
  }

  @Override
  public void onEvent(Event<ConversationRegistry, ConversationState> event) {
    ConversationState state = event.getData();
    String username = state == null || state.getIdentity() == null ? null : state.getIdentity().getUserId();
    if (StringUtils.isNotBlank(username) && !StringUtils.equals(username, IdentityConstants.ANONIM)) {
      onboardingTokenService.deleteTokensByUsernameAndType(storedUserName(username), CookieTokenService.ONBOARD_TOKEN);
    }
  }

  private String storedUserName(String username) {
    try {
      User user = organizationService.getUserHandler().findUserByName(username, UserStatus.ANY);
      return user == null ? username : user.getUserName();
    } catch (Exception e) {
      LOG.warn("Can not read the user {} that signed in, ending its onboarding links by the name it used", username, e);
      return username;
    }
  }

}
