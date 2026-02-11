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

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.services.organization.GroupEventListener;
import org.exoplatform.services.organization.NestedMembership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

@Component
public class GroupLinkGroupListener extends GroupEventListener {

  @Autowired
  private IdentityRegistry     identityRegistry;

  @Autowired
  private ConversationRegistry conversationRegistry;

  @Autowired
  private OrganizationService  organizationService;

  @PostConstruct
  public void setup() {
    this.organizationService.getGroupHandler().addGroupEventListener(this);
  }

  @Override
  public void linkGroups(NestedMembership nestedMembership) throws Exception {
    CompletableFuture.runAsync(() -> refreshIdentitiesMemberships(nestedMembership));
  }

  @Override
  public void unlinkGroups(NestedMembership nestedMembership) throws Exception {
    CompletableFuture.runAsync(() -> refreshIdentitiesMemberships(nestedMembership));
  }

  @SneakyThrows
  private void refreshIdentitiesMemberships(NestedMembership nestedMembership) {
    identityRegistry.getIdentities()
                    .stream()
                    .filter(identity -> hasMatchingMembership(identity, nestedMembership))
                    .map(Identity::getUserId)
                    .forEach(this::clearIdentityCache);
  }

  private boolean hasMatchingMembership(Identity identity, NestedMembership nestedMembership) {
    if (identity.getMemberships() == null) {
      return false;
    }
    return identity.getMemberships()
                   .stream()
                   .anyMatch(m -> m.getGroup().equals(nestedMembership.getNestedGroupId())
                                  && (nestedMembership.isIncludeAllMembershipTypes()
                                      || m.getMembershipType().equals(nestedMembership.getNestedMembershipType())));
  }

  private void clearIdentityCache(String userId) {
    identityRegistry.unregister(userId);
    conversationRegistry.unregisterByUserId(userId);
  }

}
