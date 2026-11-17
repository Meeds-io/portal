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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.portal.branding.BrandingService;

import io.meeds.portal.theme.constant.ModesOffered;
import io.meeds.portal.theme.model.ConfiguredTheme;

/**
 * The configuration resolution of exo.properties (§2.4): every value through
 * the validator, the shared settings apart, and the reserved values.
 */
public class ThemeConfigurationTest {

  private final MockEnvironment    environment   = new MockEnvironment();

  private final ThemeConfiguration configuration = new ThemeConfiguration();

  @BeforeEach
  void setUp() {
    BrandingService brandingService = mock(BrandingService.class);
    when(brandingService.getDefaultThemeStyle()).thenReturn(new HashMap<>(Map.of("primaryColor", "#3f8487",
                                                                                  "fontFamily", "Arimo",
                                                                                  "appBorderRadiusTopLeft", "8px")));
    when(brandingService.compileThemeStyle(anyMap())).thenReturn("");
    ReflectionTestUtils.setField(configuration, "environment", environment);
    ReflectionTestUtils.setField(configuration, "brandingService", brandingService);
  }

  @Test
  void configuredThemesAreReadValidatedAndPlaced() {
    environment.setProperty("exo.branding.themes", "ocean, default, forest, ocean,");
    environment.setProperty("exo.branding.themes.ocean.name", "Ocean");
    environment.setProperty("exo.branding.themes.ocean.primaryColor", "#123456");
    environment.setProperty("exo.branding.themes.ocean.dark.primaryColor", "#654321");
    environment.setProperty("exo.branding.themes.ocean.fontFamily", "Open Sans");
    environment.setProperty("exo.branding.themes.ocean.dark.fontFamily", "Inter");
    environment.setProperty("exo.branding.themes.ocean.appBorderRadiusTopLeft", "4");
    environment.setProperty("exo.branding.themes.forest.primaryColor", "red");

    List<ConfiguredTheme> themes = configuration.getConfiguredThemes();
    assertEquals(List.of("ocean", "forest"), themes.stream().map(ConfiguredTheme::getKey).toList(), "default and duplicates ignored");
    ConfiguredTheme ocean = themes.get(0);
    assertEquals("Ocean", ocean.getName());
    assertEquals(Map.of("primaryColor", "#123456"), ocean.getSettings().getLight());
    assertEquals(Map.of("primaryColor", "#654321"), ocean.getSettings().getDark(), "a dark shared setting is ignored");
    assertEquals(Map.of("fontFamily", "Open Sans", "appBorderRadiusTopLeft", "4px"), ocean.getSettings().getShared());
    ConfiguredTheme forest = themes.get(1);
    assertEquals("forest", forest.getName(), "the name defaults to the key");
    assertTrue(forest.getSettings().getLight().isEmpty(), "a refused value is ignored");
  }

  @Test
  void aKeyOrANameTooLongIgnoresTheTheme() {
    environment.setProperty("exo.branding.themes", "ocean," + "k".repeat(101));
    environment.setProperty("exo.branding.themes.ocean.name", "N".repeat(201));
    assertTrue(configuration.getConfiguredThemes().isEmpty());
  }

  @Test
  void theDefaultThemesDarkValuesAndTheReservedValues() {
    environment.setProperty("exo.branding.theme.dark.primaryColor", "#9BB8E0");
    environment.setProperty("exo.branding.modesOffered", "SEPIA");
    assertEquals(Map.of("primaryColor", "#9BB8E0"), configuration.getDefaultThemeDarkValues());
    assertEquals(ModesOffered.LIGHT_AND_DARK, configuration.getConfiguredModesOffered());
    environment.setProperty("exo.branding.modesOffered", "DARK");
    assertEquals(ModesOffered.DARK, configuration.getConfiguredModesOffered());
    assertEquals(ThemeService.DEFAULT_THEME_CONFIG_KEY, configuration.getConfiguredThemeInUse());
    environment.setProperty("exo.branding.themeInUse", " ocean ");
    assertEquals("ocean", configuration.getConfiguredThemeInUse());
  }

}
