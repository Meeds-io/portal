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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.springframework.test.context.ActiveProfiles;
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
import org.exoplatform.services.security.Identity;

import io.meeds.portal.theme.constant.ModesOffered;
import io.meeds.portal.theme.constant.ThemeOrigin;
import io.meeds.portal.theme.dao.BrandingStateDAO;
import io.meeds.portal.theme.dao.ThemeDAO;
import io.meeds.portal.theme.dao.ThemeJpaTestConfiguration;
import io.meeds.portal.theme.entity.BrandingStateEntity;
import io.meeds.portal.theme.entity.ThemeEntity;
import io.meeds.portal.theme.model.BrandingPublication;
import io.meeds.portal.theme.model.PlannedTheme;
import io.meeds.portal.theme.model.PublicationPlan;
import io.meeds.portal.theme.model.PublicationResult;
import io.meeds.portal.theme.model.ThemePublication;
import io.meeds.portal.theme.model.ThemeReference;
import io.meeds.portal.theme.model.ThemeSettings;
import io.meeds.portal.theme.storage.ThemeFileStorage;
import io.meeds.portal.theme.storage.ThemeStorage;
import io.meeds.portal.theme.util.ThemeMapper;
import io.meeds.portal.theme.util.ThemeValueResolver;

/**
 * The publication of the Branding and Theme page, over the real tables on
 * HSQLDB. The transactional entry point is woven by the container aspect, so
 * the tests drive its two un-advised halves: {@code plan} (validation and new
 * files, before the transaction) and {@code writePublication} (the rows, in
 * the transaction a {@link TransactionTemplate} opens here).
 */
