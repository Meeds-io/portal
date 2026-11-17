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

import static io.meeds.portal.theme.constant.ThemeVariables.FONT_FAMILY;
import static io.meeds.portal.theme.constant.ThemeVariables.PAGE_BACKGROUND_ATTACHMENT;
import static io.meeds.portal.theme.constant.ThemeVariables.PAGE_BACKGROUND_POSITION;
import static io.meeds.portal.theme.constant.ThemeVariables.PAGE_BACKGROUND_REPEAT;
import static io.meeds.portal.theme.constant.ThemeVariables.PAGE_BACKGROUND_SIZE;
import static io.meeds.portal.theme.constant.ThemeVariables.PAGE_VARIABLES;
import static io.meeds.portal.theme.constant.ThemeVariables.PAGE_WIDTH;
import static io.meeds.portal.theme.constant.ThemeVariables.TOP_BAR_BACKGROUND_IMAGE;
import static io.meeds.portal.theme.constant.ThemeVariables.TOP_BAR_STICKY;
import static io.meeds.portal.theme.constant.ThemeVariables.imageKeyOf;
import static io.meeds.portal.theme.constant.ThemeVariables.isFileKey;
import static io.meeds.portal.theme.constant.ThemeVariables.isImageKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.branding.BrandingServiceImpl;

/**
 * The grammar of every theme value an administrator, a configuration or a
 * legacy writer stores. Each value ends up in the stylesheet every user loads,
 * anonymous pages included, and in mails: a value is refused with an
 * {@link IllegalArgumentException} carrying a message code, and nothing is
 * stored. Beyond the per-key grammar, the maps of a theme are compiled for
 * both versions before anything is written, so a value the Less compiler
 * refuses never removes the platform stylesheet.
 */
public class ThemeValueValidator {

  public static final String       INVALID_VALUE_MESSAGE      = "branding.theme.invalidValue:";

  public static final String       UNKNOWN_KEY_MESSAGE        = "branding.theme.unknownKey:";

  public static final String       FORBIDDEN_VALUE_MESSAGE    = "branding.css.forbiddenValue";

  public static final String       FONT_UNSUPPORTED_MESSAGE   = "branding.fontFamily.unsupported";

  public static final String       COMPILATION_ERROR_MESSAGE  = "branding.theme.stylesheetCompilationError";

  private static final Pattern     COLOR_PATTERN              = Pattern.compile("^(#([0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})|transparent|initial)$");

  private static final Pattern     SIZE_PATTERN               = Pattern.compile("^(-?\\d{1,4}(px)?|initial)$");

  private static final Pattern     UNITLESS_SIZE_PATTERN      = Pattern.compile("^-?\\d{1,4}$");

  private static final Pattern     UNITLESS_SIZE_TOKEN        = Pattern.compile("(?<![\\w.-])(-?\\d{1,4})(?![\\w.%])");

  private static final Pattern     RADIUS_PATTERN             = Pattern.compile("^(initial|(-?\\d{1,4}(px)?)( -?\\d{1,4}(px)?){0,3})$");

  private static final Pattern     KEYWORD_PATTERN            = Pattern.compile("^[a-zA-Z -]{1,30}$");

  private static final Pattern     BOX_SHADOW_PATTERN         = Pattern.compile("^(initial|none|[0-9a-zA-Z#\\s(),.-]{1,200})$");

  private static final Pattern     BACKGROUND_EFFECT_PATTERN  =
                                                             Pattern.compile("^(initial|none|(linear|radial|conic)-gradient\\([#0-9a-zA-Z(),.%\\s-]{1,300}\\))$");

  private static final Pattern     GRADIENT_FUNCTION          = Pattern.compile("(linear|radial|conic)-gradient\\(", Pattern.CASE_INSENSITIVE);

  private static final Pattern     EMPTY_ARGUMENT             = Pattern.compile("\\(\\s*,|,\\s*,|,\\s*\\)|\\(\\s*\\)");

  private static final Pattern     FILE_ID_PATTERN            = Pattern.compile("^\\d{1,19}$");

  private static final Pattern     PAGE_WIDTH_PATTERN         = Pattern.compile("^((\\d{3,4})px|100%)$");

  private static final Pattern     FORBIDDEN_CHARACTERS       = Pattern.compile("[\";<>{}\\\\]");

