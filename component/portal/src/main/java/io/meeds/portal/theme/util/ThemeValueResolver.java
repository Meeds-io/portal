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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.meeds.portal.theme.constant.BackgroundKind;
import io.meeds.portal.theme.constant.TextLevel;
import io.meeds.portal.theme.model.ResolvedTheme;
import io.meeds.portal.theme.util.ThemeColorCalculator;

/**
 * Computes a theme's two versions from the values an administrator or the
 * configuration set: an absent text or icon color is Auto, an absent dark
 * color is Automatic, an image without a dark file shares the light one; and
 * adds the properties the calculator alone produces (Primary and Secondary as
 * text, the second panel, the comment bubbles).
 */
public final class ThemeValueResolver {

  public static final String       PRIMARY_TEXT_COLOR       = "primaryTextColor";

  public static final String       SECONDARY_TEXT_COLOR     = "secondaryTextColor";

  public static final String       SECOND_PANEL_BACKGROUND  = "secondPanelBackground";

  public static final String       COMMENT_BUBBLE_COLOR     = "commentBubbleColor";

  public static final String       DRAWER_COMMENT_BUBBLE    = "drawerCommentBubbleColor";

  private static final Pattern     BUBBLE_COLOUR            = Pattern.compile("#([0-9a-f]{3,4}|[0-9a-f]{6}|[0-9a-f]{8})",
                                                                            Pattern.CASE_INSENSITIVE);

  private static final Pattern     BUBBLE_UNCOLOURED        = Pattern.compile("#(f{3,4}|f{6}|f{8}|[0-9a-f]{3}0|[0-9a-f]{6}00)",
                                                                            Pattern.CASE_INSENSITIVE);

  private static final String[]    AREAS                    = { "topBar", "sideBar", "drawer", "app" };

  private static final Pattern     HEX                      = Pattern.compile("#([0-9a-fA-F]{8}|[0-9a-fA-F]{6}|[0-9a-fA-F]{3,4})\\b");

  private static final Set<String> DEFAULT_TEXT_COLORS      = Set.of(ThemeColorCalculator.LIGHT_NEUTRAL.toLowerCase(),
                                                                     ThemeColorCalculator.LIGHT_GREY.toLowerCase(),
                                                                     ThemeColorCalculator.DARK_NEUTRAL.toLowerCase(),
                                                                     ThemeColorCalculator.DARK_GREY.toLowerCase(),
                                                                     "#707070");

  private ThemeValueResolver() {
    // pure functions
  }

  /**
   * @param defaults the configured or built-in default of every variable
   * @param light the light values set: configured then stored, shared
   *          settings included
   * @param dark the dark values set: configured then stored
   * @return both versions, resolved
   */
  public static ResolvedTheme resolve(Map<String, String> defaults, Map<String, String> light, Map<String, String> dark) {
    ResolvedTheme resolved = new ResolvedTheme();
    Map<String, String> lightValues = new HashMap<>(defaults);
    lightValues.putAll(light);
    Map<String, String> lightSet = defaultsOfTexts(defaults);
    lightSet.putAll(light);
    Set<String> textKeys = new HashSet<>(lightValues.keySet().stream().filter(ThemeValueResolver::isTextKey).toList());
    resolveTexts(lightValues, lightSet, textKeys, false, resolved.getLightRatios(), resolved.getLightWarnings());
    addDerived(lightValues, false);
    resolved.getLight().putAll(lightValues);

    Map<String, String> darkValues = new HashMap<>(lightValues);
    darkValues.keySet().removeIf(key -> isTextKey(key) || isDerivedKey(key));
    lightValues.forEach((key, value) -> {
      if (!dark.containsKey(key)) {
        String automatic = automaticDark(key, value);
        if (automatic != null) {
          darkValues.put(key, automatic);
        }
      }
    });
    darkValues.putAll(dark);
    resolveTexts(darkValues, dark, textKeys, true, resolved.getDarkRatios(), resolved.getDarkWarnings());
    addDerived(darkValues, true);
    resolved.getDark().putAll(darkValues);
    return resolved;
  }

