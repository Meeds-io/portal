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
package org.gatein.portal.idm.impl.store.attribute;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;
import org.picketlink.idm.api.Attribute;
import org.picketlink.idm.api.CredentialEncoder;
import org.picketlink.idm.api.User;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.impl.api.PasswordCredential;
import org.picketlink.idm.impl.api.session.IdentitySessionImpl;
import org.picketlink.idm.impl.api.session.managers.AttributesManagerImpl;

import java.util.HashMap;
import java.util.Map;

public class ExtendedAttributeManager extends AttributesManagerImpl {

  private static final Log            LOG                              = ExoLogger.getExoLogger(ExtendedAttributeManager.class);

  public static final String          OLD_PASSWORD_SALT_USER_ATTRIBUTE = "passwordSalt";

  public static final String          PASSWORD_SALT_USER_ATTRIBUTE     = "passwordSalt128";

  private static final String         DEFAULT_ENCODER                  =
                                                      "org.exoplatform.web.security.hash.Argon2IdPasswordEncoder";

  private static final String         OLD_ENCODER_CLASS_PROPERTY       = "oldCredentialEncoder.class";

  private static PicketLinkIDMService idmService;

  public ExtendedAttributeManager(IdentitySessionImpl session) {
    super(session);
  }

  @Override
  public boolean validatePassword(User user, String password) throws IdentityException {
    Attribute salt = getAttribute(user.getKey(), PASSWORD_SALT_USER_ATTRIBUTE);

    if (!getCredentialEncoder().getClass().getName().equals(DEFAULT_ENCODER)) {
      return super.validatePassword(user, password);
    }

    if (salt != null) {
      // Case of hash updated during authentication phase
      // Case of password has been updated by upgradePlugin, and user was not
      // connected since
      // passwordHash was created like this passwordHash =
      // newEncoder(oldEncoder(password))
      if (super.validatePassword(user, password)) {
        return true;
      } else {
        return validateAndUpdateHash(getCredentialEncoder(),
                                     user,
                                     (String) new PasswordCredential(password,
                                                                     getOldCredentialEncoder(),
                                                                     user.getKey()).getEncodedValue(),
                                     password);
      }
    } else {
      // user didn't reconnect after new passwordEncoder modification,
      // but upgrade plugin was not executed for him, his passwordHash is still
      // oldEncoder(password)
      // includes case of old encoder (such as MD5) which doesn't use a salt
      return validateAndUpdateHash(getOldCredentialEncoder(), user, password, password);
    }
  }

  private boolean validateAndUpdateHash(CredentialEncoder credentialEncoder,
                                        User user,
                                        String password, String newPassword) throws IdentityException {
    if (getRepository().validateCredential(getInvocationContext(),
                                           createIdentityObject(user),
                                           new PasswordCredential(password, credentialEncoder, user.getKey()))) {
      removeAttributes(user.getKey(), new String[] { OLD_PASSWORD_SALT_USER_ATTRIBUTE });
      updatePassword(user, newPassword);
      return true;
    }
    return false;
  }

  private CredentialEncoder getOldCredentialEncoder() {
    try {
      Class<?> credentialClass = Class.forName(getCredentialEncoderProps().get(OLD_ENCODER_CLASS_PROPERTY));
      CredentialEncoder oldCredentialEncoder = (CredentialEncoder) credentialClass.getDeclaredConstructor().newInstance();
      oldCredentialEncoder.setIdentitySession(getIdentitySession());
      oldCredentialEncoder.initialize(getCredentialEncoderProps(),
                                      getIdmService().getIdentityConfiguration().getIdentityConfigurationRegistry());
      return oldCredentialEncoder;
    } catch (Exception e) {
      LOG.error("Error while initializing old credential encoder", e);
      throw new RuntimeException();
    }
  }

  private Map<String, String> getCredentialEncoderProps() {
    Map<String, String> props = new HashMap<>();
    getIdmService().getConfigMD().getRealms().get(0).getOptions().forEach((k, v) -> props.put(k, v.get(0)));
    return props;
  }

  private static PicketLinkIDMService getIdmService() {
    if (idmService == null) {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      if (container instanceof RootContainer rootContainer) {
        container = rootContainer.getPortalContainer(PortalContainer.DEFAULT_PORTAL_CONTAINER_NAME);
      }
      idmService = container.getComponentInstanceOfType(PicketLinkIDMService.class);
    }
    return idmService;
  }

  public CredentialEncoder getDefaultCredentialEncoder() {
    return getCredentialEncoder();
  }
}
