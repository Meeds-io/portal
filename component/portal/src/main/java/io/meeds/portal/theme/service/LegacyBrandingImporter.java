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

import static io.meeds.portal.theme.constant.ThemeVariables.FILE_SUFFIX;
import static io.meeds.portal.theme.constant.ThemeVariables.PAGE_BACKGROUND_ATTACHMENT;
import static io.meeds.portal.theme.constant.ThemeVariables.PAGE_BACKGROUND_COLOR;
import static io.meeds.portal.theme.constant.ThemeVariables.PAGE_BACKGROUND_IMAGE;
import static io.meeds.portal.theme.constant.ThemeVariables.PAGE_BACKGROUND_POSITION;
import static io.meeds.portal.theme.constant.ThemeVariables.PAGE_BACKGROUND_REPEAT;
import static io.meeds.portal.theme.constant.ThemeVariables.PAGE_BACKGROUND_SIZE;
import static io.meeds.portal.theme.constant.ThemeVariables.PAGE_WIDTH;
import static io.meeds.portal.theme.constant.ThemeVariables.isImageKey;
import static io.meeds.portal.theme.constant.ThemeVariables.isShared;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.branding.BrandingServiceImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.portal.theme.model.LegacyBranding;

/**
 * Reads what the single-theme platform stored, for the one-time import into
 * the Default Theme. The Branding page used to send back every default it
 * loaded, so one save stored them all: the import keeps only what differs from
 * its configured or built-in default (#707070 counting as the old grey
 * default), the gradient part of an image value (its file goes to the
 * companion key), no topbar gradient, and every value the validator accepts;
 * so Auto and Automatic apply to what the administrator never chose.
 */
@Component
public class LegacyBrandingImporter {

  public static final String              TERTIARY_COLOR      = "tertiaryColor";

  public static final String              OLD_GREY            = "#707070";

  public static final String              NEW_GREY            = "#6b6b6b";

  private static final Log                LOG                 = ExoLogger.getLogger(LegacyBrandingImporter.class);

  private static final Pattern            IMAGE_LAYER         = Pattern.compile("url\\([^)]*(\\)|$)\\s*,?\\s*",
                                                                                Pattern.CASE_INSENSITIVE);

  /** Image key to the legacy setting holding its file id. */
  private static final Map<String, String> LEGACY_IMAGE_FILES = new LinkedHashMap<>();

  /** Page key to the legacy global setting holding its value. */
  private static final Map<String, String> LEGACY_PAGE_KEYS   = new LinkedHashMap<>();

  static {
    LEGACY_IMAGE_FILES.put("topBarBackgroundImage", BrandingServiceImpl.BRANDING_TOP_BAR_BG_ID_SETTING_KEY);
    LEGACY_IMAGE_FILES.put("sideBarBackgroundImage", BrandingServiceImpl.BRANDING_SIDEBAR_BG_ID_SETTING_KEY);
    LEGACY_IMAGE_FILES.put("drawerBackgroundImage", BrandingServiceImpl.BRANDING_DRAWER_BG_ID_SETTING_KEY);
    LEGACY_IMAGE_FILES.put("appBackgroundImage", BrandingServiceImpl.BRANDING_APP_BG_ID_SETTING_KEY);
    LEGACY_IMAGE_FILES.put("appTextTitleBackgroundImage", BrandingServiceImpl.BRANDING_APP_TITLE_BG_ID_KEY);
    LEGACY_IMAGE_FILES.put("appTextHeaderBackgroundImage", BrandingServiceImpl.BRANDING_APP_HEADER_BG_ID_KEY);
    LEGACY_IMAGE_FILES.put(PAGE_BACKGROUND_IMAGE, BrandingServiceImpl.BRANDING_PAGE_BG_ID_SETTING_KEY);

    LEGACY_PAGE_KEYS.put(PAGE_WIDTH, BrandingServiceImpl.BRANDING_PAGE_WIDTH_KEY);
    LEGACY_PAGE_KEYS.put(PAGE_BACKGROUND_COLOR, BrandingServiceImpl.BRANDING_PAGE_BG_COLOR_KEY);
    LEGACY_PAGE_KEYS.put(PAGE_BACKGROUND_IMAGE, BrandingServiceImpl.BRANDING_PAGE_BG_EFFECT_KEY);
    LEGACY_PAGE_KEYS.put(PAGE_BACKGROUND_SIZE, BrandingServiceImpl.BRANDING_PAGE_BG_SIZE_KEY);
    LEGACY_PAGE_KEYS.put(PAGE_BACKGROUND_POSITION, BrandingServiceImpl.BRANDING_PAGE_BG_POSITION_KEY);
    LEGACY_PAGE_KEYS.put(PAGE_BACKGROUND_REPEAT, BrandingServiceImpl.BRANDING_PAGE_BG_REPEAT_KEY);
    LEGACY_PAGE_KEYS.put(PAGE_BACKGROUND_ATTACHMENT, BrandingServiceImpl.BRANDING_PAGE_BG_ATTACHMENT_KEY);
  }

  @Autowired
  private SettingService                  settingService;

  @Autowired
  private BrandingService                 brandingService;

  private ThemeValueValidator             valueValidator;

