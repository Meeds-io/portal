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
package io.meeds.portal.theme.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import io.meeds.portal.theme.constant.ModesOffered;
import io.meeds.portal.theme.constant.ThemeOrigin;
import io.meeds.portal.theme.entity.BrandingStateEntity;
import io.meeds.portal.theme.entity.ThemeEntity;

/**
 * The theme statements on a real engine (in-memory HSQLDB) through the real
 * repository proxies, over the changelog's schema: the seed, the first id a
 * JPA insert draws, the case-insensitive name uniqueness the database enforces,
 * the configured-key lookups and the locked read of the state row.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles(ThemeJpaTestConfiguration.PROFILE)
@EnableAutoConfiguration
@ContextConfiguration(classes = ThemeJpaTestConfiguration.class)
@TestPropertySource(properties = { ThemeJpaTestConfiguration.LIQUIBASE_CHANGELOG, ThemeJpaTestConfiguration.NO_DDL })
public class ThemeDAOTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private ThemeDAO          themeDAO;

  @Autowired
  private BrandingStateDAO  brandingStateDAO;

  @Test
  void theSeedHoldsTheDefaultThemeAndTheStateRow() {
    ThemeEntity defaultTheme = themeDAO.findById(1L).orElseThrow();
    assertEquals(ThemeOrigin.DEFAULT, defaultTheme.getOrigin());
    assertEquals("default theme", defaultTheme.getNameKey());
    assertEquals("{}", defaultTheme.getLightSettings());

    BrandingStateEntity state = brandingStateDAO.findById(BrandingStateEntity.STATE_ID).orElseThrow();
    assertEquals(0L, state.getVersion());
    assertNull(state.getThemeInUseId(), "the configured theme in use applies");
    assertNull(state.getModesOffered(), "the configured modes apply");
  }

  /** On a database of its own: the sequence is not transactional, other tests' inserts would draw from it. */
  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  void theFirstInsertNeverDrawsTheSeedId() {
    ThemeEntity theme = persist("Ocean", null);
    assertEquals(2L, theme.getId());
  }

  @Test
  void twoNamesDifferingByCaseCollide() {
    persist("Ocean", null);
    assertThrows(DataIntegrityViolationException.class, () -> {
      themeDAO.saveAndFlush(newTheme("OCEAN", null));
    });
  }

  @Test
  void findsByNameKeyAndByConfigKey() {
    long ocean = persist("Ocean", null).getId();
    long forest = persist("Forest", "forest").getId();

    assertEquals(ocean, themeDAO.findByNameKey("ocean").getId());
    assertNull(themeDAO.findByNameKey("Ocean"), "the key is the lower-cased name, matched exactly");
    assertEquals(forest, themeDAO.findByConfigKey("forest").getId());
    assertNull(themeDAO.findByConfigKey("ocean"));
    assertEquals(List.of(forest), themeDAO.findByConfigKeyNotNull(ThemeDAO.BY_ID).stream().map(ThemeEntity::getId).toList());
  }

  @Test
  void theLockedReadReturnsTheStateRowAndItsWritesPersist() {
    long themeId = persist("Ocean", null).getId();
    BrandingStateEntity state = brandingStateDAO.findForUpdate(BrandingStateEntity.STATE_ID);
    assertNotNull(state);
    state.setVersion(state.getVersion() + 1);
    state.setThemeInUseId(themeId);
    state.setModesOffered(ModesOffered.DARK);
    brandingStateDAO.saveAndFlush(state);
    entityManager.clear();

    BrandingStateEntity reloaded = brandingStateDAO.findById(BrandingStateEntity.STATE_ID).orElseThrow();
    assertEquals(1L, reloaded.getVersion());
    assertEquals(themeId, reloaded.getThemeInUseId());
    assertEquals(ModesOffered.DARK, reloaded.getModesOffered());
  }

  /**
   * HSQLDB ignores a lock timeout, so the wait cannot be shown here: this pin
   * kills the mutant that drops the hint, which PostgreSQL would turn into an
   * endless wait behind another node's writer.
   */
  @Test
  void theLockedReadWaitsForTheRowAtMostTheLockTimeout() throws Exception {
    org.springframework.data.jpa.repository.QueryHints hints =
                                                            BrandingStateDAO.class.getMethod("findForUpdate", long.class)
                                                                                  .getAnnotation(org.springframework.data.jpa.repository.QueryHints.class);
    assertNotNull(hints, "findForUpdate declares its query hints");
    assertEquals(1, hints.value().length);
    assertEquals("jakarta.persistence.lock.timeout", hints.value()[0].name());
    assertEquals(BrandingStateDAO.LOCK_TIMEOUT_MILLIS, hints.value()[0].value());
    assertEquals(10000, Integer.parseInt(BrandingStateDAO.LOCK_TIMEOUT_MILLIS));
  }

  @Test
  void theThemeInUseMustExist() {
    BrandingStateEntity state = brandingStateDAO.findForUpdate(BrandingStateEntity.STATE_ID);
    state.setThemeInUseId(999L);
    assertThrows(DataIntegrityViolationException.class, () -> brandingStateDAO.saveAndFlush(state));
  }

  private ThemeEntity persist(String name, String configKey) {
    ThemeEntity theme = entityManager.persistFlushFind(newTheme(name, configKey));
    entityManager.clear();
    return theme;
  }

  private static ThemeEntity newTheme(String name, String configKey) {
    return new ThemeEntity(null,
                           name,
                           false,
                           name.toLowerCase(java.util.Locale.ROOT),
                           configKey == null ? ThemeOrigin.UI : ThemeOrigin.CONFIGURATION,
                           configKey,
                           null,
                           "{}",
                           "{}",
                           "{}",
                           new Date());
  }
}
