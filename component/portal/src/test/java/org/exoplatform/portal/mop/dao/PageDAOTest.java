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

import org.exoplatform.portal.jdbc.entity.PageEntity;
import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.mop.PageType;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.page.PageKey;

public class PageDAOTest extends AbstractDAOTest {
  private PageDAO pageDAO;
  private SiteDAO siteDAO;
  
  @Override
  protected void setUp() throws Exception {    
    super.setUp();
    this.pageDAO = getService(PageDAO.class);
    this.siteDAO = getService(SiteDAO.class);
    begin();
  }

  @Override
  protected void tearDown() throws Exception {
    pageDAO.deleteAll();
    siteDAO.deleteAll();
    super.tearDown();
    end();
  }

  public void testCreatePage() {
    PageEntity entity = createInstance("portal::b::c", "testCreatePage", "create page description");
    pageDAO.create(entity);
    restartTransaction();
    
    PageEntity result = pageDAO.find(entity.getId());
    assertNotNull(result);
    assertPage(entity, result);
  }
  
  public void testFindByKey() {
    
    PageEntity entity = createInstance("portal::b::c", "testPage", null);
    pageDAO.create(entity);
    restartTransaction();
    
    PageEntity result = pageDAO.findByKey(PageKey.parse("portal::b::c"));
    assertNotNull(result);
    assertPage(entity, result);
  }
  
  public void testFindByQuery() throws Exception {
    PageEntity page1 = createInstance("portal::b::c1", "aBc dEf", null);
    pageDAO.create(page1);    
    PageEntity page2 = createInstance("portal::b::c2", "Efg Hik", null);
    pageDAO.create(page2);    
    restartTransaction();
    assertEquals(2, pageDAO.findByQuery(SiteType.PORTAL, "b", "ef", 0, 0).getSize());
    assertEquals(1, pageDAO.findByQuery(SiteType.PORTAL, "b", "hik", 0, 0).getSize());
  }

  public <T> T getService(Class<T> clazz) {
    return (T) getContainer().getComponentInstanceOfType(clazz);
  }

  private PageEntity createInstance(String key, String displayName, String description) {
    PageEntity entity = new PageEntity();
    PageKey pageKey = PageKey.parse(key);
    entity.setOwner(getOrCreateSite(pageKey.getSite().getName()));
    entity.setName(pageKey.getName());
    entity.setDisplayName(displayName);
    entity.setDescription(description);
    entity.setShowMaxWindow(true);
    entity.setHideSharedLayout(true);
    entity.setShowSharedLayout(true);
    entity.setFactoryId("testFactoryId");
    entity.setPageBody("testPageBody");
    entity.setPageType(PageType.PAGE);
    return entity;
  }
  
  private void assertPage(PageEntity entity, PageEntity result) {
    assertEquals(entity.getId(), result.getId());
    assertEquals(entity.getDescription(), result.getDescription());
    assertEquals(entity.getDisplayName(), result.getDisplayName());
    assertEquals(entity.getFactoryId(), result.getFactoryId());
    assertEquals(entity.getOwnerId(), result.getOwnerId());
    assertEquals(entity.getOwnerType(), result.getOwnerType());
    assertEquals(entity.getName(), result.getName());
    assertEquals(entity.getFactoryId(), result.getFactoryId());
    assertEquals(entity.getPageBody(), result.getPageBody());
    assertEquals(entity.getPageType(), result.getPageType());
  }
  
  private SiteEntity getOrCreateSite(String name) {
    SiteEntity siteEntity = siteDAO.findByKey(SiteType.PORTAL.key(name));
    if (siteEntity == null) {
      siteEntity = new SiteEntity();
      siteEntity.setSiteType(SiteType.PORTAL);
      siteEntity.setName(name);
      siteDAO.create(siteEntity);
    }
    return siteEntity;
  }
}
