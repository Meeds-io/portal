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
package org.exoplatform.web.login.recovery;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.web.security.security.CookieTokenService;

public class DefaultChangePasswordConnector extends ChangePasswordConnector {
  
  private OrganizationService organizationService;

  private CookieTokenService  cookieTokenService;

  public final static String LOG_SERVICE_NAME = "changePassword";
  
  protected static Log log = ExoLogger.getLogger(DefaultChangePasswordConnector.class);
  
  public DefaultChangePasswordConnector(InitParams initParams,
                                        OrganizationService organizationService,
                                        CookieTokenService cookieTokenService) {
    this.organizationService=organizationService;
    this.cookieTokenService = cookieTokenService;

  }
  
  /**
   * @return the allowChangeExternalPassword
   */
  @Override
  public boolean isAllowChangeExternalPassword() {
    return false;
  }
  
  @Override
  public void changePassword(final String username, final String password) throws Exception {
    User user = organizationService.getUserHandler().findUserByName(username);
    
    if (user.isInternalStore()) {
      changeInternalPassword(user, password);
    } else {
      throw new Exception("Change password in external store in not allowed");
    }
  }
  
  private void changeInternalPassword(User user, String password) throws Exception {
    long startTime = System.currentTimeMillis();
    user.setPassword(password);
    organizationService.getUserHandler().saveUser(user, true);
    cookieTokenService.deleteTokensByUsernameAndType(user.getUserName(), "");
    long totalTime = System.currentTimeMillis() - startTime;

    log.info("service={} operation={} parameters=\"user:{}\" status=ok duration_ms={}",
             LOG_SERVICE_NAME, "changeInternalPassword", user.getUserName(), totalTime);
  }
}
