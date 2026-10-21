/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package org.exoplatform.portal.mop.storage;

import static org.junit.Assert.assertThrows;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.EntityTransaction;

import org.json.simple.JSONArray;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.portal.config.model.ApplicationBackgroundStyle;
import org.exoplatform.portal.config.model.ModelStyle;
import org.exoplatform.portal.config.model.PersistentApplicationState;
import org.exoplatform.portal.jdbc.entity.ComponentEntity;
import org.exoplatform.portal.jdbc.entity.ContainerEntity;
import org.exoplatform.portal.jdbc.entity.WindowEntity;
import org.exoplatform.portal.jdbc.entity.WindowEntity.AppType;
import org.exoplatform.portal.mop.dao.AbstractDAOTest;
import org.exoplatform.portal.mop.dao.WindowDAO;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.storage.cache.CacheLayoutStorage;
import org.exoplatform.portal.mop.storage.utils.MOPUtils;
import org.exoplatform.portal.pom.data.ComponentData;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

public class LayoutStorageTest extends AbstractDAOTest {

  private LayoutStorage      layoutStorage;

  private EntityTransaction transaction;

  @Override
  protected void setUp() throws Exception {
    begin();
    super.setUp();
    this.layoutStorage = getContainer().getComponentInstanceOfType(LayoutStorage.class);

    EntityManagerService managerService = getContainer().getComponentInstanceOfType(EntityManagerService.class);
    transaction = managerService.getEntityManager().getTransaction();
    transaction.begin();
  }

  @Override
  protected void tearDown() throws Exception {
    if (transaction.isActive()) {
      transaction.rollback();
    }
    super.tearDown();
    end();
  }

