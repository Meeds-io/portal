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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.web.security.security.CookieTokenService;

import jakarta.annotation.PostConstruct;

/**
 * Deletes the stored tokens of a user who is deleted or disabled: every token of
 * a deleted user, and the remember-me tokens of a disabled user.
 */
@Component
public class UserTokenRemovalListener extends UserEventListener {

  // the remember-me token service, not one of the services extending it
  @Autowired
  @Qualifier("org.exoplatform.web.security.security.CookieTokenService")
  private CookieTokenService  cookieTokenService;

  @Autowired
  private OrganizationService organizationService;

  @PostConstruct
  public void setup() {
    organizationService.getUserHandler().addUserEventListener(this);
  }

  @Override
  public void postSetEnabled(User user) {
    if (!user.isEnabled()) {
      cookieTokenService.deleteTokensByUsernameAndType(user.getUserName(), CookieTokenService.REMEMBER_ME_TOKEN);
    }
  }

  @Override
  public void postDelete(User user) {
    cookieTokenService.deleteTokensOfUser(user.getUserName());
  }

}
