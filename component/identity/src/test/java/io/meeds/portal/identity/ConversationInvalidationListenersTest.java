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
package io.meeds.portal.identity;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationRegistry;

import io.meeds.services.organization.listener.UserConversationInvalidationListener;

/**
 * A disabled or deleted user is no longer served its conversations, on
 * every node.
 */
@RunWith(MockitoJUnitRunner.class)
public class ConversationInvalidationListenersTest {

  @Mock
  private ConversationRegistry                       conversationRegistry;

  @Mock
  private OrganizationService                        organizationService;

  @InjectMocks
  private UserConversationInvalidationListener       userListener;

  @Test
  public void testADisabledUserLosesItsConversations() {
    userListener.postSetEnabled(user(false));

    verify(conversationRegistry).invalidateUser("john");
  }

  @Test
  public void testAnEnabledUserKeepsItsConversations() {
    userListener.postSetEnabled(user(true));

    verify(conversationRegistry, never()).invalidateUser("john");
  }

  @Test
  public void testADeletedUserLosesItsConversations() {
    userListener.postDelete(user(true));

    verify(conversationRegistry).invalidateUser("john");
  }

  private User user(boolean enabled) {
    User user = mock(User.class);
    when(user.getUserName()).thenReturn("john");
    org.mockito.Mockito.lenient().when(user.isEnabled()).thenReturn(enabled);
    return user;
  }

}