  public void testTextBackgroundStylePersistedAndReloaded() {
    ModelStyle style = new ModelStyle();
    style.setTextTitleBackgroundColor("#FFFFFFFF");
    style.setTextTitleBackgroundImage("/portal/rest/v1/social/attachments/containerTextTitleBackground/1/2");
    style.setTextTitleBackgroundEffect("linear-gradient(#FFFFFFFF 0%, #999999FF 100%)");
    style.setTextTitleBackgroundPosition("top left");
    style.setTextTitleBackgroundSize("cover");
    style.setTextTitleBackgroundRepeat("no-repeat");
    style.setTextTitleBackgroundPadding("10px 10px 10px 10px");
    style.setTextTitleBackgroundRadius("8px 8px 8px 8px");

    style.setTextHeaderBackgroundColor("#EEEEEEFF");
    style.setTextHeaderBackgroundImage("/portal/rest/v1/social/attachments/containerTextHeaderBackground/1/2");
    style.setTextHeaderBackgroundEffect("linear-gradient(#EEEEEEFF 0%, #888888FF 100%)");
    style.setTextHeaderBackgroundPosition("top right");
    style.setTextHeaderBackgroundSize("contain");
    style.setTextHeaderBackgroundRepeat("repeat");
    style.setTextHeaderBackgroundPadding("5px 5px 5px 5px");
    style.setTextHeaderBackgroundRadius("4px 4px 4px 4px");

    style.setTextBackgroundColor("#DDDDDDFF");
    style.setTextBackgroundImage("/portal/rest/v1/social/attachments/containerTextBodyBackground/1/2");
    style.setTextBackgroundEffect("linear-gradient(#DDDDDDFF 0%, #777777FF 100%)");
    style.setTextBackgroundPosition("bottom left");
    style.setTextBackgroundSize("cover");
    style.setTextBackgroundRepeat("no-repeat");
    style.setTextBackgroundPadding("3px 3px 3px 3px");
    style.setTextBackgroundRadius("2px 2px 2px 2px");

    style.setTextSubtitleBackgroundColor("#CCCCCCFF");
    style.setTextSubtitleBackgroundImage("/portal/rest/v1/social/attachments/containerTextSubtitleBackground/1/2");
    style.setTextSubtitleBackgroundEffect("linear-gradient(#CCCCCCFF 0%, #666666FF 100%)");
    style.setTextSubtitleBackgroundPosition("bottom right");
    style.setTextSubtitleBackgroundSize("contain");
    style.setTextSubtitleBackgroundRepeat("repeat-x");
    style.setTextSubtitleBackgroundPadding("1px 1px 1px 1px");
    style.setTextSubtitleBackgroundRadius("1px 1px 1px 1px");

    ContainerData containerData = new ContainerData(null,
                                                     "testTextBackground",
                                                     "testTextBackground",
                                                     null,
                                                     "system:/groovy/portal/webui/container/UIContainer.gtmpl",
                                                     null,
                                                     null,
                                                     null,
                                                     null,
                                                     null,
                                                     null,
                                                     null,
                                                     style,
                                                     null,
                                                     Collections.emptyList(),
                                                     Collections.emptyList());

    List<ComponentEntity> saved = layoutStorage.saveChildren(new JSONArray(),
                                                             Collections.<ComponentData> singletonList(containerData));
    assertEquals(1, saved.size());
    restartTransaction();

    ContainerEntity savedEntity = (ContainerEntity) saved.get(0);
    JSONArray body = new JSONArray();
    body.add(savedEntity.toJSON());

    List<ComponentData> loaded = layoutStorage.buildChildren(body);
    assertEquals(1, loaded.size());
    ContainerData loadedContainer = (ContainerData) loaded.get(0);
    ModelStyle loadedStyle = loadedContainer.getCssStyle();
    assertNotNull(loadedStyle);

    assertEquals("#FFFFFFFF", loadedStyle.getTextTitleBackgroundColor());
    assertEquals("/portal/rest/v1/social/attachments/containerTextTitleBackground/1/2", loadedStyle.getTextTitleBackgroundImage());
    assertEquals("linear-gradient(#FFFFFFFF 0%, #999999FF 100%)", loadedStyle.getTextTitleBackgroundEffect());
    assertEquals("top left", loadedStyle.getTextTitleBackgroundPosition());
    assertEquals("cover", loadedStyle.getTextTitleBackgroundSize());
    assertEquals("no-repeat", loadedStyle.getTextTitleBackgroundRepeat());
    assertEquals("10px 10px 10px 10px", loadedStyle.getTextTitleBackgroundPadding());
    assertEquals("8px 8px 8px 8px", loadedStyle.getTextTitleBackgroundRadius());

    assertEquals("#EEEEEEFF", loadedStyle.getTextHeaderBackgroundColor());
    assertEquals("/portal/rest/v1/social/attachments/containerTextHeaderBackground/1/2", loadedStyle.getTextHeaderBackgroundImage());
    assertEquals("linear-gradient(#EEEEEEFF 0%, #888888FF 100%)", loadedStyle.getTextHeaderBackgroundEffect());
    assertEquals("top right", loadedStyle.getTextHeaderBackgroundPosition());
    assertEquals("contain", loadedStyle.getTextHeaderBackgroundSize());
    assertEquals("repeat", loadedStyle.getTextHeaderBackgroundRepeat());
    assertEquals("5px 5px 5px 5px", loadedStyle.getTextHeaderBackgroundPadding());
    assertEquals("4px 4px 4px 4px", loadedStyle.getTextHeaderBackgroundRadius());

    assertEquals("#DDDDDDFF", loadedStyle.getTextBackgroundColor());
    assertEquals("/portal/rest/v1/social/attachments/containerTextBodyBackground/1/2", loadedStyle.getTextBackgroundImage());
    assertEquals("linear-gradient(#DDDDDDFF 0%, #777777FF 100%)", loadedStyle.getTextBackgroundEffect());
    assertEquals("bottom left", loadedStyle.getTextBackgroundPosition());
    assertEquals("cover", loadedStyle.getTextBackgroundSize());
    assertEquals("no-repeat", loadedStyle.getTextBackgroundRepeat());
    assertEquals("3px 3px 3px 3px", loadedStyle.getTextBackgroundPadding());
    assertEquals("2px 2px 2px 2px", loadedStyle.getTextBackgroundRadius());

    assertEquals("#CCCCCCFF", loadedStyle.getTextSubtitleBackgroundColor());
    assertEquals("/portal/rest/v1/social/attachments/containerTextSubtitleBackground/1/2",
                 loadedStyle.getTextSubtitleBackgroundImage());
    assertEquals("linear-gradient(#CCCCCCFF 0%, #666666FF 100%)", loadedStyle.getTextSubtitleBackgroundEffect());
    assertEquals("bottom right", loadedStyle.getTextSubtitleBackgroundPosition());
    assertEquals("contain", loadedStyle.getTextSubtitleBackgroundSize());
    assertEquals("repeat-x", loadedStyle.getTextSubtitleBackgroundRepeat());
    assertEquals("1px 1px 1px 1px", loadedStyle.getTextSubtitleBackgroundPadding());
    assertEquals("1px 1px 1px 1px", loadedStyle.getTextSubtitleBackgroundRadius());
  }

