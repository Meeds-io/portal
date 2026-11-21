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
package io.meeds.web.security.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Qualifier;

import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.web.security.security.CookieTokenService;

import jakarta.annotation.PostConstruct;

@RunWith(MockitoJUnitRunner.class)
public class UserTokenRemovalListenerTest {

  @Mock
  private CookieTokenService       cookieTokenService;

  @Mock
  private OrganizationService      organizationService;

  @Mock
  private UserHandler              userHandler;

  @Mock
  private User                     user;

  @InjectMocks
  private UserTokenRemovalListener listener;

  @Test
  public void testADeletedUsersTokensAreAllDeleted() {
    when(user.getUserName()).thenReturn("jdoe");

    listener.postDelete(user);

    verify(cookieTokenService).deleteTokensOfUser("jdoe");
  }

  @Test
  public void testADisabledUsersRememberMeTokensAreDeleted() {
    when(user.getUserName()).thenReturn("jdoe");
    when(user.isEnabled()).thenReturn(false);

    listener.postSetEnabled(user);

    verify(cookieTokenService).deleteTokensByUsernameAndType("jdoe", "");
  }

  @Test
  public void testAnEnabledUsersTokensAreKept() {
    when(user.isEnabled()).thenReturn(true);

    listener.postSetEnabled(user);

    verifyNoInteractions(cookieTokenService);
  }

  @Test
  public void testTheListenerListensToUserEvents() throws Exception {
    when(organizationService.getUserHandler()).thenReturn(userHandler);

    listener.setup();

    verify(userHandler).addUserEventListener(listener);
    assertNotNull(UserTokenRemovalListener.class.getMethod("setup").getAnnotation(PostConstruct.class));
  }

  @Test
  public void testTheListenerUsesTheRememberMeTokenService() throws Exception {
    // the kernel names its components' beans after their key, and the services
    // extending CookieTokenService are beans of the same type
    Qualifier qualifier = UserTokenRemovalListener.class.getDeclaredField("cookieTokenService").getAnnotation(Qualifier.class);
    assertNotNull(qualifier);
    assertEquals(CookieTokenService.class.getName(), qualifier.value());
  }

}
