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

import static io.meeds.portal.theme.service.ThemeService.CONFIG_NAME_KEY;
import static io.meeds.portal.theme.service.ThemeService.DARK_FAVICON_COLUMN;
import static io.meeds.portal.theme.service.ThemeService.DARK_LOGO_COLUMN;
import static io.meeds.portal.theme.service.ThemeService.DEFAULT_THEME_CONFIG_KEY;
import static io.meeds.portal.theme.service.ThemeService.DEFAULT_THEME_ID;
import static io.meeds.portal.theme.service.ThemeService.FAVICON_COLUMN;
import static io.meeds.portal.theme.service.ThemeService.LOGO_COLUMN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.common.ContainerTransactional;
import io.meeds.portal.theme.ThemeReadiness;
import io.meeds.portal.theme.constant.ModesOffered;
import io.meeds.portal.theme.constant.ThemeOrigin;
import io.meeds.portal.theme.model.BrandingState;
import io.meeds.portal.theme.model.ConfigurationSyncResult;
import io.meeds.portal.theme.model.ConfiguredTheme;
import io.meeds.portal.theme.model.ImportPlan;
import io.meeds.portal.theme.model.LegacyBranding;
import io.meeds.portal.theme.model.Theme;
import io.meeds.portal.theme.model.ThemeSettings;
import io.meeds.portal.theme.storage.ThemeFileStorage;
import io.meeds.portal.theme.storage.ThemeStorage;
import io.meeds.portal.theme.util.ThemeMapper;

/**
 * Brings the theme rows in line with the configuration, imports the
 * single-theme branding once, and reads the configured default files, at each
 * start of a node, once social's context has refreshed (never in a
 * PostConstruct, which would hold the singleton lock while waiting on the row
 * lock another node holds). Every node runs it: each locked step reads the
 * state row under its lock first, and files are copied between the two steps,
 * outside any lock. It sets the readiness flag when it ends, also when a step
 * failed: that step rolled back, and runs again at the next start.
 */
@Component
public class ThemeConfigurationSynchronizer implements ApplicationListener<ContextRefreshedEvent> {

  public static final String                 LOGO_PROPERTY          = "exo.branding.company.logo";

  public static final String                 FAVICON_PROPERTY       = "exo.branding.company.favicon";

  public static final String                 DARK_LOGO_PROPERTY     = "exo.branding.company.logo.dark";

  public static final String                 DARK_FAVICON_PROPERTY  = "exo.branding.company.favicon.dark";

  public static final String                 DEFAULT_LOGO_PATH      = "/skin/images/logo/DefaultLogo.png";

  public static final String                 DEFAULT_FAVICON_PATH   = "/skin/images/favicon.ico";

  private static final Log                   LOG                    = ExoLogger.getLogger(ThemeConfigurationSynchronizer.class);

  private final AtomicBoolean                started                = new AtomicBoolean();

  @Autowired
  private ThemeStorage                       themeStorage;

  @Autowired
  private ThemeFileStorage                   themeFileStorage;

  @Autowired
  private ThemeConfiguration                 themeConfiguration;

  @Autowired
  private ThemeService                       themeService;

  @Autowired
  private BrandingService                    brandingService;

