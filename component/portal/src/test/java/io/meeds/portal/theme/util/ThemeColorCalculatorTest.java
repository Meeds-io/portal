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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.meeds.portal.theme.constant.BackgroundKind;
import io.meeds.portal.theme.constant.TextLevel;
import io.meeds.portal.theme.util.ThemeColorCalculator;

/**
 * The note's contrast table, reproduced, and the Automatic and Auto rules.
 */
public class ThemeColorCalculatorTest {

  @ParameterizedTest
  @CsvSource({
    // text, background, ratio of the note's table (light / dark per surface)
    "#20282C,#F0F0F0,13.2", "#E6E9EB,#14181B,14.6", "#6B6B6B,#F0F0F0,4.7", "#9BA3A8,#14181B,7.0",
    "#20282C,#FFFFFF,15.0", "#E6E9EB,#1E2328,13.0", "#6B6B6B,#FFFFFF,5.3", "#9BA3A8,#1E2328,6.2",
    "#E6E9EB,#2B3035,10.9", "#9BA3A8,#2B3035,5.2",
    "#E6E9EB,#252B31,11.7", "#9BA3A8,#252B31,5.6",
  })
  void reproducesTheNotesContrastTable(String text, String background, double ratio) {
    assertEquals(ratio, ThemeColorCalculator.roundedContrast(text, background), 0.001);
  }

  @Test
  void aMixIsLessMixRoundedAsLessRounds() {
    // 169 * 0.25 + 35 * 0.75 = 68.5, rounded up as Less writes it (the light-to-dark stylesheet test pins the same value)
    assertEquals("#2E4549", ThemeColorCalculator.mix("#5FA9AC", "#1E2328", 0.25));
    assertEquals("#5FA9AC", ThemeColorCalculator.mix("#5fa9ac", "#000", 1));
    assertEquals("#1E2328", ThemeColorCalculator.mix("#5FA9AC80", "#1E2328", 0), "an alpha is not read");
  }

  @Test
  void theOldGreyMissesTheReadableRatio() {
    assertEquals(4.35, ThemeColorCalculator.contrast("#707070", "#F0F0F0"), 0.01);
    assertTrue(ThemeColorCalculator.contrast("#707070", "#F0F0F0") < ThemeColorCalculator.READABLE_RATIO);
  }

  @Test
  void autoPicksTheDefaultThatReadsBest() {
    assertEquals(ThemeColorCalculator.LIGHT_NEUTRAL, ThemeColorCalculator.autoText(TextLevel.NEUTRAL, "#FFFFFF"));
    assertEquals(ThemeColorCalculator.DARK_NEUTRAL, ThemeColorCalculator.autoText(TextLevel.NEUTRAL, "#1E2328"));
    assertEquals(ThemeColorCalculator.DARK_GREY, ThemeColorCalculator.autoText(TextLevel.GREY, "#1E2328"));
    assertFalse(ThemeColorCalculator.isAutoTextLowContrast(TextLevel.GREY, "#FFFFFF"));
    // a mid-grey background on which no default reaches 4.5:1
    assertTrue(ThemeColorCalculator.isAutoTextLowContrast(TextLevel.GREY, "#808080"));
  }

  @Test
  void automaticDarkBackgroundsFollowTheNotesTable() {
    assertEquals("#14181B", ThemeColorCalculator.automaticDarkBackground(BackgroundKind.PAGE, "#F0F0F0"));
    assertEquals("#1E2328", ThemeColorCalculator.automaticDarkBackground(BackgroundKind.APP, "#ffffff"));
    assertEquals("#252B31", ThemeColorCalculator.automaticDarkBackground(BackgroundKind.DRAWER, "#FFF"));
    String custom = ThemeColorCalculator.automaticDarkBackground(BackgroundKind.APP, "#FFF4E0");
    assertTrue(ThemeColorCalculator.luminance(ThemeColorCalculator.parse(custom)) < 0.1, custom + " stays dark");
    assertTrue(ThemeColorCalculator.contrast(ThemeColorCalculator.DARK_NEUTRAL, custom) >= ThemeColorCalculator.READABLE_RATIO);
    assertEquals("80", ThemeColorCalculator.automaticDarkBackground(BackgroundKind.APP, "#FFF4E080").substring(7),
                 "the alpha is kept");
  }

  @Test
  void aBackgroundBarelyDifferentFromTheDefaultTakesTheDarkDefault() {
    assertEquals("#14181B", ThemeColorCalculator.automaticDarkBackground(BackgroundKind.PAGE, "#F2F2F2FF"));
    assertEquals("#1E2328F0", ThemeColorCalculator.automaticDarkBackground(BackgroundKind.APP, "#FFFFFFF0"));
    String custom = ThemeColorCalculator.automaticDarkBackground(BackgroundKind.PAGE, "#E0E0E0");
    assertTrue(!custom.equals("#14181B"), custom + " is a colour of its own, inverted");
    assertEquals("#14181B", ThemeColorCalculator.automaticDarkBackground(BackgroundKind.PAGE, "#EBEBEB"));
    assertTrue(!ThemeColorCalculator.automaticDarkBackground(BackgroundKind.PAGE, "#E8E8E8").equals("#14181B"),
               "just past the tolerance the colour is inverted");
    assertEquals("#1E2328", ThemeColorCalculator.automaticDarkBackground(BackgroundKind.APP, "#FFFDF5"),
                 "a faint tint within the tolerance is the default");
    assertTrue(!ThemeColorCalculator.automaticDarkBackground(BackgroundKind.APP, "#FFF8F0").startsWith("#1E2328"),
               "a tint past the tolerance keeps its own hue");
    assertEquals("#1E232800", ThemeColorCalculator.automaticDarkBackground(BackgroundKind.APP, "#FFF0"));
    assertTrue(!ThemeColorCalculator.automaticDarkBackground(BackgroundKind.APP, "#ECFFFC").startsWith("#1E2328"),
               "a green-cyan tint past the tolerance keeps its own hue");
  }

  @Test
  void brandColorsReadOnTheirBackground() {
    String darkPrimary = ThemeColorCalculator.automaticDarkBrandColor("#3F8487");
    assertTrue(ThemeColorCalculator.contrast(darkPrimary, ThemeColorCalculator.DARK_APP_BACKGROUND) >= 4.5, darkPrimary);
    String textPrimary = ThemeColorCalculator.readableOn("#7FC4C7", "#FFFFFF");
    assertTrue(ThemeColorCalculator.contrast(textPrimary, "#FFFFFF") >= 4.5, textPrimary);
    assertEquals("#20282C", ThemeColorCalculator.readableOn("#20282C", "#FFFFFF"), "a readable color is kept");
  }

  @Test
  void theDarkBubbleIsLighterAndKeepsAutoTextReadable() {
    String bubble = ThemeColorCalculator.commentBubble(ThemeColorCalculator.DARK_APP_BACKGROUND);
    assertTrue(ThemeColorCalculator.luminance(ThemeColorCalculator.parse(bubble)) > ThemeColorCalculator.luminance(ThemeColorCalculator.parse("#1E2328")),
               bubble);
    assertFalse(ThemeColorCalculator.isAutoTextLowContrast(TextLevel.GREY, bubble), bubble);
  }

}
