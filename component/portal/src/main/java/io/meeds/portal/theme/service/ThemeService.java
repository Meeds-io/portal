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
import static io.meeds.portal.theme.constant.ThemeVariables.imageKeyOf;
import static io.meeds.portal.theme.constant.ThemeVariables.isFileKey;
import static io.meeds.portal.theme.constant.ThemeVariables.isImageKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.branding.model.Branding;
import org.exoplatform.portal.branding.model.BrandingFile;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.common.ContainerTransactional;
import io.meeds.portal.theme.constant.DisplayMode;
import io.meeds.portal.theme.constant.ModesOffered;
import io.meeds.portal.theme.constant.ThemeOrigin;
import io.meeds.portal.theme.constant.ThemeVariables;
import io.meeds.portal.theme.exception.ThemePublishConflictException;
import io.meeds.portal.theme.model.BrandingFilePublication;
import io.meeds.portal.theme.model.BrandingIdentityPublication;
import io.meeds.portal.theme.model.BrandingPublication;
import io.meeds.portal.theme.model.BrandingState;
import io.meeds.portal.theme.model.ConfiguredTheme;
import io.meeds.portal.theme.model.ImageBytes;
import io.meeds.portal.theme.model.PlannedTheme;
import io.meeds.portal.theme.model.PublicationPlan;
import io.meeds.portal.theme.model.PublicationResult;
import io.meeds.portal.theme.model.ResolvedTheme;
import io.meeds.portal.theme.model.Theme;
import io.meeds.portal.theme.model.ThemePublication;
import io.meeds.portal.theme.model.ThemeReference;
import io.meeds.portal.theme.model.ThemeSettings;
import io.meeds.portal.theme.storage.ThemeFileStorage;
import io.meeds.portal.theme.storage.ThemeStorage;
import io.meeds.portal.theme.util.BrandingImageValidator;
import io.meeds.portal.theme.util.ThemeColorCalculator;
import io.meeds.portal.theme.util.ThemeMapper;
import io.meeds.portal.theme.util.ThemeValueResolver;

/**
 * The themes, the branding identity and the modes offered to users: their
 * reads, and the publication of the Branding and Theme page. Every write
 * checks that its user is a platform administrator, takes the branding state
 * row's lock first, compares and increments its version.
 */
@Service
public class ThemeService {

  public static final long    DEFAULT_THEME_ID                = 1L;

  public static final String  DEFAULT_THEME_CONFIG_KEY        = "default";

  public static final String  MODES_OFFERED_UPDATED_EVENT     = "branding.modesOffered.updated";

  public static final String  MODES_OFFERED_DATA_KEY          = "modesOffered";

  public static final String  THEME_IN_USE_ID_DATA_KEY        = "themeInUseId";

  public static final String  DISPLAY_MODE_UPDATED_EVENT      = "branding.displayMode.updated";

  public static final String  DISPLAY_MODE_NOT_OFFERED_MESSAGE = "branding.displayMode.notOffered";

  public static final String  DISPLAY_MODE_KEY                = "displayMode";

  public static final Scope   DISPLAY_MODE_SCOPE              = Scope.APPLICATION.id("Branding");

  public static final String  PUBLISH_CONFLICT_MESSAGE        = "branding.publish.conflict";

  public static final String  PUBLISH_FAILED_MESSAGE          = "branding.publish.failed";

  public static final String  NAME_ALREADY_EXISTS_MESSAGE     = "theme.name.alreadyExists";

  public static final String  NAME_MANDATORY_MESSAGE          = "theme.name.mandatory";

  public static final String  NAME_TOO_LONG_MESSAGE           = "theme.name.tooLong";

  public static final String  DEFAULT_THEME_RENAME_MESSAGE    = "theme.defaultTheme.rename";

  public static final String  NOT_DELETABLE_MESSAGE           = "theme.notDeletable";

  public static final String  THEME_IN_USE_INVALID_MESSAGE    = "theme.themeInUse.invalid";

  public static final String  FILE_REFERENCE_INVALID_MESSAGE  = "theme.file.invalidReference:";

  public static final String  COMPANY_NAME_TOO_LONG_MESSAGE   = "branding.companyName.tooLong";

  public static final String  WRONG_VERSION_MESSAGE           = "branding.theme.wrongVersion:";

  public static final String  LOGO_COLUMN                     = "logo";

  public static final String  DARK_LOGO_COLUMN                = "darkLogo";

  public static final String  FAVICON_COLUMN                  = "favicon";

  public static final String  DARK_FAVICON_COLUMN             = "darkFavicon";

  public static final String  UPLOAD_SUFFIX                   = "Upload";

  /** The upload id the former Branding page sent to remove a file. */
  public static final String  LEGACY_RESET_UPLOAD_ID          = "0";

  public static final int     NAME_MAX_LENGTH                 = 200;

  /** At most one state re-read per node in this window, whatever the stylesheet requests ask. */
  public static final long    STATE_REFRESH_WINDOW_MS         = 5000;

  /** Key of the configured name in a bound theme's configured settings. */
  public static final String  CONFIG_NAME_KEY                 = "name";

  private static final Log    LOG                             = ExoLogger.getLogger(ThemeService.class);

  private static final String NAME_KEY_CONSTRAINT             = "NAME_KEY";


  @Autowired
  private ThemeStorage        themeStorage;

  @Autowired
  private ThemeFileStorage    themeFileStorage;

  @Autowired
  private BrandingService     brandingService;

  @Autowired
  private ThemeConfiguration  themeConfiguration;

  @Autowired
  private UserACL             userAcl;

  @Autowired
  private ListenerService     listenerService;

  @Autowired
  private SettingService      settingService;

  private ThemeValueValidator valueValidator;

  private Long                configurationHash;

  private final AtomicLong    lastStateRefresh               = new AtomicLong();

  private LongSupplier        clock                          = System::currentTimeMillis;

  /**
   * @return every theme, sorted by id, as stored
   */
  public List<Theme> getThemes() {
    return themeStorage.getThemes().stream().map(ThemeService::copy).toList();
  }

  /**
   * @param id a theme id
   * @return the theme as stored
   * @throws ObjectNotFoundException when no theme has this id
   */
  public Theme getTheme(long id) throws ObjectNotFoundException {
    Theme theme = themeStorage.getTheme(id);
    if (theme == null) {
      throw new ObjectNotFoundException("Theme " + id + " not found");
    }
    return copy(theme);
  }

  /**
   * @return the theme in use: the one an administrator selected, else the one
   *         exo.branding.themeInUse names, else the Default Theme
   */
  public Theme getThemeInUse() {
    BrandingState state = themeStorage.getState();
    Theme theme = state.getThemeInUseId() == null ? null : themeStorage.getTheme(state.getThemeInUseId());
    String configuredThemeInUse = themeConfiguration.getConfiguredThemeInUse();
    if (theme == null && !DEFAULT_THEME_CONFIG_KEY.equals(configuredThemeInUse)) {
      theme = getThemes().stream()
                         .filter(t -> StringUtils.equals(configuredThemeInUse, t.getConfigKey()))
                         .findFirst()
                         .orElse(null);
    }
    if (theme == null) {
      theme = themeStorage.getTheme(DEFAULT_THEME_ID);
    }
    return copy(theme);
  }