  @Autowired
  private LegacyBrandingImporter             legacyImporter;

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (started.compareAndSet(false, true)) {
      synchronize();
    }
  }

  /**
   * The startup sequence: configuration (locked), file copies (unlocked),
   * import (locked), then the legacy tertiary color's delete and the flag.
   */
  public void synchronize() {
    try {
      runSteps();
    } finally {
      ThemeReadiness.setReady();
      compileFirstStylesheet();
    }
  }

  /** The first compile of the stylesheet, once the themes are readable: the node's start no longer compiles it. */
  private void compileFirstStylesheet() {
    try {
      brandingService.getThemeStylesheet();
    } catch (RuntimeException e) {
      LOG.warn("The branding stylesheet could not be compiled at start, the first request compiles it", e);
    }
  }

  private void runSteps() {
    ConfigurationSyncResult sync = null;
    try {
      sync = synchronizeConfiguration();
    } catch (Exception e) {
      if (isLockTimeout(e)) {
        // the state row is held longer than its lock timeout: another node is synchronising the same configuration
        LOG.warn("The configured themes were not synchronised by this node, another node holds the branding state");
        LOG.debug("Branding state lock timeout", e);
      } else {
        LOG.error("The configured themes could not be synchronised, the next start retries", e);
      }
    }
    if (sync != null) {
      ImportPlan plan = null;
      try {
        plan = planImport(sync);
        importInTransaction(plan).forEach(themeFileStorage::deleteFile);
      } catch (Exception e) {
        LOG.error("The legacy branding and the configured default files could not be imported, the next start retries", e);
        if (plan != null) {
          plan.getAllCopies().forEach(themeFileStorage::deleteFile);
        }
      }
    }
    legacyImporter.deleteTertiaryColor();
    themeStorage.evictAll();
  }

  /**
   * @param e a failure of a synchronisation step
   * @return true when it, or a cause, is a lock wait that timed out on the
   *         branding state row
   */
  static boolean isLockTimeout(Throwable e) {
    for (Throwable cause = e; cause != null; cause = cause.getCause()) {
      if (cause instanceof org.springframework.dao.PessimisticLockingFailureException
          || cause instanceof jakarta.persistence.LockTimeoutException
          || cause instanceof jakarta.persistence.PessimisticLockException
          || cause instanceof org.hibernate.PessimisticLockException) {
        return true;
      }
    }
    return false;
  }

  @ContainerTransactional(atomic = true)
  public ConfigurationSyncResult synchronizeConfiguration() {
    return syncConfiguration();
  }

  @ContainerTransactional(atomic = true)
  public List<Long> importInTransaction(ImportPlan plan) {
    return writeImport(plan);
  }

  ConfigurationSyncResult syncConfiguration() { // NOSONAR one step of the note's concurrency model
    BrandingState state = themeStorage.lockState();
    boolean changed = false;
    List<Theme> rows = themeStorage.getThemeRows();
    List<ConfiguredTheme> configuredThemes = themeConfiguration.getConfiguredThemes();
    Set<String> configuredKeys = themeConfiguration.getConfiguredKeys();
    for (ConfiguredTheme configured : configuredThemes) {
      changed |= bind(configured, rows);
    }
    for (Theme row : rows) {
      if (row.getConfigKey() != null && !configuredKeys.contains(row.getConfigKey())) {
        unbind(row, state, rows);
        changed = true;
      }
    }
    long effectiveThemeId = effectiveThemeInUse(state, rows);
    ModesOffered effectiveModes = themeService.effectiveModes(state.getModesOffered());
    if (state.getLastEffectiveModes() != null && state.getLastEffectiveModes() != ModesOffered.LIGHT_AND_DARK
        && effectiveModes == ModesOffered.LIGHT_AND_DARK) {
      // a changed configuration takes the users back to Automatic, as an administrator's return to both modes does
      state.setModesGeneration(state.getModesGeneration() + 1);
      changed = true;
    }
    if (state.getLastEffectiveThemeId() == null || state.getLastEffectiveThemeId() != effectiveThemeId
        || state.getLastEffectiveModes() != effectiveModes) {
      state.setLastEffectiveThemeId(effectiveThemeId);
      state.setLastEffectiveModes(effectiveModes);
      changed = true;
    }
    if (changed) {
      state.setVersion(state.getVersion() + 1);
    }
    state = themeStorage.saveState(state);
    ConfigurationSyncResult result = new ConfigurationSyncResult();
    result.setLegacyImported(state.isLegacyImported());
    result.getDefaultFileSources().putAll(themeStorage.getDefaultFileSources());
    return result;
  }

  ImportPlan planImport(ConfigurationSyncResult sync) {
    ImportPlan plan = new ImportPlan();
    try {
      if (!sync.isLegacyImported()) {
        LegacyBranding legacy = legacyImporter.read();
        plan.setLegacy(legacy);
        legacy.getImageFileIds().forEach((imageKey, fileId) -> {
          Long copy = themeFileStorage.copyFile(fileId);
          if (copy != null) {
            plan.getImageCopies().put(imageKey, copy);
          }
        });
        if (legacy.getLogoFileId() != null) {
          plan.setLogoCopy(themeFileStorage.copyFile(legacy.getLogoFileId()));
        }
        if (legacy.getFaviconFileId() != null) {
          plan.setFaviconCopy(themeFileStorage.copyFile(legacy.getFaviconFileId()));
        }
      }
      configuredPaths().forEach((column, path) -> {
        if (path != null && !StringUtils.equals(path, sync.getDefaultFileSources().get(column))) {
          plan.getDefaultPaths().put(column, path);
          plan.getDefaultCopies().put(column, themeService.copyConfiguredFile(column));
        }
      });
    } catch (RuntimeException e) {
      plan.getAllCopies().forEach(themeFileStorage::deleteFile);
      throw e;
    }
    return plan;
  }

  List<Long> writeImport(ImportPlan plan) {
    BrandingState state = themeStorage.lockState();
    List<Long> unused = new ArrayList<>();
    boolean changed = false;
    if (plan.getLegacy() != null && !state.isLegacyImported()) {
      importLegacy(plan, state, unused);
      changed = true;
    } else if (plan.getLegacy() != null) {
      // another node imported between the two steps
      unused.addAll(plan.getImageCopies().values());
      addIfNotNull(unused, plan.getLogoCopy());
      addIfNotNull(unused, plan.getFaviconCopy());
    }
    Map<String, String> sources = themeStorage.getDefaultFileSources();
    for (Map.Entry<String, String> entry : plan.getDefaultPaths().entrySet()) {
      String column = entry.getKey();
      Long copy = plan.getDefaultCopies().get(column);
      if (StringUtils.equals(entry.getValue(), sources.get(column))) {
        addIfNotNull(unused, copy);
        continue;
      }
      // a configured default only ever fills an empty column: an upload wins, and a removal stores the configured
      // file of its time
      if (copy != null && fileColumns(state).get(column) == null) {
        setFileColumn(state, column, copy);
      } else {
        addIfNotNull(unused, copy);
      }
      sources.put(column, entry.getValue());
      changed = true;
    }
    if (changed) {
      themeStorage.saveDefaultFileSources(sources);
      state.setVersion(state.getVersion() + 1);
      themeStorage.saveState(state);
    }
    return unused;
  }

  private void importLegacy(ImportPlan plan, BrandingState state, List<Long> unused) {
    LegacyBranding legacy = plan.getLegacy();
    Theme defaultTheme = themeStorage.getThemeRow(DEFAULT_THEME_ID);
    if (defaultTheme == null) {
      // the changelog seeds it and no publication deletes it
      throw new IllegalStateException("The Default Theme row is missing");
    }
    Map<String, String> shared = defaultTheme.getSettings().getShared();
    Map<String, String> light = defaultTheme.getSettings().getLight();
    legacy.getSettings().getShared().forEach(shared::putIfAbsent);
    legacy.getSettings().getLight().forEach(light::putIfAbsent);
    plan.getImageCopies().forEach((imageKey, copy) -> {
      if (light.putIfAbsent(LegacyBrandingImporter.fileKey(imageKey), String.valueOf(copy)) != null) {
        unused.add(copy);
      }
    });
    themeStorage.saveTheme(defaultTheme);
    if (state.getCompanyName() == null && legacy.getCompanyName() != null) {
      if (legacy.getCompanyName().length() > ThemeService.NAME_MAX_LENGTH) {
        LOG.warn("The legacy company name is longer than {} characters, it is not imported: the configured name applies",
                 ThemeService.NAME_MAX_LENGTH);
      } else {
        state.setCompanyName(legacy.getCompanyName());
      }
    }
    if (plan.getLogoCopy() != null && state.getLogoFileId() == null) {
      state.setLogoFileId(plan.getLogoCopy());
    } else {
      addIfNotNull(unused, plan.getLogoCopy());
    }
    if (plan.getFaviconCopy() != null && state.getFaviconFileId() == null) {
      state.setFaviconFileId(plan.getFaviconCopy());
    } else {
      addIfNotNull(unused, plan.getFaviconCopy());
    }
    state.setLegacyImported(true);
  }

  /**
   * @return true when the row of this configured theme changed
   */
  private boolean bind(ConfiguredTheme configured, List<Theme> rows) {
    Theme row = rows.stream().filter(r -> configured.getKey().equals(r.getConfigKey())).findFirst().orElse(null);
    String before = row == null ? null : fingerprint(row);
    String nameKey = ThemeStorage.nameKey(configured.getName());
    if (row == null) {
      Theme sameName = rows.stream().filter(r -> nameKey.equals(ThemeStorage.nameKey(r.getName()))).findFirst().orElse(null);
      if (sameName != null && sameName.getOrigin() == ThemeOrigin.UI) {
        // takeover: the first time, the configured values replace the interface values, once they compile
        if (!compiles(trimmed(sameName, configured), configured)) {
          return false;
        }
        row = takeOver(sameName, configured);
      } else if (sameName != null) {
        LOG.error("Configured theme '{}' is named '{}', the name of another theme: it is not created until the clash is gone",
                  configured.getKey(),
                  configured.getName());
        return false;
      } else {
        row = new Theme(0, configured.getName(), false, ThemeOrigin.CONFIGURATION, configured.getKey(), new ThemeSettings(), null, null);
      }
    }
    if (!row.isNameSetInUI() && !StringUtils.equals(row.getName(), configured.getName())) {
      Theme current = row;
      boolean clash = rows.stream().anyMatch(r -> r != current && nameKey.equals(ThemeStorage.nameKey(r.getName())));
      if (clash) {
        LOG.error("Configured theme '{}' is named '{}', the name of another theme: it keeps its name '{}'",
                  configured.getKey(),
                  configured.getName(),
                  row.getName());
      } else {
        row.setName(configured.getName());
      }
    }
    ThemeSettings snapshot = ThemeMapper.copy(configured.getSettings());
    snapshot.getShared().put(CONFIG_NAME_KEY, configured.getName());
    boolean configuredChanged = !StringUtils.equals(ThemeService.canonical(snapshot), ThemeService.canonical(row.getConfigSettings()));
    boolean compiled = !configuredChanged || compiles(row.getSettings(), configured);
    if (configuredChanged && compiled) {
      row.setConfigSettings(snapshot);
    }
    boolean isNew = row.getId() == 0;
    if (isNew && !compiled) {
      return false;
    }
    if (!isNew && fingerprint(row).equals(before)) {
      return false;
    }
    Theme saved = themeStorage.saveTheme(row);
    if (isNew) {
      rows.add(saved);
    } else {
      rows.replaceAll(r -> r.getId() == saved.getId() ? saved : r);
    }
    return true;
  }

  /** The UI row's settings as a takeover leaves them, for its trial compile: the row itself is not changed. */
  private static ThemeSettings trimmed(Theme row, ConfiguredTheme configured) {
    ThemeSettings copy = ThemeMapper.copy(row.getSettings());
    copy.getShared().keySet().removeAll(configured.getSettings().getShared().keySet());
    copy.getLight().keySet().removeAll(configured.getSettings().getLight().keySet());
    copy.getDark().keySet().removeAll(configured.getSettings().getDark().keySet());
    return copy;
  }

  private Theme takeOver(Theme row, ConfiguredTheme configured) {
    ThemeSettings settings = trimmed(row, configured);
    row.setSettings(settings);
    row.setConfigKey(configured.getKey());
    row.setOrigin(ThemeOrigin.CONFIGURATION);
    LOG.info("Configured theme '{}' takes over the theme '{}'", configured.getKey(), row.getName());
    return row;
  }

  /**
   * A theme removed from the configuration keeps its look: the configured
   * values it did not override become stored values, and it becomes a theme
   * like the ones added from the page. The theme in use at the last start
   * stays in use when no other configured choice replaces it.
   */
  private void unbind(Theme row, BrandingState state, List<Theme> rows) {
    ThemeSettings snapshot = row.getConfigSettings();
    if (snapshot != null) {
      ThemeSettings stored = row.getSettings();
      snapshot.getShared().forEach((key, value) -> {
        if (!CONFIG_NAME_KEY.equals(key)) {
          stored.getShared().putIfAbsent(key, value);
        }
      });
      snapshot.getLight().forEach(stored.getLight()::putIfAbsent);
      snapshot.getDark().forEach(stored.getDark()::putIfAbsent);
    }
    String configKey = row.getConfigKey();
    row.setConfigKey(null);
    row.setConfigSettings(null);
    row.setOrigin(ThemeOrigin.UI);
    themeStorage.saveTheme(row);
    String configuredThemeInUse = themeConfiguration.getConfiguredThemeInUse();
    boolean configuredChoiceBound = !DEFAULT_THEME_CONFIG_KEY.equals(configuredThemeInUse)
        && rows.stream().anyMatch(r -> configuredThemeInUse.equals(r.getConfigKey()));
    if (state.getThemeInUseId() == null && state.getLastEffectiveThemeId() != null && row.getId() == state.getLastEffectiveThemeId()
        && (!configuredChoiceBound || configuredThemeInUse.equals(configKey))) {
      state.setThemeInUseId(row.getId());
    }
    LOG.info("Theme '{}' is no longer configured by '{}', it is kept as a theme of the page", row.getName(), configKey);
  }

  private boolean compiles(ThemeSettings stored, ConfiguredTheme configured) {
    ThemeSettings merged = ThemeMapper.copy(configured.getSettings());
    try {
      themeConfiguration.getValueValidator()
                        .validateCompiles(themeService.getEffectiveValues(stored, merged, false),
                                          themeService.getEffectiveValues(stored, merged, true));
      return true;
    } catch (IllegalArgumentException e) {
      LOG.error("Configured theme '{}' does not compile ({}): its configured values are ignored for this start",
                configured.getKey(),
                e.getMessage());
      return false;
    }
  }

  private long effectiveThemeInUse(BrandingState state, List<Theme> rows) {
    if (state.getThemeInUseId() != null) {
      return state.getThemeInUseId();
    }
    String configuredThemeInUse = themeConfiguration.getConfiguredThemeInUse();
    return rows.stream()
               .filter(r -> configuredThemeInUse.equals(r.getConfigKey()))
               .map(Theme::getId)
               .findFirst()
               .orElse(DEFAULT_THEME_ID);
  }

  private Map<String, String> configuredPaths() {
    Map<String, String> paths = new LinkedHashMap<>();
    for (String column : List.of(LOGO_COLUMN, FAVICON_COLUMN, DARK_LOGO_COLUMN, DARK_FAVICON_COLUMN)) {
      paths.put(column, themeService.configuredFilePath(column));
    }
    return paths;
  }

  /** What a start may change on a row: compared so that a start that changes nothing writes nothing. */
  private static String fingerprint(Theme row) {
    return String.join("\u0000",
                       String.valueOf(row.getName()),
                       String.valueOf(row.getOrigin()),
                       String.valueOf(row.getConfigKey()),
                       ThemeService.canonical(row.getConfigSettings()),
                       ThemeService.canonical(row.getSettings()));
  }

  private static Map<String, Long> fileColumns(BrandingState state) {
    Map<String, Long> columns = new HashMap<>();
    columns.put(LOGO_COLUMN, state.getLogoFileId());
    columns.put(FAVICON_COLUMN, state.getFaviconFileId());
    columns.put(DARK_LOGO_COLUMN, state.getDarkLogoFileId());
    columns.put(DARK_FAVICON_COLUMN, state.getDarkFaviconFileId());
    return columns;
  }

  private static void setFileColumn(BrandingState state, String column, Long fileId) {
    switch (column) {
    case LOGO_COLUMN -> state.setLogoFileId(fileId);
    case FAVICON_COLUMN -> state.setFaviconFileId(fileId);
    case DARK_LOGO_COLUMN -> state.setDarkLogoFileId(fileId);
    default -> state.setDarkFaviconFileId(fileId);
    }
  }

  private static void addIfNotNull(List<Long> list, Long value) {
    if (value != null) {
      list.add(value);
    }
  }

}
