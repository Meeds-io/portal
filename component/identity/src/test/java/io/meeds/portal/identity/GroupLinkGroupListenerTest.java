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
package io.meeds.portal.identity;

import io.meeds.services.organization.listener.GroupLinkGroupListener;
import org.exoplatform.services.organization.NestedMembership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.List;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GroupLinkGroupListenerTest {

  @Mock
  private IdentityRegistry       identityRegistry;

  @Mock
  private ConversationRegistry   conversationRegistry;

  @Mock
  private OrganizationService    organizationService;

  @InjectMocks
  private GroupLinkGroupListener listener;

  @Test
  @SuppressWarnings("deprecation")
  public void shouldUnregisterIdentityWhenMembershipMatches() throws Exception {
    // Given
    String userId = "root";
    String groupId = "/organization/management/executive-board";
    String membershipType = "member";
    Identity identity = new Identity(userId);
    identity.setMemberships(List.of(new MembershipEntry(groupId, membershipType)));

    when(identityRegistry.getIdentities()).thenReturn(List.of(identity));
    // use the default nested membership type
    NestedMembership nestedMembership = NestedMembership.builder().nestedGroupId(groupId).build();

    // When
    listener.linkGroups(nestedMembership);
    // Then
    verify(identityRegistry, timeout(500).times(1)).unregister(userId);
    verify(conversationRegistry, timeout(500).times(1)).unregisterByUserId(userId);

    // use specific nested membership type
    nestedMembership = NestedMembership.builder().nestedGroupId(groupId).nestedMembershipType(membershipType).build();
    // When
    listener.linkGroups(nestedMembership);

    // Then
    verify(identityRegistry, timeout(500).times(2)).unregister(userId);
    verify(conversationRegistry, timeout(500).times(2)).unregisterByUserId(userId);
  }

}
