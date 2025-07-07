
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
package io.meeds.web.security.authenticator;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.UsernameCredential;
import org.exoplatform.services.security.jaas.AbstractLoginModule;
import org.exoplatform.services.security.jaas.RolePrincipal;
import org.exoplatform.services.security.jaas.UserPrincipal;

import lombok.SneakyThrows;

public class DigestLoginModule extends AbstractLoginModule {

  protected static final Log                  LOG = ExoLogger.getLogger(DigestLoginModule.class);

  protected static DigestAuthenticatorService digestAuthenticatorService;

  protected static IdentityRegistry           identityRegistry;

  protected Identity                          identity;

  @Override
  @SuppressWarnings("unchecked")
  public boolean login() throws LoginException {
    String username = null;
    try {
      if (sharedState.containsKey("exo.security.identity")) {
        if (LOG.isDebugEnabled())
          LOG.debug("Use Identity from previous LoginModule");
        identity = (Identity) sharedState.get("exo.security.identity");
      } else {
        Callback[] callbacks = new Callback[10];
        callbacks[0] = new NameCallback("Username");
        callbacks[1] = new PasswordCallback("Password", false);
        callbacks[2] = new TextInputCallback("nonce");
        callbacks[3] = new TextInputCallback("nc");
        callbacks[4] = new TextInputCallback("cnonce");
        callbacks[5] = new TextInputCallback("qop");
        callbacks[6] = new TextInputCallback("realmName");
        callbacks[7] = new TextInputCallback("digestA2");
        callbacks[8] = new TextInputCallback("algorithm");
        callbacks[9] = new TextInputCallback("authMethod");

        // Interact with the user to retrieve the username and password
        String password;
        String nonce;
        String nc;
        String cnonce;
        String qop;
        String realmName;
        String digestA2;
        String algorithm;

        callbackHandler.handle(callbacks);
        username = ((NameCallback) callbacks[0]).getName();
        char[] passwordArray = ((PasswordCallback) callbacks[1]).getPassword();
        password = (passwordArray == null) ? null : new String(passwordArray);
        nonce = ((TextInputCallback) callbacks[2]).getText();
        nc = ((TextInputCallback) callbacks[3]).getText();
        cnonce = ((TextInputCallback) callbacks[4]).getText();
        qop = ((TextInputCallback) callbacks[5]).getText();
        realmName = ((TextInputCallback) callbacks[6]).getText();
        digestA2 = ((TextInputCallback) callbacks[7]).getText();
        algorithm = ((TextInputCallback) callbacks[8]).getText();

        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
          return false;
        }

        username = getDigestAuthenticatorService().validateUser(username, password, nonce, nc, cnonce, qop, realmName, digestA2, algorithm);
        identity = getDigestAuthenticatorService().createIdentity(username);
        sharedState.put("javax.security.auth.login.name", username);
        subject.getPublicCredentials().add(new UsernameCredential(username));
      }
      return username != null;
    } catch (final Exception e) {
      LOG.warn("Login error for user {}", username, e);
      return false;
    }
  }

  @Override
  public boolean commit() throws LoginException {
    try {
      getIdentityRegistry().register(identity);
      Set<Principal> principals = subject.getPrincipals();
      for (String role : identity.getRoles()) {
        principals.add(new RolePrincipal(role));
      }
      principals.add(new UserPrincipal(identity.getUserId()));
    } catch (final Exception e) {
      LOG.error(e.getLocalizedMessage());
      throw new LoginException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean abort() throws LoginException {
    return true;
  }

  @Override
  public boolean logout() throws LoginException {
    return true;
  }

  @Override
  protected Log getLogger() {
    return LOG;
  }

  @SneakyThrows
  private DigestAuthenticatorService getDigestAuthenticatorService() {
    if (digestAuthenticatorService == null) {
      digestAuthenticatorService = getContainer().getComponentInstanceOfType(DigestAuthenticatorService.class); // NOSONAR
    }
    return digestAuthenticatorService;
  }

  @SneakyThrows
  private IdentityRegistry getIdentityRegistry() {
    if (identityRegistry == null) {
      identityRegistry = getContainer().getComponentInstanceOfType(IdentityRegistry.class); // NOSONAR
    }
    return identityRegistry;
  }

}
