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

import org.apache.catalina.LifecycleException;
import org.apache.catalina.authenticator.DigestAuthenticator;

public class DigestAuthenticatorValve extends DigestAuthenticator {

  private static final String ALGORITHM  = AuthDigest.MD5.getRfcName();

  private static final String DIGEST_KEY = "DigestAuthenticator";

  @Override
  protected void startInternal() throws LifecycleException {
    setKey(DIGEST_KEY); // Use a fixed Key in order to allow preserving session
                        // on Client Side between server restarts
    setValidateUri(false);
    setChangeSessionIdOnAuthentication(false);
    setAlgorithms(ALGORITHM); // can't use SHA-256, not known
                              // by MS Word nor Windows OS
    super.startInternal();
  }

}