  public void testImageBackgroundStylePersistedAndReloaded() {
    ModelStyle style = new ModelStyle();
    style.setBackgroundColor("#FFFFFF00");
    style.setBackgroundImage("/portal/rest/v1/social/attachments/containerBackground/1/2");
    style.setBackgroundSize("cover");
    style.setBackgroundPosition("center");
    style.setBackgroundRepeat("no-repeat");
    style.setBackgroundAttachment("fixed");

    ContainerData containerData = new ContainerData(null,
                                                    "testImageBackground",
                                                    "testImageBackground",
                                                    null,
                                                    "system:/groovy/portal/webui/container/UIContainer.gtmpl",
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    style,
                                                    null,
                                                    Collections.emptyList(),
                                                    Collections.emptyList());

    List<ComponentEntity> saved = layoutStorage.saveChildren(new JSONArray(),
                                                             Collections.<ComponentData> singletonList(containerData));
    assertEquals(1, saved.size());
    restartTransaction();

    ContainerEntity savedEntity = (ContainerEntity) saved.get(0);
    JSONArray body = new JSONArray();
    body.add(savedEntity.toJSON());

    List<ComponentData> loaded = layoutStorage.buildChildren(body);
    assertEquals(1, loaded.size());
    ContainerData loadedContainer = (ContainerData) loaded.get(0);
    ModelStyle loadedStyle = loadedContainer.getCssStyle();
    assertNotNull(loadedStyle);

    assertEquals("#FFFFFF00", loadedStyle.getBackgroundColor());
    assertEquals("/portal/rest/v1/social/attachments/containerBackground/1/2", loadedStyle.getBackgroundImage());
    assertEquals("cover", loadedStyle.getBackgroundSize());
    assertEquals("center", loadedStyle.getBackgroundPosition());
    assertEquals("no-repeat", loadedStyle.getBackgroundRepeat());
    assertEquals("fixed", loadedStyle.getBackgroundAttachment());
  }

  public void testIconColorPersistedAndReloaded() {
    // The page/app icon colour, stored as the icon-color attribute next to text-color
    ModelStyle style = new ModelStyle();
    style.setTextColor("#20282C");
    style.setIconColor("#AABBCC");

    ContainerData containerData = new ContainerData(null,
                                                    "testIconColor",
                                                    "testIconColor",
                                                    null,
                                                    "system:/groovy/portal/webui/container/UIContainer.gtmpl",
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    style,
                                                    null,
                                                    Collections.emptyList(),
                                                    Collections.emptyList());

    List<ComponentEntity> saved = layoutStorage.saveChildren(new JSONArray(),
                                                             Collections.<ComponentData> singletonList(containerData));
    assertEquals(1, saved.size());
    restartTransaction();

    ContainerEntity savedEntity = (ContainerEntity) saved.get(0);
    JSONArray body = new JSONArray();
    body.add(savedEntity.toJSON());

    List<ComponentData> loaded = layoutStorage.buildChildren(body);
    assertEquals(1, loaded.size());
    ModelStyle loadedStyle = ((ContainerData) loaded.get(0)).getCssStyle();
    assertNotNull(loadedStyle);
    assertEquals("#20282C", loadedStyle.getTextColor());
    assertEquals("#AABBCC", loadedStyle.getIconColor());
  }