  /**
   * @return true for a text or icon color key: Auto when absent
   */
  public static boolean isTextKey(String key) {
    return "textColor".equals(key) || "greyColorLighten1".equals(key)
           || (key.endsWith("Color") && (key.contains("Text") || key.endsWith("IconColor")) && !key.contains("Background"));
  }

  static String automaticDark(String key, String lightValue) {
    if (lightValue == null || isTextKey(key)) {
      return null;
    }
    if ("primaryColor".equals(key) || "secondaryColor".equals(key)) {
      return ThemeColorCalculator.isHexColor(lightValue) ? ThemeColorCalculator.automaticDarkBrandColor(lightValue) : null;
    }
    if (key.endsWith("BackgroundColor") || key.endsWith("BorderColor") || "pageBackgroundColor".equals(key)) {
      return ThemeColorCalculator.isHexColor(lightValue) ? ThemeColorCalculator.automaticDarkBackground(kindOf(key), lightValue) :
                                                       null;
    }
    if (key.endsWith("BackgroundImage")) {
      // gradient colors are Automatic too
      Matcher hex = HEX.matcher(lightValue);
      StringBuilder dark = new StringBuilder();
      while (hex.find()) {
        hex.appendReplacement(dark, ThemeColorCalculator.automaticDarkBackground(BackgroundKind.APP, hex.group()));
      }
      hex.appendTail(dark);
      return dark.toString();
    }
    return null;
  }

  private static void resolveTexts(Map<String, String> values,
                                   Map<String, String> set,
                                   Set<String> textKeys,
                                   boolean darkVersion,
                                   Map<String, Double> ratios,
                                   Set<String> warnings) {
    for (String key : textKeys) {
      String background = backgroundOf(key, values, darkVersion);
      TextLevel level = levelOf(key);
      String value = set.get(key);
      if (value == null) {
        value = ThemeColorCalculator.autoText(level, background);
        values.put(key, value);
      }
      if (ThemeColorCalculator.isHexColor(value) && ThemeColorCalculator.isHexColor(background)) {
        double ratio = ThemeColorCalculator.roundedContrast(value, background);
        ratios.put(key, ratio);
        if (ThemeColorCalculator.contrast(value, background) < ThemeColorCalculator.READABLE_RATIO) {
          warnings.add(key);
        }
      }
    }
  }

  private static void addDerived(Map<String, String> values, boolean darkVersion) {
    String appBackground = colorOr(values.get("appBackgroundColor"),
                                   darkVersion ? ThemeColorCalculator.DARK_APP_BACKGROUND : ThemeColorCalculator.LIGHT_APP_BACKGROUND);
    String drawerBackground = colorOr(values.get("drawerBackgroundColor"),
                                      darkVersion ? ThemeColorCalculator.DARK_DRAWER_BACKGROUND :
                                                  ThemeColorCalculator.LIGHT_DRAWER_BACKGROUND);
    if (ThemeColorCalculator.isHexColor(values.get("primaryColor"))) {
      values.put(PRIMARY_TEXT_COLOR, ThemeColorCalculator.readableOn(values.get("primaryColor"), opaque(appBackground)));
    }
    if (ThemeColorCalculator.isHexColor(values.get("secondaryColor"))) {
      values.put(SECONDARY_TEXT_COLOR, ThemeColorCalculator.readableOn(values.get("secondaryColor"), opaque(appBackground)));
    }
    values.put(SECOND_PANEL_BACKGROUND, (darkVersion ? ThemeColorCalculator.DARK_GREY : ThemeColorCalculator.LIGHT_GREY) + "1A");
    if (darkVersion) {
      values.put(COMMENT_BUBBLE_COLOR, ThemeColorCalculator.commentBubble(opaque(appBackground)));
      values.put(DRAWER_COMMENT_BUBBLE, ThemeColorCalculator.commentBubble(opaque(drawerBackground)));
    } else {
      // the value the light stylesheet holds for the bubbles (branding.less .commentBackgroundColor), for the preview;
      // the stylesheet itself keeps compiling them from the background values
      values.put(COMMENT_BUBBLE_COLOR, lightCommentBubble(values.get("appBackgroundColor")));
      values.put(DRAWER_COMMENT_BUBBLE, lightCommentBubble(values.get("drawerBackgroundColor")));
    }
  }

