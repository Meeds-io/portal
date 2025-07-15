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
package io.meeds.portal.security.listener;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.impl.MembershipImpl;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;

import io.meeds.portal.security.constant.UserRegistrationType;
import io.meeds.portal.security.service.SecuritySettingService;

@RunWith(MockitoJUnitRunner.class)
public class DefaultUserMembershipListenerTest {

  private static final String                    INTERNAL_USERS_GROUP = "/platform/users";

  private static final String                    GROUP_ID             = "group1";

  private static final String                    USERNAME             = "username";

  private static final String                    MT                   = "member";

  @Mock
  ExoContainer                                   container;

  @Mock
  OrganizationService                            organizationService;

  @Mock
  SecuritySettingService                         securitySettingService;

  @Mock
  IdentityRegistry                               identityRegistry;

  @Mock
  ConversationRegistry                           conversationRegistry;

  @Mock
  Authenticator                                  authenticator;

  @Mock
  Event<ConversationRegistry, ConversationState> event;

  @Mock
  ConversationState                              state;

  @Mock
  Identity                                       identity;

  @Mock
  UserHandler                                    userHandler;

  @Mock
  GroupHandler                                   groupHandler;

  @Mock
  MembershipTypeHandler                          membershipTypeHandler;

  @Mock
  MembershipHandler                              membershipHandler;

  @Mock
  User                                           user;

  @Mock
  Group                                          group;

  @Mock
  MembershipType                                 membershipType;

  @Before
  public void setUp() throws Exception {
    when(container.getComponentInstanceOfType(OrganizationService.class)).thenReturn(organizationService);
    when(container.getComponentInstanceOfType(SecuritySettingService.class)).thenReturn(securitySettingService);
    when(container.getComponentInstanceOfType(IdentityRegistry.class)).thenReturn(identityRegistry);
    when(container.getComponentInstanceOfType(ConversationRegistry.class)).thenReturn(conversationRegistry);
    when(container.getComponentInstanceOfType(Authenticator.class)).thenReturn(authenticator);
    when(event.getData()).thenReturn(state);
    when(state.getIdentity()).thenReturn(identity);
    when(identity.getUserId()).thenReturn(USERNAME);
    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(organizationService.getGroupHandler()).thenReturn(groupHandler);
    when(organizationService.getMembershipTypeHandler()).thenReturn(membershipTypeHandler);
    when(organizationService.getMembershipHandler()).thenReturn(membershipHandler);
    when(userHandler.findUserByName(USERNAME)).thenReturn(user);
    when(user.getUserName()).thenReturn(USERNAME);
    when(groupHandler.findGroupById(GROUP_ID)).thenReturn(group);
    when(membershipTypeHandler.findMembershipType(MT)).thenReturn(membershipType);
  }

  @Test
  public void testConversationStateWhenNull() throws Exception {
    DefaultUserMembershipListener listener = new DefaultUserMembershipListener(container);
    when(event.getData()).thenReturn(null);
    listener.onEvent(event);
    verifyNoInteractions(organizationService);
    verifyNoInteractions(securitySettingService);
  }

  @Test
  public void testAddConversationStateWhenIdentityNull() throws Exception {
    DefaultUserMembershipListener listener = new DefaultUserMembershipListener(container);
    when(state.getIdentity()).thenReturn(null);
    listener.onEvent(event);
    verifyNoInteractions(organizationService);
    verifyNoInteractions(securitySettingService);
  }

  @Test
  public void testAddConversationStateWhenUserNull() throws Exception {
    DefaultUserMembershipListener listener = new DefaultUserMembershipListener(container);
    when(identity.getUserId()).thenReturn(null);
    listener.onEvent(event);
    verifyNoInteractions(organizationService);
    verifyNoInteractions(securitySettingService);
  }

  @Test
  public void testAddConversationStateWhenUserAnonym() throws Exception {
    DefaultUserMembershipListener listener = new DefaultUserMembershipListener(container);
    when(identity.getUserId()).thenReturn(IdentityConstants.ANONIM);
    listener.onEvent(event);
    verifyNoInteractions(organizationService);
    verifyNoInteractions(securitySettingService);
  }

  @Test
  public void testAddConversationStateWhenNoGroups() throws Exception {
    DefaultUserMembershipListener listener = new DefaultUserMembershipListener(container);
    listener.onEvent(event);
    verify(userHandler).saveUser(user, true);
    verify(securitySettingService).getRegistrationGroupIds();
  }

  @Test
  public void testLoginNotFirstTime() throws Exception {
    DefaultUserMembershipListener listener = new DefaultUserMembershipListener(container);
    when(user.getLastLoginTime()).thenReturn(new Date());
    listener.onEvent(event);
    verify(membershipHandler, never()).linkMembership(user, group, membershipType, true);
  }

  @Test
  public void testLoginFirstTimeWithExternalRegistrationAndNotOpen() throws Exception {
    DefaultUserMembershipListener listener = new DefaultUserMembershipListener(container);
    when(securitySettingService.getRegistrationGroupIds()).thenReturn(new String[] { GROUP_ID });
    listener.onEvent(event);
    verify(membershipHandler, never()).linkMembership(user, group, membershipType, true);
  }

  @Test
  public void testLoginFirstTimeWithInternalRegistrationAndNotOpen() throws Exception {
    DefaultUserMembershipListener listener = new DefaultUserMembershipListener(container);
    when(securitySettingService.getRegistrationGroupIds()).thenReturn(new String[] { GROUP_ID });
    when(membershipHandler.findMembershipsByUserAndGroup(USERNAME,
                                                         INTERNAL_USERS_GROUP)).thenReturn(Collections.singleton(new MembershipImpl()));
    listener.onEvent(event);
    verify(membershipHandler, times(1)).linkMembership(user, group, membershipType, true);
  }

  @Test
  public void testLoginFirstTimeWhenOpenRegistration() throws Exception {
    DefaultUserMembershipListener listener = new DefaultUserMembershipListener(container);
    when(securitySettingService.getRegistrationGroupIds()).thenReturn(new String[] { GROUP_ID });
    when(securitySettingService.getRegistrationType()).thenReturn(UserRegistrationType.OPEN);
    listener.onEvent(event);
    verify(membershipHandler, times(1)).linkMembership(user, group, membershipType, true);
  }

}