  /**
   * @return the stored legacy branding, cleaned
   */
  public LegacyBranding read() {
    Map<String, String> stored = new HashMap<>();
    for (String variable : getValueValidator().getDeclaredVariables()) {
      if (!TERTIARY_COLOR.equals(variable)) {
        String value = settingValue(BrandingServiceImpl.BRANDING_CONTEXT, BrandingServiceImpl.BRANDING_SCOPE, variable);
        if (value != null) {
          stored.put(variable, value);
        }
      }
    }
    LEGACY_PAGE_KEYS.forEach((key, setting) -> {
      String value = settingValue(Context.GLOBAL, Scope.GLOBAL, setting);
      if (value != null) {
        stored.put(key, value);
      }
    });
    LegacyBranding legacy = clean(stored, brandingService.getDefaultThemeStyle());
    LEGACY_IMAGE_FILES.forEach((imageKey, setting) -> {
      Long fileId = settingLong(setting);
      if (fileId != null) {
        legacy.getImageFileIds().put(imageKey, fileId);
      }
    });
    legacy.setCompanyName(settingValue(Context.GLOBAL, Scope.GLOBAL, BrandingServiceImpl.BRANDING_COMPANY_NAME_SETTING_KEY));
    legacy.setLogoFileId(settingLong(BrandingServiceImpl.BRANDING_LOGO_ID_SETTING_KEY));
    legacy.setFaviconFileId(settingLong(BrandingServiceImpl.BRANDING_FAVICON_ID_SETTING_KEY));
    return legacy;
  }

  /**
   * The legacy tertiary color is no longer read; deleted at every start, so a
   * failed delete is harmless.
   */
  public void deleteTertiaryColor() {
    try {
      if (settingService.get(BrandingServiceImpl.BRANDING_CONTEXT, BrandingServiceImpl.BRANDING_SCOPE, TERTIARY_COLOR) != null) {
        settingService.remove(BrandingServiceImpl.BRANDING_CONTEXT, BrandingServiceImpl.BRANDING_SCOPE, TERTIARY_COLOR);
      }
    } catch (Exception e) {
      LOG.warn("The legacy tertiary color setting could not be deleted, the next start retries", e);
    }
  }

  /**
   * @param stored the stored legacy values, theme variables and page keys
   * @param defaults the configured or built-in defaults
   * @return the values to import into the Default Theme
   */
  LegacyBranding clean(Map<String, String> stored, Map<String, String> defaults) {
    Map<String, String> values = new HashMap<>(stored);
    values.values().removeIf(StringUtils::isBlank);
    BrandingServiceImpl.neutralizeTopBarGradient(values);
    LegacyBranding legacy = new LegacyBranding();
    values.forEach((key, value) -> {
      String cleaned = isImageKey(key) ? stripImageLayers(value) : value.trim();
      String defaultValue = PAGE_BACKGROUND_IMAGE.equals(key) ? defaults.get("pageBackgroundEffect") : defaults.get(key);
      Map<String, String> single = new HashMap<>(Map.of(key, cleaned));
      ThemeValueValidator.normalizeSizeUnits(single);
      String normalizedDefault = defaultValue == null ? null : ThemeValueValidator.normalizeSizeUnit(key, defaultValue);
      if (isDefault(single.get(key), normalizedDefault)
          || (isImageKey(key) && "none".equals(cleaned) && StringUtils.isBlank(defaultValue))) {
        return;
      }
      try {
        getValueValidator().validateValues(single);
      } catch (IllegalArgumentException e) {
        LOG.warn("Legacy branding value '{}' of {} is refused ({}), it is not imported", cleaned, key, e.getMessage());
        return;
      }
      (isShared(key) ? legacy.getSettings().getShared() : legacy.getSettings().getLight()).put(key, single.get(key));
    });
    return legacy;
  }

  static boolean isDefault(String value, String defaultValue) {
    if (StringUtils.isBlank(defaultValue)) {
      return false;
    }
    String color = normalizeColor(value);
    String defaultColor = normalizeColor(defaultValue);
    if (color != null && defaultColor != null) {
      return color.equals(defaultColor)
             || (OLD_GREY.equals(color) && (OLD_GREY.equals(defaultColor) || NEW_GREY.equals(defaultColor)));
    }
    return value.trim().equalsIgnoreCase(defaultValue.trim());
  }

  /**
   * @return #rrggbb or #rrggbbaa lower-cased, an opaque alpha dropped; null
   *         when the value is not a hex color
   */
  static String normalizeColor(String value) {
    String color = StringUtils.trimToEmpty(value).toLowerCase(Locale.ROOT);
    if (!color.matches("#([0-9a-f]{3,4}|[0-9a-f]{6}|[0-9a-f]{8})")) {
      return null;
    }
    if (color.length() <= 5) {
      StringBuilder expanded = new StringBuilder("#");
      for (char c : color.substring(1).toCharArray()) {
        expanded.append(c).append(c);
      }
      color = expanded.toString();
    }
    return color.length() == 9 && color.endsWith("ff") ? color.substring(0, 7) : color;
  }

  static String stripImageLayers(String value) {
    String effect = IMAGE_LAYER.matcher(value).replaceAll("").trim().replaceAll("(^,\\s*)|(\\s*,$)", "").trim();
    return StringUtils.isBlank(effect) ? "none" : effect;
  }

  static String fileKey(String imageKey) {
    return imageKey + FILE_SUFFIX;
  }

  private ThemeValueValidator getValueValidator() {
    if (valueValidator == null) {
      valueValidator = ThemeValueValidator.of(brandingService);
    }
    return valueValidator;
  }

  private String settingValue(Context context, Scope scope, String key) {
    SettingValue<?> value = settingService.get(context, scope, key);
    return value == null || value.getValue() == null || StringUtils.isBlank(value.getValue().toString()) ? null :
                                                                                                          value.getValue().toString();
  }

  private Long settingLong(String key) {
    String value = settingValue(Context.GLOBAL, Scope.GLOBAL, key);
    return StringUtils.isNumeric(value) ? Long.valueOf(value) : null;
  }

}