  /**
   * @return the modes offered to users: the administrator's choice, else
   *         exo.branding.modesOffered, else both
   */
  public ModesOffered getModesOffered() {
    return effectiveModes(themeStorage.getState().getModesOffered());
  }

  /**
   * The display mode a user chose, read leniently: it is read on every page
   * render, so a value stored before an upgrade, or before the modes last
   * went back to both, that does not parse reads as Automatic.
   *
   * @param username a user, null for an anonymous page
   * @return the user's stored mode, Automatic by default
   */
  public DisplayMode getDisplayMode(String username) {
    if (StringUtils.isBlank(username)) {
      return DisplayMode.AUTOMATIC;
    }
    try {
      SettingValue<?> value = settingService.get(Context.USER.id(username), DISPLAY_MODE_SCOPE, DISPLAY_MODE_KEY);
      return parseDisplayMode(value == null || value.getValue() == null ? null : value.getValue().toString(),
                              themeStorage.getState().getModesGeneration());
    } catch (Exception e) {
      LOG.debug("Display mode of {} could not be read, Automatic applies", username, e);
      return DisplayMode.AUTOMATIC;
    }
  }

  /**
   * @param username the current user, as the REST layer resolved it from the
   *          security context
   * @param mode the chosen mode
   */
  public void setDisplayMode(String username, DisplayMode mode) {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException("branding.displayMode.user.mandatory");
    }
    if (mode == null) {
      throw new IllegalArgumentException("branding.displayMode.mandatory");
    }
    if (getModesOffered() != ModesOffered.LIGHT_AND_DARK) {
      // a single mode offered is every page's mode: there is no choice to store
      throw new IllegalArgumentException(DISPLAY_MODE_NOT_OFFERED_MESSAGE);
    }
    String value = mode.name() + "@" + themeStorage.readState().getModesGeneration();
    SettingValue<?> stored = settingService.get(Context.USER.id(username), DISPLAY_MODE_SCOPE, DISPLAY_MODE_KEY);
    if (stored != null && stored.getValue() != null && value.equals(stored.getValue().toString())) {
      return;
    }
    settingService.set(Context.USER.id(username), DISPLAY_MODE_SCOPE, DISPLAY_MODE_KEY, SettingValue.create(value));
    broadcast(DISPLAY_MODE_UPDATED_EVENT, username, mode.name());
  }

  /**
   * The mode a page renders in: the single mode offered, else the user's
   * choice, Automatic for an anonymous page.
   *
   * @param username the signed-in user, null for an anonymous page
   * @return the page's effective mode
   */
  public DisplayMode getEffectiveDisplayMode(String username) {
    ModesOffered modes = getModesOffered();
    if (modes == ModesOffered.LIGHT) {
      return DisplayMode.LIGHT;
    } else if (modes == ModesOffered.DARK) {
      return DisplayMode.DARK;
    }
    return getDisplayMode(username);
  }

  static DisplayMode parseDisplayMode(String value, int generation) {
    if (StringUtils.isBlank(value)) {
      return DisplayMode.AUTOMATIC;
    }
    String[] parts = value.trim().split("@", -1);
    try {
      if (parts.length != 2 || Integer.parseInt(parts[1]) < generation) {
        return DisplayMode.AUTOMATIC;
      }
      return DisplayMode.valueOf(parts[0]);
    } catch (IllegalArgumentException e) {
      return DisplayMode.AUTOMATIC;
    }
  }

  /**
   * @return the file the dark version shows as the logo: the dark logo, else
   *         the light one; null when there is none
   */
  public Long getEffectiveDarkLogoFileId() {
    BrandingState state = getState();
    return state.getDarkLogoFileId() == null ? state.getLogoFileId() : state.getDarkLogoFileId();
  }

  /**
   * @return the file the dark version shows as the favicon: the dark favicon,
   *         else the light one; null when there is none
   */
  public Long getEffectiveDarkFaviconFileId() {
    BrandingState state = getState();
    return state.getDarkFaviconFileId() == null ? state.getFaviconFileId() : state.getDarkFaviconFileId();
  }

  /**
   * @return the branding state as stored, its version included
   */
  public BrandingState getState() {
    BrandingState state = themeStorage.getState();
    return new BrandingState(state.getVersion(),
                             state.isLegacyImported(),
                             state.getCompanyName(),
                             state.getLogoFileId(),
                             state.getDarkLogoFileId(),
                             state.getFaviconFileId(),
                             state.getDarkFaviconFileId(),
                             state.getThemeInUseId(),
                             state.getModesOffered(),
                             state.getLastEffectiveThemeId(),
                             state.getLastEffectiveModes(),
                             state.getModesGeneration());
  }

  /**
   * The page's Apply: every change since the page loaded, in one
   * transaction. Values and uploads are checked, and new files written,
   * before it; the files nothing uses any more are deleted after its commit,
   * the new ones when it does not commit.
   *
   * @param publication what the page sends
   * @param username the user applying it
   * @return the new version of the published state
   * @throws IllegalAccessException when the user is not a platform
   *           administrator
   * @throws ObjectNotFoundException when a theme it names does not exist
   * @throws IllegalArgumentException with a message code, for a refused value
   * @throws ThemePublishConflictException when the state changed since the
   *           page loaded
   */
  public long publish(BrandingPublication publication, String username) throws IllegalAccessException,
                                                                       ObjectNotFoundException {
    checkAdministrator(username);
    return publishChecked(publication, username, false);
  }

  /**
   * Saves the custom CSS appended to the theme stylesheet, by its own
   * request, apart from a publication. The value is validated before any
   * write, then stored as a setting, then the stylesheet stamp moves by one
   * in its own transaction (the state row locked first), so that every node
   * recompiles. A failed setting write leaves the version unchanged and
   * throws; a stamp that fails to move after the setting was written throws
   * too, the next save moving it. It carries no client version: the CSS is
   * a single value that the last save wins.
   *
   * @param customCss the CSS, blank to remove it
   * @param username the user saving it
   * @return the new version of the published state
   * @throws IllegalAccessException when the user is not a platform
   *           administrator
   * @throws IllegalArgumentException {@code branding.customCss.forbiddenValue}
   *           when the CSS holds a forbidden construct
   * @throws IllegalStateException {@link #PUBLISH_FAILED_MESSAGE} when the
   *           setting or the version could not be written
   */
  public long saveCustomCss(String customCss, String username) throws IllegalAccessException {
    checkAdministrator(username);
    brandingService.validateCustomCss(customCss);
    long version;
    try {
      brandingService.updateCustomCss(customCss);
      version = moveVersionInTransaction();
    } catch (RuntimeException e) {
      throw translate(e);
    }
    themeStorage.evictAll();
    broadcast(BrandingService.BRANDING_UPDATED_EVENT, username, version);
    return version;
  }

  /**
   * The stamp move of {@link #saveCustomCss}: the state row locked, its
   * version incremented, nothing else written.
   */
  @ContainerTransactional(atomic = true)
  long moveVersionInTransaction() {
    BrandingState state = themeStorage.lockState();
    state.setVersion(state.getVersion() + 1);
    return themeStorage.saveState(state).getVersion();
  }

  /**
   * The deprecated single-theme writers (the Branding page's former PUT, the
   * single-field updates): their values are merged into the theme in use's
   * light and shared settings, validated and written under the same lock as
   * a publication, which increments the version. An empty value resets its
   * option, an absent one is kept. The former page sends back every value it
   * loaded: a value equal to its default is not stored, nor one equal to the
   * value the theme resolves for an option it does not store (an Auto text
   * colour), which stays Auto. Their signatures carry no user: the REST layer
   * checks the role, as it always did.
   *
   * @param branding the former page's branding, or a single-field update
   * @param themeStyleOnly true when only the theme values are written
   * @return the new version
   */
  public long publishLegacy(Branding branding, boolean themeStyleOnly) throws ObjectNotFoundException {
    try {
      return publishChecked(legacyPublication(branding, themeStyleOnly), null, true);
    } catch (IllegalStateException e) {
      if (!isConflict(e)) {
        throw e;
      }
      // the conflict evicted the node's snapshot: these writers carry no client version, so the maps are rebuilt
      // from a fresh read and the write is tried once more
      return publishChecked(legacyPublication(branding, themeStyleOnly), null, false);
    }
  }

  private BrandingPublication legacyPublication(Branding branding, boolean themeStyleOnly) {
    BrandingState state = getState();
    Theme theme = getThemeInUse();
    Map<String, String> submitted = new HashMap<>();
    if (branding.getThemeStyle() != null) {
      branding.getThemeStyle().forEach((key, value) -> {
        if (StringUtils.isBlank(value)) {
          submitted.put(key, null);
        } else {
          submitted.put(key, ThemeVariables.isImageKey(key) ? LegacyBrandingImporter.stripImageLayers(value) : value.trim());
        }
      });
    }
    if (!themeStyleOnly) {
      putSubmitted(submitted, ThemeVariables.PAGE_WIDTH, branding.getPageWidth());
      putSubmitted(submitted, ThemeVariables.PAGE_BACKGROUND_COLOR, branding.getPageBackgroundColor());
      putSubmitted(submitted, ThemeVariables.PAGE_BACKGROUND_IMAGE, branding.getPageBackgroundEffect());
      putSubmitted(submitted, ThemeVariables.PAGE_BACKGROUND_SIZE, branding.getPageBackgroundSize());
      putSubmitted(submitted, ThemeVariables.PAGE_BACKGROUND_POSITION, branding.getPageBackgroundPosition());
      putSubmitted(submitted, ThemeVariables.PAGE_BACKGROUND_REPEAT, branding.getPageBackgroundRepeat());
      putSubmitted(submitted, ThemeVariables.PAGE_BACKGROUND_ATTACHMENT, branding.getPageBackgroundAttachment());
      putUpload(submitted, "topBarBackgroundImage", branding.getTopBarBackground());
      putUpload(submitted, "sideBarBackgroundImage", branding.getSideBarBackground());
      putUpload(submitted, "drawerBackgroundImage", branding.getDrawerBackground());
      putUpload(submitted, "appBackgroundImage", branding.getAppBackground());
      putUpload(submitted, "appTextTitleBackgroundImage", branding.getAppTextTitleBackground());
      putUpload(submitted, "appTextHeaderBackgroundImage", branding.getAppTextHeaderBackground());
      putUpload(submitted, ThemeVariables.PAGE_BACKGROUND_IMAGE, branding.getPageBackground());
    }
    Map<String, String> defaults = brandingService.getDefaultThemeStyle();
    ThemeSettings stored = theme.getSettings();
    Map<String, String> shared = new HashMap<>(stored.getShared());
    Map<String, String> light = new HashMap<>(stored.getLight());
    Map<String, String> resolved = resolve(theme).getLight();
    submitted.forEach((key, value) -> {
      Map<String, String> target = ThemeVariables.isShared(key) ? shared : light;
      String defaultValue = ThemeVariables.PAGE_BACKGROUND_IMAGE.equals(key) ? defaults.get("pageBackgroundEffect") : defaults.get(key);
      if (value == null) {
        target.remove(key);
      } else if (!key.endsWith(UPLOAD_SUFFIX) && LegacyBrandingImporter.isDefault(value, defaultValue)) {
        target.remove(key);
      } else if (!key.endsWith(UPLOAD_SUFFIX) && !getValueValidator().getDeclaredVariables().contains(key)
                 && !ThemeVariables.PAGE_VARIABLES.contains(key)) {
        LOG.debug("Legacy branding key {} is not a theme variable, it is ignored", key);
      } else if (key.endsWith(UPLOAD_SUFFIX) || target.containsKey(key) || !sameValue(value, resolved.get(key))) {
        target.put(key, value);
      }
    });
    BrandingPublication publication = new BrandingPublication();
    publication.setVersion(state.getVersion());
    publication.getThemes()
               .add(new ThemePublication(theme.getId(), null, theme.getName(), shared, light, null, null));
    if (!themeStyleOnly) {
      publication.setBranding(new BrandingIdentityPublication(branding.getCompanyName(),
                                                              legacyFile(branding.getLogo()),
                                                              null,
                                                              legacyFile(branding.getFavicon()),
                                                              null));
    }
    return publication;
  }

  private long publishChecked(BrandingPublication publication,
                              String username,
                              boolean keepUploadsOnConflict) throws ObjectNotFoundException {
    if (publication == null) {
      throw new IllegalArgumentException("branding.publication.mandatory");
    }
    PublicationPlan plan = new PublicationPlan();
    PublicationResult result;
    try {
      plan(publication, plan);
      result = publishInTransaction(plan);
    } catch (ObjectNotFoundException e) {
      plan.getNewFileIds().forEach(themeFileStorage::deleteFile);
      throw e;
    } catch (RuntimeException e) {
      plan.getNewFileIds().forEach(themeFileStorage::deleteFile);
      RuntimeException translated = translate(e);
      if (isConflict(translated)) {
        // this node's snapshot is behind the database: evicted, so that the page's reload reads the database instead of
        // conflicting again until the snapshot expires
        themeStorage.evictAll();
        if (!keepUploadsOnConflict) {
          removeUploads(plan);
        }
      }
      throw translated;
    }
    removeUploads(plan);
    afterCommit(result, username);
    return result.getVersion();
  }

  private void removeUploads(PublicationPlan plan) {
    plan.getConsumedUploadIds().forEach(themeFileStorage::removeUpload);
  }

  /**
   * A page field of the former page: null leaves it out, an empty value
   * resets it.
   */
  private static void putSubmitted(Map<String, String> map, String key, String value) {
    if (value != null) {
      map.put(key, StringUtils.isBlank(value) ? null : value.trim());
    }
  }

  private static boolean sameValue(String value, String resolved) {
    return resolved != null && (value.equals(resolved.trim()) || ThemeColorCalculator.sameColor(value, resolved));
  }

  private static void putUpload(Map<String, String> map, String imageKey, BrandingFile file) {
    if (file == null || StringUtils.isBlank(file.getUploadId())) {
      return;
    }
    if (LEGACY_RESET_UPLOAD_ID.equals(file.getUploadId())) {
      // the former page's removal of a background image
      map.put(imageKey + FILE_SUFFIX, null);
    } else {
      map.put(imageKey + UPLOAD_SUFFIX, file.getUploadId());
    }
  }

  private static BrandingFilePublication legacyFile(BrandingFile file) {
    if (file == null || StringUtils.isBlank(file.getUploadId())) {
      return null;
    }
    return LEGACY_RESET_UPLOAD_ID.equals(file.getUploadId()) ? new BrandingFilePublication(null, true) :
                                                              new BrandingFilePublication(file.getUploadId(), false);
  }

  /**
   * The transaction of a publication: rows only, the state row locked first.
   */
  @ContainerTransactional(atomic = true)
  PublicationResult publishInTransaction(PublicationPlan plan) throws ObjectNotFoundException {
    return writePublication(plan);
  }

  /**
   * The effective values of one version of stored and configured settings,
   * over the configured defaults; the Less template's own default applies to
   * a variable absent from the result.
   *
   * @param settings a theme's stored settings
   * @param configSettings its configured settings, null for an unbound theme
   * @param dark true for the dark version
   * @return variable to value, null-free
   */
  public Map<String, String> getEffectiveValues(ThemeSettings settings, ThemeSettings configSettings, boolean dark) {
    Map<String, String> values = new HashMap<>(brandingService.getDefaultThemeStyle());
    if (configSettings != null) {
      values.putAll(configSettings.getShared());
      values.remove(CONFIG_NAME_KEY);
      values.putAll(configSettings.getLight());
    }
    values.putAll(settings.getShared());
    values.putAll(settings.getLight());
    if (dark) {
      if (configSettings != null) {
        values.putAll(configSettings.getDark());
      }
      values.putAll(settings.getDark());
    }
    values.values().removeIf(Objects::isNull);
    return values;
  }

  /**
   * The preview of a theme being edited, unsaved edits included: an existing
   * theme or a Start-from copy, its submitted maps over its stored ones (a
   * null value removing a key), resolved for both versions. Nothing is
   * stored.
   *
   * @param edited the theme as the page holds it
   * @param username the user previewing it
   * @return both versions' effective values and contrast ratios
   * @throws IllegalAccessException when the user is not a platform
   *           administrator
   * @throws ObjectNotFoundException when the theme or its source does not
   *           exist
   */
  public ResolvedTheme resolve(ThemePublication edited, String username) throws IllegalAccessException, ObjectNotFoundException {
    checkAdministrator(username);
    if (edited == null) {
      throw new IllegalArgumentException("branding.theme.mandatory");
    }
    Theme theme;
    if (edited.getId() != null) {
      theme = getTheme(edited.getId());
    } else {
      Theme source = edited.getStartFrom() == null || edited.getStartFrom().getId() == null ? null :
                                                                                              getTheme(edited.getStartFrom()
                                                                                                             .getId());
      theme = new Theme(0, edited.getName(), false, ThemeOrigin.UI, null, baseOf(source), null, null);
    }
    ThemeSettings stored = theme.getSettings();
    theme.setSettings(new ThemeSettings(edited.getShared() == null ? stored.getShared() : merge(stored.getShared(), edited.getShared()),
                                        edited.getLight() == null ? stored.getLight() : merge(stored.getLight(), edited.getLight()),
                                        edited.getDark() == null ? stored.getDark() : merge(stored.getDark(), edited.getDark())));
    return resolve(theme);
  }

  /**
   * @param themeId a theme
   * @param imageKey one of its image keys
   * @param dark true for the dark version, which shares the light image
   *          unless it has its own
   * @return the file id the version shows, null when it shows no image
   * @throws ObjectNotFoundException when the theme does not exist or the key
   *           is no image key
   */
  public Long getThemeImageFileId(long themeId, String imageKey, boolean dark) throws ObjectNotFoundException {
    if (imageKey == null || !ThemeVariables.isImageKey(imageKey)) {
      throw new ObjectNotFoundException("No theme image " + imageKey);
    }
    ThemeSettings settings = getTheme(themeId).getSettings();
    String fileKey = imageKey + FILE_SUFFIX;
    String fileId = dark ? settings.getDark().get(fileKey) : null;
    if (fileId == null) {
      fileId = settings.getLight().get(fileKey);
    }
    return StringUtils.isNumeric(fileId) ? Long.valueOf(fileId) : null;
  }

  /**
   * @param fileId a branding file
   * @return its bytes and detected type, cached per node
   */
  public ImageBytes readImage(long fileId) {
    return themeFileStorage.readImage(fileId);
  }

  /**
   * Evicts the cached state and themes, at most once per window, so that the
   * next read comes from the database: the stylesheet's v= comes from anonymous
   * clients, and a forged one must not cost a database read per request. The
   * themes go with the state, since the stylesheet pairs the stamp it reads
   * with the values it compiles.
   *
   * @return true when the snapshot was evicted
   */
  public boolean refreshState() {
    long now = clock.getAsLong();
    long last = lastStateRefresh.get();
    if (now - last < STATE_REFRESH_WINDOW_MS || !lastStateRefresh.compareAndSet(last, now)) {
      return false;
    }
    themeStorage.evictAll();
    return true;
  }

  /**
   * @param clock the time {@link #refreshState} reads, in milliseconds
   */
  void setClock(LongSupplier clock) {
    this.clock = clock;
  }

  /**
   * @return a hash of the resolved configuration (configured themes, the
   *         Default Theme's configured values, theme in use, modes), computed
   *         once: the configuration is boot-time, and a value changed at a
   *         restart must change the stylesheet's stamp
   */
  public long getConfigurationHash() {
    if (configurationHash == null) {
      // a digest of a canonical form: the same configuration gives the same stamp on every node and at every start
      StringBuilder canonical = new StringBuilder();
      for (ConfiguredTheme theme : themeConfiguration.getConfiguredThemes()) {
        canonical.append(theme.getKey()).append('|').append(theme.getName()).append('|').append(canonical(theme.getSettings())).append('\n');
      }
      canonical.append(new java.util.TreeMap<>(themeConfiguration.getDefaultThemeDarkValues()))
               .append('|')
               .append(themeConfiguration.getConfiguredThemeInUse())
               .append('|')
               .append(themeConfiguration.getConfiguredModesOffered().name())
               .append('|')
               .append(new java.util.TreeMap<>(brandingService.getDefaultThemeStyle()));
      java.util.zip.CRC32 crc = new java.util.zip.CRC32();
      crc.update(canonical.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      configurationHash = crc.getValue();
    }
    return configurationHash;
  }

  static String canonical(ThemeSettings settings) {
    return settings == null ? "" :
                            new java.util.TreeMap<>(settings.getShared()) + "|" + new java.util.TreeMap<>(settings.getLight()) + "|"
                                + new java.util.TreeMap<>(settings.getDark());
  }

  /**
   * @param theme a theme as stored, or as being edited
   * @return both versions' effective values, Auto and Automatic computed,
   *         with their contrast ratios
   */
  public ResolvedTheme resolve(Theme theme) {
    ThemeSettings configured = getConfiguredSettings(theme);
    Map<String, String> light = new HashMap<>();
    Map<String, String> dark = new HashMap<>();
    if (configured != null) {
      light.putAll(configured.getShared());
      light.remove(CONFIG_NAME_KEY);
      light.putAll(configured.getLight());
    }
    light.putAll(theme.getSettings().getShared());
    light.putAll(theme.getSettings().getLight());
    light.values().removeIf(Objects::isNull);
    if (!theme.isDefaultTheme()) {
      // a variable whose light value is the Default Theme's takes its configured dark value too
      themeConfiguration.getDefaultThemeDarkValues().forEach((key, value) -> {
        if (!light.containsKey(key)) {
          dark.put(key, value);
        }
      });
    }
    if (configured != null) {
      dark.putAll(configured.getDark());
    }
    dark.putAll(theme.getSettings().getDark());
    dark.values().removeIf(Objects::isNull);
    return ThemeValueResolver.resolve(getResolutionDefaults(), light, dark);
  }

  /**
   * @return the template's built-in literals overridden by the configured
   *         defaults that are set: every variable the stylesheet emits, so
   *         each one has a dark value and every text an Auto one
   */
  Map<String, String> getResolutionDefaults() {
    return brandingService.getResolutionDefaults();
  }

  /**
   * @param theme a theme
   * @return its configured values: a bound theme's snapshot, the Default
   *         Theme's configured dark values (its light ones are the configured
   *         defaults every theme resolves to), null for any other theme
   */
  public ThemeSettings getConfiguredSettings(Theme theme) {
    if (theme.isDefaultTheme()) {
      return new ThemeSettings(new HashMap<>(), new HashMap<>(), new HashMap<>(themeConfiguration.getDefaultThemeDarkValues()));
    }
    return ThemeMapper.copy(theme.getConfigSettings());
  }

  PublicationResult writePublication(PublicationPlan plan) throws ObjectNotFoundException { // NOSONAR the write order of the note
    BrandingState state = themeStorage.lockState();
    if (state.getVersion() != plan.getVersion()) {
      throw new ThemePublishConflictException();
    }
    PublicationResult result = new PublicationResult();
    Map<Long, Theme> rows = new HashMap<>();
    themeStorage.getThemeRows().forEach(row -> rows.put(row.getId(), row));
    for (Long id : plan.getDeletedThemeIds()) {
      Theme row = rows.get(id);
      if (row == null) {
        throw new ObjectNotFoundException("Theme " + id + " not found");
      } else if (row.getOrigin() != ThemeOrigin.UI) {
        throw new IllegalArgumentException(NOT_DELETABLE_MESSAGE);
      }
    }
    // (1) the theme in use moves to the Default Theme before it is deleted
    if (state.getThemeInUseId() != null && plan.getDeletedThemeIds().contains(state.getThemeInUseId())) {
      state.setThemeInUseId(DEFAULT_THEME_ID);
      state = themeStorage.saveState(state);
    }
    // (2) the deleted rows
    for (Long id : plan.getDeletedThemeIds()) {
      Theme row = rows.remove(id);
      result.getUnusedFileIds().addAll(fileIds(row.getSettings()));
      themeStorage.deleteTheme(row.getId());
    }
    // (3) a renamed row frees its name before any final name is written
    for (PlannedTheme planned : plan.getThemes()) {
      Theme row = planned.getId() == null ? null : rows.get(planned.getId());
      if (row != null && !StringUtils.equals(ThemeStorage.nameKey(planned.getName()), ThemeStorage.nameKey(row.getName()))) {
        themeStorage.freeName(row.getId());
      }
    }
    // (4) final names, maps and new rows
    Map<String, Long> clientIds = new HashMap<>();
    for (PlannedTheme planned : plan.getThemes()) {
      Theme row;
      if (planned.getId() == null) {
        row = new Theme(0, null, false, ThemeOrigin.UI, null, null, null, null);
      } else {
        row = rows.get(planned.getId());
        if (row == null) {
          throw new ObjectNotFoundException("Theme " + planned.getId() + " not found");
        }
        Set<Long> previous = fileIds(row.getSettings());
        previous.removeAll(fileIds(planned.getSettings()));
        result.getUnusedFileIds().addAll(previous);
        updateNameSetInUI(row, planned.getName());
      }
      row.setName(planned.getName().trim());
      row.setSettings(ThemeMapper.copy(planned.getSettings()));
      row = themeStorage.saveTheme(row);
      rows.put(row.getId(), row);
      if (planned.getClientId() != null) {
        clientIds.put(planned.getClientId(), row.getId());
      }
    }
    // (5) the theme in use
    if (plan.getThemeInUse() != null) {
      Long themeInUseId = plan.getThemeInUse().getId() != null ? plan.getThemeInUse().getId() :
                                                              clientIds.get(plan.getThemeInUse().getClientId());
      if (themeInUseId == null || !rows.containsKey(themeInUseId)) {
        throw new IllegalArgumentException(THEME_IN_USE_INVALID_MESSAGE);
      }
      state.setThemeInUseId(themeInUseId);
    }
    writeIdentity(plan, state, result);
    ModesOffered previousModes = effectiveModes(state.getModesOffered());
    if (plan.getModesOffered() != null) {
      state.setModesOffered(plan.getModesOffered());
    }
    ModesOffered modes = effectiveModes(state.getModesOffered());
    if (previousModes != ModesOffered.LIGHT_AND_DARK && modes == ModesOffered.LIGHT_AND_DARK) {
      // every user's stored choice of an older generation reads as Automatic
      state.setModesGeneration(state.getModesGeneration() + 1);
    }
    state.setLastEffectiveModes(modes);
    state.setVersion(state.getVersion() + 1);
    state = themeStorage.saveState(state);
    result.setVersion(state.getVersion());
    result.setModesChanged(previousModes != modes);
    result.setModesOffered(modes);
    return result;
  }

  void plan(BrandingPublication publication, PublicationPlan plan) throws ObjectNotFoundException {
    plan.setVersion(publication.getVersion());
    plan.setModesOffered(publication.getModesOffered());
    plan.setThemeInUse(publication.getThemeInUse());
    if (publication.getDeletedThemeIds() != null) {
      publication.getDeletedThemeIds().stream().filter(Objects::nonNull).distinct().forEach(plan.getDeletedThemeIds()::add);
    }
    Map<Long, Theme> themes = new HashMap<>();
    getThemes().forEach(theme -> themes.put(theme.getId(), theme));
    planIdentity(publication.getBranding(), plan);
    Set<String> clientIds = new HashSet<>();
    if (publication.getThemes() != null) {
      for (ThemePublication themePublication : publication.getThemes()) {
        if (themePublication == null
            || (themePublication.getId() != null && plan.getDeletedThemeIds().contains(themePublication.getId()))) {
          continue;
        }
        PlannedTheme planned = planTheme(themePublication, themes, plan);
        if (planned.getClientId() != null && !clientIds.add(planned.getClientId())) {
          throw new IllegalArgumentException("theme.clientId.duplicated");
        }
        plan.getThemes().add(planned);
      }
    }
    checkNames(plan, themes);
    ThemeReference themeInUse = plan.getThemeInUse();
    if (themeInUse != null
        && ((themeInUse.getId() == null && !clientIds.contains(themeInUse.getClientId()))
            || (themeInUse.getId() != null
                && (!themes.containsKey(themeInUse.getId()) || plan.getDeletedThemeIds().contains(themeInUse.getId()))))) {
      throw new IllegalArgumentException(THEME_IN_USE_INVALID_MESSAGE);
    }
  }

  ModesOffered effectiveModes(ModesOffered stored) {
    return stored != null ? stored : themeConfiguration.getConfiguredModesOffered();
  }


  ThemeValueValidator getValueValidator() {
    if (valueValidator == null) {
      valueValidator = ThemeValueValidator.of(brandingService);
    }
    return valueValidator;
  }


  private PlannedTheme planTheme(ThemePublication publication, Map<Long, Theme> themes, PublicationPlan plan) throws ObjectNotFoundException {
    Theme existing = null;
    Theme source = null;
    if (publication.getId() != null) {
      existing = themes.get(publication.getId());
      if (existing == null) {
        throw new ObjectNotFoundException("Theme " + publication.getId() + " not found");
      }
    } else if (StringUtils.isBlank(publication.getClientId())) {
      throw new IllegalArgumentException("theme.clientId.mandatory");
    } else if (publication.getStartFrom() != null && publication.getStartFrom().getId() != null) {
      source = themes.get(publication.getStartFrom().getId());
      if (source == null) {
        throw new ObjectNotFoundException("Theme " + publication.getStartFrom().getId() + " not found");
      }
    }
    String name = publication.getName() == null && existing != null ? existing.getName() : publication.getName();
    if (existing != null && existing.isDefaultTheme() && !StringUtils.equals(StringUtils.trim(name), existing.getName())) {
      throw new IllegalArgumentException(DEFAULT_THEME_RENAME_MESSAGE);
    }
    ThemeSettings base = existing != null ? existing.getSettings() : baseOf(source);
    // an existing theme's submitted map is its whole stored map, an absent map leaves it unchanged
    ThemeSettings settings = new ThemeSettings(planMap(existing, base.getShared(), publication.getShared()),
                                               planMap(existing, base.getLight(), publication.getLight()),
                                               planMap(existing, base.getDark(), publication.getDark()));
    checkVersionPlacement(settings.getShared(), true);
    checkVersionPlacement(settings.getLight(), false);
    checkVersionPlacement(settings.getDark(), false);
    // file references: only the ones the theme (or its source) already holds, new files only through an upload
    Map<String, String> allowedFiles = fileReferences(base);
    for (Map<String, String> map : List.of(settings.getShared(), settings.getLight(), settings.getDark())) {
      checkFileReferences(map, allowedFiles);
    }
    for (Map<String, String> map : List.of(settings.getShared(), settings.getLight(), settings.getDark())) {
      Map<String, String> uploads = extractUploads(map);
      // an upload's key is a declared image key, checked before any file is written
      uploads.keySet().forEach(imageKey -> getValueValidator().validateValue(imageKey + FILE_SUFFIX, null));
      ThemeValueValidator.normalizeSizeUnits(map);
      getValueValidator().validateValues(map);
      uploads.forEach((imageKey, uploadId) -> map.put(imageKey + FILE_SUFFIX, String.valueOf(writeUpload(uploadId, imageKey, plan))));
    }
    // the trial compiles the maps the stylesheet will compile: both resolved versions
    Theme candidate = existing == null ? new Theme(0, name, false, ThemeOrigin.UI, null, settings, null, null) :
                                       new Theme(existing.getId(),
                                                 name,
                                                 existing.isNameSetInUI(),
                                                 existing.getOrigin(),
                                                 existing.getConfigKey(),
                                                 settings,
                                                 existing.getConfigSettings(),
                                                 null);
    ResolvedTheme trial = resolve(candidate);
    getValueValidator().validateCompiles(trial.getLight(), trial.getDark());
    if (source != null) {
      copySourceFiles(settings, plan);
    }
    return new PlannedTheme(publication.getId(), publication.getClientId(), name, settings);
  }

  /**
   * A copy starts from the source's whole look: its stored values, and for a
   * configured source the configured values it has not overridden.
   */
  private ThemeSettings baseOf(Theme source) {
    if (source == null) {
      return new ThemeSettings();
    }
    ThemeSettings base = ThemeMapper.copy(source.getSettings());
    ThemeSettings configured = source.getConfigSettings();
    if (configured != null) {
      configured.getShared().forEach((key, value) -> {
        if (!CONFIG_NAME_KEY.equals(key)) {
          base.getShared().putIfAbsent(key, value);
        }
      });
      configured.getLight().forEach(base.getLight()::putIfAbsent);
      configured.getDark().forEach(base.getDark()::putIfAbsent);
    }
    return base;
  }

  private static Map<String, String> planMap(Theme existing, Map<String, String> base, Map<String, String> submitted) {
    if (existing != null) {
      return submitted == null ? new HashMap<>(base) : merge(null, submitted);
    }
    return merge(base, submitted);
  }

  /**
   * The font, the sizes and the layout are shared by both versions (US6): one
   * of them in a version's map, or a color in the shared map, is refused.
   */
  private static void checkVersionPlacement(Map<String, String> map, boolean sharedMap) {
    map.keySet().forEach(key -> {
      String baseKey = key;
      if (key.endsWith(UPLOAD_SUFFIX)) {
        baseKey = key.substring(0, key.length() - UPLOAD_SUFFIX.length());
      } else if (isFileKey(key)) {
        baseKey = imageKeyOf(key);
      }
      if (ThemeVariables.isShared(baseKey) != sharedMap) {
        throw new IllegalArgumentException(WRONG_VERSION_MESSAGE + key);
      }
    });
  }

  /**
   * @param base the map a new theme starts from, null for an existing theme,
   *          whose submitted map is its whole stored map
   * @param submitted the page's map, a null value removing the key
   * @return the merged map, null-free
   */
  private static Map<String, String> merge(Map<String, String> base, Map<String, String> submitted) {
    Map<String, String> merged = new HashMap<>();
    if (base != null) {
      merged.putAll(base);
    }
    if (submitted != null) {
      submitted.forEach((key, value) -> {
        if (value == null) {
          merged.remove(key);
        } else {
          merged.put(key, value);
        }
      });
    }
    merged.values().removeIf(Objects::isNull);
    return merged;
  }

  private static Map<String, String> fileReferences(ThemeSettings settings) {
    Map<String, String> references = new HashMap<>();
    for (Map<String, String> map : List.of(settings.getShared(), settings.getLight(), settings.getDark())) {
      map.forEach((key, value) -> {
        if (isFileKey(key)) {
          references.put(value, key);
        }
      });
    }
    return references;
  }

  /**
   * A file id is accepted only when the theme, or the theme it starts from,
   * already references it under the same key: any other id would serve an
   * arbitrary stored file through the anonymous theme image endpoint.
   */
  private static void checkFileReferences(Map<String, String> map, Map<String, String> allowedFiles) {
    map.forEach((key, value) -> {
      if (isFileKey(key) && !StringUtils.equals(key, allowedFiles.get(value))) {
        throw new IllegalArgumentException(FILE_REFERENCE_INVALID_MESSAGE + key);
      }
    });
  }

  private static Map<String, String> extractUploads(Map<String, String> map) {
    Map<String, String> uploads = new HashMap<>();
    map.entrySet().removeIf(entry -> {
      if (!entry.getKey().endsWith(UPLOAD_SUFFIX)) {
        return false;
      }
      String imageKey = entry.getKey().substring(0, entry.getKey().length() - UPLOAD_SUFFIX.length());
      if (!isImageKey(imageKey) || StringUtils.isBlank(entry.getValue())) {
        throw new IllegalArgumentException(ThemeValueValidator.UNKNOWN_KEY_MESSAGE + entry.getKey());
      }
      uploads.put(imageKey, entry.getValue());
      return true;
    });
    // an image key's file is either uploaded now or the one already referenced
    uploads.keySet().forEach(imageKey -> map.remove(imageKey + FILE_SUFFIX));
    return uploads;
  }

  private long writeUpload(String uploadId, String imageKey, PublicationPlan plan) {
    long fileId = storeThemeImage(uploadId, imageKey, plan);
    plan.getNewFileIds().add(fileId);
    return fileId;
  }

  /** The source's files the copy keeps are copied into new ids: one file, one reference. */
  private void copySourceFiles(ThemeSettings settings, PublicationPlan plan) {
    for (Map<String, String> map : List.of(settings.getShared(), settings.getLight(), settings.getDark())) {
      for (Map.Entry<String, String> entry : new ArrayList<>(map.entrySet())) {
        if (isFileKey(entry.getKey()) && !plan.getNewFileIds().contains(Long.valueOf(entry.getValue()))) {
          Long copy = themeFileStorage.copyFile(Long.parseLong(entry.getValue()));
          if (copy == null) {
            map.remove(entry.getKey());
          } else {
            plan.getNewFileIds().add(copy);
            map.put(entry.getKey(), String.valueOf(copy));
          }
        }
      }
    }
  }

  private void checkNames(PublicationPlan plan, Map<Long, Theme> themes) {
    Map<Long, String> finalNames = new HashMap<>();
    themes.forEach((id, theme) -> {
      if (!plan.getDeletedThemeIds().contains(id)) {
        finalNames.put(id, theme.getName());
      }
    });
    Set<String> names = new HashSet<>();
    long newIndex = -1;
    for (PlannedTheme planned : plan.getThemes()) {
      String name = StringUtils.trim(planned.getName());
      if (StringUtils.isBlank(name)) {
        throw new IllegalArgumentException(NAME_MANDATORY_MESSAGE);
      } else if (name.length() > NAME_MAX_LENGTH) {
        throw new IllegalArgumentException(NAME_TOO_LONG_MESSAGE);
      }
      finalNames.put(planned.getId() == null ? newIndex-- : planned.getId(), name);
    }
    for (String name : finalNames.values()) {
      if (!names.add(ThemeStorage.nameKey(name))) {
        throw new IllegalArgumentException(NAME_ALREADY_EXISTS_MESSAGE);
      }
    }
  }

  private void planIdentity(BrandingIdentityPublication branding, PublicationPlan plan) {
    if (branding == null) {
      return;
    }
    if (branding.getCompanyName() != null) {
      String companyName = StringUtils.trimToNull(branding.getCompanyName());
      if (companyName != null && companyName.length() > NAME_MAX_LENGTH) {
        throw new IllegalArgumentException(COMPANY_NAME_TOO_LONG_MESSAGE);
      }
      plan.setCompanyNameChanged(true);
      plan.setCompanyName(companyName);
    }
    planBrandingFile(LOGO_COLUMN, branding.getLogo(), plan);
    planBrandingFile(DARK_LOGO_COLUMN, branding.getDarkLogo(), plan);
    planBrandingFile(FAVICON_COLUMN, branding.getFavicon(), plan);
    planBrandingFile(DARK_FAVICON_COLUMN, branding.getDarkFavicon(), plan);
  }

  private long storeThemeImage(String uploadId, String imageKey, PublicationPlan plan) {
    byte[] data = themeFileStorage.readUpload(uploadId);
    plan.getConsumedUploadIds().add(uploadId);
    String type = BrandingImageValidator.validateImage(data);
    return themeFileStorage.writeFile(imageKey + imageExtension(type), type, data);
  }

  private long storeBrandingFile(String uploadId, String column, PublicationPlan plan) {
    byte[] data = themeFileStorage.readUpload(uploadId);
    plan.getConsumedUploadIds().add(uploadId);
    String type = column.toLowerCase(Locale.ROOT).contains(FAVICON_COLUMN) ? BrandingImageValidator.validateFavicon(data) :
                                                                         BrandingImageValidator.validateLogo(data);
    return themeFileStorage.writeFile(column + imageExtension(type), type, data);
  }

  /**
   * @param column a logo or favicon column
   * @return a stored copy of the file the deployment configures for it, null
   *         when none is configured, it cannot be read or it is no image
   *         ({@link BrandingImageValidator#validateConfigured})
   */
  Long copyConfiguredFile(String column) {
    String path = configuredFilePath(column);
    byte[] data = themeFileStorage.readConfiguredFile(path);
    if (data == null) {
      return null;
    }
    String type;
    try {
      type = BrandingImageValidator.validateConfigured(data);
    } catch (IllegalArgumentException e) {
      LOG.warn("The configured branding file {} is refused ({}), it is not used", path, e.getMessage());
      return null;
    }
    return themeFileStorage.writeFile(column + imageExtension(type), type, data);
  }

  /**
   * @param column a logo or favicon column
   * @return the file the deployment configures for it, the light columns'
   *         built-in defaults otherwise, null for a dark column with none
   */
  String configuredFilePath(String column) {
    return switch (column) {
    case LOGO_COLUMN -> themeConfiguration.getConfiguredPath(ThemeConfigurationSynchronizer.LOGO_PROPERTY,
                                                             ThemeConfigurationSynchronizer.DEFAULT_LOGO_PATH);
    case FAVICON_COLUMN -> themeConfiguration.getConfiguredPath(ThemeConfigurationSynchronizer.FAVICON_PROPERTY,
                                                                ThemeConfigurationSynchronizer.DEFAULT_FAVICON_PATH);
    case DARK_LOGO_COLUMN -> themeConfiguration.getConfiguredPath(ThemeConfigurationSynchronizer.DARK_LOGO_PROPERTY, null);
    default -> themeConfiguration.getConfiguredPath(ThemeConfigurationSynchronizer.DARK_FAVICON_PROPERTY, null);
    };
  }

  private static String imageExtension(String type) {
    return switch (type) {
    case BrandingImageValidator.JPEG -> ".jpg";
    case BrandingImageValidator.GIF -> ".gif";
    case BrandingImageValidator.WEBP -> ".webp";
    case BrandingImageValidator.SVG -> ".svg";
    case BrandingImageValidator.ICO -> ".ico";
    default -> ".png";
    };
  }

  private void planBrandingFile(String column, BrandingFilePublication file, PublicationPlan plan) {
    if (file == null) {
      return;
    }
    if (file.isRemove()) {
      // a removed light logo or favicon falls back to the configured one, stored as a copy as at the first start; a
      // removed dark one falls back to the light one
      Long copy = LOGO_COLUMN.equals(column) || FAVICON_COLUMN.equals(column) ? copyConfiguredFile(column) : null;
      if (copy != null) {
        plan.getNewFileIds().add(copy);
      }
      plan.getBrandingFiles().put(column, copy);
    } else if (StringUtils.isNotBlank(file.getUploadId())) {
      long fileId = storeBrandingFile(file.getUploadId(), column, plan);
      plan.getNewFileIds().add(fileId);
      plan.getBrandingFiles().put(column, fileId);
    }
  }

  private void writeIdentity(PublicationPlan plan, BrandingState state, PublicationResult result) {
    if (plan.isCompanyNameChanged()) {
      state.setCompanyName(plan.getCompanyName());
    }
    plan.getBrandingFiles().forEach((column, fileId) -> {
      Long previous = switch (column) {
      case LOGO_COLUMN -> state.getLogoFileId();
      case DARK_LOGO_COLUMN -> state.getDarkLogoFileId();
      case FAVICON_COLUMN -> state.getFaviconFileId();
      case DARK_FAVICON_COLUMN -> state.getDarkFaviconFileId();
      default -> throw new IllegalArgumentException("branding.file.unknown:" + column);
      };
      switch (column) {
      case LOGO_COLUMN -> state.setLogoFileId(fileId);
      case DARK_LOGO_COLUMN -> state.setDarkLogoFileId(fileId);
      case FAVICON_COLUMN -> state.setFaviconFileId(fileId);
      default -> state.setDarkFaviconFileId(fileId);
      }
      if (previous != null) {
        result.getUnusedFileIds().add(previous);
      }
    });
  }

  private void updateNameSetInUI(Theme row, String name) {
    if (row.getOrigin() != ThemeOrigin.CONFIGURATION || StringUtils.equals(StringUtils.trim(name), row.getName())) {
      return;
    }
    ThemeSettings configured = row.getConfigSettings();
    String configuredName = configured == null ? null : configured.getShared().get(CONFIG_NAME_KEY);
    // renaming a configured theme back to its configured name lets the configuration name it again
    row.setNameSetInUI(!StringUtils.equals(StringUtils.trim(name), configuredName));
  }

  private void afterCommit(PublicationResult result, String username) {
    themeStorage.evictAll();
    result.getUnusedFileIds().forEach(themeFileStorage::deleteFile);
    broadcast(BrandingService.BRANDING_UPDATED_EVENT, username, result.getVersion());
    if (result.isModesChanged()) {
      Map<String, Object> data = new HashMap<>();
      data.put(MODES_OFFERED_DATA_KEY, result.getModesOffered().name());
      // the theme in use whatever chose it: an administrator, the configuration or the default
      data.put(THEME_IN_USE_ID_DATA_KEY, getThemeInUse().getId());
      broadcast(MODES_OFFERED_UPDATED_EVENT, username, data);
    }
  }

  private void broadcast(String eventName, Object source, Object data) {
    try {
      listenerService.broadcast(eventName, source, data);
    } catch (Exception e) {
      LOG.warn("Error broadcasting event {} after a branding publication", eventName, e);
    }
  }

  private void checkAdministrator(String username) throws IllegalAccessException {
    if (StringUtils.isBlank(username) || !userAcl.isAdministrator(userAcl.getUserIdentity(username))) {
      throw new IllegalAccessException("User " + username + " is not a platform administrator");
    }
  }

  /**
   * An engine failure, at a flush or at the commit, comes wrapped: a
   * violation of the name key is the user's error, anything else a server
   * error whose engine text stays in the debug log.
   */
  static RuntimeException translate(RuntimeException e) {
    for (Throwable cause = e; cause != null; cause = cause.getCause()) {
      if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
          && StringUtils.containsIgnoreCase(violation.getConstraintName(), NAME_KEY_CONSTRAINT)) {
        return new IllegalArgumentException(NAME_ALREADY_EXISTS_MESSAGE);
      }
    }
    if (e instanceof IllegalArgumentException || isConflict(e)) {
      return e;
    }
    LOG.debug("Branding publication failed", e);
    return new IllegalStateException(PUBLISH_FAILED_MESSAGE, e);
  }

  private static boolean isConflict(RuntimeException e) {
    return e instanceof ThemePublishConflictException;
  }

  private static Set<Long> fileIds(ThemeSettings settings) {
    Set<Long> ids = new HashSet<>();
    for (Map<String, String> map : List.of(settings.getShared(), settings.getLight(), settings.getDark())) {
      map.forEach((key, value) -> {
        if (isFileKey(key) && StringUtils.isNumeric(value)) {
          ids.add(Long.valueOf(value));
        }
      });
    }
    return ids;
  }

  private static Theme copy(Theme theme) {
    return theme == null ? null :
                         new Theme(theme.getId(),
                                   theme.getName(),
                                   theme.isNameSetInUI(),
                                   theme.getOrigin(),
                                   theme.getConfigKey(),
                                   ThemeMapper.copy(theme.getSettings()),
                                   ThemeMapper.copy(theme.getConfigSettings()),
                                   theme.getUpdatedDate());
  }

}