  private static final Set<String> PAGE_SIZES                 = Set.of("cover", "contain");

  private static final Set<String> PAGE_POSITIONS             = Set.of("center", "top right", "top left", "bottom right", "bottom left");

  private static final Set<String> PAGE_REPEATS               = Set.of("no-repeat", "repeat");

  private static final Set<String> PAGE_ATTACHMENTS           = Set.of("fixed", "scroll");

  private final Set<String>        declaredVariables;

  private final List<String>       supportedFontFamilies;

  private final ThemeCompiler      compiler;

  /**
   * @param declaredVariables the theme variables the Less template declares
   * @param supportedFontFamilies the font families the platform ships
   * @param compiler the trial compiler, null to skip the trial compile
   */
  public ThemeValueValidator(Set<String> declaredVariables, List<String> supportedFontFamilies, ThemeCompiler compiler) {
    this.declaredVariables = declaredVariables;
    this.supportedFontFamilies = supportedFontFamilies;
    this.compiler = compiler;
  }

  /**
   * @param brandingService the branding service declaring the variables and
   *          compiling the template
   * @return a validator of the variables it declares
   */
  public static ThemeValueValidator of(BrandingService brandingService) {
    Set<String> declared = new HashSet<>(brandingService.getDefaultThemeStyle().keySet());
    // a legacy page key: in a theme, the page effect is the gradient part of pageBackgroundImage
    declared.remove("pageBackgroundEffect");
    return new ThemeValueValidator(Set.copyOf(declared),
                                   BrandingServiceImpl.SUPPORTED_FONT_FAMILIES,
                                   brandingService::compileThemeStyle);
  }

  public Set<String> getDeclaredVariables() {
    return declaredVariables;
  }

  /**
   * A size without a unit is stored with px: the skin combines sizes in calc()
   * and border-radius, where a unitless non-zero number drops the declaration.
   *
   * @param values a map to normalise in place, null values kept
   */
  public static void normalizeSizeUnits(Map<String, String> values) {
    if (values != null) {
      values.replaceAll(ThemeValueValidator::normalizeSizeUnit);
    }
  }

  public static String normalizeSizeUnit(String key, String value) {
    if (StringUtils.isBlank(value)) {
      return value;
    } else if (isSizeKey(key) && UNITLESS_SIZE_PATTERN.matcher(value).matches()) {
      return value + "px";
    } else if (key.endsWith("BackgroundRadius")) {
      return UNITLESS_SIZE_TOKEN.matcher(value).replaceAll("$1px");
    } else {
      return value;
    }
  }

  /**
   * Checks every key and non-null value of a map; a null value is a removal
   * and is never checked.
   *
   * @param values a theme map, shared, light or dark
   * @throws IllegalArgumentException with the message code of the first
   *           refused key or value
   */
  public void validateValues(Map<String, String> values) {
    if (values != null) {
      values.forEach(this::validateValue);
    }
  }

  /**
   * @param key a theme key
   * @param value its value, null for a removal
   */
  public void validateValue(String key, String value) {
    if (StringUtils.isBlank(key) || !isKnownKey(key)) {
      throw new IllegalArgumentException(UNKNOWN_KEY_MESSAGE + key);
    }
    if (value == null) {
      return;
    }
    String lowerValue = value.toLowerCase(Locale.ROOT);
    if (FORBIDDEN_CHARACTERS.matcher(value).find() || lowerValue.contains("javascript") || lowerValue.contains("eval")) {
      throw new IllegalArgumentException(FORBIDDEN_VALUE_MESSAGE);
    }
    if (FONT_FAMILY.equals(key)) {
      if (StringUtils.isNotBlank(value) && !supportedFontFamilies.contains(value)) {
        throw new IllegalArgumentException(FONT_UNSUPPORTED_MESSAGE);
      }
    } else if (!isValidValue(key, value)) {
      throw new IllegalArgumentException(INVALID_VALUE_MESSAGE + key);
    }
  }

