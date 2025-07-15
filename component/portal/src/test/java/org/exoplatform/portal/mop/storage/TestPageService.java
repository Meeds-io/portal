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

import java.util.Arrays;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.exoplatform.portal.config.AbstractConfigTest;
import org.exoplatform.portal.jdbc.entity.ComponentEntity;
import org.exoplatform.portal.jdbc.entity.ComponentEntity.TYPE;
import org.exoplatform.portal.jdbc.entity.ContainerEntity;
import org.exoplatform.portal.jdbc.entity.PageEntity;
import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.jdbc.entity.WindowEntity;
import org.exoplatform.portal.mop.QueryResult;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.dao.ContainerDAO;
import org.exoplatform.portal.mop.dao.PageDAO;
import org.exoplatform.portal.mop.dao.SiteDAO;
import org.exoplatform.portal.mop.dao.WindowDAO;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;

public class TestPageService extends AbstractConfigTest {

  private PageStorage  pageService;

  private SiteDAO      siteDAO;

  private PageDAO      pageDAO;

  private ContainerDAO containerDAO;

  private WindowDAO    windowDAO;

  @Override
  protected void setUp() throws Exception {    
    begin();
    super.setUp();
    this.pageService = getContainer().getComponentInstanceOfType(PageStorage.class);
    this.pageDAO = getContainer().getComponentInstanceOfType(PageDAO.class);
    this.containerDAO = getContainer().getComponentInstanceOfType(ContainerDAO.class);
    this.windowDAO = getContainer().getComponentInstanceOfType(WindowDAO.class);
    this.siteDAO = getContainer().getComponentInstanceOfType(SiteDAO.class);
  }

  @Override
  protected void tearDown() throws Exception {
    QueryResult<PageContext> results = pageService.findPages(0, -1, null, null, null, null);
    Iterator<PageContext> iter = results.iterator();
    while (iter.hasNext()) {
      PageContext page = iter.next();
      pageService.destroyPage(page.getKey());
    }
    siteDAO.deleteAll();
    super.tearDown();
    end();
  }

  public void testClonePage() throws Exception {
    WindowEntity app1 = createWindow("win1");
    windowDAO.create(app1);
    WindowEntity app2 = createWindow("win2");
    windowDAO.create(app2);
    
    ContainerEntity container = createContainer("cont1");
    container.setChildren(Arrays.<ComponentEntity>asList(app2));
    containerDAO.create(container);
    
    getOrCreateSite(SiteKey.portal("srcPortal"));
    getOrCreateSite(SiteKey.portal("targetPortal"));
    
    PageKey srcKey = new PageKey(SiteKey.portal("srcPortal"), "srcName");
    PageEntity src = createPage(srcKey);
    src.setChildren(Arrays.asList(container, app1));
    src.setPageBody(((JSONArray)src.toJSON().get("children")).toJSONString());
    
    pageDAO.create(src);
    restartTransaction();
    
    PageKey dstKey = new PageKey(SiteKey.portal("targetPortal"), "targetName");
    pageService.clone(srcKey, dstKey);
    
    PageEntity result = pageDAO.findByKey(dstKey);
    assertNotNull(result);
    
    JSONParser parser = new JSONParser();
    JSONArray children = (JSONArray)parser.parse(result.getPageBody());
    assertEquals(2, children.size());
    JSONObject cont = (JSONObject)children.get(0);
    assertEquals(TYPE.CONTAINER.name(), cont.get("type"));    
  }

  private WindowEntity createWindow(String title) {
    WindowEntity window = new WindowEntity();
    window.setTitle(title);
    return window;
  }

  private ContainerEntity createContainer(String name) {
    ContainerEntity container = new ContainerEntity();
    container.setName(name);
    return container;
  }

  private PageEntity createPage(PageKey srcKey) {
    PageEntity page = new PageEntity();
    page.setName(srcKey.getName());
    page.setOwner(getOrCreateSite(srcKey.getSite()));
    return page;
  }
  
  private SiteEntity getOrCreateSite(SiteKey siteKey) {
    SiteEntity siteEntity = siteDAO.findByKey(siteKey);
    if (siteEntity == null) {
      siteEntity = new SiteEntity();
      siteEntity.setSiteType(siteKey.getType());
      siteEntity.setName(siteKey.getName());
      siteDAO.create(siteEntity);
    }
    return siteEntity;
  }
}