  public void testApplicationMarginsPersistedAndReloaded() {
    // default margins of the applications of a page, stored as app-margin-* attributes
    ApplicationBackgroundStyle appStyle = new ApplicationBackgroundStyle();
    appStyle.setBackgroundColor("#FFFFFFFF");
    appStyle.setMarginTop(0);
    appStyle.setMarginRight(12);
    appStyle.setMarginBottom(24);
    appStyle.setMarginLeft(null);

    ContainerData containerData = new ContainerData(null,
                                                    "testApplicationMargins",
                                                    "testApplicationMargins",
                                                    null,
                                                    "system:/groovy/portal/webui/container/UIContainer.gtmpl",
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    appStyle,
                                                    Collections.emptyList(),
                                                    Collections.emptyList());

    List<ComponentEntity> saved = layoutStorage.saveChildren(new JSONArray(),
                                                             Collections.<ComponentData> singletonList(containerData));
    assertEquals(1, saved.size());
    restartTransaction();

    ContainerEntity savedEntity = (ContainerEntity) saved.get(0);
    JSONArray body = new JSONArray();
    body.add(savedEntity.toJSON());

    List<ComponentData> loaded = layoutStorage.buildChildren(body);
    assertEquals(1, loaded.size());
    ApplicationBackgroundStyle loadedStyle = ((ContainerData) loaded.get(0)).getAppBackgroundStyle();
    assertNotNull(loadedStyle);
    assertEquals("#FFFFFFFF", loadedStyle.getBackgroundColor());
    // 0 is a value (an explicit "no margin"), null is "not set"
    assertEquals(Integer.valueOf(0), loadedStyle.getMarginTop());
    assertEquals(Integer.valueOf(12), loadedStyle.getMarginRight());
    assertEquals(Integer.valueOf(24), loadedStyle.getMarginBottom());
    assertNull(loadedStyle.getMarginLeft());
  }

  /**
   * The preferences cache keeps a window until a write through the storage
   * empties it: fails the mutant reading the window through it
   */
  public void testLoadCommittedReadsTheRowNotThePreferencesCache() {
    WindowDAO windowDAO = getContainer().getComponentInstanceOfType(WindowDAO.class);
    WindowEntity window = new WindowEntity();
    window.setAppType(AppType.PORTLET);
    window.setContentId("app/portlet");
    window.setCustomization(MOPUtils.serialize(new Portlet().setValue("name", "cached")));
    window = windowDAO.create(window);
    restartTransaction();
    String storageId = String.valueOf(window.getId());
    PersistentApplicationState state = new PersistentApplicationState(storageId);
    assertEquals("cached", layoutStorage.load(state).getValue("name"));

    // Written through the DAO, the cache entry is kept
    WindowEntity committedWindow = windowDAO.find(window.getId());
    committedWindow.setCustomization(MOPUtils.serialize(new Portlet().setValue("name", "committed")));
    windowDAO.update(committedWindow);
    restartTransaction();

    assertEquals("cached", layoutStorage.load(state).getValue("name"));
    assertEquals("committed", layoutStorage.loadCommitted(storageId).getValue("name"));
  }

  /**
   * A request's persistence context returns a window as the request first
   * loaded it, whatever another request committed since: fails the mutants of
   * the storage and of the service reading that window
   */
  public void testLoadCommittedThroughTheServiceReadsWhatAnotherRequestCommitted() throws Exception {
    LayoutService layoutService = getContainer().getComponentInstanceOfType(LayoutService.class);
    WindowDAO windowDAO = getContainer().getComponentInstanceOfType(WindowDAO.class);
    Long id = createWindow(windowDAO, new Portlet().setValue("name", "loaded"));
    String storageId = String.valueOf(id);
    assertNotNull(windowDAO.find(id));

    inAnotherRequest(() -> {
      WindowEntity window = windowDAO.find(id);
      window.setCustomization(MOPUtils.serialize(new Portlet().setValue("name", "committed")));
      windowDAO.update(window);
    });
    assertEquals("committed", layoutService.loadCommitted(storageId).getValue("name"));

    inAnotherRequest(() -> windowDAO.delete(windowDAO.find(id)));
    assertNotNull(windowDAO.find(id));
    assertNull(layoutService.loadCommitted(storageId));
  }

  /**
   * A request's persistence context still holds a window another request
   * deleted: fails the mutants of the storage and of the service answering
   * from that window
   */
  public void testExistsCommittedThroughTheServiceReadsWhatAnotherRequestCommitted() throws Exception {
    LayoutService layoutService = getContainer().getComponentInstanceOfType(LayoutService.class);
    WindowDAO windowDAO = getContainer().getComponentInstanceOfType(WindowDAO.class);
    Long id = createWindow(windowDAO, new Portlet().setValue("name", "loaded"));
    String storageId = String.valueOf(id);
    assertNotNull(windowDAO.find(id));
    assertTrue(layoutService.existsCommitted(storageId));

    inAnotherRequest(() -> windowDAO.delete(windowDAO.find(id)));
    assertNotNull(windowDAO.find(id));
    assertFalse(layoutService.existsCommitted(storageId));
  }

