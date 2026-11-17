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

import java.util.Locale;

import io.meeds.portal.theme.constant.BackgroundKind;
import io.meeds.portal.theme.constant.TextLevel;

/**
 * The only implementation of the theme's color rules: WCAG 2.x contrast, the
 * Auto text color, the Automatic dark colors, the readable text form of
 * Primary and Secondary, and the dark comment bubble. Pure Java, so the
 * server alone computes them and the page never reimplements them.
 * <p>
 * The tuning constants (lightness bounds and steps) are fixed with the PO on
 * the playground's output; the unit tests pin the note's contrast table.
 */
public final class ThemeColorCalculator {

  /** The ratio from which a text reads well (WCAG AA, normal text). */
  public static final double READABLE_RATIO          = 4.5;

  public static final String WHITE                   = "#FFFFFF";

  public static final String BLACK                   = "#000000";

  public static final String LIGHT_NEUTRAL           = "#20282C";

  public static final String LIGHT_GREY              = "#6B6B6B";

  public static final String DARK_NEUTRAL            = "#E6E9EB";

  public static final String DARK_GREY               = "#9BA3A8";

  public static final String LIGHT_PAGE_BACKGROUND   = "#F0F0F0";

  public static final String LIGHT_APP_BACKGROUND    = WHITE;

  public static final String LIGHT_DRAWER_BACKGROUND = WHITE;

  public static final String DARK_PAGE_BACKGROUND    = "#14181B";

  public static final String DARK_APP_BACKGROUND     = "#1E2328";

  public static final String DARK_DRAWER_BACKGROUND  = "#252B31";

  /** OKLCH lightness step used when moving a color until it reads well. */
  static final double        LIGHTNESS_STEP          = 0.01;

  /** Highest lightness an inverted dark background takes: a dark background stays dark. */
  static final double        DARK_BACKGROUND_MAX     = 0.35;

  /**
   * Largest OKLCH distance at which a light background still counts as the
   * light default: a shipped page's #F2F2F2 or #FFFFFFF0 is the default to
   * the eye, and inverting it would give a near-black background.
   */
  static final double        DEFAULT_TOLERANCE       = 0.02;

  private ThemeColorCalculator() {
    // pure functions
  }

  /**
   * @return the WCAG 2.x contrast ratio of two opaque colors, 1 to 21
   */
  public static double contrast(String color1, String color2) {
    double l1 = luminance(parse(color1));
    double l2 = luminance(parse(color2));
    return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
  }

  /**
   * @return the ratio rounded as the page shows it, one decimal
   */
  public static double roundedContrast(String color1, String color2) {
    return Math.round(contrast(color1, color2) * 10) / 10d;
  }

  /**
   * Auto: of the level's light and dark default text colors, the one that
   * reads best on the background.
   *
   * @return the Auto text color
   */
  public static String autoText(TextLevel level, String background) {
    return contrast(level.getLightDefault(), background) >= contrast(level.getDarkDefault(), background) ? level.getLightDefault() :
                                                                                                         level.getDarkDefault();
  }

  /**
   * @return true when neither default of the level reaches 4.5:1 on the
   *         background: the page shows the low-contrast warning in Auto too
   */
  public static boolean isAutoTextLowContrast(TextLevel level, String background) {
    return contrast(autoText(level, background), background) < READABLE_RATIO;
  }

  /**
   * Automatic dark background: the dark default when the light value is the
   * light default or barely differs from it (its alpha kept), else the light
   * value's hue with its lightness inverted in OKLCH, kept dark.
   */
  public static String automaticDarkBackground(BackgroundKind kind, String lightValue) {
    if (lightValue == null || sameColor(lightValue, kind.getLightDefault())) {
      return kind.getDarkDefault();
    }
    if (isHexColor(lightValue) && oklabDistance(lightValue, kind.getLightDefault()) <= DEFAULT_TOLERANCE) {
      return withAlpha(kind.getDarkDefault(), lightValue);
    }
    double[] lch = toOklch(parse(lightValue));
    lch[0] = Math.min(1 - lch[0], DARK_BACKGROUND_MAX);
    return withAlpha(format(fromOklch(lch)), lightValue);
  }

  /**
   * Automatic dark brand color (Primary, Secondary, a gradient stop): hue and
   * chroma kept, lightness raised until it reads on the dark application
   * background as it would be shown on it.
   */
  public static String automaticDarkBrandColor(String lightValue) {
    return withAlpha(readableOn(lightValue, DARK_APP_BACKGROUND), lightValue);
  }

  /**
   * Primary or Secondary used as text: the color's OKLCH lightness moved
   * until it reaches 4.5:1 on the background it sits on.
   */
  public static String readableOn(String color, String background) {
    double[] rgb = parse(color);
    if (contrast(color, background) >= READABLE_RATIO) {
      return format(rgb);
    }
    double[] lch = toOklch(rgb);
    boolean lighter = luminance(parse(background)) < 0.18;
    for (int i = 0; i < 100; i++) {
      lch[0] = clamp(lch[0] + (lighter ? LIGHTNESS_STEP : -LIGHTNESS_STEP));
      String candidate = format(fromOklch(lch));
      if (contrast(candidate, background) >= READABLE_RATIO || lch[0] <= 0 || lch[0] >= 1) {
        return candidate;
      }
    }
    return lighter ? WHITE : BLACK;
  }

