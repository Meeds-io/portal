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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.portal.branding.BrandingService;

import io.meeds.portal.theme.model.LegacyBranding;

/**
 * The cleaning of the one-time import: an earlier Branding page stored every
 * default it loaded, so only what differs from its default is imported, or
 * Auto and Automatic would never apply.
 */
@ExtendWith(MockitoExtension.class)
public class LegacyBrandingImporterTest {

  @Mock
  private SettingService         settingService;

  @Mock
  private BrandingService        brandingService;

  @InjectMocks
  private LegacyBrandingImporter importer;

  private final Map<String, String> defaults = new HashMap<>();

  @BeforeEach
  void setUp() {
    defaults.put("primaryColor", "#3F8487");
    defaults.put("textColor", "#20282C");
    defaults.put("appTextHeaderColor", "#6B6B6B");
    defaults.put("fontFamily", "Arimo");
    defaults.put("topBarBackgroundColor", "#FFFFFF");
    defaults.put("topBarBackgroundImage", "");
    defaults.put("sideBarBackgroundImage", "");
    defaults.put("appBorderRadiusTopLeft", "8px");
    defaults.put("pageWidth", "100%");
    defaults.put("pageBackgroundColor", "#F0F0F0");
    defaults.put("pageBackgroundEffect", "none");
    lenient().when(brandingService.getDefaultThemeStyle()).thenReturn(defaults);
    lenient().when(brandingService.compileThemeStyle(anyMap())).thenReturn("");
  }

  @Test
  void aValueEqualToItsDefaultIsNotImported() {
    LegacyBranding legacy = importer.clean(Map.of("primaryColor", "#3f8487ff",
                                                  "textColor", "#20282C",
                                                  "appTextHeaderColor", "#707070",
                                                  "fontFamily", "Arimo",
                                                  "pageBackgroundColor", "#f0f0f0",
                                                  "pageWidth", "100%",
                                                  "pageBackgroundImage", "none"),
                                           defaults);
    assertTrue(legacy.getSettings().getLight().isEmpty(), legacy.getSettings().toString());
    assertTrue(legacy.getSettings().getShared().isEmpty(), legacy.getSettings().toString());
  }

  @Test
  void aChosenValueIsImportedSharedOrLight() {
    LegacyBranding legacy = importer.clean(Map.of("primaryColor", "#123456",
                                                  "fontFamily", "Open Sans",
                                                  "appBorderRadiusTopLeft", "4",
                                                  "pageWidth", "1320px"),
                                           defaults);
    assertEquals(Map.of("primaryColor", "#123456"), legacy.getSettings().getLight());
    assertEquals(Map.of("fontFamily", "Open Sans", "appBorderRadiusTopLeft", "4px", "pageWidth", "1320px"),
                 legacy.getSettings().getShared());
  }

  @Test
  void anImageValueKeepsItsGradientAndTheTopbarGradientGoes() {
    LegacyBranding legacy = importer.clean(Map.of("sideBarBackgroundImage",
                                                  "url(/portal/rest/v1/platform/branding/sideBarBackground?v=1), linear-gradient(#fff, #000)",
                                                  "topBarBackgroundImage",
                                                  "linear-gradient(#112233, #000)",
                                                  "topBarBackgroundColor",
                                                  "#FFFFFF"),
                                           defaults);
    assertEquals("linear-gradient(#fff, #000)", legacy.getSettings().getLight().get("sideBarBackgroundImage"));
    assertFalse(legacy.getSettings().getLight().containsKey("topBarBackgroundImage"));
    assertEquals("#112233", legacy.getSettings().getLight().get("topBarBackgroundColor"),
                 "the topbar showed the gradient's first colour, it keeps showing it");
  }

  @Test
  void aValueTheValidatorRefusesIsDropped() {
    LegacyBranding legacy = importer.clean(Map.of("primaryColor", "red", "textColor", "#111111"), defaults);
    assertNull(legacy.getSettings().getLight().get("primaryColor"));
    assertEquals("#111111", legacy.getSettings().getLight().get("textColor"));
  }

