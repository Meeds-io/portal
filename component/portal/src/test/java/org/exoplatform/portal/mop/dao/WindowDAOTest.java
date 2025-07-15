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
package org.exoplatform.portal.mop.dao;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.EntityTransaction;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.portal.jdbc.entity.WindowEntity;
import org.exoplatform.portal.jdbc.entity.WindowEntity.AppType;

public class WindowDAOTest extends AbstractDAOTest {
  private WindowDAO         windowDAO;

  private EntityTransaction transaction;

  @Override
  protected void setUp() throws Exception {
    begin();
    super.setUp();
    this.windowDAO = getContainer().getComponentInstanceOfType(WindowDAO.class);

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

  public void testCreateContainer() {
    WindowEntity entity = createInstance("content1", AppType.PORTLET);
    windowDAO.create(entity);
    restartTransaction();

    WindowEntity result = windowDAO.find(entity.getId());
    assertNotNull(result);
    assertContainer(entity, result);
  }

  public void testFindByIds() {
    WindowEntity entity1 = createInstance("content1", AppType.PORTLET);
    windowDAO.create(entity1);
    WindowEntity entity2 = createInstance("content2", AppType.PORTLET);
    windowDAO.create(entity2);
    restartTransaction();

    List<WindowEntity> results = windowDAO.findByIds(Arrays.asList(entity1.getId(), entity2.getId()));
    assertEquals(2, results.size());
  }

  public void testDeleteByContentId() {
    String toDeleteContentId = "App1/toDeletePortlet1";
    windowDAO.create(createInstance(toDeleteContentId, AppType.PORTLET));
    windowDAO.create(createInstance(toDeleteContentId, AppType.PORTLET));
    windowDAO.create(createInstance(toDeleteContentId, AppType.PORTLET));
    windowDAO.create(createInstance(toDeleteContentId, AppType.PORTLET));
    windowDAO.create(createInstance(toDeleteContentId, AppType.PORTLET));

    windowDAO.create(createInstance("App2/toNotDeletePortlet2", AppType.PORTLET));
    restartTransaction();

    List<Long> results = windowDAO.findIdsByContentIds(Arrays.asList(toDeleteContentId, "App2/toNotDeletePortlet2"),
                                                       0,
                                                       10);
    assertEquals(6, results.size());

    windowDAO.deleteByContentId(toDeleteContentId);
    results = windowDAO.findIdsByContentIds(Arrays.asList(toDeleteContentId), 0, 10);
    assertEquals(0, results.size());
    results = windowDAO.findIdsByContentIds(Arrays.asList(toDeleteContentId, "App2/toNotDeletePortlet2"), 0, 10);
    assertEquals(1, results.size());
  }

  public void testUpdateContentId() {
    String oldContentId = "App1/toUpdatePortlet1";
    String newContentId = "App1/updatedPortlet1";
    windowDAO.create(createInstance(oldContentId, AppType.PORTLET));
    windowDAO.create(createInstance(oldContentId, AppType.PORTLET));
    windowDAO.create(createInstance(oldContentId, AppType.PORTLET));
    windowDAO.create(createInstance(oldContentId, AppType.PORTLET));
    windowDAO.create(createInstance(oldContentId, AppType.PORTLET));

    windowDAO.create(createInstance("App2/toNotUpdatePortlet2", AppType.PORTLET));
    restartTransaction();

    List<Long> results = windowDAO.findIdsByContentIds(Arrays.asList(newContentId), 0, 10);
    assertEquals(0, results.size());

    results = windowDAO.findIdsByContentIds(Arrays.asList(oldContentId, "App2/toNotUpdatePortlet2"),
                                            0,
                                            10);
    assertEquals(6, results.size());

    windowDAO.updateContentId(oldContentId, newContentId);

    results = windowDAO.findIdsByContentIds(Arrays.asList(oldContentId), 0, 10);
    assertEquals(0, results.size());

    results = windowDAO.findIdsByContentIds(Arrays.asList(oldContentId, "App2/toNotUpdatePortlet2"), 0, 10);
    assertEquals(1, results.size());

    results = windowDAO.findIdsByContentIds(Arrays.asList(oldContentId, newContentId, "App2/toNotUpdatePortlet2"),
                                            0,
                                            10);
    assertEquals(6, results.size());

    results = windowDAO.findIdsByContentIds(Arrays.asList(newContentId), 0, 10);
    assertEquals(5, results.size());
  }

  public void testfindByContentIds() {
    WindowEntity entity1 = createInstance("App1/portlet1", AppType.PORTLET);
    windowDAO.create(entity1);
    WindowEntity entity2 = createInstance("App2/portlet2", AppType.PORTLET);
    windowDAO.create(entity2);
    restartTransaction();

    List<Long> results = windowDAO.findIdsByContentIds(Arrays.asList("App1/portlet1", "App2/portlet1"), 0, 10);
    assertEquals(1, results.size());

    results = windowDAO.findIdsByContentIds(Arrays.asList("app1/portlet1", "app2/portlet2"), 0, 10);
    assertEquals(0, results.size());

    results = windowDAO.findIdsByContentIds(Arrays.asList("App1/portlet1", "App2/portlet2"), 0, 10);
    assertEquals(2, results.size());

    results = windowDAO.findIdsByContentIds(Arrays.asList("App1/portlet1", "App2/portlet2"), 0, 1);
    assertEquals(1, results.size());

    results = windowDAO.findIdsByContentIds(Arrays.asList("App1/portlet1", "App2/portlet2"), 1, 1);
    assertEquals(1, results.size());

    results = windowDAO.findIdsByContentIds(Arrays.asList("App1/portlet1", "App2/portlet2"), 2, 1);
    assertEquals(0, results.size());

    results = windowDAO.findIdsByContentIds(Arrays.asList(), 0, 10);
    assertNotNull(results);
    assertEquals(0, results.size());

    results = windowDAO.findIdsByContentIds(Arrays.asList("app1"), 0, 10);
    assertNotNull(results);
    assertEquals(0, results.size());
  }

  private WindowEntity createInstance(String contentId, AppType type) {
    WindowEntity entity = new WindowEntity();
    entity.setAppType(type);
    entity.setContentId(contentId);
    entity.setCustomization("testCustom".getBytes());
    entity.setHeight("testHeight");
    entity.setIcon("testIcon");
    entity.setProperties("testProps");
    entity.setShowApplicationMode(true);
    entity.setShowApplicationState(true);
    entity.setShowInfoBar(true);
    entity.setTheme("testTheme");
    entity.setTitle("testTitle");
    entity.setDescription("testDesc");
    entity.setTitle("testTitle");
    entity.setWidth("testWidth");
    return entity;
  }

  private void assertContainer(WindowEntity expected, WindowEntity result) {
    assertEquals(expected.getDescription(), result.getDescription());
    assertEquals(expected.getHeight(), result.getHeight());
    assertEquals(expected.getIcon(), result.getIcon());
    assertEquals(expected.getId(), result.getId());
    assertEquals(expected.getProperties(), result.getProperties());
    assertEquals(expected.getTitle(), result.getTitle());
    assertEquals(expected.getWidth(), result.getWidth());
    assertEquals(expected.getContentId(), result.getContentId());
    assertEquals(expected.getTheme(), result.getTheme());
    assertEquals(expected.isShowApplicationMode(), result.isShowApplicationMode());
    assertEquals(expected.isShowApplicationState(), result.isShowApplicationState());
    assertEquals(expected.isShowInfoBar(), result.isShowInfoBar());
    assertEquals(expected.getAppType(), result.getAppType());
    assertEquals(expected.getCustomization().length, result.getCustomization().length);
  }
}