  /**
   * The dark comment bubble: the background moved towards the light end, one
   * step at a time, while the Auto text on it still reaches 4.5:1; the
   * background itself when no step does.
   */
  public static String commentBubble(String darkBackground) {
    double[] lch = toOklch(parse(darkBackground));
    String bubble = format(parse(darkBackground));
    for (int i = 0; i < 20; i++) {
      lch[0] = clamp(lch[0] + 2 * LIGHTNESS_STEP);
      String candidate = format(fromOklch(lch));
      if (contrast(autoText(TextLevel.NEUTRAL, candidate), candidate) < READABLE_RATIO
          || contrast(autoText(TextLevel.GREY, candidate), candidate) < READABLE_RATIO) {
        break;
      }
      bubble = candidate;
      if (i >= 2) {
        break;
      }
    }
    return bubble;
  }

  /**
   * @return true for a 3, 4, 6 or 8 digit hex color
   */
  public static boolean isHexColor(String value) {
    return value != null && value.trim().matches("#([0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})");
  }

  public static boolean sameColor(String color1, String color2) {
    return isHexColor(color1) && isHexColor(color2) && format(parse(color1)).equals(format(parse(color2)))
           && alpha(color1).equals(alpha(color2));
  }

  static double[] parse(String color) {
    String hex = color.trim().substring(1);
    if (hex.length() <= 4) {
      StringBuilder expanded = new StringBuilder();
      for (char c : hex.toCharArray()) {
        expanded.append(c).append(c);
      }
      hex = expanded.toString();
    }
    return new double[] { Integer.parseInt(hex.substring(0, 2), 16) / 255d, Integer.parseInt(hex.substring(2, 4), 16) / 255d,
        Integer.parseInt(hex.substring(4, 6), 16) / 255d };
  }

  static String format(double[] rgb) {
    return String.format(Locale.ROOT,
                         "#%02X%02X%02X",
                         Math.round(clamp(rgb[0]) * 255),
                         Math.round(clamp(rgb[1]) * 255),
                         Math.round(clamp(rgb[2]) * 255));
  }

  private static String alpha(String color) {
    String hex = color.trim().substring(1).toUpperCase(Locale.ROOT);
    if (hex.length() == 4) {
      return "" + hex.charAt(3) + hex.charAt(3);
    }
    return hex.length() == 8 ? hex.substring(6) : "FF";
  }

  private static double oklabDistance(String color1, String color2) {
    double[] lch1 = toOklch(parse(color1));
    double[] lch2 = toOklch(parse(color2));
    return Math.sqrt(Math.pow(lch1[0] - lch2[0], 2) + Math.pow(lch1[1] * Math.cos(lch1[2]) - lch2[1] * Math.cos(lch2[2]), 2)
        + Math.pow(lch1[1] * Math.sin(lch1[2]) - lch2[1] * Math.sin(lch2[2]), 2));
  }

  private static String withAlpha(String opaque, String source) {
    String alpha = alpha(source);
    return "FF".equals(alpha) ? opaque : opaque + alpha;
  }

  static double luminance(double[] rgb) {
    return 0.2126 * linear(rgb[0]) + 0.7152 * linear(rgb[1]) + 0.0722 * linear(rgb[2]);
  }

  private static double linear(double channel) {
    return channel <= 0.04045 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
  }

  private static double gamma(double channel) {
    return channel <= 0.0031308 ? 12.92 * channel : 1.055 * Math.pow(channel, 1 / 2.4) - 0.055;
  }

  static double[] toOklch(double[] rgb) {
    double r = linear(rgb[0]);
    double g = linear(rgb[1]);
    double b = linear(rgb[2]);
    double l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
    double m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
    double s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
    double lightness = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s;
    double a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s;
    double bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s;
    return new double[] { lightness, Math.hypot(a, bb), Math.atan2(bb, a) };
  }

  /**
   * Back to sRGB; a color out of gamut has its chroma reduced until it fits.
   */
  static double[] fromOklch(double[] lch) {
    double chroma = lch[1];
    for (int i = 0; i < 50; i++) {
      double[] rgb = oklabToRgb(lch[0], chroma * Math.cos(lch[2]), chroma * Math.sin(lch[2]));
      if (inGamut(rgb) || chroma <= 0) {
        return rgb;
      }
      chroma = Math.max(0, chroma - 0.005);
    }
    return oklabToRgb(lch[0], 0, 0);
  }

  private static double[] oklabToRgb(double lightness, double a, double b) {
    double l = Math.pow(lightness + 0.3963377774 * a + 0.2158037573 * b, 3);
    double m = Math.pow(lightness - 0.1055613458 * a - 0.0638541728 * b, 3);
    double s = Math.pow(lightness - 0.0894841775 * a - 1.2914855480 * b, 3);
    return new double[] { gamma(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s),
        gamma(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s),
        gamma(-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s) };
  }

  private static boolean inGamut(double[] rgb) {
    for (double channel : rgb) {
      if (Double.isNaN(channel) || channel < -0.0005 || channel > 1.0005) {
        return false;
      }
    }
    return true;
  }

  private static double clamp(double value) {
    return Math.max(0, Math.min(1, value));
  }

}
