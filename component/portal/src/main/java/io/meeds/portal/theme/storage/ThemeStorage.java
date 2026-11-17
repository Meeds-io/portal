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
package io.meeds.portal.theme.storage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import io.meeds.portal.theme.dao.BrandingStateDAO;
import io.meeds.portal.theme.dao.ThemeDAO;
import io.meeds.portal.theme.entity.BrandingStateEntity;
import io.meeds.portal.theme.entity.ThemeEntity;
import io.meeds.portal.theme.model.BrandingState;
import io.meeds.portal.theme.model.Theme;
import io.meeds.portal.theme.model.ThemeSnapshot;
import io.meeds.portal.theme.util.ThemeMapper;

/**
 * The theme rows and the branding state row. Its reads are served from one
 * cached {@link ThemeSnapshot}; its writes take and return DTOs, and map them
 * onto the entity the transaction already holds (the write's first read, the
 * state row under lock, loads it into the persistence context).
 */
@Component
public class ThemeStorage {

  /**
   * The one cache of the state and the themes. They are a single entry, loaded
   * and evicted together, so that:
   * <ul>
   * <li>the VERSION a client reads validates the very maps it saw: a version
   * and maps cached apart could pair a newer version with older maps, and a
   * publication would then overwrite the change in between;</li>
   * <li>the stylesheet's stamp, read before its values, never pairs a newer
   * stamp with older values: both come from the same load, state first.</li>
   * </ul>
   */
  public static final String SNAPSHOT_CACHE = "portal.themeSnapshot";

  /** The temporary name key of a row whose name another row takes in the same publication. */
  public static final String NAME_PLACEHOLDER_PREFIX = "~";

  @Autowired
  private ThemeDAO               themeDAO;

  @Autowired
  private BrandingStateDAO       brandingStateDAO;

  /** This bean's proxy: a call on {@code this} would bypass the cache. */
  @Autowired
  @Lazy
  private ThemeStorage           self;

  /**
   * Loaded state first, then the themes: a publication committing in between
   * can only leave themes newer than the version, which a client's next
   * publication then reports as a conflict.
   *
   * @return the cached snapshot
   */
  @Cacheable(cacheNames = SNAPSHOT_CACHE, key = "'snapshot'", sync = true)
  public ThemeSnapshot getSnapshot() {
    BrandingState state = readState();
    List<Theme> themes = themeDAO.findAll(ThemeDAO.BY_ID).stream().map(ThemeMapper::toTheme).toList();
    return new ThemeSnapshot(state, themes);
  }

  public List<Theme> getThemes() {
    return self.getSnapshot().getThemes();
  }

  /**
   * @param id a theme id
   * @return the theme, null when there is none; an unknown id costs no read
   */
  public Theme getTheme(long id) {
    return self.getSnapshot().getTheme(id);
  }

  public BrandingState getState() {
    return self.getSnapshot().getState();
  }

  /**
   * Evicts the snapshot, whole: the next read comes from the database. Called
   * after the commit of a write, and when a write found the snapshot behind
   * the database.
   */
  @CacheEvict(cacheNames = SNAPSHOT_CACHE, allEntries = true)
  public void evictAll() {
    // evicted by the annotation
  }

  /**
   * @return the state read from the database, never from the cache
   */
  public BrandingState readState() {
    return ThemeMapper.toState(brandingStateDAO.findById(BrandingStateEntity.STATE_ID).orElseThrow());
  }

  /**
   * The first read of every write: the state row under a pessimistic lock,
   * held until the transaction ends. Uncached.
   *
   * @return the locked state
   */
  public BrandingState lockState() {
    return ThemeMapper.toState(brandingStateDAO.findForUpdate(BrandingStateEntity.STATE_ID));
  }

