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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

/**
 * The grammar of the theme values: every value a writer stores ends up in the
 * stylesheet every user loads and in mails.
 */
public class ThemeValueValidatorTest {

  private static final Set<String>  DECLARED  = Set.of("primaryColor",
                                                       "fontFamily",
                                                       "topBarSticky",
                                                       "topBarBackgroundImage",
                                                       "sideBarBackgroundImage",
                                                       "appBorderRadiusTopLeft",
                                                       "appTextTitleBackgroundRadius",
                                                       "appBoxShadow",
                                                       "topBarTextFontWeight",
                                                       "mysteryVariable");

  private final List<Map<String, String>> compiled  = new ArrayList<>();

  private final ThemeValueValidator validator = new ThemeValueValidator(DECLARED,
                                                                        List.of("Arimo", "Open Sans"),
                                                                        compiled::add);

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
    "primaryColor|#3F8487",
    "primaryColor|#fff",
    "primaryColor|#3F848780",
    "primaryColor|transparent",
    "fontFamily|Open Sans",
    "topBarSticky|true",
    "sideBarBackgroundImage|linear-gradient(90deg, #fff 0%, #000 100%)",
    "sideBarBackgroundImage|none",
    "topBarBackgroundImage|none",
    "sideBarBackgroundImageFile|12",
    "appBorderRadiusTopLeft|8px",
    "appTextTitleBackgroundRadius|4px 4px 0px 0px",
    "appBoxShadow|0 2px 4px #00000033",
    "topBarTextFontWeight|bold",
    "pageWidth|1320px",
    "pageWidth|100%",
    "pageBackgroundColor|#F0F0F0",
    "pageBackgroundImage|radial-gradient(#fff, #000)",
    "pageBackgroundSize|cover",
    "pageBackgroundPosition|top left",
    "pageBackgroundRepeat|no-repeat",
    "pageBackgroundAttachment|fixed",
  })
  void acceptsWhatTheDrawersWrite(String key, String value) {
    assertDoesNotThrow(() -> validator.validateValue(key, value));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
    "primaryColor|#12345",
    "primaryColor|red",
    "fontFamily|Comic Sans",
    "topBarSticky|yes",
    "sideBarBackgroundImage|url(x.png)",
    "sideBarBackgroundImage|URL(x.png)",
    "sideBarBackgroundImage|linear-gradient(red, url(x))",
    "sideBarBackgroundImage|linear-gradient(red, )",
    "sideBarBackgroundImage|linear-gradient(#fff, #000), linear-gradient(#000, #fff)",
    "sideBarBackgroundImage|linear-gradient(#fff, #000",
    "topBarBackgroundImage|linear-gradient(#fff, #000)",
    "sideBarBackgroundImageFile|12a",
    "appBoxShadow|0 2px 4px red; } body { display: none } x {",
    "topBarTextFontWeight|bold<script>",
    "mysteryVariable|anything",
    "pageWidth|1320",
    "pageWidth|99px",
    "pageWidth|3001px",
    "pageWidth|50%",
    "pageBackgroundSize|auto",
    "pageBackgroundPosition|center center",
    "pageBackgroundRepeat|repeat-x",
    "pageBackgroundAttachment|local",
  })
  void refusesEverythingElse(String key, String value) {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> validator.validateValue(key, value));
    assertTrue(e.getMessage().startsWith("branding."), e.getMessage());
  }

  @Test
  void refusesAQuoteAndScriptWordsInAnyValue() {
    assertEquals(ThemeValueValidator.FORBIDDEN_VALUE_MESSAGE,
                 assertThrows(IllegalArgumentException.class,
                              () -> validator.validateValue("topBarTextFontWeight", "bo\"ld")).getMessage());
    assertEquals(ThemeValueValidator.FORBIDDEN_VALUE_MESSAGE,
                 assertThrows(IllegalArgumentException.class,
                              () -> validator.validateValue("topBarTextFontWeight", "JavaScript")).getMessage());
  }

  @Test
  void refusesAKeyNoVariableDeclares() {
    assertEquals(ThemeValueValidator.UNKNOWN_KEY_MESSAGE + "fooColor",
                 assertThrows(IllegalArgumentException.class, () -> validator.validateValue("fooColor", "#fff")).getMessage());
    assertEquals(ThemeValueValidator.UNKNOWN_KEY_MESSAGE + "primaryColorFile",
                 assertThrows(IllegalArgumentException.class,
                              () -> validator.validateValue("primaryColorFile", "12")).getMessage(),
                 "a File companion exists for image keys only");
  }

  @Test
  void aNullValueIsARemovalAndIsNotChecked() {
    Map<String, String> values = new HashMap<>();
    values.put("primaryColor", null);
    assertDoesNotThrow(() -> validator.validateValues(values));
  }

  @Test
  void normalizesUnitlessSizes() {
    Map<String, String> values = new HashMap<>(Map.of("appBorderRadiusTopLeft", "8", "appTextTitleBackgroundRadius", "4 4 0 0"));
    ThemeValueValidator.normalizeSizeUnits(values);
    assertEquals("8px", values.get("appBorderRadiusTopLeft"));
    assertEquals("4px 4px 0px 0px", values.get("appTextTitleBackgroundRadius"));
  }

  @Test
  void compilesBothVersionsWithoutTheFileKeys() {
    validator.validateCompiles(Map.of("primaryColor", "#fff", "sideBarBackgroundImageFile", "3"), Map.of("primaryColor", "#000"));
    assertEquals(List.of(Map.of("primaryColor", "#fff"), Map.of("primaryColor", "#000")), compiled);
  }

  @Test
  void aDarkVersionThatDoesNotCompileIsRefused() {
    ThemeValueValidator failingDark = new ThemeValueValidator(DECLARED, List.of("Arimo"), values -> {
      if ("#000".equals(values.get("primaryColor"))) {
        throw new IllegalStateException("does not compile");
      }
    });
    assertEquals(ThemeValueValidator.COMPILATION_ERROR_MESSAGE,
                 assertThrows(IllegalArgumentException.class,
                              () -> failingDark.validateCompiles(Map.of("primaryColor", "#fff"),
                                                                 Map.of("primaryColor", "#000"))).getMessage());
  }

}
