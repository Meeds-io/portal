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
package org.exoplatform.services.resources;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.configuration.ConfigurationManagerImpl;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.resources.impl.LocaleConfigImpl;
import org.exoplatform.services.resources.impl.LocaleConfigServiceImpl;

public class TestLocaleConfigService extends AbstractResourceBundleTest { // NOSONAR

  private ListenerService listenerService;

  public void testParseLocaleConfigFile() throws Exception {
    PropertyManager.setProperty(PropertyManager.DEVELOPING, "false");
    LocaleConfigService service = createService();
    Map<String, LocaleConfig> map = createMap(service);
    assertEquals(4, map.size());
    assertCommonConfigs(service);
  }

  public void testDevMode() throws Exception {
    PropertyManager.setProperty(PropertyManager.DEVELOPING, "true");
    LocaleConfigService service = createService();
    Map<String, LocaleConfig> map = createMap(service);
    assertEquals(5, map.size());
    assertCommonConfigs(service);
    LocaleConfig ma = service.getLocaleConfig("ma");
    assertLocaleConfig(ma,
                       "ma",
                       "Default configuration for the debugging locale",
                       "UTF-8",
                       "UTF-8",
                       Orientation.LT,
                       IdentityResourceBundle.MAGIC_LOCALE);
  }

  public void testDefaultConfiguredLanguage() throws Exception {
    LocaleConfigService service = createService("fr");
    LocaleConfig defaultLocaleConfig = service.getDefaultLocaleConfig();
    assertNotNull(defaultLocaleConfig);
    assertEquals("fr", defaultLocaleConfig.getLocaleName());
  }

  public void testDefaultStoredLanguage() throws Exception {
    LocaleConfigService service = createService("fr");
    service.saveDefaultLocaleConfig("en");

    verify(listenerService, times(1)).broadcast(LocaleConfigServiceImpl.LOCALE_DEFAULT_MODIFIED_EVENT, "fr", "en");

    LocaleConfig defaultLocaleConfig = service.getDefaultLocaleConfig();
    assertNotNull(defaultLocaleConfig);
    assertEquals("en", defaultLocaleConfig.getLocaleName());
  }

  public void testRemoveDefaultStoredLanguage() throws Exception {
    LocaleConfigService service = createService("fr");
    service.saveDefaultLocaleConfig("en");
    verify(listenerService, times(1)).broadcast(LocaleConfigServiceImpl.LOCALE_DEFAULT_MODIFIED_EVENT, "fr", "en");
    service.saveDefaultLocaleConfig(null);
    verify(listenerService, times(1)).broadcast(LocaleConfigServiceImpl.LOCALE_DEFAULT_MODIFIED_EVENT, "en", "fr");

    LocaleConfig defaultLocaleConfig = service.getDefaultLocaleConfig();
    assertNotNull(defaultLocaleConfig);
    assertEquals("fr", defaultLocaleConfig.getLocaleName());
  }

  public void testLocaleConfig() {
    LocaleConfig ma = new LocaleConfigImpl();
    ma.setLocale("ma");
    assertEquals("ma", ma.getLocaleName());

    LocaleConfig ma1 = new LocaleConfigImpl();
    ma1.setLocale(new Locale("ma"));
    assertEquals("ma", ma1.getLocaleName());
  }

  private Map<String, LocaleConfig> createMap(LocaleConfigService service) {
    Map<String, LocaleConfig> map = new HashMap<>();
    for (LocaleConfig config : service.getLocalConfigs()) {
      map.put(config.getLanguage(), config);
    }
    return map;
  }

  private LocaleConfigService createService() throws Exception {
    return createService(null);
  }

  private LocaleConfigService createService(String defaultLang) throws Exception {
    ConfigurationManagerImpl cm = new ConfigurationManagerImpl();
    InitParams params = new InitParams();
    ValueParam param = new ValueParam();
    param.setName("locale.config.file");
    param.setValue("classpath:/resources/locales-config.xml");
    params.addParameter(param);

    if (StringUtils.isNotBlank(defaultLang)) {
      param = new ValueParam();
      param.setName("locale.config.default");
      param.setValue(defaultLang);
      params.addParameter(param);
    }

    SettingService settingService = mock(SettingService.class);
    this.listenerService = mock(ListenerService.class);
    doAnswer(invocation -> {
      when(settingService.get(invocation.getArgument(0),
                              invocation.getArgument(1),
                              invocation.getArgument(2))).thenReturn(invocation.getArgument(3));
      return null;
    }).when(settingService)
      .set(any(),
           any(),
           any(),
           any());
    doAnswer(invocation -> {
      reset(settingService);
      return null;
    }).when(settingService)
      .remove(any(),
              any(),
              any());

    return new LocaleConfigServiceImpl(settingService, listenerService, cm, params);
  }

  private void assertCommonConfigs(LocaleConfigService service) {
    LocaleConfig en = service.getLocaleConfig("en");
    LocaleConfig fr = service.getLocaleConfig("fr");
    LocaleConfig ar = service.getLocaleConfig("ar");
    LocaleConfig vi = service.getLocaleConfig("vi");
    assertLocaleConfig(en,
                       "en",
                       "Default configuration for english locale",
                       "UTF-8",
                       "UTF-8",
                       Orientation.LT,
                       Locale.ENGLISH);
    assertLocaleConfig(fr,
                       "fr",
                       "Default configuration for the french locale",
                       "UTF-8",
                       "UTF-8",
                       Orientation.LT,
                       Locale.FRENCH);
    assertLocaleConfig(ar,
                       "ar",
                       "Default configuration for the arabic locale",
                       "UTF-8",
                       "UTF-8",
                       Orientation.RT,
                       new Locale("ar"));
    assertLocaleConfig(vi,
                       "vi",
                       "Default configuration for the vietnam locale",
                       "UTF-8",
                       "UTF-8",
                       Orientation.LT,
                       new Locale("vi"));
  }

  private void assertLocaleConfig(LocaleConfig config, String language, String description, String inputEncoding,
                                  String outputEncoding, Orientation orientation, Locale locale) {
    assertNotNull(config);
    assertEquals(language, config.getLanguage());
    assertEquals(description, config.getDescription());
    assertEquals(inputEncoding, config.getInputEncoding());
    assertEquals(outputEncoding, config.getOutputEncoding());
    assertEquals(orientation, config.getOrientation());
    assertEquals(locale, config.getLocale());
  }
}
