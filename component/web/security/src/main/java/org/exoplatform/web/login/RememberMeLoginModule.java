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
package org.exoplatform.web.login;

import javax.security.auth.login.LoginException;
import jakarta.servlet.http.HttpServletRequest;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.UsernameCredential;
import org.exoplatform.services.security.j2ee.TomcatLoginModule;
import org.exoplatform.web.security.security.CookieTokenService;
import org.gatein.sso.agent.tomcat.ServletAccess;

public class RememberMeLoginModule extends TomcatLoginModule {

  private static final Log log = ExoLogger.getLogger(RememberMeLoginModule.class);

  @Override
  public boolean login() throws LoginException {
    try {
      HttpServletRequest servletRequest = ServletAccess.getRequest();
      if (servletRequest == null) {
        LOG.warn("HttpServletRequest is null. RemembermeLoginModule will be ignored.");
        return false;
      }
      String token = LoginUtils.getRememberMeTokenCookie(servletRequest);

      if (token != null) {
        CookieTokenService tokenservice = getContainer().getComponentInstanceOfType(CookieTokenService.class);
        String username = tokenservice.validateToken(token, false);
        if (username != null) {
          Authenticator authenticator = getContainer().getComponentInstanceOfType(Authenticator.class);
          if (authenticator == null) {
            throw new LoginException("No Authenticator component found, check your configuration");
          }
          identity = authenticator.createIdentity(username);
          sharedState.put("javax.security.auth.login.name", username);
          sharedState.put("exo.security.identity", identity);
          subject.getPublicCredentials().add(new UsernameCredential(username));

        } else {
          log.debug("Rememberme cookie is not valid. RemembermeLoginModule will be ignored.");

        }

      } else {
        log.debug("No username has been committed. RemembermeLoginModule will be ignored.");
      }

      return true;
    } catch (final Exception e) {
      throw new LoginException(e.getMessage());
    }
  }

  @Override
  public boolean commit() throws LoginException {
    if (identity != null) {
      super.commit();
    }
    return true;
  }
}