@DataJpaTest(showSql = false)
@ActiveProfiles(ThemeJpaTestConfiguration.PROFILE)
@EnableAutoConfiguration
@ContextConfiguration(classes = ThemeJpaTestConfiguration.class)
@Import({ ThemeStorage.class, ThemeService.class, ThemeConfiguration.class })
@TestPropertySource(properties = { ThemeJpaTestConfiguration.LIQUIBASE_CHANGELOG, ThemeJpaTestConfiguration.NO_DDL })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ThemeServiceTest {

  private static final String        ADMIN       = "root";

  private static final byte[]        PNG         = { (byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4 };

  @MockitoBean
  private BrandingService            brandingService;

  @MockitoBean
  private UserACL                    userAcl;

  @MockitoBean
  private org.exoplatform.commons.api.settings.SettingService settingService;

  @MockitoBean
  private ListenerService            listenerService;

  @MockitoBean
  private ThemeFileStorage           themeFileStorage;

  @Autowired
  private org.springframework.core.env.ConfigurableEnvironment environment;

  @Autowired
  private ThemeService               themeService;

  @Autowired
  private ThemeDAO                   themeDAO;

  @Autowired
  private BrandingStateDAO           brandingStateDAO;

  @Autowired
  private PlatformTransactionManager transactionManager;

  private final AtomicLong           nextFileId  = new AtomicLong(100);

  @BeforeEach
  void setUp() {
    Map<String, String> defaults = new HashMap<>();
    defaults.put("primaryColor", "#3f8487");
    defaults.put("secondaryColor", "#476A9C");
    defaults.put("sideBarBackgroundImage", "none");
    defaults.put("topBarBackgroundImage", "none");
    defaults.put("pageBackgroundEffect", "none");
    defaults.put("textColor", "#20282C");
    when(brandingService.getDefaultThemeStyle()).thenReturn(defaults);
    when(brandingService.compileThemeStyle(anyMap())).thenReturn("");
    // the interface's merge of the template literals and the configured defaults
    when(brandingService.getResolutionDefaults()).thenCallRealMethod();
    Identity admin = new Identity(ADMIN);
    when(userAcl.getUserIdentity(ADMIN)).thenReturn(admin);
    when(userAcl.isAdministrator(admin)).thenReturn(true);
    when(themeFileStorage.readUpload(anyString())).thenReturn(PNG);
    when(themeFileStorage.writeFile(anyString(), anyString(), any())).thenAnswer(i -> nextFileId.incrementAndGet());
    when(themeFileStorage.copyFile(anyLong())).thenAnswer(i -> nextFileId.incrementAndGet());
  }

  @Test
  void addsAThemeAndSelectsItInTheSamePublication() throws Exception {
    BrandingPublication publication = publication(0);
    publication.getThemes().add(newTheme("new-1", "Ocean", Map.of("primaryColor", "#123456")));
    publication.setThemeInUse(new ThemeReference(null, "new-1"));

    PublicationResult result = publish(publication);

    ThemeEntity ocean = themeDAO.findByNameKey("ocean");
    assertEquals(ThemeOrigin.UI, ocean.getOrigin());
    assertEquals(Map.of("primaryColor", "#123456"), ThemeMapper.readMap(ocean.getLightSettings()));
    BrandingStateEntity state = state();
    assertEquals(ocean.getId(), state.getThemeInUseId());
    assertEquals(1L, state.getVersion());
    assertEquals(1L, result.getVersion());
  }

  @Test
  void aPublicationFromAnOlderVersionIsAConflictAndWritesNothing() throws Exception {
    publish(withTheme(publication(0), "new-1", "Ocean"));
    BrandingPublication stale = withTheme(publication(0), "new-2", "Forest");

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> publish(stale));
    assertEquals(ThemeService.PUBLISH_CONFLICT_MESSAGE, e.getMessage());
    assertNull(themeDAO.findByNameKey("forest"));
    assertEquals(1L, state().getVersion());
  }

  @Test
  void deletingTheThemeInUseMovesTheSelectionToTheDefaultTheme() throws Exception {
    BrandingPublication add = withTheme(publication(0), "new-1", "Ocean");
    add.setThemeInUse(new ThemeReference(null, "new-1"));
    publish(add);
    long ocean = themeDAO.findByNameKey("ocean").getId();

    BrandingPublication delete = publication(1);
    delete.getDeletedThemeIds().add(ocean);
    publish(delete);

    assertFalse(themeDAO.existsById(ocean));
    assertEquals(ThemeService.DEFAULT_THEME_ID, state().getThemeInUseId());
  }

  @Test
  void theDefaultThemeAndConfiguredThemesCannotBeDeleted() {
    long configured = insertConfigured("forest", "Forest", null).getId();
    for (long id : List.of(ThemeService.DEFAULT_THEME_ID, configured)) {
      BrandingPublication delete = publication(0);
      delete.getDeletedThemeIds().add(id);
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> publish(delete));
      assertEquals(ThemeService.NOT_DELETABLE_MESSAGE, e.getMessage());
    }
    assertEquals(0L, state().getVersion());
  }

  @Test
  void twoThemesSwapTheirNamesInOnePublication() throws Exception {
    BrandingPublication add = withTheme(withTheme(publication(0), "new-1", "Ocean"), "new-2", "Forest");
    publish(add);
    long ocean = themeDAO.findByNameKey("ocean").getId();
    long forest = themeDAO.findByNameKey("forest").getId();

    BrandingPublication swap = publication(1);
    swap.getThemes().add(existingTheme(ocean, "Forest", Map.of()));
    swap.getThemes().add(existingTheme(forest, "Ocean", Map.of()));
    publish(swap);

    assertEquals(forest, themeDAO.findByNameKey("ocean").getId());
    assertEquals(ocean, themeDAO.findByNameKey("forest").getId());
  }

  @Test
  void aDeletedNameIsReusedInTheSamePublication() throws Exception {
    publish(withTheme(publication(0), "new-1", "Ocean"));
    long ocean = themeDAO.findByNameKey("ocean").getId();

    BrandingPublication replace = withTheme(publication(1), "new-2", "OCEAN");
    replace.getDeletedThemeIds().add(ocean);
    publish(replace);

    assertTrue(themeDAO.findByNameKey("ocean").getId() > ocean);
  }

  @Test
  void twoNamesDifferingByCaseAreRefusedBeforeTheTransaction() {
    BrandingPublication publication = withTheme(withTheme(publication(0), "new-1", "Ocean"), "new-2", "OCEAN");
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> publish(publication));
    assertEquals(ThemeService.NAME_ALREADY_EXISTS_MESSAGE, e.getMessage());
    assertEquals(1L, themeDAO.count());
  }

  @Test
  void theDefaultThemeCannotBeRenamed() {
    BrandingPublication publication = publication(0);
    publication.getThemes().add(existingTheme(ThemeService.DEFAULT_THEME_ID, "My default", Map.of()));
    assertEquals(ThemeService.DEFAULT_THEME_RENAME_MESSAGE,
                 assertThrows(IllegalArgumentException.class, () -> publish(publication)).getMessage());
  }

  @Test
  void startingFromAConfiguredThemeCopiesItsLookAndKeepsARestore() throws Exception {
    ThemeSettings configured = new ThemeSettings(new HashMap<>(),
                                                 new HashMap<>(Map.of("primaryColor", "#111111",
                                                                      "secondaryColor", "#222222")),
                                                 new HashMap<>());
    ThemeEntity forest = insertConfigured("forest", "Forest", configured);
    forest.setLightSettings(ThemeMapper.writeMap(Map.of("sideBarBackgroundImage",
                                                        "none",
                                                        "sideBarBackgroundImageFile",
                                                        "7")));
    themeDAO.saveAndFlush(forest);

    BrandingPublication publication = publication(0);
    Map<String, String> light = new HashMap<>();
    // the admin restored the colours area on the copy: the configured secondary travels as a null
    light.put("secondaryColor", null);
    ThemePublication copy = newTheme("new-1", "Forest copy", light);
    copy.setStartFrom(new ThemeReference(forest.getId(), null));
    publication.getThemes().add(copy);
    publish(publication);

    Map<String, String> stored = ThemeMapper.readMap(themeDAO.findByNameKey("forest copy").getLightSettings());
    assertEquals("#111111", stored.get("primaryColor"), "the configured look is copied");
    assertFalse(stored.containsKey("secondaryColor"), "the restore is kept, not the source's value");
    assertEquals("101", stored.get("sideBarBackgroundImageFile"), "the source's file is copied into a new id");
    verify(themeFileStorage).copyFile(7L);
  }

  @Test
  void aFileIdTheThemeDoesNotHoldIsRefused() throws Exception {
    publish(withTheme(publication(0), "new-1", "Ocean"));
    long ocean = themeDAO.findByNameKey("ocean").getId();

    BrandingPublication forged = publication(1);
    forged.getThemes().add(existingTheme(ocean, "Ocean", Map.of("sideBarBackgroundImageFile", "4242")));
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> publish(forged));
    assertEquals(ThemeService.FILE_REFERENCE_INVALID_MESSAGE + "sideBarBackgroundImageFile", e.getMessage());

    BrandingPublication forgedNew = publication(1);
    forgedNew.getThemes().add(newTheme("new-2", "Forest", Map.of("sideBarBackgroundImageFile", "4242")));
    assertThrows(IllegalArgumentException.class, () -> publish(forgedNew));
  }

  @Test
  void anUploadBecomesTheImageKeysFileAndTheReplacedFileIsUnused() throws Exception {
    publish(withTheme(publication(0), "new-1", "Ocean", Map.of("sideBarBackgroundImageUpload", "up-1")));
    ThemeEntity ocean = themeDAO.findByNameKey("ocean");
    assertEquals("101", ThemeMapper.readMap(ocean.getLightSettings()).get("sideBarBackgroundImageFile"));

    BrandingPublication replace = publication(1);
    replace.getThemes().add(existingTheme(ocean.getId(), "Ocean", Map.of("sideBarBackgroundImageUpload", "up-2")));
    PublicationResult result = publish(replace);

    assertEquals("102", ThemeMapper.readMap(themeDAO.findByNameKey("ocean").getLightSettings()).get("sideBarBackgroundImageFile"));
    assertEquals(List.of(101L), result.getUnusedFileIds());
  }

  @Test
  void goingBackToBothModesBumpsTheGeneration() throws Exception {
    BrandingPublication dark = publication(0);
    dark.setModesOffered(ModesOffered.DARK);
    PublicationResult first = publish(dark);
    assertTrue(first.isModesChanged());
    assertEquals(0, state().getModesGeneration());

    BrandingPublication both = publication(1);
    both.setModesOffered(ModesOffered.LIGHT_AND_DARK);
    PublicationResult second = publish(both);
    assertTrue(second.isModesChanged());
    assertEquals(1, state().getModesGeneration());
  }

  @Test
  void aRefusedPublicationLeavesItsUploadsForTheCorrectedResend() throws Exception {
    ThemeService service = transactionalSpy();
    publish(withTheme(publication(0), "new-1", "Ocean"));
    BrandingPublication duplicate = withTheme(publication(1), "new-2", "Ocean", Map.of("sideBarBackgroundImageUpload", "up-1"));

    assertEquals(ThemeService.NAME_ALREADY_EXISTS_MESSAGE,
                 assertThrows(IllegalArgumentException.class, () -> service.publish(duplicate, ADMIN)).getMessage());
    verify(themeFileStorage, never()).removeUpload(anyString());

    BrandingPublication corrected = withTheme(publication(1), "new-2", "Forest", Map.of("sideBarBackgroundImageUpload", "up-1"));
    assertEquals(2L, service.publish(corrected, ADMIN));
    verify(themeFileStorage).removeUpload("up-1");
  }

  @Test
  void aUserWhoIsNotAnAdministratorCannotPublish() {
    Identity john = new Identity("john");
    when(userAcl.getUserIdentity("john")).thenReturn(john);
    assertThrows(IllegalAccessException.class, () -> themeService.publish(publication(0), "john"));
    verify(brandingService, never()).compileThemeStyle(any());
  }

  /**
   * The tests below run the publication in a transaction they open
   * themselves, as the woven method needs the portal container: this pin kills
   * the mutant that removes the annotation, or its atomic flag, from the entry
   * point that makes the writes commit together.
   */
  @Test
  void thePublicationEntryPointIsAnAtomicContainerTransaction() throws Exception {
    java.lang.reflect.Method entry = ThemeService.class.getDeclaredMethod("publishInTransaction", PublicationPlan.class);
    io.meeds.common.ContainerTransactional transactional = entry.getAnnotation(io.meeds.common.ContainerTransactional.class);
    assertTrue(transactional != null && transactional.atomic(),
               "publishInTransaction is @ContainerTransactional(atomic = true)");
  }

  @Test
  void theTransactionalEntryPointIsNotPublicSoNoCallerSkipsTheValidation() throws Exception {
    assertFalse(java.lang.reflect.Modifier.isPublic(ThemeService.class.getDeclaredMethod("publishInTransaction", PublicationPlan.class)
                                                                      .getModifiers()),
                "publishInTransaction takes a validated plan: the checked writers are the only callers");
  }

  @Test
  void aWriteFailingMidwayLeavesNoRowAndNoStateChange() {
    PublicationPlan plan = new PublicationPlan();
    plan.setVersion(0);
    plan.setModesOffered(ModesOffered.DARK);
    // two new themes with one name: the second insert breaks the name key, after the first was written
    plan.getThemes().add(new PlannedTheme(null, "new-1", "Ocean", new ThemeSettings()));
    plan.getThemes().add(new PlannedTheme(null, "new-2", "ocean", new ThemeSettings()));

    RuntimeException e = assertThrows(RuntimeException.class,
                                      () -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                                        try {
                                          themeService.writePublication(plan);
                                        } catch (Exception ex) {
                                          throw new RuntimeException(ex); // NOSONAR test
                                        }
                                      }));
    assertEquals(ThemeService.NAME_ALREADY_EXISTS_MESSAGE, ThemeService.translate(e).getMessage());
    assertEquals(1L, themeDAO.count(), "the first insert is rolled back");
    BrandingStateEntity state = state();
    assertEquals(0L, state.getVersion());
    assertNull(state.getModesOffered());
  }

  @Test
  void aCommittedPublicationDeletesTheReplacedFilesAndAnnouncesItself() throws Exception {
    publish(withTheme(publication(0), "new-1", "Ocean", Map.of("sideBarBackgroundImageUpload", "up-1")));
    long ocean = themeDAO.findByNameKey("ocean").getId();
    ThemeService service = transactionalSpy();

    BrandingPublication replace = publication(1);
    replace.getThemes().add(existingTheme(ocean, "Ocean", Map.of("sideBarBackgroundImageUpload", "up-2")));
    replace.setModesOffered(ModesOffered.DARK);
    assertEquals(2L, service.publish(replace, ADMIN));

    verify(themeFileStorage).deleteFile(101L);
    verify(themeFileStorage, never()).deleteFile(102L);
    verify(listenerService).broadcast(eq(org.exoplatform.portal.branding.BrandingService.BRANDING_UPDATED_EVENT), eq(ADMIN), eq(2L));
    verify(listenerService).broadcast(eq(ThemeService.MODES_OFFERED_UPDATED_EVENT),
                                      eq(ADMIN),
                                      eq(Map.of("modesOffered", "DARK", "themeInUseId", ThemeService.DEFAULT_THEME_ID)));
  }

  @Test
  void aConflictDeletesEveryFileThePublicationWrote() throws Exception {
    ThemeService service = transactionalSpy();
    publish(withTheme(publication(0), "new-1", "Ocean"));

    BrandingPublication stale = withTheme(publication(0), "new-2", "Forest", Map.of("sideBarBackgroundImageUpload", "up-1"));
    stale.setBranding(new io.meeds.portal.theme.model.BrandingIdentityPublication(null,
                                                                                  new io.meeds.portal.theme.model.BrandingFilePublication("up-2",
                                                                                                                                          false),
                                                                                  null,
                                                                                  null,
                                                                                  null));
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> service.publish(stale, ADMIN));
    assertEquals(ThemeService.PUBLISH_CONFLICT_MESSAGE, e.getMessage());
    verify(themeFileStorage).deleteFile(101L);
    verify(themeFileStorage).deleteFile(102L);
    verify(listenerService, never()).broadcast(anyString(), any(), any());
  }

  @Test
  void anUploadForAnUndeclaredImageKeyWritesNoFile() {
    BrandingPublication publication = withTheme(publication(0), "new-1", "Ocean", Map.of("fooBackgroundImageUpload", "up-1"));
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> publish(publication));
    assertEquals(ThemeValueValidator.UNKNOWN_KEY_MESSAGE + "fooBackgroundImageFile", e.getMessage());
    verify(themeFileStorage, never()).readUpload(anyString());
  }

  @Test
  void aSharedSettingInAVersionMapIsRefused() {
    BrandingPublication publication = publication(0);
    ThemePublication theme = newTheme("new-1", "Ocean", Map.of());
    theme.setDark(new HashMap<>(Map.of("fontFamily", "Open Sans")));
    publication.getThemes().add(theme);
    assertEquals(ThemeService.WRONG_VERSION_MESSAGE + "fontFamily",
                 assertThrows(IllegalArgumentException.class, () -> publish(publication)).getMessage());

    BrandingPublication colorShared = publication(0);
    ThemePublication other = newTheme("new-1", "Ocean", Map.of());
    other.setShared(new HashMap<>(Map.of("primaryColor", "#123456")));
    colorShared.getThemes().add(other);
    assertEquals(ThemeService.WRONG_VERSION_MESSAGE + "primaryColor",
                 assertThrows(IllegalArgumentException.class, () -> publish(colorShared)).getMessage());
  }

  @Test
  void aMapTheBodyLeavesOutKeepsItsStoredValues() throws Exception {
    publish(withTheme(publication(0), "new-1", "Ocean", Map.of("primaryColor", "#123456")));
    long ocean = themeDAO.findByNameKey("ocean").getId();
    BrandingPublication rename = publication(1);
    rename.getThemes().add(new ThemePublication(ocean, null, "Deep ocean", null, null, null, null));
    publish(rename);
    assertEquals(Map.of("primaryColor", "#123456"), ThemeMapper.readMap(themeDAO.findById(ocean).orElseThrow().getLightSettings()));
  }

  @Test
  void aDisplayModeIsReadLenientlyAndAnOlderGenerationReadsAsAutomatic() {
    assertEquals(io.meeds.portal.theme.constant.DisplayMode.DARK, ThemeService.parseDisplayMode("DARK@0", 0));
    assertEquals(io.meeds.portal.theme.constant.DisplayMode.AUTOMATIC, ThemeService.parseDisplayMode("DARK@0", 1));
    for (String malformed : new String[] { null, "", "DARK", "SEPIA@0", "DARK@x", "@0", "DARK@0@1" }) {
      assertEquals(io.meeds.portal.theme.constant.DisplayMode.AUTOMATIC, ThemeService.parseDisplayMode(malformed, 0), malformed);
    }
  }

  @Test
  void aUsersModeIsStoredWithTheGenerationAndAnnounced() {
    themeService.setDisplayMode("john", io.meeds.portal.theme.constant.DisplayMode.DARK);
    org.mockito.ArgumentCaptor<org.exoplatform.commons.api.settings.SettingValue> value =
                                                                                          org.mockito.ArgumentCaptor.forClass(org.exoplatform.commons.api.settings.SettingValue.class);
    verify(settingService).set(eq(org.exoplatform.commons.api.settings.data.Context.USER.id("john")),
                               eq(ThemeService.DISPLAY_MODE_SCOPE),
                               eq(ThemeService.DISPLAY_MODE_KEY),
                               value.capture());
    assertEquals("DARK@0", value.getValue().getValue());
    verify(listenerService).broadcast(ThemeService.DISPLAY_MODE_UPDATED_EVENT, "john", "DARK");
  }

  @Test
  void aSingleModeOfferedIsEveryPagesModeAndAnAnonymousPageFollowsTheDevice() throws Exception {
    when(settingService.get(any(), any(), eq(ThemeService.DISPLAY_MODE_KEY))).thenReturn((org.exoplatform.commons.api.settings.SettingValue) org.exoplatform.commons.api.settings.SettingValue.create("DARK@0"));
    assertEquals(io.meeds.portal.theme.constant.DisplayMode.DARK, themeService.getEffectiveDisplayMode("john"));
    assertEquals(io.meeds.portal.theme.constant.DisplayMode.AUTOMATIC, themeService.getEffectiveDisplayMode(null));
    BrandingPublication light = publication(0);
    light.setModesOffered(ModesOffered.LIGHT);
    publish(light);
    assertEquals(io.meeds.portal.theme.constant.DisplayMode.LIGHT, themeService.getEffectiveDisplayMode("john"));
  }

  @Test
  void theFormerPageMergesIntoTheThemeInUseWithoutPinningDefaults() throws Exception {
    publish(withTheme(publication(0), "new-1", "Ocean", Map.of("secondaryColor", "#112233")));
    ThemeService service = transactionalSpy();
    org.exoplatform.portal.branding.model.Branding branding = new org.exoplatform.portal.branding.model.Branding();
    branding.setThemeStyle(Map.of("primaryColor", "#123456", "secondaryColor", "#476A9C"));
    branding.setCompanyName("ACME");
    service.publishLegacy(branding, false);

    ThemeEntity defaultTheme = themeDAO.findById(ThemeService.DEFAULT_THEME_ID).orElseThrow();
    assertEquals(Map.of("primaryColor", "#123456"), ThemeMapper.readMap(defaultTheme.getLightSettings()),
                 "the value equal to its default is not stored");
    assertEquals("ACME", state().getCompanyName());
    assertEquals(2L, state().getVersion());
    assertEquals(Map.of("secondaryColor", "#112233"), ThemeMapper.readMap(themeDAO.findByNameKey("ocean").getLightSettings()),
                 "only the theme in use is written");

    // a single-field update keeps every other key
    org.exoplatform.portal.branding.model.Branding style = new org.exoplatform.portal.branding.model.Branding();
    style.setThemeStyle(Map.of("textColor", "#333333"));
    when(brandingService.getDefaultThemeStyle()).thenReturn(new HashMap<>(Map.of("primaryColor", "#3f8487",
                                                                                 "secondaryColor", "#476A9C",
                                                                                 "textColor", "#20282C")));
    service.publishLegacy(style, true);
    assertEquals(Map.of("primaryColor", "#123456", "textColor", "#333333"),
                 ThemeMapper.readMap(themeDAO.findById(ThemeService.DEFAULT_THEME_ID).orElseThrow().getLightSettings()));
  }

  @Test
  void aDarkVersionSharesTheLightImageUnlessItHasItsOwn() throws Exception {
    publish(withTheme(publication(0), "new-1", "Ocean", Map.of("sideBarBackgroundImageUpload", "up-1")));
    long ocean = themeDAO.findByNameKey("ocean").getId();
    assertEquals(101L, themeService.getThemeImageFileId(ocean, "sideBarBackgroundImage", true));
    BrandingPublication darkImage = publication(1);
    ThemePublication theme = existingTheme(ocean, "Ocean", Map.of("sideBarBackgroundImageFile", "101"));
    theme.setDark(new HashMap<>(Map.of("sideBarBackgroundImageUpload", "up-2")));
    darkImage.getThemes().add(theme);
    publish(darkImage);
    assertEquals(102L, themeService.getThemeImageFileId(ocean, "sideBarBackgroundImage", true));
    assertEquals(101L, themeService.getThemeImageFileId(ocean, "sideBarBackgroundImage", false));
    assertThrows(org.exoplatform.commons.exception.ObjectNotFoundException.class,
                 () -> themeService.getThemeImageFileId(ocean, "primaryColor", false));
  }

  @Test
  void aTemplateDefaultWithABlankConfiguredValueGetsItsDarkValue() throws Exception {
    when(brandingService.getTemplateDefaults()).thenReturn(Map.of("appBackgroundColor", "#FFFFFF", "appTextTitleColor", "#20282C"));
    io.meeds.portal.theme.model.ResolvedTheme resolved = themeService.resolve(themeService.getTheme(ThemeService.DEFAULT_THEME_ID));
    assertEquals("#1E2328", resolved.getDark().get("appBackgroundColor"));
    assertEquals("#E6E9EB", resolved.getDark().get("appTextTitleColor"));
  }

  @Test
  void anExistingThemesSubmittedMapIsItsWholeStoredMap() throws Exception {
    publish(withTheme(publication(0), "new-1", "Ocean", Map.of("primaryColor", "#123456", "secondaryColor", "#654321")));
    long ocean = themeDAO.findByNameKey("ocean").getId();
    assertEquals(Map.of("primaryColor", "#123456", "secondaryColor", "#654321"),
                 ThemeMapper.readMap(themeDAO.findById(ocean).orElseThrow().getLightSettings()));

    BrandingPublication restore = publication(1);
    restore.getThemes().add(existingTheme(ocean, "Ocean", Map.of("primaryColor", "#123456")));
    publish(restore);

    assertEquals(Map.of("primaryColor", "#123456"),
                 ThemeMapper.readMap(themeDAO.findById(ocean).orElseThrow().getLightSettings()),
                 "a key the page no longer sends is dropped, not merged back");
  }

  @Test
  void aCustomThemeDoesNotInheritTheDefaultThemesStoredValues() throws Exception {
    ThemeEntity defaultTheme = themeDAO.findById(ThemeService.DEFAULT_THEME_ID).orElseThrow();
    defaultTheme.setLightSettings(ThemeMapper.writeMap(Map.of("primaryColor", "#aa0000")));
    themeDAO.saveAndFlush(defaultTheme);
    publish(withTheme(publication(0), "new-1", "Ocean", Map.of("secondaryColor", "#00aa00")));

    io.meeds.portal.theme.model.ResolvedTheme resolved = themeService.resolve(themeService.getTheme(themeDAO.findByNameKey("ocean").getId()));
    assertEquals("#00aa00", resolved.getLight().get("secondaryColor"), "its own stored value");
    assertEquals("#3f8487", resolved.getLight().get("primaryColor"), "the configured default, never the Default Theme's stored value");
    assertEquals("#aa0000", themeService.resolve(themeService.getTheme(ThemeService.DEFAULT_THEME_ID)).getLight().get("primaryColor"));
  }

  @Test
  void aCustomThemeTakesTheDefaultThemesConfiguredDarkValueOnlyWhileItsLightValueIsInherited() throws Exception {
    environment.getPropertySources()
               .addFirst(new org.springframework.core.env.MapPropertySource("darkDefaults",
                                                                            Map.of("exo.branding.theme.dark.primaryColor", "#8AB4F8",
                                                                                   "exo.branding.theme.dark.secondaryColor", "#99CC99")));
    publish(withTheme(publication(0), "new-1", "Ocean", Map.of("secondaryColor", "#00aa00")));

    io.meeds.portal.theme.model.ResolvedTheme resolved = themeService.resolve(themeService.getTheme(themeDAO.findByNameKey("ocean").getId()));
    assertEquals("#8AB4F8", resolved.getDark().get("primaryColor"), "the light value is the Default Theme's: its configured dark value");
    assertEquals(ThemeValueResolver.resolve(Map.of(), Map.of("secondaryColor", "#00aa00"), Map.of()).getDark().get("secondaryColor"),
                 resolved.getDark().get("secondaryColor"),
                 "the light value is the theme's own: Automatic from it, not the Default Theme's dark value");
  }

  @Test
  void aCustomThemesOwnDarkValueWinsOverTheDefaultThemesConfiguredOne() throws Exception {
    environment.getPropertySources()
               .addFirst(new org.springframework.core.env.MapPropertySource("darkDefaults",
                                                                            Map.of("exo.branding.theme.dark.primaryColor", "#8AB4F8")));
    BrandingPublication creation = withTheme(publication(0), "new-1", "Ocean", Map.of());
    creation.getThemes().get(creation.getThemes().size() - 1).getDark().put("primaryColor", "#112233");
    publish(creation);

    io.meeds.portal.theme.model.ResolvedTheme resolved = themeService.resolve(themeService.getTheme(themeDAO.findByNameKey("ocean").getId()));
    assertEquals("#112233", resolved.getDark().get("primaryColor"));
  }

  @Test
  void aBoundThemeResolvesItsConfiguredValuesUnderItsStoredOnes() throws Exception {
    ThemeSettings configured = new ThemeSettings(new HashMap<>(),
                                                 new HashMap<>(Map.of("primaryColor", "#111111", "secondaryColor", "#222222")),
                                                 new HashMap<>());
    ThemeEntity forest = insertConfigured("forest", "Forest", configured);
    forest.setLightSettings(ThemeMapper.writeMap(Map.of("secondaryColor", "#333333")));
    themeDAO.saveAndFlush(forest);

    io.meeds.portal.theme.model.ResolvedTheme resolved = themeService.resolve(themeService.getTheme(forest.getId()));

    assertEquals("#111111", resolved.getLight().get("primaryColor"), "the configured value over the configured default");
    assertEquals("#333333", resolved.getLight().get("secondaryColor"), "the stored value over the configured one");
  }

  @Test
  void theDarkLogoAndFaviconFallBackToTheLightOnesUnlessSet() throws Exception {
    BrandingStateEntity row = state();
    row.setLogoFileId(5L);
    row.setFaviconFileId(6L);
    brandingStateDAO.saveAndFlush(row);
    assertEquals(5L, themeService.getEffectiveDarkLogoFileId());
    assertEquals(6L, themeService.getEffectiveDarkFaviconFileId());

    row = state();
    row.setDarkLogoFileId(7L);
    row.setDarkFaviconFileId(8L);
    brandingStateDAO.saveAndFlush(row);
    assertEquals(7L, themeService.getEffectiveDarkLogoFileId());
    assertEquals(8L, themeService.getEffectiveDarkFaviconFileId());

    row = state();
    row.setLogoFileId(null);
    row.setDarkLogoFileId(null);
    brandingStateDAO.saveAndFlush(row);
    assertNull(themeService.getEffectiveDarkLogoFileId());
  }

  @Test
  void theConfigurationHashIsADigestOfACanonicalForm() {
    String canonical = "{}|default|LIGHT_AND_DARK|" + new java.util.TreeMap<>(brandingService.getDefaultThemeStyle());
    java.util.zip.CRC32 crc = new java.util.zip.CRC32();
    crc.update(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    assertEquals(crc.getValue(), themeService.getConfigurationHash());
  }

  @Test
  void aPublicationNeverWritesTheCustomCss() throws Exception {
    transactionalSpy().publish(withTheme(publication(0), "new-1", "Ocean"), ADMIN);

    verify(brandingService, never()).validateCustomCss(any());
    verify(brandingService, never()).updateCustomCss(any());
  }

  @Test
  void theCustomCssSaveWritesTheSettingThenMovesTheVersionAndBroadcastsIt() throws Exception {
    ThemeService service = transactionalSpy();

    long version = service.saveCustomCss(".a { color: red; }", ADMIN);

    assertEquals(1L, version);
    assertEquals(1L, state().getVersion());
    org.mockito.InOrder order = org.mockito.Mockito.inOrder(service, brandingService, listenerService);
    order.verify(brandingService).validateCustomCss(".a { color: red; }");
    order.verify(brandingService).updateCustomCss(".a { color: red; }");
    order.verify(service).moveVersionInTransaction();
    order.verify(listenerService).broadcast(BrandingService.BRANDING_UPDATED_EVENT, ADMIN, 1L);
  }

  @Test
  void theCustomCssSaveIsForAdministratorsOnly() throws Exception {
    ThemeService service = transactionalSpy();

    assertThrows(IllegalAccessException.class, () -> service.saveCustomCss(".a { color: red; }", "mary"));
    assertThrows(IllegalAccessException.class, () -> service.saveCustomCss(".a { color: red; }", null));

    verify(brandingService, never()).updateCustomCss(any());
    assertEquals(0L, state().getVersion());
  }

  @Test
  void aForbiddenCustomCssIsRefusedBeforeAnyWrite() throws Exception {
    ThemeService service = transactionalSpy();
    org.mockito.Mockito.doThrow(new IllegalArgumentException("branding.customCss.forbiddenValue"))
                       .when(brandingService)
                       .validateCustomCss("a { width: eval(1) }");

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                              () -> service.saveCustomCss("a { width: eval(1) }", ADMIN));
    assertEquals("branding.customCss.forbiddenValue", e.getMessage());
    verify(brandingService, never()).updateCustomCss(any());
    verify(service, never()).moveVersionInTransaction();
    verify(listenerService, never()).broadcast(eq(BrandingService.BRANDING_UPDATED_EVENT), any(), any());
    assertEquals(0L, state().getVersion());
  }

  @Test
  void aFailingCustomCssWriteIsAServerErrorAndLeavesTheVersionUnchanged() throws Exception {
    ThemeService service = transactionalSpy();
    org.mockito.Mockito.doThrow(new RuntimeException("engine text")).when(brandingService).updateCustomCss(any());

    RuntimeException e = assertThrows(RuntimeException.class, () -> service.saveCustomCss(".a { color: red; }", ADMIN));
    assertEquals(ThemeService.PUBLISH_FAILED_MESSAGE, e.getMessage());
    assertEquals(0L, state().getVersion());
    verify(service, never()).moveVersionInTransaction();
    verify(listenerService, never()).broadcast(eq(BrandingService.BRANDING_UPDATED_EVENT), any(), any());
  }

  @Test
  void theStateIsReReadAtMostOncePerWindow() {
    int refreshes = 0;
    for (int i = 0; i < 100; i++) {
      refreshes += themeService.refreshState() ? 1 : 0;
    }
    assertEquals(1, refreshes);
  }

  /**
   * The service with its transactional entry point run in a transaction this
   * test opens: the woven method needs the portal container.
   */
  private ThemeService transactionalSpy() throws Exception {
    ThemeService service = org.mockito.Mockito.spy(themeService);
    org.mockito.Mockito.doAnswer(invocation -> {
      List<PublicationResult> result = new ArrayList<>();
      new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
        try {
          result.add(themeService.writePublication(invocation.getArgument(0)));
        } catch (RuntimeException e) {
          throw e;
        } catch (Exception e) {
          throw new WrappedException(e);
        }
      });
      return result.get(0);
    }).when(service).publishInTransaction(any());
    org.mockito.Mockito.doAnswer(invocation -> new TransactionTemplate(transactionManager)
                                                                                         .execute(status -> themeService.moveVersionInTransaction()))
                       .when(service)
                       .moveVersionInTransaction();
    return service;
  }

  private PublicationResult publish(BrandingPublication publication) throws Exception {
    PublicationPlan plan = new PublicationPlan();
    themeService.plan(publication, plan);
    List<PublicationResult> result = new ArrayList<>();
    try {
      new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
        try {
          result.add(themeService.writePublication(plan));
        } catch (Exception e) {
          throw new WrappedException(e);
        }
      });
    } catch (WrappedException e) {
      throw (Exception) e.getCause();
    }
    return result.get(0);
  }

  private BrandingStateEntity state() {
    return brandingStateDAO.findById(BrandingStateEntity.STATE_ID).orElseThrow();
  }

  private ThemeEntity insertConfigured(String key, String name, ThemeSettings configured) {
    return themeDAO.saveAndFlush(new ThemeEntity(null,
                                                 name,
                                                 false,
                                                 name.toLowerCase(),
                                                 ThemeOrigin.CONFIGURATION,
                                                 key,
                                                 ThemeMapper.writeSettings(configured),
                                                 "{}",
                                                 "{}",
                                                 "{}",
                                                 new java.util.Date()));
  }

  private static BrandingPublication publication(long version) {
    BrandingPublication publication = new BrandingPublication();
    publication.setVersion(version);
    return publication;
  }

  private static BrandingPublication withTheme(BrandingPublication publication, String clientId, String name) {
    return withTheme(publication, clientId, name, Map.of());
  }

  private static BrandingPublication withTheme(BrandingPublication publication,
                                               String clientId,
                                               String name,
                                               Map<String, String> light) {
    publication.getThemes().add(newTheme(clientId, name, light));
    return publication;
  }

  private static ThemePublication newTheme(String clientId, String name, Map<String, String> light) {
    return new ThemePublication(null, clientId, name, new HashMap<>(), new HashMap<>(light), new HashMap<>(), null);
  }

  private static ThemePublication existingTheme(long id, String name, Map<String, String> light) {
    return new ThemePublication(id, null, name, new HashMap<>(), new HashMap<>(light), new HashMap<>(), null);
  }

  private static class WrappedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    WrappedException(Exception cause) {
      super(cause);
    }
  }

}