  @Test
  void theLegacyValuesAreReadFromTheirOwnContextsAndTertiaryIsSkipped() {
    stubSetting(org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_CONTEXT,
                org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_SCOPE,
                "primaryColor",
                "#123456");
    stubSetting(org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_CONTEXT,
                org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_SCOPE,
                LegacyBrandingImporter.TERTIARY_COLOR,
                "#E25D5D");
    stubSetting(org.exoplatform.commons.api.settings.data.Context.GLOBAL,
                org.exoplatform.commons.api.settings.data.Scope.GLOBAL,
                "page.backgroundEffect",
                "linear-gradient(#fff, #000)");
    stubSetting(org.exoplatform.commons.api.settings.data.Context.GLOBAL,
                org.exoplatform.commons.api.settings.data.Scope.GLOBAL,
                "sideBar.background",
                "42");
    stubSetting(org.exoplatform.commons.api.settings.data.Context.GLOBAL,
                org.exoplatform.commons.api.settings.data.Scope.GLOBAL,
                "exo.branding.company.name",
                "ACME");
    stubSetting(org.exoplatform.commons.api.settings.data.Context.GLOBAL,
                org.exoplatform.commons.api.settings.data.Scope.GLOBAL,
                "exo.branding.company.id",
                "7");
    defaults.put("tertiaryColor", "#476A9C");
    defaults.put("sideBarBackgroundImage", "");

    LegacyBranding legacy = importer.read();
    assertEquals("#123456", legacy.getSettings().getLight().get("primaryColor"));
    assertFalse(legacy.getSettings().getLight().containsKey("tertiaryColor"));
    assertEquals("linear-gradient(#fff, #000)", legacy.getSettings().getLight().get("pageBackgroundImage"));
    assertEquals(Map.of("sideBarBackgroundImage", 42L), legacy.getImageFileIds());
    assertEquals("ACME", legacy.getCompanyName());
    assertEquals(7L, legacy.getLogoFileId());
  }

  @Test
  void theTertiaryDeleteRunsWhenTheSettingExistsAndNeverFails() {
    stubSetting(org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_CONTEXT,
                org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_SCOPE,
                LegacyBrandingImporter.TERTIARY_COLOR,
                "#E25D5D");
    importer.deleteTertiaryColor();
    org.mockito.Mockito.verify(settingService)
                       .remove(org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_CONTEXT,
                               org.exoplatform.portal.branding.BrandingServiceImpl.BRANDING_SCOPE,
                               LegacyBrandingImporter.TERTIARY_COLOR);
    org.mockito.Mockito.doThrow(new IllegalStateException("down"))
                       .when(settingService)
                       .remove(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    importer.deleteTertiaryColor();
  }

  @Test
  void aUnitlessStoredSizeEqualToItsDefaultIsNotImported() {
    LegacyBranding legacy = importer.clean(Map.of("appBorderRadiusTopLeft", "8"), defaults);
    assertTrue(legacy.getSettings().getShared().isEmpty(), legacy.getSettings().toString());
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void stubSetting(org.exoplatform.commons.api.settings.data.Context context,
                           org.exoplatform.commons.api.settings.data.Scope scope,
                           String key,
                           String value) {
    lenient().when(settingService.get(context, scope, key))
             .thenReturn((org.exoplatform.commons.api.settings.SettingValue) org.exoplatform.commons.api.settings.SettingValue.create(value));
  }

  @Test
  void colorsAreComparedByValue() {
    assertTrue(LegacyBrandingImporter.isDefault("#FFF", "#ffffff"));
    assertTrue(LegacyBrandingImporter.isDefault("#FFFFFFFF", "#ffffff"));
    assertFalse(LegacyBrandingImporter.isDefault("#FFFFFF80", "#ffffff"));
    assertTrue(LegacyBrandingImporter.isDefault("#707070", "#6B6B6B"));
    assertFalse(LegacyBrandingImporter.isDefault("#6B6B6B", "#707070"));
    assertFalse(LegacyBrandingImporter.isDefault("#123456", ""));
  }

}
