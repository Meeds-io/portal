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
package io.meeds.portal.theme.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.meeds.portal.theme.model.ResolvedTheme;
import io.meeds.portal.theme.util.ThemeColorCalculator;
import io.meeds.portal.theme.util.ThemeValueResolver;

/**
 * Auto, Automatic and the derived properties of a theme's two versions.
 */
public class ThemeValueResolverTest {

  private static final Map<String, String> DEFAULTS = Map.of("primaryColor", "#3F8487",
                                                             "appBackgroundColor", "#FFFFFF",
                                                             "drawerBackgroundColor", "#FFFFFF",
                                                             "topBarBackgroundColor", "#FFFFFF",
                                                             "appTextColor", "#20282C",
                                                             "appTextHeaderColor", "#707070",
                                                             "topBarIconColor", "#6B6B6B",
                                                             "sideBarBackgroundImage", "linear-gradient(#FFFFFF, #F0F0F0)");

  @Test
  void anUnsetTextIsAutoOnItsBackgroundInBothVersions() {
    ResolvedTheme resolved = ThemeValueResolver.resolve(DEFAULTS, Map.of(), Map.of());
    assertEquals("#20282C", resolved.getLight().get("appTextColor"));
    assertEquals("#6B6B6B", resolved.getLight().get("appTextHeaderColor"), "the old grey default reads as Auto");
    assertEquals("#1E2328", resolved.getDark().get("appBackgroundColor"));
    assertEquals("#252B31", resolved.getDark().get("drawerBackgroundColor"));
    assertEquals("#E6E9EB", resolved.getDark().get("appTextColor"));
    assertEquals("#9BA3A8", resolved.getDark().get("topBarIconColor"));
    assertEquals(15.0, resolved.getLightRatios().get("appTextColor"), 0.001);
    assertEquals(13.0, resolved.getDarkRatios().get("appTextColor"), 0.001);
    assertTrue(resolved.getLightWarnings().isEmpty() && resolved.getDarkWarnings().isEmpty());
  }

  @Test
  void aChosenTextIsKeptAndWarnedWhenItReadsBadly() {
    ResolvedTheme resolved = ThemeValueResolver.resolve(DEFAULTS, Map.of("appTextColor", "#CCCCCC"), Map.of());
    assertEquals("#CCCCCC", resolved.getLight().get("appTextColor"));
    assertTrue(resolved.getLightWarnings().contains("appTextColor"));
    assertEquals("#E6E9EB", resolved.getDark().get("appTextColor"), "the dark version stays Auto");
  }

  @Test
  void aCustomizedDarkValueIsKeptAndAnAutomaticOneFollowsTheLight() {
    Map<String, String> light = new HashMap<>(Map.of("appBackgroundColor", "#FFF4E0", "primaryColor", "#123456"));
    ResolvedTheme resolved = ThemeValueResolver.resolve(DEFAULTS, light, Map.of("drawerBackgroundColor", "#333333"));
    assertEquals("#333333", resolved.getDark().get("drawerBackgroundColor"));
    String darkApp = resolved.getDark().get("appBackgroundColor");
    assertTrue(ThemeColorCalculator.luminance(ThemeColorCalculator.parse(darkApp)) < 0.1, darkApp);
    assertTrue(ThemeColorCalculator.contrast(resolved.getDark().get("primaryColor"), "#1E2328") >= 4.5);
    assertFalse(resolved.getDark().get("sideBarBackgroundImage").contains("#FFFFFF"), "gradient colors are Automatic too");
  }

  @Test
  void theDerivedPropertiesAreEmittedPerVersion() {
    ResolvedTheme resolved = ThemeValueResolver.resolve(DEFAULTS, Map.of("primaryColor", "#7FC4C7"), Map.of());
    assertTrue(ThemeColorCalculator.contrast(resolved.getLight().get(ThemeValueResolver.PRIMARY_TEXT_COLOR), "#FFFFFF") >= 4.5);
    assertEquals("#6B6B6B1A", resolved.getLight().get(ThemeValueResolver.SECOND_PANEL_BACKGROUND));
    assertEquals("#9BA3A81A", resolved.getDark().get(ThemeValueResolver.SECOND_PANEL_BACKGROUND));
    assertTrue(resolved.getDark().containsKey(ThemeValueResolver.COMMENT_BUBBLE_COLOR));
    assertTrue(resolved.getLight().containsKey(ThemeValueResolver.COMMENT_BUBBLE_COLOR));
  }

  @Test
  void theLightBubblesAreTheOnesTheLightStylesheetHolds() {
    ResolvedTheme resolved = ThemeValueResolver.resolve(DEFAULTS,
                                                        Map.of("appBackgroundColor", "#EBADCBFF", "drawerBackgroundColor", "#3F8487"),
                                                        Map.of());
    assertEquals("color-mix(in srgb, #EBADCBFF 60%, white)", resolved.getLight().get(ThemeValueResolver.COMMENT_BUBBLE_COLOR));
    assertEquals("color-mix(in srgb, #3F8487 60%, white)", resolved.getLight().get(ThemeValueResolver.DRAWER_COMMENT_BUBBLE));
    resolved = ThemeValueResolver.resolve(DEFAULTS, Map.of("appBackgroundColor", "#ABC"), Map.of());
    assertEquals("color-mix(in srgb, #aabbcc 60%, white)", resolved.getLight().get(ThemeValueResolver.COMMENT_BUBBLE_COLOR));
  }

  @Test
  void theLightBubblesAreInitialForWhiteUnsetTransparentOrNonHexBackgrounds() {
    for (String background : new String[] { "#FFFFFF", "#fff", "#FFFFFFFF", "transparent", "#AABBCC00", "#ABC0", "rgb(1,2,3)", "#AABBC",
        "" }) {
      ResolvedTheme resolved = ThemeValueResolver.resolve(DEFAULTS,
                                                          Map.of("appBackgroundColor", background, "drawerBackgroundColor", background),
                                                          Map.of());
      assertEquals("initial", resolved.getLight().get(ThemeValueResolver.COMMENT_BUBBLE_COLOR), background);
      assertEquals("initial", resolved.getLight().get(ThemeValueResolver.DRAWER_COMMENT_BUBBLE), background);
    }
    ResolvedTheme unset = ThemeValueResolver.resolve(Map.of(), Map.of(), Map.of());
    assertEquals("initial", unset.getLight().get(ThemeValueResolver.COMMENT_BUBBLE_COLOR));
    assertEquals("initial", unset.getLight().get(ThemeValueResolver.DRAWER_COMMENT_BUBBLE));
  }

}
