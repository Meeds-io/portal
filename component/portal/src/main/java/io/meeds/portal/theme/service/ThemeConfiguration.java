/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.portal.theme.service;

import static io.meeds.portal.theme.constant.ThemeVariables.PAGE_VARIABLES;
import static io.meeds.portal.theme.constant.ThemeVariables.isShared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.portal.theme.constant.ModesOffered;
import io.meeds.portal.theme.model.ConfiguredTheme;
import io.meeds.portal.theme.model.ThemeSettings;

/**
 * The themes a technical administrator configures in exo.properties
 * (boot-time): exo.branding.themes lists their keys, and
 * exo.branding.themes.&lt;key&gt;.name, .&lt;variable&gt; and
 * .dark.&lt;variable&gt; set them; exo.branding.theme.dark.&lt;variable&gt;
 * sets the Default Theme's dark values. Every value goes through the same
 * validator as an administrator's: a refused value is logged and ignored.
 */
@Component
public class ThemeConfiguration {

  public static final String THEMES_PROPERTY            = "exo.branding.themes";

  public static final String DEFAULT_THEME_DARK_PREFIX  = "exo.branding.theme.dark.";

  public static final int    CONFIG_KEY_MAX_LENGTH      = 100;

  public static final String THEME_IN_USE_PROPERTY      = "exo.branding.themeInUse";

  public static final String MODES_OFFERED_PROPERTY     = "exo.branding.modesOffered";

  private static final Log   LOG                        = ExoLogger.getLogger(ThemeConfiguration.class);

  @Autowired
  private Environment        environment;

  @Autowired
  private BrandingService    brandingService;

  private ThemeValueValidator valueValidator;

  /**
   * @return the configured themes other than the Default Theme, in their
   *         configured order, their values validated
   */
  public List<ConfiguredTheme> getConfiguredThemes() {
    List<ConfiguredTheme> themes = new ArrayList<>();
    for (String key : getConfiguredKeys()) {
      String prefix = THEMES_PROPERTY + "." + key + ".";
      String name = StringUtils.defaultIfBlank(StringUtils.trim(environment.getProperty(prefix + "name")), key);
      if (key.length() > CONFIG_KEY_MAX_LENGTH || name.length() > ThemeService.NAME_MAX_LENGTH) {
        LOG.error("Configured theme '{}' is ignored: its key is limited to {} characters and its name ({}name) to {}",
                  StringUtils.abbreviate(key, 120),
                  CONFIG_KEY_MAX_LENGTH,
                  prefix,
                  ThemeService.NAME_MAX_LENGTH);
        continue;
      }
      ThemeSettings settings = new ThemeSettings();
      for (String variable : getVariables()) {
        put(settings, variable, environment.getProperty(prefix + variable), false, prefix + variable);
        put(settings, variable, environment.getProperty(prefix + "dark." + variable), true, prefix + "dark." + variable);
      }
      themes.add(new ConfiguredTheme(key, name, settings));
    }
    return themes;
  }

  /**
   * @return the Default Theme's configured dark values (its light values are
   *         the existing exo.branding.theme.&lt;variable&gt; defaults)
   */
  public Map<String, String> getDefaultThemeDarkValues() {
    ThemeSettings settings = new ThemeSettings();
    for (String variable : getVariables()) {
      put(settings, variable, environment.getProperty(DEFAULT_THEME_DARK_PREFIX + variable), true, DEFAULT_THEME_DARK_PREFIX + variable);
    }
    return settings.getDark();
  }

  /**
   * @return exo.branding.themeInUse: the key of the theme in use while no
   *         administrator selected one, "default" for the Default Theme
   */
  public String getConfiguredThemeInUse() {
    return StringUtils.defaultIfBlank(StringUtils.trim(environment.getProperty(THEME_IN_USE_PROPERTY)),
                                      ThemeService.DEFAULT_THEME_CONFIG_KEY);
  }

  /**
   * @return exo.branding.modesOffered, both modes when it is absent or not a
   *         reserved value
   */
  public ModesOffered getConfiguredModesOffered() {
    String value = StringUtils.trimToNull(environment.getProperty(MODES_OFFERED_PROPERTY));
    if (value == null) {
      return ModesOffered.LIGHT_AND_DARK;
    }
    try {
      return ModesOffered.valueOf(value);
    } catch (IllegalArgumentException e) {
      LOG.warn("{} value '{}' is not one of {}, both modes are offered", MODES_OFFERED_PROPERTY, value, List.of(ModesOffered.values()));
      return ModesOffered.LIGHT_AND_DARK;
    }
  }

  /**
   * @param property a configured file path property
   * @param defaultPath its default, null for none
   * @return the configured path
   */
  public String getConfiguredPath(String property, String defaultPath) {
    return StringUtils.defaultIfBlank(StringUtils.trim(environment.getProperty(property)), defaultPath);
  }

  public Set<String> getConfiguredKeys() {
    Set<String> keys = new LinkedHashSet<>();
    for (String key : StringUtils.split(StringUtils.defaultString(environment.getProperty(THEMES_PROPERTY)), ',')) {
      String trimmed = StringUtils.trimToNull(key);
      if (trimmed == null) {
        continue;
      }
      if (ThemeService.DEFAULT_THEME_CONFIG_KEY.equals(trimmed)) {
        LOG.warn("{} lists '{}', the key of the Default Theme: configure it with exo.branding.theme.*, the entry is ignored",
                 THEMES_PROPERTY,
                 trimmed);
      } else if (!keys.add(trimmed)) {
        LOG.warn("{} lists '{}' twice", THEMES_PROPERTY, trimmed);
      }
    }
    return keys;
  }

  ThemeValueValidator getValueValidator() {
    if (valueValidator == null) {
      valueValidator = ThemeValueValidator.of(brandingService);
    }
    return valueValidator;
  }

  private Set<String> getVariables() {
    Set<String> variables = new LinkedHashSet<>(getValueValidator().getDeclaredVariables());
    variables.addAll(PAGE_VARIABLES);
    return variables;
  }

  private void put(ThemeSettings settings, String variable, String value, boolean dark, String property) {
    if (value == null) {
      return;
    }
    String trimmed = value.trim();
    if (dark && isShared(variable)) {
      LOG.warn("{} is ignored: {} is shared by the light and dark versions", property, variable);
      return;
    }
    Map<String, String> single = new HashMap<>(Map.of(variable, trimmed));
    ThemeValueValidator.normalizeSizeUnits(single);
    try {
      getValueValidator().validateValues(single);
    } catch (IllegalArgumentException e) {
      LOG.warn("{} value '{}' is refused ({}), it is ignored", property, trimmed, e.getMessage());
      return;
    }
    Map<String, String> target = dark ? settings.getDark() : isShared(variable) ? settings.getShared() : settings.getLight();
    target.put(variable, single.get(variable));
  }

}
