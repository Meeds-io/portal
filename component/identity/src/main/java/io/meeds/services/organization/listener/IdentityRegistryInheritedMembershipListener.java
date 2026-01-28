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
package io.meeds.services.organization.listener;

import org.apache.commons.collections.CollectionUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.GroupEventListener;
import org.exoplatform.services.organization.NestedMembership;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class IdentityRegistryInheritedMembershipListener extends GroupEventListener {

  protected static final Log LOG = ExoLogger.getLogger(IdentityRegistryInheritedMembershipListener.class);

  private final IdentityRegistry    identityRegistry;

  private final ConversationRegistry conversationRegistry;

  public IdentityRegistryInheritedMembershipListener(IdentityRegistry identityRegistry, ConversationRegistry conversationRegistry) {
    this.identityRegistry = identityRegistry;
    this.conversationRegistry = conversationRegistry;
  }

  @Override
  public void linkGroups(NestedMembership nestedMembership) throws Exception {
    CompletableFuture.runAsync(() -> refreshIdentitiesMemberships(nestedMembership));
  }

  @Override
  public void unlinkGroups(NestedMembership nestedMembership) throws Exception {
    CompletableFuture.runAsync(() -> refreshIdentitiesMemberships(nestedMembership));
  }

  private void refreshIdentitiesMemberships(NestedMembership nestedMembership) {
    List<Identity> identities = new ArrayList<>();
    try {
      identities = identityRegistry.getIdentities()
                                   .stream()
                                   .map(identity -> (Identity) identity)
                                   .filter(identity -> hasMatchingMembership(identity, nestedMembership))
                                   .toList();
    } catch (Exception exception) {
      LOG.error("Error while fetching cached identities", exception);
      return;
    }
    if (CollectionUtils.isNotEmpty(identities)) {
      for (Identity identity : identities) {
        identityRegistry.unregister(identity.getUserId());
        conversationRegistry.unregisterByUserId(identity.getUserId());
      }
    }
  }

  private boolean hasMatchingMembership(Identity identity, NestedMembership nestedMembership) {
    if (identity.getMemberships() == null) {
      return false;
    }
    return identity.getMemberships().stream()
            .anyMatch(m -> m.getGroup().equals(nestedMembership.getNestedGroupId())
                    && (nestedMembership.isIncludeAllMembershipTypes()
                    || m.getMembershipType().equals(nestedMembership.getNestedMembershipType())));
  }
}
