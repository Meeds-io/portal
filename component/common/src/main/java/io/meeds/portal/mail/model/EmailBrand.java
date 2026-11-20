/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package io.meeds.portal.mail.model;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.branding.BrandingService;

/**
 * The brand an email is dressed in: the company name and the primary
 * color of the site, with the text color that stays readable on it
 */
public final class EmailBrand {

  /** Used when the site has no valid primary color: the default primary color of the platform theme */
  public static final String   DEFAULT_COLOR = "#3f8487";

  /** Used when the site has no valid border radius, as the platform does */
  public static final String   DEFAULT_RADIUS = "8px";

  /** Used when the site has no valid font, the default font of the platform */
  public static final String   DEFAULT_FONT_FAMILY = "Arimo";

  static final long            CACHE_MILLIS  = 5000L;

  private static final AtomicReference<CachedEmailBrand> CACHED = new AtomicReference<>();

  private static final String  DARK_TEXT     = "#1d2327";

  private static final String  LIGHT_TEXT    = "#ffffff";

  private static final Pattern HEX_COLOR     = Pattern.compile("#[0-9a-fA-F]{6}");

  // the forms the branding accepts for a color: #rgb, #rgba, #rrggbb and #rrggbbaa
  private static final Pattern HEX_SHORT     = Pattern.compile("#[0-9a-fA-F]{3,4}");

  private static final Pattern HEX_ALPHA     = Pattern.compile("#[0-9a-fA-F]{8}");

  // a font name as the platform lists them: letters, digits, spaces and hyphens, never a quote or a separator
  private static final Pattern FONT_NAME      = Pattern.compile("[A-Za-z0-9][A-Za-z0-9 \\-]{0,59}");

  private static final Pattern RADIUS_PATTERN = Pattern.compile("(?:\\d|1\\d|2\\d)px");

  private final String         companyName;

  private final String         color;

  private final String         fill;

  private final String         fontFamily;

  private final String         radius;

  /**
   * @param companyName the name shown in the header of the email
   * @param primaryColor a #rgb, #rrggbb or #rrggbbaa color (the alpha is
   *          dropped); anything else, a css injection attempt included, is
   *          replaced by {@link #DEFAULT_COLOR}
   */
  public EmailBrand(String companyName, String primaryColor) {
    this(companyName, primaryColor, null, null);
  }

  /**
   * @param companyName the name shown in the header of the email
   * @param primaryColor the main color of the site, as #rgb, #rrggbb or #rrggbbaa
   * @param fontFamily the font of the platform branding; the default font
   *          of the platform when it is not a font name
   * @param borderRadius the rounding of its buttons, from 0px to 29px;
   *          {@link #DEFAULT_RADIUS} otherwise
   */
  public EmailBrand(String companyName, String primaryColor, String fontFamily, String borderRadius) {
    this.companyName = companyName == null ? "" : companyName;
    this.color = valid(primaryColor, DEFAULT_COLOR);
    this.fill = readableFill(this.color);
    this.fontFamily = fontFamily != null && FONT_NAME.matcher(fontFamily.trim()).matches() ? fontFamily.trim() : DEFAULT_FONT_FAMILY;
    this.radius = borderRadius != null && RADIUS_PATTERN.matcher(borderRadius.trim()).matches() ? borderRadius.trim() : DEFAULT_RADIUS;
  }

  /**
   * The brand of the site, for a template that has no branding service at
   * hand; the default brand when the site cannot be reached. The brand is
   * kept a few seconds, so that the many helper calls of one rendering (and
   * of the emails sent in a row) read the theme once. A branding saved in
   * the meantime shows in the emails after at most {@link #CACHE_MILLIS}
   * milliseconds (five seconds).
   */
  public static EmailBrand current() {
    return current(System.currentTimeMillis());
  }

  /**
   * Forgets the brand kept by {@link #current()}, so that the next call reads
   * the theme again
   */
  public static void clearCache() {
    CACHED.set(null);
  }

  public static EmailBrand from(BrandingService brandingService) {
    Map<String, String> style = brandingService.getThemeStyle();
    if (style == null) {
      return new EmailBrand(brandingService.getCompanyName(), null);
    }
    return new EmailBrand(brandingService.getCompanyName(),
                          style.get("primaryColor"),
                          style.get("fontFamily"),
                          style.get("borderRadius"));
  }

  public String getCompanyName() {
    return companyName;
  }

  /**
   * @return the primary color as #rrggbb
   */
  public String getColor() {
    return color;
  }

  /**
   * @return the primary color as it is painted behind text (the header and
   *         the buttons): the color itself, or the nearest one that text can
   *         be read on
   */
  public String getFillColor() {
    return fill;
  }

  /**
   * @return the font of the platform branding, for the stack of fonts of the
   *         email: its name, as the administrator chose it in the branding
   */
  public String getFontFamily() {
    return fontFamily;
  }

