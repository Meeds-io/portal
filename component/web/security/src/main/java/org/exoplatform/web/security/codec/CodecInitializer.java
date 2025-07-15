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
package org.exoplatform.web.security.codec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.security.security.TokenServiceInitializationException;

import lombok.Getter;

/**
 * Initialize codec from configuration.properties. Use default values if
 * properties not provided
 *
 */
public class CodecInitializer implements Startable {

  public static final String   CODECKEY_RELATIVE_PATH_PARAM = "codeckey.relative.path";

  public static final String   CONF_DIR_PARAM               = "gatein.conf.dir";

  private static final Log     LOG                          = ExoLogger.getLogger(CodecInitializer.class);

  private static final Context CONTEXT                      = Context.GLOBAL;

  private static final Scope   SCOPE                        = Scope.GLOBAL.id("CodecInitializer");

  private static final String  INIT_KEY                     = "CodecKeyInitialized";

  @Getter
  private String               confDir;

  @Getter
  private String               codecFileRelativePath;

  private SettingService       settingService;

  private AbstractCodec        codec;

  public CodecInitializer(SettingService settingService, InitParams initParams) {
    this(initParams);
    this.settingService = settingService;
  }

  public CodecInitializer(InitParams initParams) {
    this.confDir = getParamValue(initParams, CONF_DIR_PARAM, null);
    this.codecFileRelativePath = getParamValue(initParams, CODECKEY_RELATIVE_PATH_PARAM, "/codec/codeckey.txt");
  }

  @Override
  public void start() {
    if (settingService == null) {
      return;
    }
    try {
      String encodedInitKey = getCodec().encode(INIT_KEY);
      SettingValue<?> settingValue = settingService.get(CONTEXT, SCOPE, INIT_KEY);
      if (settingValue == null || settingValue.getValue() == null) {
        settingService.set(CONTEXT, SCOPE, INIT_KEY, SettingValue.create(encodedInitKey));
      } else {
        String savedEncodedInitKey = settingValue.getValue().toString();
        if (!StringUtils.equals(savedEncodedInitKey, encodedInitKey)) {
          throw new CodecKeyChangedException("Symetric-Key " + codecFileRelativePath
              + " used to encrypt/decrypt sensitive data seems to be changed. This may lead to data inconsistency.");
        }
      }
    } catch (RuntimeException e) {
      // Must rethrow the exception to stop server startup
      // to avoid data inconsistency
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Error while checking init key content", e);
    }
  }

  @Override
  public void stop() {
    // Nothing to do
  }

  /**
   * @return                                     shared instance of codec. If
   *                                             shared instance is not
   *                                             available, then new instance
   *                                             will be initialized
   * @throws TokenServiceInitializationException if some error happen during
   *                                               codec initialization
   */
  public AbstractCodec getCodec() throws TokenServiceInitializationException {
    AbstractCodec helper = codec;
    if (helper == null) {
      synchronized (this) {
        helper = codec;
        if (helper == null) {
          this.codec = helper = initCodec();
        }
      }
    }
    return helper;
  }

  /**
   * Codec initialization. Method is protected, so it could be overriden if
   * needed
   * 
   * @return                                     Initialized codec
   * @throws TokenServiceInitializationException if some error happen during
   *                                               initialization
   */
  protected AbstractCodec initCodec() throws TokenServiceInitializationException { // NOSONAR
    String builderType = PropertyManager.getProperty("gatein.codec.builderclass");
    Map<String, String> config = new HashMap<>();

    if (builderType != null) {
      // If there is config for codec in configuration.properties, we read the
      // config parameters from config file
      // referenced in configuration.properties
      String configFile = PropertyManager.getProperty("gatein.codec.config");
      File f = new File(configFile);
      try (InputStream in = new FileInputStream(f)) {
        Properties properties = new Properties();
        properties.load(in);
        for (Map.Entry<?, ?> entry : properties.entrySet()) {
          config.put((String) entry.getKey(), (String) entry.getValue());
        }
        config.put("gatein.codec.config.basedir", f.getParentFile().getAbsolutePath());
      } catch (IOException e) {
        throw new TokenServiceInitializationException("Failed to read the config parameters from file '" + configFile
            + "'.", e);
      }
    } else {
      // If there is no config for codec in configuration.properties, we
      // generate key if it does not exist and setup the
      // default config
      builderType = "org.exoplatform.web.security.codec.JCASymmetricCodecBuilder";

      String gtnConfDir = null;
      if (confDir != null) {
        ConfigurationManager confManager = RootContainer.getInstance().getComponentInstanceOfType(ConfigurationManager.class);
        try {
          gtnConfDir = confManager.getResource(confDir).getPath();
        } catch (Exception ex) {
          LOG.error("Failed to process the path to gateinConfDir", ex);
        }
      }

      if (gtnConfDir == null) {
        gtnConfDir = PropertyManager.getProperty(CONF_DIR_PARAM);
        if (gtnConfDir == null || gtnConfDir.length() == 0) {
          throw new TokenServiceInitializationException("'gatein.conf.dir' property must be set.");
        }
      }

      try {
        gtnConfDir = URLDecoder.decode(gtnConfDir, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        LOG.error("fail to decode the path " + gtnConfDir, e);
      }
      File f = new File(gtnConfDir + codecFileRelativePath);
      if (!f.exists()) {
        File codecDir = f.getParentFile();
        if (!codecDir.exists()) {
          codecDir.mkdir();
        }
        try (OutputStream out = new FileOutputStream(f)) {
          KeyGenerator keyGen = KeyGenerator.getInstance("AES");
          keyGen.init(128);
          SecretKey key = keyGen.generateKey();
          KeyStore store = KeyStore.getInstance("JCEKS");
          store.load(null, "gtnStorePass".toCharArray());
          store.setEntry("gtnKey",
                         new KeyStore.SecretKeyEntry(key),
                         new KeyStore.PasswordProtection("gtnKeyPass".toCharArray()));
          store.store(out, "gtnStorePass".toCharArray());
        } catch (Exception e) {
          throw new TokenServiceInitializationException(e);
        }
      }
      config.put("gatein.codec.jca.symmetric.keyalg", "AES");
      config.put("gatein.codec.jca.symmetric.keystore", f.getName());
      config.put("gatein.codec.jca.symmetric.storetype", "JCEKS");
      config.put("gatein.codec.jca.symmetric.alias", "gtnKey");
      config.put("gatein.codec.jca.symmetric.keypass", "gtnKeyPass");
      config.put("gatein.codec.jca.symmetric.storepass", "gtnStorePass");
      config.put("gatein.codec.config.basedir", f.getParentFile().getAbsolutePath());
    }

    try {
      LOG.info("Initialized codec using builder " + builderType);
      return Class.forName(builderType).asSubclass(AbstractCodecBuilder.class).newInstance().build(config);
    } catch (Exception e) {
      throw new TokenServiceInitializationException("Could not initialize codec.", e);
    }
  }

  private String getParamValue(InitParams initParams, String param, String defaultValue) {
    ValueParam valueParam = initParams.getValueParam(param);
    String value = valueParam != null ? valueParam.getValue() : null;
    return StringUtils.isNotBlank(value) ? value : defaultValue;
  }

}
