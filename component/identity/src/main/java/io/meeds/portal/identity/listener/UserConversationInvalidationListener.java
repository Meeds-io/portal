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
package io.meeds.portal.identity.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.security.ConversationRegistry;

import jakarta.annotation.PostConstruct;

/**
 * Makes every node of a cluster build again the conversations a disabled or
 * deleted user holds, so that no node keeps serving them: the conversation
 * built again ends the session.
 */
@Component
public class UserConversationInvalidationListener extends UserEventListener {

  @Autowired
  private ConversationRegistry conversationRegistry;

  @Autowired
  private OrganizationService  organizationService;

  @PostConstruct
  public void setup() {
    organizationService.getUserHandler().addUserEventListener(this);
  }

  @Override
  public void postSetEnabled(User user) {
    if (!user.isEnabled()) {
      invalidate(user.getUserName());
    }
  }

  @Override
  public void postDelete(User user) {
    invalidate(user.getUserName());
  }

  private void invalidate(String userName) {
    // Once more after the commit: a node that reads the user before it would
    // build the conversation again from the user as it was
    if (organizationService instanceof PicketLinkIDMOrganizationServiceImpl idmOrganizationService) {
      idmOrganizationService.runNowAndAfterTransaction(() -> conversationRegistry.invalidateUser(userName));
    } else {
      conversationRegistry.invalidateUser(userName);
    }
  }

}