  /**
   * @param background the application or drawer background as set
   * @return what branding.less declares for the light comment bubble: the
   *         background mixed 60% with white, or initial for an unset, white,
   *         transparent or non-hex background
   */
  static String lightCommentBubble(String background) {
    if (background == null || !BUBBLE_COLOUR.matcher(background).matches() || BUBBLE_UNCOLOURED.matcher(background).matches()) {
      return "initial";
    }
    // less4j writes a 3-digit hex out in 6 digits
    String color = background.length() == 4 ? "#" + background.substring(1).replaceAll("(.)", "$1$1").toLowerCase() : background;
    return "color-mix(in srgb, " + color + " 60%, white)";
  }

  private static boolean isDerivedKey(String key) {
    return PRIMARY_TEXT_COLOR.equals(key) || SECONDARY_TEXT_COLOR.equals(key) || SECOND_PANEL_BACKGROUND.equals(key)
           || COMMENT_BUBBLE_COLOR.equals(key) || DRAWER_COMMENT_BUBBLE.equals(key);
  }

  /**
   * A configured text default that is one of the default text colors is
   * Auto; any other configured text default is a chosen value.
   */
  private static Map<String, String> defaultsOfTexts(Map<String, String> defaults) {
    Map<String, String> texts = new HashMap<>();
    defaults.forEach((key, value) -> {
      if (isTextKey(key) && value != null && !value.isBlank() && !DEFAULT_TEXT_COLORS.contains(value.trim().toLowerCase())) {
        texts.put(key, value);
      }
    });
    return texts;
  }

  private static String backgroundOf(String key, Map<String, String> values, boolean darkVersion) {
    for (String area : AREAS) {
      if (key.startsWith(area)) {
        return opaque(colorOr(values.get(area + "BackgroundColor"), defaultBackground(area, darkVersion)));
      }
    }
    return opaque(colorOr(values.get("appBackgroundColor"), defaultBackground("app", darkVersion)));
  }

  private static String defaultBackground(String area, boolean darkVersion) {
    if ("drawer".equals(area)) {
      return darkVersion ? ThemeColorCalculator.DARK_DRAWER_BACKGROUND : ThemeColorCalculator.LIGHT_DRAWER_BACKGROUND;
    }
    return darkVersion ? ThemeColorCalculator.DARK_APP_BACKGROUND : ThemeColorCalculator.LIGHT_APP_BACKGROUND;
  }

  private static TextLevel levelOf(String key) {
    return key.contains("Header") || key.contains("Subtitle") || key.endsWith("IconColor") || "greyColorLighten1".equals(key) ?
                                                                                                                             TextLevel.GREY :
                                                                                                                             TextLevel.NEUTRAL;
  }

  private static BackgroundKind kindOf(String key) {
    if (key.startsWith("page")) {
      return BackgroundKind.PAGE;
    }
    return key.startsWith("drawer") ? BackgroundKind.DRAWER : BackgroundKind.APP;
  }

  private static String colorOr(String value, String fallback) {
    return ThemeColorCalculator.isHexColor(value) ? value : fallback;
  }

  /** The ratio is computed on the opaque color: a translucent background shows the page behind it. */
  private static String opaque(String color) {
    String hex = color.trim();
    return hex.length() == 9 ? hex.substring(0, 7) : hex.length() == 5 ? hex.substring(0, 4) : hex;
  }

}
