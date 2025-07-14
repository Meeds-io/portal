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
package org.gatein.portal.encoder;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.management.annotations.Impact;
import org.exoplatform.management.annotations.ImpactType;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.annotations.ManagedName;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.management.rest.annotations.RESTEndpoint;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.picocontainer.Startable;

/**
 * Helper JMX component for encoding/decoding plain text into masked string. It's useful for password masking
 *
 */
@Managed
@ManagedDescription("Encoder Service")
@NameTemplate({ @Property(key = "name", value = "encoderService"), @Property(key = "service", value = "EncoderService") })
@RESTEndpoint(path = "encoderService")
public class EncoderService implements Startable {

    // Same value like SimpleLDAPIdentityStoreConfiguration.ENCODING_KEY_STORE_PASSWORD_DEFAULT
    private static final String ENCODING_KEY_STORE_PASSWORD_DEFAULT =  "somearbitrarycrazystringthatdoesnotmatter";

    private static final Log  log                                 = ExoLogger.getLogger(EncoderService.class);

    private final char[] keyStorePassword;
    private final byte[] salt;
    private final int iterationCount;

    /** The secret key that corresponds to the keystore password */
    private SecretKey cipherKey;

    /** The encode/decode cipher algorithm */
    private final String cipherAlgorithm;

    /** Cipher specification, which specifies info about salt and iterationsCount **/
    private PBEParameterSpec cipherSpec;

    public EncoderService(InitParams params) throws UnsupportedEncodingException {
        ValueParam keyStorePasswordParam = params.getValueParam("keyStorePassword");
        String keyStorePassword = keyStorePasswordParam != null ? keyStorePasswordParam.getValue() : ENCODING_KEY_STORE_PASSWORD_DEFAULT;
        this.keyStorePassword = keyStorePassword.toCharArray();
        this.cipherAlgorithm = getParam(params, "cipherAlgorithm");
        String saltParam = getParam(params, "salt");
        String iterationCountParam = getParam(params, "iterationCount");

        if (saltParam.length() < 8) {
            throw new IllegalArgumentException("Salt param needs to have length at least 8. Current value is " + saltParam);
        }
        this.salt = saltParam.substring(0, 8).getBytes("UTF-8");

        this.iterationCount = Integer.parseInt(iterationCountParam);
    }

    @Override
    public void start() {
        try {
            this.cipherSpec = new PBEParameterSpec(salt, iterationCount);
            PBEKeySpec keySpec = new PBEKeySpec(keyStorePassword);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(cipherAlgorithm);
            this.cipherKey = factory.generateSecret(keySpec);
        } catch (Exception e) {
            log.error("Error starting EncoderService", e);
        }
    }

    @Override
    public void stop() {
        if (keyStorePassword != null) {
            Arrays.fill(keyStorePassword, '\0');
        }
        cipherKey = null;
    }

    @Managed
    @ManagedDescription("Encode a secret as a base64 string using the cipher algorithm and the KeyStore password")
    @Impact(ImpactType.READ)
    public String encode64(@ManagedDescription("secret") @ManagedName("The secret in plain-text to be encoded") String secret) throws Exception {
        byte[] secretBytes = secret.getBytes("UTF-8");
        return PBEUtils.encode64(secretBytes, cipherAlgorithm, cipherKey, cipherSpec);
    }

    @Managed
    @ManagedDescription("Decode a base64 secret using the cipher algorithm and the KeyStore password")
    @Impact(ImpactType.READ)
    public String decode64(@ManagedDescription("secret") @ManagedName("The masked secret to be decoded") String secret) throws Exception {
        return PBEUtils.decode64(secret, cipherAlgorithm, cipherKey, cipherSpec);
    }

    private String getParam(InitParams params, String paramName) {
        ValueParam param = params.getValueParam(paramName);
        if (param == null) {
            throw new IllegalArgumentException("Parameter '" + paramName + "' needs to be provided");
        }

        return param.getValue();
    }

}
