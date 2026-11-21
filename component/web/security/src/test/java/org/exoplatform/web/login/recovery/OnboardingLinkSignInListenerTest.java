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

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.organization.idm.UserImpl;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.web.security.security.OnboardingTokenService;

public class OnboardingLinkSignInListenerTest {

  @Test
  public void aSignInEndsTheAccountsOnboardingLinks() throws Exception {
    OnboardingTokenService onboardingTokenService = mock(OnboardingTokenService.class);
    OrganizationService organizationService = mock(OrganizationService.class);
    UserHandler userHandler = mock(UserHandler.class);
    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(userHandler.findUserByName("JDoe", UserStatus.ANY)).thenReturn(new UserImpl("jdoe"));

    new OnboardingLinkSignInListener(onboardingTokenService,
                                     organizationService).onEvent(event(new ConversationState(new Identity("JDoe"))));
    verify(onboardingTokenService).deleteTokensByUsernameAndType("jdoe", CookieTokenService.ONBOARD_TOKEN);
  }

  @Test
  public void anAnonymousSessionEndsNoLink() {
    OnboardingTokenService onboardingTokenService = mock(OnboardingTokenService.class);
    OnboardingLinkSignInListener listener = new OnboardingLinkSignInListener(onboardingTokenService,
                                                                             mock(OrganizationService.class));
    listener.onEvent(event(new ConversationState(new Identity(IdentityConstants.ANONIM))));
    listener.onEvent(event(new ConversationState(new Identity(""))));
    listener.onEvent(event(null));
    verify(onboardingTokenService, never()).deleteTokensByUsernameAndType(nullable(String.class), nullable(String.class));
  }

  private static Event<ConversationRegistry, ConversationState> event(ConversationState state) {
    return new Event<>("exo.core.security.ConversationRegistry.register", mock(ConversationRegistry.class), state);
  }

}
