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
package org.exoplatform.services.resources.impl;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.IdentityResourceBundle;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.Orientation;

public class LocaleConfigServiceImpl implements LocaleConfigService {

  public static final String                    LOCALE_DEFAULT_MODIFIED_EVENT    = "locale.config.default.modified";

  private static final String                   DEFAULT_LOCALE_CONFIG_PARAM      = "locale.config.default";

  private static final Context                  GLOBAL_SETTINGS_CONTEXT          = Context.GLOBAL.id("GLOBAL_SETTINGS");

  private static final Scope                    GLOBAL_SETTINGS_SCOPE            = Scope.APPLICATION.id("GLOBAL_SETTINGS");

  private static final String                   GLOBAL_SETTINGS_DEFAULT_LANGUAGE = "DEFAULT_LANGUAGE";

  private static final Log                      LOG                              =
                                                    ExoLogger.getLogger(LocaleConfigServiceImpl.class);

  private SettingService                        settingService;

  private ListenerService                       listenerService;

  private DocumentBuilderFactory                factory;

  private String                                defaultLocale;

  private LocaleConfig                          firstConfig;

  private LocaleConfig                          defaultConfig;

  private Map<String, LocaleConfig>             configs;

  private static final Map<String, Orientation> orientations                     = new HashMap<>();

  static {
    orientations.put("lt", Orientation.LT);
    orientations.put("rt", Orientation.RT);
    orientations.put("tl", Orientation.TL);
    orientations.put("tr", Orientation.TR);
  }

  public LocaleConfigServiceImpl(SettingService settingService,
                                 ListenerService listenerService,
                                 ConfigurationManager cmanager,
                                 InitParams params)
      throws Exception {
    this.settingService = settingService;
    this.listenerService = listenerService;

    this.configs = new HashMap<>(10);
    this.factory = DocumentBuilderFactory.newInstance();
    if (params.containsKey(DEFAULT_LOCALE_CONFIG_PARAM)) {
      this.defaultLocale = params.getValueParam(DEFAULT_LOCALE_CONFIG_PARAM).getValue();
    }
    String confResource = params.getValueParam("locale.config.file").getValue();
    InputStream is = cmanager.getInputStream(confResource);
    parseConfiguration(is);
  }

  @Override
  public LocaleConfig getDefaultLocaleConfig() {
    if (defaultConfig == null) {
      computeDefaultConfig();
    }
    return defaultConfig;
  }

  @Override
  public void saveDefaultLocaleConfig(String locale) {
    String oldDefaultLocale = getDefaultLocaleConfig().getLocaleName();

    try {
      if (StringUtils.isBlank(locale)) {
        settingService.remove(GLOBAL_SETTINGS_CONTEXT,
                              GLOBAL_SETTINGS_SCOPE,
                              GLOBAL_SETTINGS_DEFAULT_LANGUAGE);
      } else {
        if (!configs.containsKey(locale)) {
          throw new IllegalArgumentException(String.format("Locale %s is not supported", locale));
        }
        settingService.set(GLOBAL_SETTINGS_CONTEXT,
                           GLOBAL_SETTINGS_SCOPE,
                           GLOBAL_SETTINGS_DEFAULT_LANGUAGE,
                           SettingValue.create(locale));
      }
    } finally {
      this.defaultConfig = null;
    }

    String newDefaultLocale = getDefaultLocaleConfig().getLocaleName();
    try {
      this.listenerService.broadcast(LOCALE_DEFAULT_MODIFIED_EVENT, oldDefaultLocale, newDefaultLocale);
    } catch (Exception e) {
      LOG.warn("Error broadcasting locale change from {} to {}", oldDefaultLocale, newDefaultLocale, e);
    }
  }

  @Override
  public LocaleConfig getLocaleConfig(String lang) {
    return configs.getOrDefault(lang, getDefaultLocaleConfig());
  }

  @Override
  public Collection<LocaleConfig> getLocalConfigs() {
    return configs.values();
  }

  protected void parseConfiguration(InputStream is) throws Exception { // NOSONAR
    factory.setIgnoringComments(true);
    factory.setCoalescing(true);
    factory.setNamespaceAware(false);
    factory.setValidating(false);
    DocumentBuilder parser = factory.newDocumentBuilder();
    Document document = parser.parse(is);
    NodeList nodes = document.getElementsByTagName("locale-config");
    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      NodeList children = node.getChildNodes();
      LocaleConfig config = new LocaleConfigImpl();
      for (int j = 0; j < children.getLength(); j++) {
        Node element = children.item(j);
        if ("locale".equals(element.getNodeName())) {
          config.setLocale(element.getFirstChild().getNodeValue());
        } else if ("output-encoding".equals(element.getNodeName())) {
          config.setOutputEncoding(element.getFirstChild().getNodeValue());
        } else if ("input-encoding".equals(element.getNodeName())) {
          config.setInputEncoding(element.getFirstChild().getNodeValue());
        } else if ("description".equals(element.getNodeName())) {
          config.setDescription(element.getFirstChild().getNodeValue());
        } else if ("orientation".equals(element.getNodeName())) {
          String s = element.getFirstChild().getNodeValue();
          Orientation orientation = orientations.get(s);
          if (orientation == null) {
            LOG.error("Wrong orientation value " + s);
          } else {
            config.setOrientation(orientation);
          }
        }
      }

      //
      if (config.getOrientation() == null) {
        LOG.debug("No orientation found on the locale config, use the LT default");
        config.setOrientation(Orientation.LT);
      }

      //
      LOG.debug("Added locale config " + config + " to the set of locale configs");

      //
      String country = config.getLocale().getCountry();
      if (StringUtils.isNotBlank(country)) {
        this.configs.put(config.getLanguage() + "-" + country, config);
      } else {
        this.configs.put(config.getLanguage(), config);
      }
      if (i == 0) {
        this.firstConfig = config;
      }
    }

    //
    if (PropertyManager.isDevelopping()) {
      LocaleConfig magicConfig = new LocaleConfigImpl();
      magicConfig.setLocale(IdentityResourceBundle.MAGIC_LOCALE);
      magicConfig.setDescription("Magic locale");
      magicConfig.setInputEncoding("UTF-8");
      magicConfig.setOutputEncoding("UTF-8");
      magicConfig.setDescription("Default configuration for the debugging locale");
      magicConfig.setOrientation(Orientation.LT);
      this.configs.put(magicConfig.getLanguage(), magicConfig);
      LOG.debug("Added magic locale for debugging bundle usage at runtime");
    }
  }

  private void computeDefaultConfig() {

    // Replace default locale from stored locale
    SettingValue<?> defaultLanguageValue = settingService.get(GLOBAL_SETTINGS_CONTEXT,
                                                              GLOBAL_SETTINGS_SCOPE,
                                                              GLOBAL_SETTINGS_DEFAULT_LANGUAGE);
    String localeName;
    if (defaultLanguageValue != null && StringUtils.isNotBlank(defaultLanguageValue.getValue().toString())) {
      localeName = defaultLanguageValue.getValue().toString();
    } else {
      localeName = this.defaultLocale;
    }

    // If default locale set by properties or stored in SettingService, use it
    // as default
    if (StringUtils.isNotBlank(localeName)) {
      this.defaultConfig = configs.entrySet()
                                  .stream()
                                  .filter(configEntry -> StringUtils.equals(configEntry.getKey(), localeName))
                                  .map(Entry::getValue)
                                  .findFirst()
                                  .orElse(null);
    }

    // If not set by properties, neither stored in SettingService, use the first
    // configured locale
    if (this.defaultConfig == null) {
      this.defaultConfig = firstConfig;
    }
  }

}