  /**
   * @return the font of the platform branding first, then the fonts that every
   *         mail client has: the email asks for the platform font and takes
   *         Arial when the client does not have it; nothing is loaded from a
   *         third party, so a client that has not the font shows Arial
   */
  public String getFontStack() {
    return "'" + fontFamily + "',Arial,Helvetica,sans-serif";
  }

  /**
   * @return the primary color, darkened as much as needed (and not more) to
   *         be seen as the outline of an outlined button on white: at least
   *         3:1
   */
  public String getOutlineColor() {
    return readableOnWhite(color, 3.0d);
  }

  /**
   * @return the primary color, darkened as much as needed (and not more) to
   *         be read as the text of a link on white: at least 4.5:1
   */
  public String getLinkColor() {
    return readableOnWhite(color);
  }

  /**
   * @return the rounding of the buttons, such as 8px
   */
  public String getRadius() {
    return radius;
  }

  /**
   * @return white or the dark ink, whichever has the better contrast on the
   *         primary color
   */
  public String getTextColor() {
    return contrast(LIGHT_TEXT, fill) >= contrast(DARK_TEXT, fill) ? LIGHT_TEXT : DARK_TEXT;
  }

  /**
   * WCAG contrast ratio between two #rrggbb colors
   */
  static double contrast(String first, String second) {
    double a = luminance(first);
    double b = luminance(second);
    return (Math.max(a, b) + 0.05d) / (Math.min(a, b) + 0.05d);
  }

  /**
   * The same as {@link #current()} at a given time, so that the expiry of the
   * kept brand can be tested
   */
  static EmailBrand current(long now) {
    CachedEmailBrand last = CACHED.get();
    if (last != null && now < last.expires()) {
      return last.brand();
    }
    try {
      BrandingService brandingService = PortalContainer.getInstance().getComponentInstanceOfType(BrandingService.class);
      if (brandingService == null) {
        return new EmailBrand("", null);
      }
      EmailBrand brand = from(brandingService);
      CACHED.set(new CachedEmailBrand(brand, now + CACHE_MILLIS));
      return brand;
    } catch (RuntimeException | LinkageError _) {
      return new EmailBrand("", null);
    }
  }

  /**
   * @param ratio how much of the second color goes into the mix, from 0 to 1
   */
  static String mix(String first, String second, double ratio) {
    int a = Integer.parseInt(first.substring(1), 16);
    int b = Integer.parseInt(second.substring(1), 16);
    int rgb = 0;
    for (int shift = 16; shift >= 0; shift -= 8) {
      int channel = (int) Math.round((a >> shift & 0xff) * (1 - ratio) + (b >> shift & 0xff) * ratio);
      rgb |= Math.clamp(channel, 0, 255) << shift;
    }
    return String.format("#%06x", rgb);
  }

  /**
   * The color itself when text on it can reach 4.5:1 (white or dark ink,
   * whichever is better), otherwise the color moved as little as needed
   * toward black or white to reach it, so that the text of the header and of
   * the buttons can be read
   */
  private static String readableFill(String color) {
    boolean light = contrast(LIGHT_TEXT, color) >= contrast(DARK_TEXT, color);
    String target = light ? "#000000" : LIGHT_TEXT;
    String ink = light ? LIGHT_TEXT : DARK_TEXT;
    String fill = color;
    for (int step = 1; contrast(ink, fill) < 4.5d && step <= 30; step++) {
      fill = mix(color, target, step * 0.02d);
    }
    return fill;
  }

  private static String valid(String color, String fallback) {
    String value = color == null ? "" : color.trim();
    if (HEX_SHORT.matcher(value).matches()) {
      // #rgb and #rgba: each digit is doubled, the alpha is dropped
      value = "#" + value.substring(1, 4).replaceAll("(.)", "$1$1");
    } else if (HEX_ALPHA.matcher(value).matches()) {
      value = value.substring(0, 7);
    }
    return HEX_COLOR.matcher(value).matches() ? value.toLowerCase() : fallback;
  }

  private static String readableOnWhite(String base) {
    return readableOnWhite(base, 4.5d);
  }

  private static String readableOnWhite(String base, double minimum) {
    String text = base;
    for (int step = 1; contrast(LIGHT_TEXT, text) < minimum && step <= 20; step++) {
      text = mix(base, "#000000", step * 0.05d);
    }
    return text;
  }

  private static double luminance(String hex) {
    int rgb = Integer.parseInt(hex.substring(1), 16);
    return 0.2126d * channel(rgb >> 16 & 0xff) + 0.7152d * channel(rgb >> 8 & 0xff) + 0.0722d * channel(rgb & 0xff);
  }

  private static double channel(int value) {
    double v = value / 255d;
    return v <= 0.03928d ? v / 12.92d : Math.pow((v + 0.055d) / 1.055d, 2.4d);
  }

}
