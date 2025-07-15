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
package org.exoplatform.web.security.hash;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.security.codec.AbstractCodec;
import org.exoplatform.web.security.codec.CodecInitializer;
import org.exoplatform.web.security.security.TokenServiceInitializationException;
import org.picketlink.idm.api.Attribute;
import org.picketlink.idm.api.AttributesManager;
import org.picketlink.idm.api.CredentialEncoder;
import org.picketlink.idm.api.SecureRandomProvider;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.impl.credential.HashingEncoder;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Argon2IdPasswordEncoder extends HashingEncoder {

  private static final Log        LOG                                               =
                                      ExoLogger.getLogger(Argon2IdPasswordEncoder.class);

  private static final String     PASSWORD_SALT_USER_ATTRIBUTE                      = "passwordSalt128";

  private static final int        ARGON2_HASHING_ITERATIONS                         = 2;

  private static final int        ARGON2_MEMORY_LIMIT                               = 32768;

  private static final int        ARGON2_PARALLEL_EXECUTIONS                        = 2;

  private static final int        ARGON2_HASH_LENGTH                                = 32;

  private static final String     OPTION_CREDENTIAL_ENCODER_SECURE_RANDOM_ALGORITHM =
                                                                                    CredentialEncoder.CREDENTIAL_ENCODER_OPTION_PREFIX
                                                                                        + "secureRandomAlgorithm";

  private static final String     OPTION_DEFAULT_SECURE_RANDOM_ALGORITHM            = "SHA1PRNG";

  public static final String      OPTION_SECURE_RANDOM_PROVIDER_REGISTRY_NAME       =
                                                                              CredentialEncoder.CREDENTIAL_ENCODER_OPTION_PREFIX
                                                                                  + "secureRandom.providerRegistryName";

  public static final String      DEFAULT_SECURE_RANDOM_PROVIDER_REGISTRY_NAME      = "secureRandomProvider";

  private SecureRandomProvider    registeredSecureRandomProvider;

  private String                  secureRandomAlgorithm;

  private static CodecInitializer codecInitializer;

  @Override
  public String encodeCredential(String username, String rawPassword) {
    Argon2Parameters.Builder builder =
                                     new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id).withIterations(ARGON2_HASHING_ITERATIONS)
                                                                                             .withMemoryAsKB(ARGON2_MEMORY_LIMIT)
                                                                                             .withParallelism(ARGON2_PARALLEL_EXECUTIONS)
                                                                                             .withSalt(getStoredSalt(username));
    Argon2BytesGenerator generator = new Argon2BytesGenerator();
    generator.init(builder.build());
    byte[] hash = new byte[ARGON2_HASH_LENGTH];
    generator.generateBytes(rawPassword.getBytes(StandardCharsets.UTF_8), hash, 0, hash.length);
    try {
      return getCodec().encode(Hex.encodeHexString(hash));
    } catch (TokenServiceInitializationException e) {
      LOG.error("Error while applying symmetrical encryption on password hash", e);
      throw new RuntimeException(e);
    }
  }

  private String getRegisteredProviderName() {
    String registeredName = getEncoderProperty(OPTION_SECURE_RANDOM_PROVIDER_REGISTRY_NAME);
    if (registeredName == null) {
      registeredName = DEFAULT_SECURE_RANDOM_PROVIDER_REGISTRY_NAME;
    }
    return registeredName;
  }

  @Override
  protected void afterInitialize() {
    super.afterInitialize();
    try {
      if (getConfigurationRegistry() != null) {
        registeredSecureRandomProvider = (SecureRandomProvider) getConfigurationRegistry().getObject(getRegisteredProviderName());
        LOG.info("Registered SecureRandomProvider will be used for random generating of password salts");
        return;
      }
    } catch (IdentityException ie) {
      LOG.info("SecureRandomProvider not registered. We will always create new SecureRandom");
    }
    secureRandomAlgorithm = getEncoderProperty(OPTION_CREDENTIAL_ENCODER_SECURE_RANDOM_ALGORITHM);
    if (secureRandomAlgorithm == null) {
      secureRandomAlgorithm = OPTION_DEFAULT_SECURE_RANDOM_ALGORITHM;
    }
    LOG.info("Algorithm {} will be used for random generating of password salts", secureRandomAlgorithm);
  }

  private byte[] getStoredSalt(String username) {
    try {
      AttributesManager attributesManager = getIdentitySession().getAttributesManager();
      Attribute salt = attributesManager.getAttribute(username, PASSWORD_SALT_USER_ATTRIBUTE);
      if (salt == null) {
        byte[] generatedSalt = generateRandomSalt();
        String saltString = Hex.encodeHexString(generatedSalt);
        attributesManager.addAttribute(username, PASSWORD_SALT_USER_ATTRIBUTE, saltString);
        return generatedSalt;
      } else {
        return Hex.decodeHex(((String) salt.getValue()).toCharArray());
      }
    } catch (Exception e) {
      LOG.error("Error while getting stored password hash salt", e);
      throw new RuntimeException(e);
    }
  }

  public byte[] generateRandomSalt() throws NoSuchAlgorithmException {
    SecureRandom secureRandom = getSecureRandomInstance();
    byte[] salt = new byte[16];
    secureRandom.nextBytes(salt);
    return salt;
  }

  private static AbstractCodec getCodec() throws TokenServiceInitializationException {
    if (codecInitializer == null) {
      PortalContainer container = PortalContainer.getInstance();
      codecInitializer = container.getComponentInstanceOfType(CodecInitializer.class);
      return codecInitializer.getCodec();
    }
    return codecInitializer.getCodec();
  }

  private SecureRandom getSecureRandomInstance() throws NoSuchAlgorithmException {
    if (registeredSecureRandomProvider == null) {
      return SecureRandom.getInstance(secureRandomAlgorithm);
    }
    return registeredSecureRandomProvider.getSecureRandom();
  }
}
