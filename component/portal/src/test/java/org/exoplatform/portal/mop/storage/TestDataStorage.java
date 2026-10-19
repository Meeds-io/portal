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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.portal.config.StaleModelException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PersistentApplicationState;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.QueryResult;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.dao.ContainerDAO;
import org.exoplatform.portal.mop.dao.PageDAO;
import org.exoplatform.portal.mop.dao.SiteDAO;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.storage.cache.CacheLayoutStorage;
import org.exoplatform.portal.mop.storage.cache.CacheNavigationStorage;
import org.exoplatform.portal.mop.storage.cache.CachePageStorage;
import org.exoplatform.portal.mop.storage.cache.CacheSiteStorage;
import org.exoplatform.portal.pom.data.ComponentData;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.PortalData;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.PortletBuilder;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.upload.UploadService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

/**
 * Created by The eXo Platform SARL Author : Tung Pham thanhtungty@gmail.com Nov
 * 13, 2007
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration-local.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/mop/navigation/configuration.xml"),
})
public class TestDataStorage extends AbstractKernelTest {

  private static final String MISSING_ROOT_CONTAINER_BODY = "[{\"type\":\"CONTAINER\",\"id\":" + Long.MAX_VALUE + "}]";

  /** . */
  private LayoutService                        storage_;
  
  /** . */
  private SiteStorage                   modelStorage;

  /** . */
  private PageStorage                        pageService;

  /** . */
  private NavigationService                  navService;

  /** . */
  private LinkedList<Event>                  events;

  /** . */
  private ListenerService                    listenerService;

  /** . */
  private OrganizationService                org;

  public TestDataStorage(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    Listener listener = new Listener() {
      @Override
      public void onEvent(Event event) throws Exception {
        events.add(event);
      }
    };

    //
    super.setUp();
    PortalContainer container = PortalContainer.getInstance();
    storage_ = (LayoutService) container.getComponentInstanceOfType(LayoutService.class);
    pageService = (PageStorage) container.getComponentInstanceOfType(PageStorage.class);
    navService = (NavigationService) container.getComponentInstanceOfType(NavigationService.class);
    modelStorage = container.getComponentInstanceOfType(SiteStorage.class);
    events = new LinkedList<Event>();
    listenerService = (ListenerService) container.getComponentInstanceOfType(ListenerService.class);
    org = container.getComponentInstanceOfType(OrganizationService.class);

    //
    listenerService.addListener(EventType.PAGE_CREATED, listener);
    listenerService.addListener(EventType.PAGE_DESTROYED, listener);
    listenerService.addListener(LayoutService.PAGE_UPDATED, listener);
    listenerService.addListener(EventType.NAVIGATION_CREATED, listener);
    listenerService.addListener(EventType.NAVIGATION_DESTROYED, listener);
    listenerService.addListener(EventType.NAVIGATION_UPDATED, listener);
    listenerService.addListener(LayoutService.PORTAL_CONFIG_CREATED, listener);
    listenerService.addListener(LayoutService.PORTAL_CONFIG_UPDATED, listener);
    listenerService.addListener(LayoutService.PORTAL_CONFIG_REMOVED, listener);

    //
    begin();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    end();
  }

  private void assertPageFound(int offset,
                               int limit,
                               SiteType siteType,
                               String siteName,
                               String pageName,
                               String title,
                               String expectedPage) {
    QueryResult<PageContext> res = pageService.findPages(offset, limit, siteType, siteName, pageName, title);
    assertEquals(1, res.getSize());
    assertEquals(expectedPage, res.iterator().next().getKey().format());
  }

  private void assertPageNotFound(int offset, int limit, SiteType siteType, String siteName, String pageName, String title) {
    QueryResult<PageContext> res = pageService.findPages(offset, limit, siteType, siteName, pageName, title);
    assertEquals(0, res.getSize());
  }

  public void testCreatePortal() throws Exception {
    String label = "portal_foo";
    String description = "This is new portal for testing";
    PortalConfig portal = new PortalConfig();
    portal.setType("portal");
    portal.setName("foo1");
    portal.setLocale("en");
    portal.setLabel(label);
    portal.setDescription(description);
    portal.setAccessPermissions(new String[] { UserACL.EVERYONE });

    //
    storage_.create(portal);
    assertEquals(1, events.size());
    portal = storage_.getPortalConfig(portal.getName());
    assertNotNull(portal);
    assertEquals("portal", portal.getType());
    assertEquals("foo1", portal.getName());
    assertEquals(label, portal.getLabel());
    assertEquals(description, portal.getDescription());

    //
    long siteId = Long.parseLong(portal.getStorageId().split("_")[1]);
    PortalConfig portal1 = storage_.getPortalConfig(siteId);
    assertNotNull(portal1);
    assertEquals(portal1.getStorageId(), portal.getStorageId());
    assertEquals("portal", portal1.getType());
    assertEquals("foo1", portal1.getName());
    assertEquals(label, portal1.getLabel());
    assertEquals(description, portal1.getDescription());
  }
  
  public void testRemovePortal() {
    PortalConfig portal = new PortalConfig();
    portal.setType("portal");
    portal.setName("foo85");
    portal.setLocale("en");
    portal.setAccessPermissions(new String[] { UserACL.EVERYONE });
    storage_.create(portal);

    portal = storage_.getPortalConfig(portal.getName());
    assertNotNull(portal);
    assertEquals("portal", portal.getType());
    assertEquals("foo85", portal.getName());

    storage_.remove(portal);

    portal = storage_.getPortalConfig(portal.getName());
    assertNull(portal);
  }

  public void testRemovePortalWhenNotRemovable() {
    String name = "foo89";

    PortalConfig portal = new PortalConfig();
    portal.setType("portal");
    portal.setName(name);
    portal.setLocale("en");
    portal.setAccessPermissions(new String[] { UserACL.EVERYONE });
    portal.setRemovable(false);
    storage_.create(portal);

    portal = storage_.getPortalConfig(portal.getName());
    assertNotNull(portal);
    assertEquals("portal", portal.getType());
    assertEquals(name, portal.getName());

    assertThrows(IllegalStateException.class, () -> storage_.remove(storage_.getPortalConfig(SiteKey.portal(name))));

    portal = storage_.getPortalConfig(portal.getName());
    assertNotNull(portal);

    portal.setRemovable(true);
    storage_.save(portal);
    storage_.remove(portal);

    portal = storage_.getPortalConfig(portal.getName());
    assertNull(portal);
  }

  public void testPortalConfigSave() throws Exception {
    PortalConfig portal = storage_.getPortalConfig("portal", "test");
    assertNotNull(portal);

    //
    portal.setLocale("vietnam");
    storage_.save(portal);
    assertEquals(1, events.size());
    //
    portal = storage_.getPortalConfig("portal", "test");
    assertNotNull(portal);
    assertEquals("vietnam", portal.getLocale());
  }

  public void testPortalConfigRemove() throws Exception {
    String siteName = "testPortalToRemove";
    createSite(SiteType.PORTAL, siteName);
    restartTransaction();

    PortalConfig portal = storage_.getPortalConfig("portal", siteName);
    assertNotNull(portal);

    storage_.remove(portal);
    assertEquals(1, events.stream().filter(event -> event.getEventName().equals(LayoutService.PORTAL_CONFIG_REMOVED)).count());
    assertNull(storage_.getPortalConfig("portal", siteName));

    try {
      // Trying to remove non existing a portal config
      storage_.remove(portal);
      fail("was expecting a Exception");
    } catch (Exception e) {

    }
  }

  public void testSavePage() throws Exception {
    PortalConfig portal = new PortalConfig();
    portal.setType("portal");
    portal.setName("test2");
    portal.setLocale("en");
    portal.setLabel("Test 2");
    portal.setDescription("Test 2");
    portal.setAccessPermissions(new String[] { UserACL.EVERYONE });

    //
    storage_.create(portal);
    assertEquals(1, events.size());

    String borderColor = "#ff1200";
    String cssClass = "custom-class";
    String height = "20px";
    String width = "30px";

    Page page = new Page();
    page.setOwnerType(PortalConfig.PORTAL_TYPE);
    page.setOwnerId(portal.getName());
    page.setName("foo");
    page.setShowMaxWindow(false);
    page.setHideSharedLayout(false);

    Container container = new Container();
    container.setTemplate("test");
    container.setHeight(height);
    container.setWidth(width);
    container.setCssClass(cssClass);
    page.setChildren(new ArrayList<>(Collections.singletonList(container)));

    Application application = new Application();
    application.setHeight(height);
    application.setWidth(width);
    application.setCssClass(cssClass);
    application.setState(new TransientApplicationState("test/test",  null));
    container.setChildren(new ArrayList<>(Collections.singletonList(application)));

    //
    try {
      storage_.save(page);
      fail();
    } catch (Exception e) {
    }
    pageService.savePage(new PageContext(page.getPageKey(), null));
    assertEquals(2, events.size());

    //
    PageContext pageContext = pageService.loadPage(page.getPageKey());
    PageState.Builder pageStateBuilder = pageContext.getState()
                                                    .builder()
                                                    .displayName("MyTitle")
                                                    .showMaxWindow(true)
                                                    .hideSharedLayout(true);
    pageContext.setState(pageStateBuilder.build());
    pageService.savePage(pageContext);

    //
    Page page2 = storage_.getPage(page.getPageId());
    page2.setTitle("MyTitle2");
    storage_.save(page2);
    assertEquals(3, events.size());

    pageService.save(page.build());

    page2 = storage_.getPage(page.getPageId());
    assertNotNull(page2);
    assertEquals("portal::" + portal.getName() + "::foo", page2.getPageId());
    assertEquals("portal", page2.getOwnerType());
    assertEquals(portal.getName(), page2.getOwnerId());
    assertEquals("foo", page2.getName());
    assertEquals(1, page2.getChildren().size());

    ModelObject childObject = page2.getChildren().get(0);
    assertEquals(height, childObject.getHeight());
    assertEquals(width, childObject.getWidth());
    assertEquals(cssClass, childObject.getCssClass());

    childObject = ((Container) childObject).getChildren().get(0);
    assertEquals(height, childObject.getHeight());
    assertEquals(width, childObject.getWidth());
    assertEquals(cssClass.trim(), childObject.getCssClass().trim());

    pageContext = pageService.loadPage(page.getPageKey());
    assertEquals("MyTitle", pageContext.getState().getDisplayName());
    assertEquals(true, pageContext.getState().isShowMaxWindow());
    assertEquals(true, pageContext.getState().isHideSharedLayout());
  }

  @SuppressWarnings("unchecked")
  public void testSaveOutdatedPage() throws Exception {
    PortalConfig portal = new PortalConfig();
    portal.setType("portal");
    portal.setName("test" + new Random().nextInt());
    portal.setLocale("en");
    portal.setLabel("Test Random Site");
    portal.setDescription("Test Random Site");
    portal.setAccessPermissions(new String[] { UserACL.EVERYONE });

    //
    storage_.create(portal);

    Page page = new Page();
    page.setOwnerType(PortalConfig.PORTAL_TYPE);
    page.setOwnerId(portal.getName());
    page.setName("foo");

    Application application = new Application();
    application.setState(new TransientApplicationState("test/test",  null));
    page.setChildren(new ArrayList<>(Collections.singletonList(application)));

    pageService.savePage(new PageContext(page.getPageKey(), null));
    storage_.save(page);

    page = storage_.getPage(page.getPageKey());
    application = ((Application)page.getChildren().get(0));
    assertNotNull(application.getStorageId());
    application.resetStorage();
    assertNull(application.getStorageId());
    application.checkStorage();

    // Conserve reference of previously created application
    page = storage_.getPage(page.getPageKey());
    Application storedApplication = ((Application)page.getChildren().get(0));

    // Delete reference of application on page
    page.setChildren(new ArrayList<>());
    storage_.save(page);

    assertThrows(ObjectNotFoundException.class, storedApplication::resetStorage);
    assertThrows(ObjectNotFoundException.class, storedApplication::checkStorage);
  }

  public void testChangingPortletThemeInPage() throws Exception {
    Page page;
    Application app;

    page = storage_.getPage("portal::classic::homepage");
    app = (Application) page.getChildren().get(0);
    assertEquals(1, page.getChildren().size());
    app.setTheme("Theme1");
    storage_.save(page);

    page = storage_.getPage("portal::classic::homepage");
    app = (Application) page.getChildren().get(0);
    assertEquals("Theme1", app.getTheme());
    app.setTheme("Theme2");
    storage_.save(page);

    page = storage_.getPage("portal::classic::homepage");
    app = (Application) page.getChildren().get(0);
    assertEquals("Theme2", app.getTheme());
  }

  public void testWindowMove2() throws Exception {
    Page page = storage_.getPage("portal::test::test3");
    Container container = new Container();
    Application application = (Application) page.getChildren().remove(0);
    container.getChildren().add(application);
    page.getChildren().add(container);

    //
    storage_.save(page);

    //
    Page page2 = storage_.getPage("portal::test::test3");

    //
    assertEquals(1, page2.getChildren().size());
    Container container2 = (Container) page2.getChildren().get(0);
    assertEquals(1, page2.getChildren().size());
    Application application2 = (Application) container2.getChildren().get(0);
    assertEquals(application2.getStorageId(), application.getStorageId());
  }

  // Test for issue GTNPORTAL-2074
  public void testWindowMove3() throws Exception {
    assertNull(storage_.getPage("portal::test::testWindowMove3"));

    Page page = new Page();
    page.setOwnerType(PortalConfig.PORTAL_TYPE);
    page.setOwnerId("test");
    page.setName("testWindowMove3");
    Application app1 = new Application();
    app1.setState(new TransientApplicationState());
    Application app2 = new Application();
    app2.setState(new TransientApplicationState());
    Container parentOfApp2 = new Container();
    parentOfApp2.getChildren().add(app2);

    page.getChildren().add(app1);
    page.getChildren().add(parentOfApp2);

    pageService.savePage(new PageContext(page.getPageKey(), null));
    storage_.save(page);

    Page page2 = storage_.getPage("portal::test::testWindowMove3");
    assertNotNull(page2);

    assertTrue(page2.getChildren().get(1) instanceof Container);
    Container container = (Container) page2.getChildren().get(1);

    assertTrue(container.getChildren().get(0) instanceof Application);
    Application persistedApp2 = (Application) container.getChildren().remove(0);

    Container transientContainer = new Container();
    transientContainer.getChildren().add(persistedApp2);

    page2.getChildren().add(transientContainer);

    storage_.save(page2);

    Page page3 = storage_.getPage("portal::test::testWindowMove3");

    assertEquals(container.getStorageId(), page3.getChildren().get(1).getStorageId());

    assertTrue(page3.getChildren().get(2) instanceof Container);
    Container formerTransientCont = (Container) page3.getChildren().get(2);
    assertEquals(1, formerTransientCont.getChildren().size());
    assertTrue(formerTransientCont.getChildren().get(0) instanceof Application);

    assertEquals(persistedApp2.getStorageId(), formerTransientCont.getChildren().get(0).getStorageId());
  }

  /**
   * Test that setting a page reference to null will actually remove the page
   * reference from the PageNode
   *
   * @throws Exception
   */
  public void testNullPageReferenceDeletes() throws Exception {
    // create portal
    PortalConfig portal = new PortalConfig();
    portal.setName("foo");
    portal.setLocale("en");
    portal.setAccessPermissions(new String[] { UserACL.EVERYONE });
    storage_.create(portal);

    // create page
    Page page = new Page();
    page.setOwnerType(PortalConfig.PORTAL_TYPE);
    page.setOwnerId("test");
    page.setName("foo");
    pageService.savePage(new PageContext(page.getPageKey(), null));

    // create a new page navigation and add node
    NavigationContext nav = new NavigationContext(SiteKey.portal("foo"), new NavigationState(0));
    navService.saveNavigation(nav);
    NodeContext<?> node = navService.loadNode(NodeModel.SELF_MODEL, nav, Scope.CHILDREN, null);
    NodeContext<?> test = node.add(null, "testPage");
    test.setState(test.getState().builder().pageRef(page.getPageKey()).build());
    navService.saveNode(node, null);

    // get the page reference from the created page and check that it exists
    NodeContext<?> pageNavigationWithPageReference = navService.loadNode(NodeModel.SELF_MODEL, nav, Scope.CHILDREN, null);
    assertNotNull("Expected page reference should not be null.",
                  pageNavigationWithPageReference.get(0)
                                                 .getState()
                                                 .getPageRef());

    // set the page reference to null and save.
    test.setState(test.getState().builder().pageRef(null).build());
    navService.saveNode(node, null);

    // check that setting the page reference to null actually removes the page
    // reference
    NodeContext<?> pageNavigationWithoutPageReference = navService
                                                                  .loadNode(NodeModel.SELF_MODEL, nav, Scope.CHILDREN, null);
    assertNull("Expected page reference should be null.", pageNavigationWithoutPageReference.get(0).getState().getPageRef());
  }

  public void testWindowScopedPortletPreferences() throws Exception {
    Page page = new Page();
    page.setPageId("portal::test::foo");
    TransientApplicationState state = new TransientApplicationState("web/BannerPortlet",
                                                                                      new PortletBuilder().add("template", "bar")
                                                                                                          .build());
    Application app = Application.createPortletApplication();
    app.setState(state);
    page.getChildren().add(app);
    pageService.savePage(new PageContext(page.getPageKey(), null));
    storage_.save(page);
    page = storage_.getPage(page.getPageId());
    app = (Application) page.getChildren().get(0);
    assertEquals("web/BannerPortlet", storage_.getId(app.getState()));
  }

  public void testPageMerge() throws Exception {
    Page page = storage_.getPage("portal::test::test4");

    String app1Id = page.getChildren().get(0).getStorageId();
    Container container = (Container) page.getChildren().get(1);
    String containerId = container.getStorageId();
    String app2Id = container.getChildren().get(0).getStorageId();
    String app3Id = container.getChildren().get(1).getStorageId();

    // Add an application
    Application groovyApp = Application.createPortletApplication();
    ApplicationState state = new TransientApplicationState("web/GroovyPortlet");
    groovyApp.setState(state);
    ((Container) page.getChildren().get(1)).getChildren().add(1, groovyApp);

    storage_.save(page);

    // Check it is existing at the correct location
    // and also that the ids are still the same
    page = storage_.getPage("portal::test::test4");
    assertEquals(2, page.getChildren().size());
    // assertEquals(PortletState.create("portal#test:/web/BannerPortlet/banner"),
    // ((Application)page.getChildren().get(0)).getInstanceState());
    assertEquals(app1Id, page.getChildren().get(0).getStorageId());
    container = (Container) page.getChildren().get(1);
    assertEquals(3, container.getChildren().size());
    assertEquals(containerId, container.getStorageId());
    // assertEquals(PortletState.create("portal#test:/web/BannerPortlet/banner"),
    // ((Application)container.getChildren().get(0)).getInstanceState());
    assertEquals(app2Id, container.getChildren().get(0).getStorageId());
    // assertEquals(PortletState.create("portal#test:/web/GroovyPortlet/groovyportlet"),
    // ((Application)container.getChildren().get(1)).getInstanceState());
    assertNotNull(container.getChildren().get(0).getStorageId());
    // assertEquals(PortletState.create("portal#test:/web/FooterPortlet/footer"),
    // ((Application)container.getChildren().get(2)).getInstanceState());
    assertEquals(app3Id, container.getChildren().get(2).getStorageId());

    // Now remove the element
    container.getChildren().remove(1);
    storage_.save(page);

    // Check it is removed
    // and also that the ids are still the same
    page = storage_.getPage("portal::test::test4");
    assertEquals(2, page.getChildren().size());
    // assertEquals(PortletState.create("portal#test:/web/BannerPortlet/banner"),
    // ((Application)page.getChildren().get(0)).getInstanceState());
    assertEquals(app1Id, page.getChildren().get(0).getStorageId());
    container = (Container) page.getChildren().get(1);
    assertEquals(2, container.getChildren().size());
    assertEquals(containerId, container.getStorageId());
    // assertEquals(PortletState.create("portal#test:/web/BannerPortlet/banner"),
    // ((Application)container.getChildren().get(0)).getInstanceState());
    assertEquals(app2Id, container.getChildren().get(0).getStorageId());
    // assertEquals(PortletState.create("portal#test:/web/FooterPortlet/footer"),
    // ((Application)container.getChildren().get(1)).getInstanceState());
    assertEquals(app3Id, container.getChildren().get(1).getStorageId());
  }

  public void testClone() throws Exception {
    // Get cloned page
    Page page = storage_.getPage("portal::test::test5");
    assertEquals(2, page.getChildren().size());
    Application banner1 = (Application) page.getChildren().get(0);
    ApplicationState instanceId = banner1.getState();

    // Check instance id format
    assertEquals("web/BannerPortlet", storage_.getId(banner1.getState()));

    // Check state
    Portlet pagePrefs = storage_.load(instanceId);
    assertEquals(new PortletBuilder().add("template", "par:/groovy/groovy/webui/component/UIBannerPortlet.gtmpl").build(),
                 pagePrefs);

    assertEquals(new PortletBuilder().add("template", "par:/groovy/groovy/webui/component/UIBannerPortlet.gtmpl").build(),
                 pagePrefs);

    pageService.clone(PageKey.parse("portal::test::test5"), PageKey.parse("portal::test::_test4"));

    Page clone = storage_.getPage("portal::test::_test4");
    assertEquals(2, clone.getChildren().size());
    banner1 = (Application) clone.getChildren().get(0);
    instanceId = banner1.getState();

    // Check instance id format
    assertEquals("web/BannerPortlet", storage_.getId(banner1.getState()));

    // Check state
    pagePrefs = storage_.load(instanceId);
    assertEquals(new PortletBuilder().add("template", "par:/groovy/groovy/webui/component/UIBannerPortlet.gtmpl").build(),
                 pagePrefs);

    // Update page prefs
    pagePrefs.setValue("template", "foo");
    storage_.save(instanceId, pagePrefs);

    // Check that page prefs have changed
    pagePrefs = storage_.load(instanceId);
    assertEquals(new PortletBuilder().add("template", "foo").build(), pagePrefs);

    // Now check the container
    Container container = (Container) clone.getChildren().get(1);
    assertEquals(2, container.getChildren().size());

    //
    Page srcPage = storage_.getPage("portal::test::test4");
    PageContext srcPageContext = pageService.loadPage(srcPage.getPageKey());
    srcPageContext.setState(srcPageContext.getState().builder().editPermission("Administrator").build());
    pageService.savePage(srcPageContext);

    //
    Application portlet = (Application) srcPage.getChildren().get(0);
    portlet.setDescription("NewPortlet");
    ArrayList<ModelObject> modelObject = srcPage.getChildren();
    modelObject.set(0, portlet);
    srcPage.setChildren(modelObject);
    storage_.save(srcPage);

    //
    PageKey dstKey = PageKey.parse(srcPage.getOwnerType() + "::" + srcPage.getOwnerId() + "::" + "_PageTest1234");
    PageContext dstPageContext = pageService.clone(srcPageContext.getKey(), dstKey);
    Page dstPage = storage_.getPage(dstKey.format());
    Application portlet1 = (Application) dstPage.getChildren().get(0);

    // Check src's edit permission and dst's edit permission
    assertNotNull(dstPageContext.getState().getEditPermission());
    assertEquals(srcPageContext.getState().getEditPermission(), dstPageContext.getState().getEditPermission());

    // Check src's children and dst's children
    assertNotNull(portlet1.getDescription());
    assertEquals(portlet.getDescription(), portlet1.getDescription());
  }

  public void testSiteScopedPreferences() throws Exception {
    Page page = storage_.getPage("portal::test::test4");
    Application app = (Application) page.getChildren().get(0);
    PersistentApplicationState state = (PersistentApplicationState) app.getState();

    //
    Portlet prefs = storage_.load(state);

    //
    prefs.setValue("template", "someanothervalue");
    storage_.save(state, prefs);

    //
    prefs = storage_.load(state);
    assertNotNull(prefs);
    assertEquals(new PortletBuilder().add("template", "someanothervalue").build(), prefs);
  }

  public void testNullPreferenceValue() throws Exception {
    Page page = storage_.getPage("portal::test::test4");
    Application app = (Application) page.getChildren().get(0);
    PersistentApplicationState state = (PersistentApplicationState) app.getState();

    //
    Portlet prefs = new Portlet();
    prefs.setValue("template", "initialvalue");
    storage_.save(state, prefs);

    //
    prefs.setValue("template", null);
    storage_.save(state, prefs);

    //
    prefs = storage_.load(state);
    assertNotNull(prefs);
    assertEquals(new PortletBuilder().add("template", (String) null).build(), prefs);
  }

  public void testSiteLayout() throws Exception {
    PortalConfig pConfig = storage_.getPortalConfig(PortalConfig.PORTAL_TYPE, "classic");
    assertNotNull(pConfig);
    assertNotNull("The Group layout of " + pConfig.getName() + " is null", pConfig.getPortalLayout());
  }

  public void testGroupLayout() throws Exception {
    GroupHandler groupHandler = org.getGroupHandler();
    Group group = groupHandler.findGroupById("groupTest");
    assertNull(group);

    group = groupHandler.createGroupInstance();
    group.setGroupName("groupTest");
    group.setLabel("group test label");

    groupHandler.addChild(null, group, true);

    group = groupHandler.findGroupById("/groupTest");
    assertNotNull(group);

    PortalConfig pConfig = new PortalConfig(PortalConfig.GROUP_TYPE, "/groupTest");
    storage_.create(pConfig);
    pConfig = storage_.getPortalConfig(PortalConfig.GROUP_TYPE, "/groupTest");
    assertNotNull("the Group's PortalConfig is not null", pConfig);

    /**
     * We need to remove the /groupTest from the groupHandler as the handler is
     * shared between the tests and can cause other tests to fail. TODO: make
     * the tests fully independent
     */
    groupHandler.removeGroup(group, false);
    group = groupHandler.findGroupById("/groupTest");
    assertNull(group);
    
    group = groupHandler.createGroupInstance();
    group.setGroupName("groupSite");
    group.setLabel("group site label");

    groupHandler.addChild(null, group, true);

    group = groupHandler.findGroupById("/groupSite");
    assertNotNull(group);

    pConfig = storage_.getPortalConfig(PortalConfig.GROUP_TYPE, "/groupSite");
    assertNull("the Group's PortalConfig is null", pConfig);
    
    groupHandler.removeGroup(group, false);
    group = groupHandler.findGroupById("/groupSite");
    assertNull(group);
  }

  public void testSaveSiteKeepsTheStoredLayoutWhenTheNewLayoutFails() throws Exception {
    String siteName = "failingLayoutSave";
    createSite(SiteType.PORTAL, siteName);
    PortalData site = modelStorage.getPortalConfig(SiteKey.portal(siteName));
    String rootContainerId = site.getPortalLayout().getStorageId();

    PortalData failingSite = new PortalData(site.getStorageId(),
                                            siteName,
                                            site.getType(),
                                            site.getLocale(),
                                            "Changed label",
                                            site.getDescription(),
                                            site.getAccessPermissions(),
                                            site.getEditPermission(),
                                            site.getProperties(),
                                            site.getSkin(),
                                            newContainer(siteName, List.of(new UnsupportedComponentData())),
                                            site.isDisplayed(),
                                            site.getDisplayOrder(),
                                            site.getBannerFileId());
    assertThrows(StaleModelException.class, () -> modelStorage.save(failingSite));
    restartTransaction();

    PortalData storedSite = modelStorage.getPortalConfig(SiteKey.portal(siteName));
    assertFalse(storedSite.isLayoutUnresolved());
    assertEquals(rootContainerId, storedSite.getPortalLayout().getStorageId());
    assertEquals(site.getLabel(), storedSite.getLabel());
  }

  public void testCreateSiteStoresNothingWhenTheLayoutFails() throws Exception {
    String siteName = "failingLayoutCreate";
    ContainerDAO containerDAO = PortalContainer.getInstance().getComponentInstanceOfType(ContainerDAO.class);
    long containersCount = containerDAO.count();

    PortalData failingSite = new PortalData(null,
                                            siteName,
                                            SiteType.PORTAL.getName(),
                                            null,
                                            null,
                                            null,
                                            new ArrayList<>(),
                                            null,
                                            null,
                                            null,
                                            newContainer(siteName, List.of(new UnsupportedComponentData())),
                                            true,
                                            5,
                                            0);
    assertThrows(StaleModelException.class, () -> modelStorage.create(failingSite));
    restartTransaction();

    assertNull(modelStorage.getPortalConfig(SiteKey.portal(siteName)));
    assertEquals(containersCount, containerDAO.count().longValue());
  }

  public void testSaveSiteReadWithAMissingRootContainerKeepsTheStoredLayout() throws Exception {
    String siteName = "unresolvedLayout";
    createSite(SiteType.PORTAL, siteName);
    SiteDAO siteDAO = PortalContainer.getInstance().getComponentInstanceOfType(SiteDAO.class);
    SiteEntity siteEntity = siteDAO.findByKey(SiteKey.portal(siteName));
    String siteBody = siteEntity.getSiteBody();
    String rootContainerId = getRootContainerId(siteBody);

    // The site row references a root container that a concurrent save deleted
    // and replaced without having committed yet
    siteEntity.setSiteBody(MISSING_ROOT_CONTAINER_BODY);
    siteDAO.update(siteEntity);
    restartTransaction();

    PortalData unresolvedSite = modelStorage.getPortalConfig(SiteKey.portal(siteName));
    assertTrue(unresolvedSite.isLayoutUnresolved());

    // The concurrent save commits the site row
    siteEntity = siteDAO.findByKey(SiteKey.portal(siteName));
    siteEntity.setSiteBody(siteBody);
    siteDAO.update(siteEntity);
    restartTransaction();

    PortalConfig unresolvedConfig = new PortalConfig(unresolvedSite);
    unresolvedConfig.setLabel("Changed label");
    storage_.save(unresolvedConfig);
    restartTransaction();

    PortalData savedSite = modelStorage.getPortalConfig(SiteKey.portal(siteName));
    assertEquals("Changed label", savedSite.getLabel());
    assertFalse(savedSite.isLayoutUnresolved());
    assertEquals(rootContainerId, savedSite.getPortalLayout().getStorageId());
  }

  public void testReadSiteAgainWhenItsRootContainerIsMissing() throws Exception {
    String siteName = "reReadLayout";
    createSite(SiteType.PORTAL, siteName);
    PortalContainer container = PortalContainer.getInstance();
    SiteDAO siteDAO = container.getComponentInstanceOfType(SiteDAO.class);
    SiteEntity siteEntity = siteDAO.findByKey(SiteKey.portal(siteName));
    String siteBody = siteEntity.getSiteBody();
    String rootContainerId = getRootContainerId(siteBody);
    siteEntity.setSiteBody(MISSING_ROOT_CONTAINER_BODY);
    siteDAO.update(siteEntity);
    restartTransaction();

    // The concurrent save commits the site row right after this thread read it
    SiteDAO concurrentlySavedSiteDAO = (SiteDAO) Proxy.newProxyInstance(SiteDAO.class.getClassLoader(),
                                                                        new Class<?>[] { SiteDAO.class },
                                                                        (proxy, method, args) -> {
                                                                          Object result = invoke(siteDAO, method, args);
                                                                          if ("findByKey".equals(method.getName())) {
                                                                            executeInTransaction("UPDATE GateInSite s SET s.siteBody = '"
                                                                                + siteBody + "' WHERE s.id = "
                                                                                + ((SiteEntity) result).getId());
                                                                          }
                                                                          return result;
                                                                        });
    SiteStorageImpl siteStorage = newSiteStorage(pageService, concurrentlySavedSiteDAO);

    PortalData site = siteStorage.getPortalConfig(SiteKey.portal(siteName));
    assertFalse(site.isLayoutUnresolved());
    assertEquals(rootContainerId, site.getPortalLayout().getStorageId());
  }

  public void testRemoveSiteKeepsTheSiteWhenTheRemovalFails() throws Exception {
    String siteName = "failingRemoval";
    createSite(SiteType.PORTAL, siteName);
    String rootContainerId = modelStorage.getPortalConfig(SiteKey.portal(siteName)).getPortalLayout().getStorageId();

    PageStorage failingPageStorage = (PageStorage) Proxy.newProxyInstance(PageStorage.class.getClassLoader(),
                                                                          new Class<?>[] { PageStorage.class },
                                                                          (proxy, method, args) -> {
                                                                            if ("destroyPages".equals(method.getName())) {
                                                                              throw new IllegalStateException("Pages removal failure");
                                                                            }
                                                                            return invoke(pageService, method, args);
                                                                          });
    SiteStorageImpl siteStorage = newSiteStorage(failingPageStorage,
                                                 PortalContainer.getInstance().getComponentInstanceOfType(SiteDAO.class));
    assertThrows(IllegalStateException.class, () -> siteStorage.remove(SiteKey.portal(siteName)));
    restartTransaction();

    PortalData storedSite = siteStorage.getPortalConfig(SiteKey.portal(siteName));
    assertNotNull(storedSite);
    assertFalse(storedSite.isLayoutUnresolved());
    assertEquals(rootContainerId, storedSite.getPortalLayout().getStorageId());
    assertNotNull(navService.loadNavigation(SiteKey.portal(siteName)));
  }

  public void testDestroyPageKeepsThePageLayoutWhenThePageRemovalFails() throws Exception {
    PageKey pageKey = createPageWithWindow("failingPageRemoval");
    PageStorageImpl pageStorage = newPageStorageFailingOn("delete");

    assertThrows(IllegalStateException.class, () -> pageStorage.destroyPage(pageKey));
    restartTransaction();

    Page storedPage = pageStorage.getPage(pageKey);
    assertNotNull(storedPage);
    assertEquals(1, storedPage.getChildren().size());
    assertEquals(1, ((Container) storedPage.getChildren().get(0)).getChildren().size());
  }

  public void testSavePageKeepsThePageLayoutWhenThePageUpdateFails() throws Exception {
    PageKey pageKey = createPageWithWindow("failingPageUpdate");
    PageStorageImpl pageStorage = newPageStorageFailingOn("update");
    Page page = pageStorage.getPage(pageKey);
    ((Container) page.getChildren().get(0)).setChildren(new ArrayList<>());

    assertThrows(IllegalStateException.class, () -> pageStorage.save(page.build()));
    restartTransaction();

    Page storedPage = pageStorage.getPage(pageKey);
    assertEquals(1, storedPage.getChildren().size());
    assertEquals(1, ((Container) storedPage.getChildren().get(0)).getChildren().size());
  }

  public void testCachesAreClearedAgainOnceTheTransactionCompletes() throws Exception {
    PortalContainer container = PortalContainer.getInstance();
    CacheService cacheService = container.getComponentInstanceOfType(CacheService.class);
    ExoCache<Long, Object> windowCache = cacheService.getCacheInstance(CacheLayoutStorage.PORTLET_PREFERENCES_CACHE_NAME);
    ExoCache<SiteKey, Object> siteCache = cacheService.getCacheInstance(CacheSiteStorage.SITE_CACHE_NAME);
    SiteKey siteKey = SiteKey.portal("evictedCaches");
    PortalConfig newSite = new PortalConfig(siteKey.getTypeName(), siteKey.getName());
    newSite.setAccessPermissions(new String[] { UserACL.EVERYONE });
    Application newWindow = new Application();
    newWindow.setState(new TransientApplicationState("test/test", null));
    Container layout = new Container();
    layout.getChildren().add(newWindow);
    newSite.setPortalLayout(layout);
    storage_.create(newSite);
    restartTransaction();

    PortalConfig site = storage_.getPortalConfig(siteKey.getTypeName(), siteKey.getName());
    Application window = getFirstApplication(site.getPortalLayout());
    assertNotNull(window);
    Long windowId = Long.valueOf(window.getStorageId());
    container.getComponentInstanceOfType(LayoutStorage.class).findWindow(windowId);
    Object cachedWindow = windowCache.get(windowId);
    Object cachedSite = siteCache.get(siteKey);
    assertNotNull(cachedWindow);
    assertNotNull(cachedSite);

    EntityTransaction transaction = beginTransaction();
    try {
      storage_.save(site);
      // Reads made by other threads before the commit cache the previous
      // states again
      windowCache.put(windowId, cachedWindow);
      siteCache.put(siteKey, cachedSite);
      transaction.commit();
    } finally {
      if (transaction.isActive()) {
        transaction.rollback();
      }
    }

    assertNull(windowCache.get(windowId));
    assertNull(siteCache.get(siteKey));
  }

  public void testRemovedSiteCachesAreClearedAgainOnceTheTransactionCompletes() throws Exception {
    String siteName = "removedSiteCaches";
    createSite(SiteType.PORTAL, siteName);
    SiteKey siteKey = SiteKey.portal(siteName);
    PageKey pageKey = siteKey.page("removedPage");
    pageService.savePage(new PageContext(pageKey, null));
    restartTransaction();

    PortalContainer container = PortalContainer.getInstance();
    CacheService cacheService = container.getComponentInstanceOfType(CacheService.class);
    ExoCache<PageKey, Object> pageCache = cacheService.getCacheInstance(CachePageStorage.PAGE_CACHE_NAME);
    ExoCache<SiteKey, Object> navigationCache = cacheService.getCacheInstance(CacheNavigationStorage.NAVIGATION_CACHE_NAME);
    ExoCache<SiteKey, Object> siteCache = cacheService.getCacheInstance(CacheSiteStorage.SITE_CACHE_NAME);
    assertNotNull(pageService.loadPage(pageKey));
    assertNotNull(navService.loadNavigation(siteKey));
    assertNotNull(modelStorage.getPortalConfig(siteKey));
    Object cachedPage = pageCache.get(pageKey);
    Object cachedNavigation = navigationCache.get(siteKey);
    Object cachedSite = siteCache.get(siteKey);
    assertNotNull(cachedPage);
    assertNotNull(cachedNavigation);
    assertNotNull(cachedSite);

    EntityTransaction transaction = beginTransaction();
    try {
      modelStorage.remove(siteKey);
      // Reads made by other threads before the commit cache the removed site,
      // page and navigation again
      pageCache.put(pageKey, cachedPage);
      navigationCache.put(siteKey, cachedNavigation);
      siteCache.put(siteKey, cachedSite);
      transaction.commit();
    } finally {
      if (transaction.isActive()) {
        transaction.rollback();
      }
    }

    assertNull(pageCache.get(pageKey));
    assertNull(navigationCache.get(siteKey));
    assertNull(siteCache.get(siteKey));
  }

  public void testSiteLoadInFlightDuringASaveIsNotCached() throws Exception {
    String siteName = "loadInFlight";
    createSite(SiteType.PORTAL, siteName);
    SiteKey siteKey = SiteKey.portal(siteName);
    PortalContainer container = PortalContainer.getInstance();
    CacheService cacheService = container.getComponentInstanceOfType(CacheService.class);
    ExoCache<SiteKey, Object> siteCache = cacheService.getCacheInstance(CacheSiteStorage.SITE_CACHE_NAME);
    PortalConfig savedSite = new PortalConfig(modelStorage.getPortalConfig(siteKey));
    siteCache.remove(siteKey);

    // The reader thread reads the site row, then waits until the save commits
    // before building and caching the site
    SiteDAO siteDAO = container.getComponentInstanceOfType(SiteDAO.class);
    Thread testThread = Thread.currentThread();
    CountDownLatch siteRowRead = new CountDownLatch(1);
    CountDownLatch siteSaved = new CountDownLatch(1);
    SiteDAO slowReaderSiteDAO = (SiteDAO) Proxy.newProxyInstance(SiteDAO.class.getClassLoader(),
                                                                 new Class<?>[] { SiteDAO.class },
                                                                 (proxy, method, args) -> {
                                                                   Object result = invoke(siteDAO, method, args);
                                                                   if ("findByKey".equals(method.getName())
                                                                       && Thread.currentThread() != testThread) {
                                                                     siteRowRead.countDown();
                                                                     siteSaved.await(10, TimeUnit.SECONDS);
                                                                   }
                                                                   return result;
                                                                 });
    CacheSiteStorage siteStorage = new CacheSiteStorage(cacheService,
                                                        container.getComponentInstanceOfType(SettingService.class),
                                                        container.getComponentInstanceOfType(ConfigurationManager.class),
                                                        container.getComponentInstanceOfType(NavigationStorage.class),
                                                        pageService,
                                                        container.getComponentInstanceOfType(LayoutStorage.class),
                                                        slowReaderSiteDAO,
                                                        container.getComponentInstanceOfType(UploadService.class),
                                                        container.getComponentInstanceOfType(FileService.class));
    AtomicReference<PortalData> readSite = new AtomicReference<>();
    Thread reader = new Thread(() -> {
      ExoContainerContext.setCurrentContainer(container);
      RequestLifeCycle.begin(container);
      try {
        readSite.set(siteStorage.getPortalConfig(siteKey));
      } finally {
        RequestLifeCycle.end();
      }
    });
    reader.start();
    try {
      assertTrue(siteRowRead.await(10, TimeUnit.SECONDS));
      savedSite.setLabel("Saved label");
      siteStorage.save(savedSite);
    } finally {
      siteSaved.countDown();
      reader.join(10000);
    }

    assertNotNull(readSite.get());
    assertNull("The site read before the save is cached", siteCache.get(siteKey));
    assertEquals("Saved label", siteStorage.getPortalConfig(siteKey).getLabel());
  }

  private PageKey createPageWithWindow(String siteName) throws Exception {
    createSite(SiteType.PORTAL, siteName);
    Application application = new Application();
    application.setState(new TransientApplicationState("test/test", null));
    Container container = new Container();
    container.setTemplate("test");
    container.setChildren(new ArrayList<>(Collections.singletonList(application)));
    Page page = new Page();
    page.setOwnerType(PortalConfig.PORTAL_TYPE);
    page.setOwnerId(siteName);
    page.setName("foo");
    page.setChildren(new ArrayList<>(Collections.singletonList(container)));
    pageService.savePage(new PageContext(page.getPageKey(), null));
    pageService.save(page.build());
    restartTransaction();
    return page.getPageKey();
  }

  private PageStorageImpl newPageStorageFailingOn(String pageDAOMethodName) {
    PortalContainer container = PortalContainer.getInstance();
    PageDAO pageDAO = container.getComponentInstanceOfType(PageDAO.class);
    PageDAO failingPageDAO = (PageDAO) Proxy.newProxyInstance(PageDAO.class.getClassLoader(),
                                                              new Class<?>[] { PageDAO.class },
                                                              (proxy, method, args) -> {
                                                                if (pageDAOMethodName.equals(method.getName())) {
                                                                  throw new IllegalStateException("Page DAO failure");
                                                                }
                                                                return invoke(pageDAO, method, args);
                                                              });
    return new PageStorageImpl(listenerService,
                               container.getComponentInstanceOfType(LayoutStorage.class),
                               container.getComponentInstanceOfType(SiteDAO.class),
                               failingPageDAO);
  }

  private SiteStorageImpl newSiteStorage(PageStorage pageStorage, SiteDAO siteDAO) {
    PortalContainer container = PortalContainer.getInstance();
    return new SiteStorageImpl(container.getComponentInstanceOfType(SettingService.class),
                               container.getComponentInstanceOfType(ConfigurationManager.class),
                               container.getComponentInstanceOfType(NavigationStorage.class),
                               pageStorage,
                               container.getComponentInstanceOfType(LayoutStorage.class),
                               siteDAO,
                               container.getComponentInstanceOfType(UploadService.class),
                               container.getComponentInstanceOfType(FileService.class));
  }

  private Application getFirstApplication(Container container) {
    for (ModelObject child : container.getChildren()) {
      if (child instanceof Application application) {
        return application;
      } else if (child instanceof Container childContainer) {
        Application application = getFirstApplication(childContainer);
        if (application != null) {
          return application;
        }
      }
    }
    return null;
  }

  private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
    try {
      return method.invoke(target, args);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  private void executeInTransaction(String statement) {
    EntityManager entityManager = getEntityManager();
    EntityTransaction transaction = entityManager.getTransaction();
    boolean begun = !transaction.isActive();
    if (begun) {
      transaction.begin();
    }
    entityManager.createQuery(statement).executeUpdate();
    if (begun) {
      transaction.commit();
    }
  }

  /**
   * The test drives the transaction boundary: the transaction that the test
   * class may have begun is committed first
   */
  private EntityTransaction beginTransaction() {
    EntityTransaction transaction = getEntityManager().getTransaction();
    if (transaction.isActive()) {
      transaction.commit();
    }
    transaction.begin();
    return transaction;
  }

  private EntityManager getEntityManager() {
    return PortalContainer.getInstance().getComponentInstanceOfType(EntityManagerService.class).getEntityManager();
  }

  private String getRootContainerId(String siteBody) throws Exception {
    JSONArray children = (JSONArray) new JSONParser().parse(siteBody);
    return String.valueOf(((JSONObject) children.get(0)).get("id"));
  }

  private ContainerData newContainer(String siteName, List<ComponentData> children) {
    return new ContainerData(null,
                             "testcontainer_" + siteName,
                             "",
                             "",
                             "",
                             "",
                             "",
                             "",
                             "",
                             "",
                             "",
                             "",
                             null,
                             null,
                             Collections.emptyList(),
                             children);
  }

  private static class UnsupportedComponentData extends ComponentData {

    private static final long serialVersionUID = 1L;

  }

  protected void createSite(SiteType type, String siteName) throws Exception {
    ContainerData container = new ContainerData(null,
                                                "testcontainer_" + siteName,
                                                "",
                                                "",
                                                "",
                                                "",
                                                "",
                                                "",
                                                "",
                                                "",
                                                "",
                                                "",
                                                null,
                                                null,
                                                Collections.emptyList(),
                                                Collections.emptyList());
      PortalData portal = new PortalData(null,
                                         siteName,
                                         type.getName(),
                                         null,
                                         null,
                                         null,
                                         new ArrayList<>(),
                                         null,
                                         null,
                                         null,
                                         container,
                                         true,
                                         5,
                                         0);
      this.modelStorage.create(portal);

      NavigationContext nav = new NavigationContext(type.key(siteName), new NavigationState(1));
      this.navService.saveNavigation(nav);

      restartTransaction();
  }

}
