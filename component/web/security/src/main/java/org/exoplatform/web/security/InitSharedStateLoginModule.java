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
package org.exoplatform.web.security;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.jaas.AbstractLoginModule;

/**
 * Login module is used to add credentials from JAAS callback into shared state. It's intended to be used in JAAS chain before
 * login modules, which depend on credentials in shared state (like
 * {@link org.exoplatform.services.security.jaas.SharedStateLoginModule}
 *
 */
public class InitSharedStateLoginModule extends AbstractLoginModule {
    /** Logger. */
    private static final Log log = ExoLogger.getLogger(InitSharedStateLoginModule.class);

    @Override
    protected Log getLogger() {
        return log;
    }

    /**
     * Adding credentials from JAAS callback to shared state
     *
     * @see javax.security.auth.spi.LoginModule#login()
     */
    public boolean login() throws LoginException {
        try {
            // Obtain username and password from callbacks
            Callback[] callbacks = new Callback[2];
            callbacks[0] = new NameCallback("Username");
            callbacks[1] = new PasswordCallback("Password", false);

            callbackHandler.handle(callbacks);
            String username = ((NameCallback) callbacks[0]).getName();
            String password = new String(((PasswordCallback) callbacks[1]).getPassword());
            ((PasswordCallback) callbacks[1]).clearPassword();
            if (username == null || password == null) {
                log.warn("Username or password not found in callbacks");
                return false;
            }

            // Add username and password to shared state
            sharedState.put("javax.security.auth.login.name", username);
            sharedState.put("javax.security.auth.login.password", password);

            return true;
        } catch (final Exception e) {
            LoginException le = new LoginException();
            le.initCause(e);
            throw le;
        }
    }

    /**
     * @see javax.security.auth.spi.LoginModule#commit()
     */
    public boolean commit() throws LoginException {
        return true;
    }

    /**
     * @see javax.security.auth.spi.LoginModule#abort()
     */
    public boolean abort() throws LoginException {
        return true;
    }

    /**
     * @see javax.security.auth.spi.LoginModule#logout()
     */
    public boolean logout() throws LoginException {
        return true;
    }
}