  /**
   * @param state the state to write: every column but the default file
   *          sources, which have their own accessors
   * @return the state as written
   */
  public BrandingState saveState(BrandingState state) {
    BrandingStateEntity entity = stateEntity();
    entity.setVersion(state.getVersion());
    entity.setLegacyImported(state.isLegacyImported());
    entity.setCompanyName(state.getCompanyName());
    entity.setLogoFileId(state.getLogoFileId());
    entity.setDarkLogoFileId(state.getDarkLogoFileId());
    entity.setFaviconFileId(state.getFaviconFileId());
    entity.setDarkFaviconFileId(state.getDarkFaviconFileId());
    entity.setThemeInUseId(state.getThemeInUseId());
    entity.setModesOffered(state.getModesOffered());
    entity.setLastEffectiveThemeId(state.getLastEffectiveThemeId());
    entity.setLastEffectiveModes(state.getLastEffectiveModes());
    entity.setModesGeneration(state.getModesGeneration());
    return ThemeMapper.toState(brandingStateDAO.saveAndFlush(entity));
  }

  /**
   * @return the configured path last tried for each default file column
   */
  public Map<String, String> getDefaultFileSources() {
    return ThemeMapper.readMap(stateEntity().getDefaultFileSources());
  }

  public void saveDefaultFileSources(Map<String, String> sources) {
    BrandingStateEntity entity = stateEntity();
    entity.setDefaultFileSources(ThemeMapper.writeMap(sources));
    brandingStateDAO.saveAndFlush(entity);
  }

  /**
   * Uncached: the rows a write reads after taking the lock. A bound theme's
   * configured name stays in its configured settings (the key {@code name} of
   * the shared map), where its writers compare it.
   *
   * @return every theme, sorted by id
   */
  public List<Theme> getThemeRows() {
    return new ArrayList<>(themeDAO.findAll(ThemeDAO.BY_ID).stream().map(entity -> ThemeMapper.toTheme(entity, true)).toList());
  }

  /**
   * @param id a theme id
   * @return the row, null when there is none
   */
  public Theme getThemeRow(long id) {
    return themeDAO.findById(id).map(entity -> ThemeMapper.toTheme(entity, true)).orElse(null);
  }

  /**
   * Inserts the theme when its id is 0, else updates the row of this id.
   *
   * @param theme the theme, with its configured settings as
   *          {@link #getThemeRows()} returns them
   * @return the row as written
   */
  public Theme saveTheme(Theme theme) {
    ThemeEntity entity = theme.getId() == 0 ? new ThemeEntity() :
                                              themeDAO.findById(theme.getId()).orElseThrow();
    entity.setName(theme.getName());
    entity.setNameKey(nameKey(theme.getName()));
    entity.setNameSetInUI(theme.isNameSetInUI());
    entity.setOrigin(theme.getOrigin());
    entity.setConfigKey(theme.getConfigKey());
    entity.setConfigSettings(ThemeMapper.writeSettings(theme.getConfigSettings()));
    entity.setSharedSettings(ThemeMapper.writeMap(theme.getSettings().getShared()));
    entity.setLightSettings(ThemeMapper.writeMap(theme.getSettings().getLight()));
    entity.setDarkSettings(ThemeMapper.writeMap(theme.getSettings().getDark()));
    entity.setUpdatedDate(new Date());
    return ThemeMapper.toTheme(themeDAO.saveAndFlush(entity), true);
  }

  /**
   * Frees the name key of a row, so that another row of the same publication
   * can take its name before this row's own final name is written.
   *
   * @param id the row's id
   */
  public void freeName(long id) {
    ThemeEntity entity = themeDAO.findById(id).orElseThrow();
    entity.setNameKey(NAME_PLACEHOLDER_PREFIX + id);
    themeDAO.saveAndFlush(entity);
  }

  public void deleteTheme(long id) {
    themeDAO.findById(id).ifPresent(themeDAO::delete);
    themeDAO.flush();
  }

  public static String nameKey(String name) {
    return name == null ? null : name.trim().toLowerCase(Locale.ROOT);
  }

  private BrandingStateEntity stateEntity() {
    return brandingStateDAO.findById(BrandingStateEntity.STATE_ID).orElseThrow();
  }

}
