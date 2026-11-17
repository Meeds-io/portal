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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.branding.model.Branding;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.Identity;

import io.meeds.portal.theme.constant.DisplayMode;
import io.meeds.portal.theme.constant.ThemeOrigin;
import io.meeds.portal.theme.dao.BrandingStateDAO;
import io.meeds.portal.theme.dao.ThemeDAO;
import io.meeds.portal.theme.dao.ThemeJpaTestConfiguration;
import io.meeds.portal.theme.entity.BrandingStateEntity;
import io.meeds.portal.theme.entity.ThemeEntity;
import io.meeds.portal.theme.model.BrandingPublication;
import io.meeds.portal.theme.model.PublicationResult;
import io.meeds.portal.theme.model.ThemePublication;
import io.meeds.portal.theme.storage.ThemeFileStorage;
import io.meeds.portal.theme.storage.ThemeStorage;

/**
 * The node-local snapshot cache of the state and the themes, over a real
 * cache manager: a drift between its {@code @Cacheable} and {@code @CacheEvict}
 * would make every eviction a silent no-op. Another node's commit is played
 * through the DAOs, which never evict.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles(ThemeJpaTestConfiguration.PROFILE)
@EnableAutoConfiguration
@ContextConfiguration(classes = { ThemeJpaTestConfiguration.class, ThemeCacheTest.CacheTestConfiguration.class })
@Import({ ThemeStorage.class, ThemeService.class, ThemeConfiguration.class })
@TestPropertySource(properties = { ThemeJpaTestConfiguration.LIQUIBASE_CHANGELOG, ThemeJpaTestConfiguration.NO_DDL })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ThemeCacheTest {

  private static final String        ADMIN = "root";

  @Configuration
  @EnableCaching
  static class CacheTestConfiguration {
    @Bean
    ConcurrentMapCacheManager cacheManager() {
      return new ConcurrentMapCacheManager(ThemeStorage.SNAPSHOT_CACHE);
    }
  }

  @MockitoBean
  private BrandingService            brandingService;

  @MockitoBean
  private UserACL                    userAcl;

  @MockitoBean
  private SettingService             settingService;

  @MockitoBean
  private ListenerService            listenerService;

  @MockitoBean
  private ThemeFileStorage           themeFileStorage;

  @Autowired
  private ThemeService               themeService;

  @Autowired
  private ThemeStorage               themeStorage;

  @Autowired
  private ThemeDAO                   themeDAO;

  @Autowired
  private BrandingStateDAO           brandingStateDAO;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @BeforeEach
  void setUp() {
    Map<String, String> defaults = new HashMap<>();
    defaults.put("primaryColor", "#3f8487");
    defaults.put("secondaryColor", "#476A9C");
    defaults.put("pageBackgroundEffect", "none");
    when(brandingService.getDefaultThemeStyle()).thenReturn(defaults);
    when(brandingService.compileThemeStyle(anyMap())).thenReturn("");
    when(brandingService.getResolutionDefaults()).thenCallRealMethod();
    Identity admin = new Identity(ADMIN);
    when(userAcl.getUserIdentity(ADMIN)).thenReturn(admin);
    when(userAcl.isAdministrator(admin)).thenReturn(true);
  }

  @Test
  void aStaleSnapshotServesTheStateAndTheThemesOfOneLoad() {
    assertEquals(0L, themeStorage.getState().getVersion());
    assertEquals(1, themeStorage.getThemes().size());

    anotherNodeCommitsATheme("Ocean", 7L);

    assertEquals(0L, themeStorage.getState().getVersion(), "the state is the one of the load the themes come from");
    assertEquals(1, themeStorage.getThemes().size());
    assertEquals(0L, themeService.getState().getVersion());
    assertEquals(1, themeService.getThemes().size());
  }

  @Test
  void evictingTheSnapshotRefreshesTheStateAndTheThemesTogether() {
    themeStorage.getState();
    anotherNodeCommitsATheme("Ocean", 7L);

    themeStorage.evictAll();

    assertEquals(7L, themeStorage.getState().getVersion());
    assertEquals(2, themeStorage.getThemes().size());
  }

  @Test
  void refreshingTheStateRefreshesTheThemesToo() {
    themeStorage.getThemes();
    anotherNodeCommitsATheme("Ocean", 7L);

    assertTrue(themeService.refreshState());

    assertEquals(7L, themeService.getState().getVersion());
    assertEquals(2, themeService.getThemes().size());
  }

  @Test
  void anUnknownThemeIdCostsNoDatabaseRead() {
    assertEquals(1, themeStorage.getThemes().size());
    long ocean = anotherNodeCommitsATheme("Ocean", 7L);

    assertNull(themeStorage.getTheme(ocean), "served from the snapshot: the row was not read");
    assertNull(themeStorage.getTheme(999L));
    themeStorage.evictAll();
    assertEquals("Ocean", themeStorage.getTheme(ocean).getName());
  }

  @Test
  void aPublicationOnThisNodeMakesTheStateAndTheThemesVisible() throws Exception {
    assertEquals(0L, themeService.getState().getVersion());
    ThemeService service = transactionalSpy();

    long version = service.publish(withTheme(publication(0), "new-1", "Ocean"), ADMIN);

    assertEquals(1L, version);
    assertEquals(1L, themeService.getState().getVersion());
    assertTrue(themeService.getThemes().stream().anyMatch(t -> "Ocean".equals(t.getName())));
  }

  @Test
  void theCustomCssSaveOnThisNodeMakesTheMovedVersionVisible() throws Exception {
    assertEquals(0L, themeService.getState().getVersion());
    ThemeService service = org.mockito.Mockito.spy(themeService);
    org.mockito.Mockito.doAnswer(invocation -> new TransactionTemplate(transactionManager).execute(status -> themeService.moveVersionInTransaction()))
                       .when(service)
                       .moveVersionInTransaction();

    long version = service.saveCustomCss(".a{color:red}", ADMIN);

    assertEquals(1L, version);
    // the page reloads after the save and sends this version with its next publication
    assertEquals(1L, themeService.getState().getVersion());
  }

  @Test
  void aConflictEvictsTheSnapshotSoTheNodeStopsConflicting() throws Exception {
    ThemeService service = transactionalSpy();
    assertEquals(0L, themeService.getState().getVersion());
    anotherNodeCommitsATheme("Ocean", 7L);

    IllegalStateException e = assertThrows(IllegalStateException.class,
                                           () -> service.publish(withTheme(publication(0), "new-1", "Forest"), ADMIN));

    assertEquals(ThemeService.PUBLISH_CONFLICT_MESSAGE, e.getMessage());
    assertEquals(7L, themeService.getState().getVersion(), "the page's reload reads the database");
    assertEquals(2, themeService.getThemes().size());
  }

  @Test
  void aConflictingPublicationFromThePageRemovesItsUploadsOnce() throws Exception {
    ThemeService service = transactionalSpy();
    when(themeFileStorage.readUpload("up-1")).thenReturn(new byte[] { (byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4 });
    when(themeFileStorage.writeFile(any(), any(), any())).thenReturn(55L);
    assertEquals(0L, themeService.getState().getVersion());
    anotherNodeCommitsATheme("Ocean", 7L);
    BrandingPublication publication = publication(0);
    publication.setBranding(new io.meeds.portal.theme.model.BrandingIdentityPublication(null,
                                                                                        new io.meeds.portal.theme.model.BrandingFilePublication("up-1",
                                                                                                                                                 false),
                                                                                        null,
                                                                                        null,
                                                                                        null));

    assertThrows(IllegalStateException.class, () -> service.publish(publication, ADMIN));

    // the page reloads after a conflict and drops what it sent: the upload is not sent again
    verify(themeFileStorage, org.mockito.Mockito.times(1)).removeUpload("up-1");
  }

  @Test
  void aLegacyWriterBehindTheDatabaseRebuildsFromAFreshReadAndWrites() throws Exception {
    ThemeService service = transactionalSpy();
    assertEquals(0L, themeService.getState().getVersion());
    anotherNodeCommitsATheme("Ocean", 7L);
    Branding branding = new Branding();
    branding.setThemeStyle(new HashMap<>(Map.of("primaryColor", "#123456")));

    long version = service.publishLegacy(branding, true);

    assertEquals(8L, version);
    assertEquals(8L, themeService.getState().getVersion());
    ThemeEntity defaultTheme = themeDAO.findById(ThemeService.DEFAULT_THEME_ID).orElseThrow();
    assertTrue(defaultTheme.getLightSettings().contains("#123456"));
  }

  @Test
  void aLegacyWriterRetryingOnAConflictStillFindsItsUploadAndRemovesItOnce() throws Exception {
    ThemeService service = transactionalSpy();
    when(themeFileStorage.readUpload("up-1")).thenReturn(new byte[] { (byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4 });
    when(themeFileStorage.writeFile(any(), any(), any())).thenReturn(55L);
    assertEquals(0L, themeService.getState().getVersion());
    anotherNodeCommitsATheme("Ocean", 7L);
    Branding branding = new Branding();
    branding.setThemeStyle(new HashMap<>());
    org.exoplatform.portal.branding.model.Logo background = new org.exoplatform.portal.branding.model.Logo();
    background.setUploadId("up-1");
    branding.setLogo(background);

    assertEquals(8L, service.publishLegacy(branding, false));

    verify(themeFileStorage, org.mockito.Mockito.times(2)).readUpload("up-1");
    verify(themeFileStorage, org.mockito.Mockito.times(1)).removeUpload("up-1");
  }

  @Test
  void aDisplayModeIsStampedWithTheGenerationOfTheDatabaseNotTheCachedOne() {
    assertEquals(0, themeStorage.getState().getModesGeneration());
    BrandingStateEntity state = brandingStateDAO.findById(BrandingStateEntity.STATE_ID).orElseThrow();
    state.setModesGeneration(1);
    brandingStateDAO.saveAndFlush(state);

    themeService.setDisplayMode("john", DisplayMode.DARK);

    org.mockito.ArgumentCaptor<SettingValue<?>> stored = org.mockito.ArgumentCaptor.forClass(SettingValue.class);
    verify(settingService).set(any(), any(), eq(ThemeService.DISPLAY_MODE_KEY), stored.capture());
    assertEquals("DARK@1", stored.getValue().getValue());
    themeStorage.evictAll();
    assertEquals(DisplayMode.DARK, ThemeService.parseDisplayMode("DARK@1", themeStorage.getState().getModesGeneration()));
  }

  private long anotherNodeCommitsATheme(String name, long version) {
    ThemeEntity theme = themeDAO.saveAndFlush(new ThemeEntity(null,
                                                              name,
                                                              false,
                                                              name.toLowerCase(),
                                                              ThemeOrigin.UI,
                                                              null,
                                                              null,
                                                              "{}",
                                                              "{}",
                                                              "{}",
                                                              new java.util.Date()));
    BrandingStateEntity state = brandingStateDAO.findById(BrandingStateEntity.STATE_ID).orElseThrow();
    state.setVersion(version);
    brandingStateDAO.saveAndFlush(state);
    return theme.getId();
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
          throw new IllegalStateException(e);
        }
      });
      return result.get(0);
    }).when(service).publishInTransaction(any());
    return service;
  }

  private static BrandingPublication publication(long version) {
    BrandingPublication publication = new BrandingPublication();
    publication.setVersion(version);
    return publication;
  }

  private static BrandingPublication withTheme(BrandingPublication publication, String clientId, String name) {
    publication.getThemes().add(new ThemePublication(null, clientId, name, new HashMap<>(), new HashMap<>(), new HashMap<>(), null));
    return publication;
  }

}