  /**
   * Compiles the template with the effective values of each version, so a
   * value that passes the grammar but not the compiler is refused before
   * anything is written.
   *
   * @param effectiveLight every variable's effective light value
   * @param effectiveDark every variable's effective dark value
   */
  public void validateCompiles(Map<String, String> effectiveLight, Map<String, String> effectiveDark) {
    if (compiler == null) {
      return;
    }
    for (Map<String, String> values : List.of(effectiveLight, effectiveDark)) {
      Map<String, String> compiled = new HashMap<>(values);
      // files are not Less variables
      compiled.keySet().removeIf(key -> isFileKey(key));
      try {
        compiler.compile(compiled);
      } catch (Exception e) { // NOSONAR the compiler's own exception types are the template engine's
        throw new IllegalArgumentException(COMPILATION_ERROR_MESSAGE, e);
      }
    }
  }

  static boolean isSizeKey(String key) {
    return key.contains("Margin") || key.endsWith("BorderSize") || key.endsWith("FontSize") || key.contains("BackgroundPadding")
           || key.contains("BorderRadius") || "borderRadius".equals(key);
  }

  static boolean hasBalancedParentheses(String value) {
    int depth = 0;
    for (char c : value.toCharArray()) {
      if (c == '(') {
        depth++;
      } else if (c == ')' && --depth < 0) {
        return false;
      }
    }
    return depth == 0;
  }

  private static boolean isBetween(int value, int min, int max) {
    return value >= min && value <= max;
  }

  private boolean isKnownKey(String key) {
    if (isFileKey(key)) {
      return isKnownKey(imageKeyOf(key)) && isImageKey(imageKeyOf(key));
    }
    return declaredVariables.contains(key) || PAGE_VARIABLES.contains(key);
  }

  private boolean isValidValue(String key, String value) { // NOSONAR one branch per grammar
    if (StringUtils.isBlank(value)) {
      return true;
    } else if (isFileKey(key)) {
      return FILE_ID_PATTERN.matcher(value).matches();
    } else if (TOP_BAR_STICKY.equals(key)) {
      return "true".equals(value) || "false".equals(value);
    } else if (PAGE_WIDTH.equals(key)) {
      Matcher width = PAGE_WIDTH_PATTERN.matcher(value);
      return width.matches() && (width.group(2) == null || isBetween(Integer.parseInt(width.group(2)), 100, 3000));
    } else if (PAGE_BACKGROUND_SIZE.equals(key)) {
      return PAGE_SIZES.contains(value);
    } else if (PAGE_BACKGROUND_POSITION.equals(key)) {
      return PAGE_POSITIONS.contains(value);
    } else if (PAGE_BACKGROUND_REPEAT.equals(key)) {
      return PAGE_REPEATS.contains(value);
    } else if (PAGE_BACKGROUND_ATTACHMENT.equals(key)) {
      return PAGE_ATTACHMENTS.contains(value);
    } else if (key.contains("Color")) {
      return COLOR_PATTERN.matcher(value).matches();
    } else if (key.endsWith("BackgroundRadius")) {
      return RADIUS_PATTERN.matcher(value).matches();
    } else if (isSizeKey(key)) {
      return SIZE_PATTERN.matcher(value).matches();
    } else if (isImageKey(key)) {
      return isValidBackgroundEffect(key, value);
    } else if (key.endsWith("BoxShadow")) {
      return BOX_SHADOW_PATTERN.matcher(value).matches() && hasBalancedParentheses(value);
    } else if (key.endsWith("FontWeight") || key.endsWith("FontStyle") || key.endsWith("BackgroundPosition")
               || key.endsWith("BackgroundSize") || key.endsWith("BackgroundRepeat") || key.endsWith("BackgroundAttachment")) {
      return KEYWORD_PATTERN.matcher(value).matches();
    }
    // a declared variable with no known grammar is refused rather than written verbatim
    return false;
  }

  /**
   * An image key holds only its gradient part: the file goes through its
   * companion key, so a url( layer is refused in any case, as are an empty
   * argument, a second gradient, and any gradient on the topbar, which offers
   * none.
   */
  private boolean isValidBackgroundEffect(String key, String value) {
    if (value.toLowerCase(Locale.ROOT).contains("url(")
        || !BACKGROUND_EFFECT_PATTERN.matcher(value).matches()
        || !hasBalancedParentheses(value)
        || EMPTY_ARGUMENT.matcher(value).find()) {
      return false;
    }
    Matcher gradients = GRADIENT_FUNCTION.matcher(value);
    int count = 0;
    while (gradients.find()) {
      count++;
    }
    return count <= 1 && (count == 0 || !TOP_BAR_BACKGROUND_IMAGE.equals(key));
  }

}
