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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.portal.theme.ThemeReadiness;
import io.meeds.portal.theme.constant.ModesOffered;
import io.meeds.portal.theme.constant.ThemeOrigin;
import io.meeds.portal.theme.dao.BrandingStateDAO;
import io.meeds.portal.theme.dao.ThemeDAO;
import io.meeds.portal.theme.dao.ThemeJpaTestConfiguration;
import io.meeds.portal.theme.entity.BrandingStateEntity;
import io.meeds.portal.theme.entity.ThemeEntity;
import io.meeds.portal.theme.model.ConfigurationSyncResult;
import io.meeds.portal.theme.model.ConfiguredTheme;
import io.meeds.portal.theme.model.ImportPlan;
import io.meeds.portal.theme.model.LegacyBranding;
import io.meeds.portal.theme.model.ThemeSettings;
import io.meeds.portal.theme.storage.ThemeFileStorage;
import io.meeds.portal.theme.storage.ThemeStorage;
import io.meeds.portal.theme.util.ThemeMapper;

/**
 * The startup synchronisation over the real tables on HSQLDB. Its two locked
 * steps are woven by the container aspect, so the tests drive their
 * un-advised seams inside a transaction this test opens.
 */
@DataJpaTest(showSql = false)
@EnableAutoConfiguration
@ContextConfiguration(classes = ThemeJpaTestConfiguration.class)
@Import({ ThemeStorage.class, ThemeService.class, ThemeConfigurationSynchronizer.class })
@TestPropertySource(properties = { ThemeJpaTestConfiguration.LIQUIBASE_CHANGELOG, ThemeJpaTestConfiguration.NO_DDL })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ThemeConfigurationSynchronizerTest {

  @MockitoBean
  private BrandingService                brandingService;

  @MockitoBean
  private UserACL                        userAcl;

  @MockitoBean
  private org.exoplatform.commons.api.settings.SettingService settingService;

  @MockitoBean
  private ListenerService                listenerService;

  @MockitoBean
  private ThemeFileStorage               themeFileStorage;

  @MockitoBean
  private ThemeConfiguration             themeConfiguration;

  @MockitoBean
  private LegacyBrandingImporter         legacyImporter;

  @Autowired
  private ThemeConfigurationSynchronizer synchronizer;

  @Autowired
  private ThemeDAO                       themeDAO;

  @Autowired
  private BrandingStateDAO               brandingStateDAO;

  @Autowired
  private PlatformTransactionManager     transactionManager;

  private final List<ConfiguredTheme>    configured = new ArrayList<>();

  private final AtomicLong               nextFileId = new AtomicLong(100);

  @org.junit.jupiter.api.AfterEach
  void tearDown() {
    ThemeReadiness.reset();
  }

  @BeforeEach
  void setUp() {
    ThemeReadiness.reset();
    Map<String, String> defaults = new HashMap<>(Map.of("primaryColor", "#3f8487", "secondaryColor", "#476A9C"));
    when(brandingService.getDefaultThemeStyle()).thenReturn(defaults);
    when(brandingService.compileThemeStyle(anyMap())).thenReturn("");
    // the interface's merge of the template literals and the configured defaults
    when(brandingService.getResolutionDefaults()).thenCallRealMethod();
    when(themeConfiguration.getConfiguredThemes()).thenAnswer(i -> configured);
    when(themeConfiguration.getConfiguredKeys()).thenAnswer(i -> {
      LinkedHashSet<String> keys = new LinkedHashSet<>();
      configured.forEach(theme -> keys.add(theme.getKey()));
      return keys;
    });
    when(themeConfiguration.getConfiguredThemeInUse()).thenReturn(ThemeService.DEFAULT_THEME_CONFIG_KEY);
    when(themeConfiguration.getConfiguredModesOffered()).thenReturn(ModesOffered.LIGHT_AND_DARK);
    ThemeValueValidator validator = ThemeValueValidator.of(brandingService);
    when(themeConfiguration.getValueValidator()).thenReturn(validator);
    when(themeConfiguration.getConfiguredPath(anyString(), any())).thenAnswer(i -> i.getArgument(1));
    when(legacyImporter.read()).thenReturn(new LegacyBranding());
    when(themeFileStorage.readConfiguredFile(anyString())).thenReturn(new byte[] { 1, 2, 3 });
    when(themeFileStorage.copyFile(anyLong())).thenAnswer(i -> nextFileId.incrementAndGet());
    when(themeFileStorage.writeFile(anyString(), anyString(), any())).thenAnswer(i -> nextFileId.incrementAndGet());
  }

  @Test
  void aConfiguredThemeIsCreatedWithItsSnapshotAndASecondStartChangesNothing() {
    configured.add(new ConfiguredTheme("ocean", "Ocean", light("primaryColor", "#123456")));
    sync();
    ThemeEntity ocean = themeDAO.findByConfigKey("ocean");
    assertEquals(ThemeOrigin.CONFIGURATION, ocean.getOrigin());
    assertEquals("Ocean", ocean.getName());
    ThemeSettings snapshot = ThemeMapper.readSettings(ocean.getConfigSettings());
    assertEquals("#123456", snapshot.getLight().get("primaryColor"));
    assertEquals("Ocean", snapshot.getShared().get(ThemeService.CONFIG_NAME_KEY));
    assertEquals("{}", ocean.getLightSettings(), "configured values are never stored values");
    long version = state().getVersion();

    sync();
    assertEquals(1L, themeDAO.findAll().stream().filter(t -> "ocean".equals(t.getConfigKey())).count());
    assertEquals(version, state().getVersion(), "a start that changes nothing keeps the stylesheet stamp");
  }

  @Test
  void aConfiguredThemeTakesOverTheUIThemeOfItsName() {
    ThemeEntity ui = themeDAO.saveAndFlush(new ThemeEntity(null,
                                                           "Ocean",
                                                           false,
                                                           "ocean",
                                                           ThemeOrigin.UI,
                                                           null,
                                                           null,
                                                           "{}",
                                                           ThemeMapper.writeMap(Map.of("primaryColor", "#000000",
                                                                                       "secondaryColor", "#111111")),
                                                           "{}",
                                                           null));
    configured.add(new ConfiguredTheme("ocean", "OCEAN", light("primaryColor", "#123456")));
    sync();
    ThemeEntity row = themeDAO.findById(ui.getId()).orElseThrow();
    assertEquals("ocean", row.getConfigKey());
    assertEquals(ThemeOrigin.CONFIGURATION, row.getOrigin());
    assertEquals(Map.of("secondaryColor", "#111111"), ThemeMapper.readMap(row.getLightSettings()),
                 "the configured key replaces the interface value, the others stay");
  }

  @Test
  void aConfiguredNameClashingWithTheDefaultThemeCreatesNoRow() {
    configured.add(new ConfiguredTheme("other", "Default Theme", light("primaryColor", "#123456")));
    sync();
    assertNull(themeDAO.findByConfigKey("other"));
    assertEquals(1L, themeDAO.count());
  }

  @Test
  void aThemeRemovedFromTheConfigurationKeepsItsLookAndStaysInUse() {
    configured.add(new ConfiguredTheme("ocean", "Ocean", light("primaryColor", "#123456")));
    when(themeConfiguration.getConfiguredThemeInUse()).thenReturn("ocean");
    sync();
    long ocean = themeDAO.findByConfigKey("ocean").getId();
    assertEquals(ocean, state().getLastEffectiveThemeId());
    assertNull(state().getThemeInUseId(), "the configured choice is read at resolution time");

    configured.clear();
    when(themeConfiguration.getConfiguredThemeInUse()).thenReturn(ThemeService.DEFAULT_THEME_CONFIG_KEY);
    sync();
    ThemeEntity row = themeDAO.findById(ocean).orElseThrow();
    assertEquals(ThemeOrigin.UI, row.getOrigin());
    assertNull(row.getConfigKey());
    assertNull(row.getConfigSettings());
    assertEquals("#123456", ThemeMapper.readMap(row.getLightSettings()).get("primaryColor"));
    assertEquals(ocean, state().getThemeInUseId(), "the theme in use stays in use once its block is removed");
  }

  @Test
  void aConfigurationGoingBackToBothModesBumpsTheGeneration() {
    when(themeConfiguration.getConfiguredModesOffered()).thenReturn(ModesOffered.DARK);
    sync();
    assertEquals(0, state().getModesGeneration());
    when(themeConfiguration.getConfiguredModesOffered()).thenReturn(ModesOffered.LIGHT_AND_DARK);
    sync();
    assertEquals(1, state().getModesGeneration());
    sync();
    assertEquals(1, state().getModesGeneration(), "once");
  }

  @Test
  void theLegacyBrandingIsImportedOnceIntoTheDefaultTheme() {
    ImportPlan plan = new ImportPlan();
    LegacyBranding legacy = new LegacyBranding();
    legacy.getSettings().getLight().put("primaryColor", "#123456");
    legacy.setCompanyName("ACME");
    plan.setLegacy(legacy);
    plan.getImageCopies().put("sideBarBackgroundImage", 201L);
    plan.setLogoCopy(202L);

    assertTrue(importInTransaction(plan).isEmpty());
    ThemeEntity defaultTheme = themeDAO.findById(ThemeService.DEFAULT_THEME_ID).orElseThrow();
    assertEquals(Map.of("primaryColor", "#123456", "sideBarBackgroundImageFile", "201"),
                 ThemeMapper.readMap(defaultTheme.getLightSettings()));
    BrandingStateEntity state = state();
    assertTrue(state.isLegacyImported());
    assertEquals("ACME", state.getCompanyName());
    assertEquals(202L, state.getLogoFileId());

    ImportPlan second = new ImportPlan();
    second.setLegacy(legacy);
    second.getImageCopies().put("sideBarBackgroundImage", 301L);
    second.setLogoCopy(302L);
    assertEquals(List.of(301L, 302L), importInTransaction(second), "another node imported first: its copies are unused");
  }

  @Test
  void aConfiguredDefaultFileFillsOnlyAnEmptyColumnAndIsTriedOncePerPath() {
    ConfigurationSyncResult sync = syncResult();
    ImportPlan plan = synchronizer.planImport(sync);
    assertTrue(plan.getDefaultPaths().containsKey(ThemeService.LOGO_COLUMN));
    importInTransaction(plan);
    Long logo = state().getLogoFileId();
    assertTrue(logo != null && logo > 100);

    // the administrator removed the logo: the same configured path does not bring it back
    BrandingStateEntity state = state();
    state.setLogoFileId(null);
    brandingStateDAO.saveAndFlush(state);
    ImportPlan again = synchronizer.planImport(syncResult());
    assertFalse(again.getDefaultPaths().containsKey(ThemeService.LOGO_COLUMN));
    importInTransaction(again);
    assertNull(state().getLogoFileId());
  }

  @Test
  void theReadinessFlagIsSetAlsoWhenAStepFailed() {
    assertFalse(ThemeReadiness.isReady());
    ThemeConfigurationSynchronizer failingRead = spy(synchronizer);
    doReturn(new ConfigurationSyncResult()).when(failingRead).synchronizeConfiguration();
    when(legacyImporter.read()).thenThrow(new IllegalStateException("settings down"));
    failingRead.synchronize();
    assertTrue(ThemeReadiness.isReady());

    ThemeReadiness.reset();
    ThemeConfigurationSynchronizer failing = spy(synchronizer);
    doThrow(new IllegalStateException("database down")).when(failing).synchronizeConfiguration();
    // the context refresh already ran one synchronisation
    org.mockito.Mockito.clearInvocations(legacyImporter);
    failing.synchronize();
    assertTrue(ThemeReadiness.isReady());
    org.mockito.Mockito.verify(legacyImporter).deleteTertiaryColor();

    ThemeReadiness.reset();
    org.mockito.Mockito.doThrow(new IllegalStateException("settings down")).when(legacyImporter).deleteTertiaryColor();
    assertThrows(IllegalStateException.class, failing::synchronize);
    assertTrue(ThemeReadiness.isReady(), "the flag is set whatever ends the sequence");
    org.mockito.Mockito.doNothing().when(legacyImporter).deleteTertiaryColor();

    ThemeReadiness.reset();
    ThemeConfigurationSynchronizer failingImport = spy(synchronizer);
    doReturn(new ConfigurationSyncResult()).when(failingImport).synchronizeConfiguration();
    doThrow(new IllegalStateException("database down")).when(failingImport).importInTransaction(any());
    failingImport.synchronize();
    assertTrue(ThemeReadiness.isReady());
  }

  @Test
  void aTakeoverWhoseConfiguredValuesDoNotCompileLeavesTheUIThemeAsItIs() {
    ThemeEntity ui = themeDAO.saveAndFlush(new ThemeEntity(null,
                                                           "Ocean",
                                                           false,
                                                           "ocean",
                                                           ThemeOrigin.UI,
                                                           null,
                                                           null,
                                                           "{}",
                                                           ThemeMapper.writeMap(Map.of("primaryColor", "#000000")),
                                                           "{}",
                                                           null));
    when(brandingService.compileThemeStyle(anyMap())).thenThrow(new IllegalArgumentException("branding.theme.stylesheetCompilationError"));
    configured.add(new ConfiguredTheme("ocean", "Ocean", light("primaryColor", "#123456")));
    sync();
    ThemeEntity row = themeDAO.findById(ui.getId()).orElseThrow();
    assertEquals(ThemeOrigin.UI, row.getOrigin());
    assertNull(row.getConfigKey());
    assertEquals(Map.of("primaryColor", "#000000"), ThemeMapper.readMap(row.getLightSettings()));
  }

  @Test
  void aNewConfiguredThemeThatDoesNotCompileIsNotCreatedNorSelected() {
    when(brandingService.compileThemeStyle(anyMap())).thenThrow(new IllegalArgumentException("branding.theme.stylesheetCompilationError"));
    configured.add(new ConfiguredTheme("ocean", "Ocean", light("primaryColor", "#123456")));
    when(themeConfiguration.getConfiguredThemeInUse()).thenReturn("ocean");

    sync();

    assertNull(themeDAO.findByConfigKey("ocean"), "no row is saved for a theme that fails its trial compile");
    assertNull(state().getThemeInUseId());
    assertNull(themeDAO.findByNameKey("ocean"));
  }

  @Test
  void theFirstStylesheetIsCompiledOnceTheThemesAreReadable() {
    List<Boolean> readyWhenCompiled = new ArrayList<>();
    when(brandingService.getThemeStylesheet()).thenAnswer(i -> {
      readyWhenCompiled.add(ThemeReadiness.isReady());
      return null;
    });
    ThemeReadiness.reset();

    synchronizer.synchronize();

    assertEquals(List.of(true), readyWhenCompiled, "compiled once, after the readiness flag");
  }

  @Test
  void aFailingFirstCompileDoesNotStopTheStart() {
    when(brandingService.getThemeStylesheet()).thenThrow(new IllegalStateException("less down"));
    ThemeReadiness.reset();

    synchronizer.synchronize();

    assertTrue(ThemeReadiness.isReady());
  }

  @Test
  void anUnboundThemeStaysOutOfUseWhenTheConfigurationNamesAnotherOne() {
    configured.add(new ConfiguredTheme("ocean", "Ocean", light("primaryColor", "#123456")));
    configured.add(new ConfiguredTheme("forest", "Forest", light("primaryColor", "#228822")));
    when(themeConfiguration.getConfiguredThemeInUse()).thenReturn("ocean");
    sync();
    configured.remove(0);
    when(themeConfiguration.getConfiguredThemeInUse()).thenReturn("forest");
    sync();
    assertNull(state().getThemeInUseId(), "the new configured choice applies");
    assertEquals(themeDAO.findByConfigKey("forest").getId(), state().getLastEffectiveThemeId());
  }

  @Test
  void anAdministratorsSelectionIsNeverReplacedByAnUnbinding() {
    configured.add(new ConfiguredTheme("ocean", "Ocean", light("primaryColor", "#123456")));
    when(themeConfiguration.getConfiguredThemeInUse()).thenReturn("ocean");
    sync();
    BrandingStateEntity state = state();
    state.setThemeInUseId(ThemeService.DEFAULT_THEME_ID);
    brandingStateDAO.saveAndFlush(state);
    configured.clear();
    when(themeConfiguration.getConfiguredThemeInUse()).thenReturn(ThemeService.DEFAULT_THEME_CONFIG_KEY);
    sync();
    assertEquals(ThemeService.DEFAULT_THEME_ID, state().getThemeInUseId());
  }

  @Test
  void aLegacyCompanyNameTooLongIsNotImportedAndTheImportStillSucceeds() {
    ImportPlan plan = new ImportPlan();
    LegacyBranding legacy = new LegacyBranding();
    legacy.setCompanyName("A".repeat(201));
    plan.setLegacy(legacy);
    importInTransaction(plan);
    assertTrue(state().isLegacyImported());
    assertNull(state().getCompanyName());
  }

  private ConfigurationSyncResult syncResult() {
    return new TransactionTemplate(transactionManager).execute(status -> synchronizer.syncConfiguration());
  }

  private void sync() {
    syncResult();
  }

  private List<Long> importInTransaction(ImportPlan plan) {
    return new TransactionTemplate(transactionManager).execute(status -> synchronizer.writeImport(plan));
  }

  private BrandingStateEntity state() {
    return brandingStateDAO.findById(BrandingStateEntity.STATE_ID).orElseThrow();
  }

  @Test
  void aLockTimeoutOnTheStateRowIsToldFromAnyOtherFailure() {
    assertTrue(ThemeConfigurationSynchronizer.isLockTimeout(new org.springframework.dao.CannotAcquireLockException("held",
                                                                                                                  new jakarta.persistence.LockTimeoutException())));
    assertTrue(ThemeConfigurationSynchronizer.isLockTimeout(new RuntimeException(new jakarta.persistence.PessimisticLockException())));
    assertFalse(ThemeConfigurationSynchronizer.isLockTimeout(new IllegalStateException("broken", new RuntimeException())));
    assertFalse(ThemeConfigurationSynchronizer.isLockTimeout(null));
  }

  private static ThemeSettings light(String key, String value) {
    return new ThemeSettings(new HashMap<>(), new HashMap<>(Map.of(key, value)), new HashMap<>());
  }

}