  /**
   * The preferences cache keeps a window until a write through the storage
   * empties it: fails the mutants of the storage and of the service writing
   * the preferences from that cached window, whose former columns they write
   * back, and the mutant keeping its cache entry
   */
  public void testSaveCommittedThroughTheServiceKeepsTheColumnsAnotherRequestCommitted() throws Exception {
    LayoutService layoutService = getContainer().getComponentInstanceOfType(LayoutService.class);
    WindowDAO windowDAO = getContainer().getComponentInstanceOfType(WindowDAO.class);
    ExoCache<Long, ?> preferencesCache = getContainer().getComponentInstanceOfType(CacheService.class)
                                                       .getCacheInstance(CacheLayoutStorage.PORTLET_PREFERENCES_CACHE_NAME);
    Long id = createWindow(windowDAO, new Portlet().setValue("name", "loaded"));
    String storageId = String.valueOf(id);
    assertEquals("loaded", layoutService.load(new PersistentApplicationState(storageId)).getValue("name"));
    assertNotNull(preferencesCache.get(id));

    // Written through the DAO, the cache entry is kept
    inAnotherRequest(() -> {
      WindowEntity window = windowDAO.find(id);
      window.setTitle("committed");
      windowDAO.update(window);
    });
    assertTrue(layoutService.saveCommitted(storageId, new Portlet().setValue("name", "written")));
    assertNull(preferencesCache.get(id));

    AtomicReference<WindowEntity> stored = new AtomicReference<>();
    inAnotherRequest(() -> stored.set(windowDAO.find(id)));
    assertEquals("committed", stored.get().getTitle());
    assertEquals("written", ((Portlet) MOPUtils.unserialize(stored.get().getCustomization())).getValue("name"));

    assertTrue(layoutService.saveCommitted(storageId, null));
    assertNotNull(layoutService.loadCommitted(storageId));
    assertFalse(layoutService.loadCommitted(storageId).iterator().hasNext());
    assertFalse(layoutService.saveCommitted(String.valueOf(Long.MAX_VALUE), new Portlet()));
    assertFalse(layoutService.saveCommitted("notAnId", new Portlet()));
  }

  public void testLoadCommittedOfAWindowWithoutPreferencesIsEmpty() {
    WindowDAO windowDAO = getContainer().getComponentInstanceOfType(WindowDAO.class);
    WindowEntity window = new WindowEntity();
    window.setAppType(AppType.PORTLET);
    window.setContentId("app/portlet");
    window = windowDAO.create(window);
    restartTransaction();

    Portlet preferences = layoutStorage.loadCommitted(String.valueOf(window.getId()));
    assertNotNull(preferences);
    assertFalse(preferences.iterator().hasNext());
  }

  /**
   * Preferences that cannot be deserialized fail their read: fails the mutant
   * reading them only to test that the window exists
   */
  public void testExistsCommittedDoesNotReadThePreferences() {
    WindowDAO windowDAO = getContainer().getComponentInstanceOfType(WindowDAO.class);
    WindowEntity window = new WindowEntity();
    window.setAppType(AppType.PORTLET);
    window.setContentId("app/portlet");
    window.setCustomization(new byte[] { 1, 2, 3 });
    window = windowDAO.create(window);
    restartTransaction();
    String storageId = String.valueOf(window.getId());
    assertThrows(IllegalStateException.class, () -> layoutStorage.loadCommitted(storageId));

    assertTrue(layoutStorage.existsCommitted(storageId));
    assertFalse(layoutStorage.existsCommitted(String.valueOf(Long.MAX_VALUE)));
    assertFalse(layoutStorage.existsCommitted("notAnId"));
    assertFalse(layoutStorage.existsCommitted(null));
  }

  public void testLoadCommittedOfAMissingWindowIsNull() {
    assertNull(layoutStorage.loadCommitted(String.valueOf(Long.MAX_VALUE)));
    assertNull(layoutStorage.loadCommitted("notAnId"));
    assertNull(layoutStorage.loadCommitted(null));
  }

  /**
   * @return the id of a window created with the given preferences, committed
   */
  private Long createWindow(WindowDAO windowDAO, Portlet preferences) {
    WindowEntity window = new WindowEntity();
    window.setAppType(AppType.PORTLET);
    window.setContentId("app/portlet");
    window.setCustomization(MOPUtils.serialize(preferences));
    window = windowDAO.create(window);
    restartTransaction();
    return window.getId();
  }

}
