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
package org.exoplatform.portal.config;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PageBody;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Utils;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.storage.DescriptionStorage;
import org.exoplatform.portal.mop.storage.PageStorage;
import org.exoplatform.portal.mop.storage.SiteStorage;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationState;

import junit.framework.AssertionFailedError;
import lombok.SneakyThrows;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration-local.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/config/conf/configuration.xml") })
public class TestUserPortalConfigService extends AbstractConfigTest {

  private static final String     DEFAULT_CLASSIC_HOME = "/portal/classic/home";

  /** . */
  private UserPortalConfigService userPortalConfigService;

  /** . */
  private LayoutService           layoutService;

  private NavigationService       navigationService;

  /** . */
  private PageStorage             pageStorage;

  /** . */
  private Authenticator           authenticator;

  /** . */
  private ListenerService         listenerService;

  /** . */
  private LinkedList<Event>       events;

  /** . */
  private boolean                 registered;

  /** . */
  private SiteStorage             siteStorage;

  public TestUserPortalConfigService() {
    registered = false;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    Listener listener = new Listener() {
      @Override
      public void onEvent(Event event) throws Exception {
        events.add(event);
      }
    };

    PortalContainer container = getContainer();
    userPortalConfigService = container.getComponentInstanceOfType(UserPortalConfigService.class);
    authenticator = container.getComponentInstanceOfType(Authenticator.class);
    listenerService = container.getComponentInstanceOfType(ListenerService.class);
    events = new LinkedList<>();
    layoutService = container.getComponentInstanceOfType(LayoutService.class);
    navigationService = container.getComponentInstanceOfType(NavigationService.class);
    pageStorage = container.getComponentInstanceOfType(PageStorage.class);
    siteStorage = container.getComponentInstanceOfType(SiteStorage.class);

    // Register only once for all unit tests
    if (!registered) {
      // I'm using this due to crappy design of
      // org.exoplatform.services.listener.ListenerService
      listenerService.addListener(LayoutService.PAGE_CREATED, listener);
      listenerService.addListener(EventType.PAGE_DESTROYED, listener);
      listenerService.addListener(EventType.PAGE_UPDATED, listener);
      listenerService.addListener(EventType.NAVIGATION_CREATED, listener);
      listenerService.addListener(EventType.NAVIGATION_DESTROYED, listener);
      listenerService.addListener(EventType.NAVIGATION_UPDATED, listener);
    }
  }

  @SneakyThrows
  public void testCreateUserPortalConfigFromTemplate() {
    OrganizationService organizationService = ExoContainerContext.getService(OrganizationService.class);
    GroupHandler groupHandler = organizationService.getGroupHandler();
    Group group = groupHandler.createGroupInstance();
    group.setGroupName("groupFromTemplate");
    group.setLabel("Group from template");
    groupHandler.addChild(null, group, true);

    String permission = "/organization/management/executive-board";
    String siteTemplate = "classic";
    String groupId = "/" + group.getGroupName();
    SiteKey groupSiteKey = SiteKey.group(groupId);
    SiteKey templateSiteKey = SiteKey.groupTemplate(siteTemplate);
    assertThrows(ObjectNotFoundException.class,
                 () -> userPortalConfigService.createSiteFromTemplate(SiteKey.groupTemplate("NotExistingTemplate"),
                                                                      groupSiteKey,
                                                                      permission));
    userPortalConfigService.createSiteFromTemplate(templateSiteKey, groupSiteKey, permission);

    PortalConfig portalConfig = layoutService.getPortalConfig(groupSiteKey);
    assertNotNull(portalConfig);
    assertEquals(1, portalConfig.getAccessPermissions().length);
    String accessPermission = "member:/organization/management/executive-board";
    assertEquals(accessPermission, portalConfig.getAccessPermissions()[0]);
    String editPermission = "manager:/organization/management/executive-board";
    assertEquals(editPermission, portalConfig.getEditPermission());
    assertEquals(0, portalConfig.getBannerFileId());
    assertEquals(1, portalConfig.getDisplayOrder());
    assertFalse(portalConfig.isDefaultLayout());
    assertFalse(portalConfig.isDisplayed());
    assertEquals("Classic Label", portalConfig.getLabel());
    assertEquals("Classic Description", portalConfig.getDescription());
    assertEquals(groupSiteKey.getName(), portalConfig.getName());
    assertEquals(groupSiteKey.getTypeName(), portalConfig.getType());
    assertNotNull(portalConfig.getPortalLayout());
    assertNotNull(portalConfig.getPortalLayout().getChildren());
    assertEquals(2, portalConfig.getPortalLayout().getChildren().size());
    assertEquals(Application.class, portalConfig.getPortalLayout().getChildren().get(0).getClass());
    assertEquals(PageBody.class, portalConfig.getPortalLayout().getChildren().get(1).getClass());
    assertEquals(UserACL.EVERYONE,
                 ((Application) portalConfig.getPortalLayout().getChildren().get(0)).getAccessPermissions()[0]);

    List<PageContext> pages = layoutService.findPages(groupSiteKey);
    assertNotNull(pages);
    assertEquals(3, pages.size());

    PageContext pageContext = pages.stream().filter(p -> p.getKey().getName().equals("homepage")).findFirst().orElseThrow();
    assertEquals(accessPermission, pageContext.getState().getAccessPermissions().get(0));
    assertEquals(editPermission, pageContext.getState().getEditPermission());
    assertEquals("Home Page", pageContext.getState().getDisplayName());

    Page page = layoutService.getPage(pageContext.getKey());
    assertNotNull(page);
    assertEquals(1, page.getChildren().size());
    assertEquals(UserACL.EVERYONE, ((Application) page.getChildren().get(0)).getAccessPermissions()[0]);

    NavigationContext navigation = navigationService.loadNavigation(groupSiteKey);
    assertNotNull(navigation);

    NodeContext<NodeContext<Object>> rootNode = navigationService.loadNode(groupSiteKey);
    assertNotNull(rootNode);
    assertEquals(2, rootNode.getNodeCount());
    NodeData data = rootNode.get(0).getData();
    NodeState state = rootNode.get(0).getState();
    assertEquals("home", data.getName());
    assertEquals("Home", state.getLabel());
    assertEquals(groupSiteKey.page("homepage"), state.getPageRef());
    assertEquals(1, rootNode.get(0).getNodeCount());

    data = rootNode.get(0).get(0).getData();
    state = rootNode.get(0).get(0).getState();
    assertEquals("testSubNode", data.getName());
    assertEquals("Sub Node", state.getLabel());
    assertEquals(groupSiteKey.page("testSubNode"), state.getPageRef());

    data = rootNode.get(1).getData();
    state = rootNode.get(1).getState();
    assertEquals("test", data.getName());
    assertEquals("Test", state.getLabel());
    assertEquals(groupSiteKey.page("test"), state.getPageRef());
  }

  @SneakyThrows
  public void testDuplicateSiteTemplate() {
    String siteTemplate = "classic";
    String targetSiteTemplate = "classic2";

    String accessPermission = "member:@owner_id@";
    String editPermission = "manager:@owner_id@";

    SiteKey sourceTemplateSiteKey = SiteKey.groupTemplate(siteTemplate);
    SiteKey targetTemplateSiteKey = SiteKey.groupTemplate(targetSiteTemplate);
    PortalConfig sourcePortalConfig = layoutService.getPortalConfig(sourceTemplateSiteKey);
    assertNotNull(sourcePortalConfig);

    userPortalConfigService.createSiteFromTemplate(sourceTemplateSiteKey, targetTemplateSiteKey);

    PortalConfig targetPortalConfig = layoutService.getPortalConfig(targetTemplateSiteKey);
    assertNotNull(targetPortalConfig);
    assertEquals(1, targetPortalConfig.getAccessPermissions().length);
    assertEquals(sourcePortalConfig.getAccessPermissions()[0], targetPortalConfig.getAccessPermissions()[0]);
    assertEquals(sourcePortalConfig.getEditPermission(), targetPortalConfig.getEditPermission());
    assertEquals(sourcePortalConfig.getDisplayOrder(), targetPortalConfig.getDisplayOrder());
    assertEquals(sourcePortalConfig.isDefaultLayout(), targetPortalConfig.isDefaultLayout());
    assertEquals(sourcePortalConfig.isDisplayed(), targetPortalConfig.isDisplayed());
    assertEquals(sourcePortalConfig.getLabel(), targetPortalConfig.getLabel());
    assertEquals(sourcePortalConfig.getDescription(), targetPortalConfig.getDescription());
    assertEquals(targetTemplateSiteKey.getName(), targetPortalConfig.getName());
    assertEquals(targetTemplateSiteKey.getTypeName(), targetPortalConfig.getType());
    assertNotNull(targetPortalConfig.getPortalLayout());
    assertNotNull(targetPortalConfig.getPortalLayout().getChildren());
    assertEquals(2, targetPortalConfig.getPortalLayout().getChildren().size());
    assertEquals(Application.class, targetPortalConfig.getPortalLayout().getChildren().get(0).getClass());
    assertEquals(PageBody.class, targetPortalConfig.getPortalLayout().getChildren().get(1).getClass());
    assertEquals(UserACL.EVERYONE,
                 ((Application) targetPortalConfig.getPortalLayout().getChildren().get(0)).getAccessPermissions()[0]);

    List<PageContext> pages = layoutService.findPages(targetTemplateSiteKey);
    assertNotNull(pages);
    assertEquals(3, pages.size());

    PageContext pageContext = pages.stream().filter(p -> p.getKey().getName().equals("homepage")).findFirst().orElseThrow();
    assertEquals(accessPermission, pageContext.getState().getAccessPermissions().get(0));
    assertEquals(editPermission, pageContext.getState().getEditPermission());
    assertEquals("Home Page", pageContext.getState().getDisplayName());

    Page page = layoutService.getPage(pageContext.getKey());
    assertNotNull(page);
    assertEquals(1, page.getChildren().size());
    assertEquals(UserACL.EVERYONE, ((Application) page.getChildren().get(0)).getAccessPermissions()[0]);

    NavigationContext navigation = navigationService.loadNavigation(targetTemplateSiteKey);
    assertNotNull(navigation);

    NodeContext<NodeContext<Object>> rootNode = navigationService.loadNode(targetTemplateSiteKey);
    assertNotNull(rootNode);
    assertEquals(2, rootNode.getNodeCount());
    NodeData data = rootNode.get(0).getData();
    NodeState state = rootNode.get(0).getState();
    assertEquals("home", data.getName());
    assertEquals("Home", state.getLabel());
    assertEquals(targetTemplateSiteKey.page("homepage"), state.getPageRef());
    assertEquals(1, rootNode.get(0).getNodeCount());

    data = rootNode.get(0).get(0).getData();
    state = rootNode.get(0).get(0).getState();
    assertEquals("testSubNode", data.getName());
    assertEquals("Sub Node", state.getLabel());
    assertEquals(targetTemplateSiteKey.page("testSubNode"), state.getPageRef());

    data = rootNode.get(1).getData();
    state = rootNode.get(1).getState();
    assertEquals("test", data.getName());
    assertEquals("Test", state.getLabel());
    assertEquals(targetTemplateSiteKey.page("test"), state.getPageRef());
  }

  public void testComputePortalSitePath() {
    new UnitTest() {
      public void execute() throws Exception {
        NewPortalConfig config = new NewPortalConfig("classpath:/org/exoplatform/portal/config/conf");
        config.setOwnerType("portal");
        config.setOverrideMode(true);
        config.setImportMode("merge");
        HashSet<String> owners = new HashSet<>();
        owners.add("test2");
        config.setPredefinedOwner(owners);
        NewPortalConfigListener newPortalConfigListener = new NewPortalConfigListener(userPortalConfigService,
                                                                                      layoutService,
                                                                                      getContainer().getComponentInstanceOfType(ConfigurationManager.class),
                                                                                      new InitParams(),
                                                                                      getContainer().getComponentInstanceOfType(NavigationService.class),
                                                                                      getContainer().getComponentInstanceOfType(DescriptionStorage.class),
                                                                                      getContainer().getComponentInstanceOfType(UserACL.class),
                                                                                      getContainer().getComponentInstanceOfType(LocaleConfigService.class));
        newPortalConfigListener.initPortalConfigDB(config);
        newPortalConfigListener.initPageDB(config);
        newPortalConfigListener.initPageNavigationDB(config);

        UserPortalConfig userPortalCfg = userPortalConfigService.getUserPortalConfig("test2", "root");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("test2", portalCfg.getName());

        String path = userPortalConfigService.getDefaultSitePath("test2", "mary");
        assertEquals("/portal/test2/test", path);

        restartTransaction();
        layoutService.remove(PageKey.parse("portal::test2::test"));
        restartTransaction();

        path = userPortalConfigService.getDefaultSitePath("test2", "mary");
        assertEquals("/portal/test2/home/page", path);
      }
    }.execute("root");
  }

  public void testUpdatePortalConfig() {
    new UnitTest() {
      public void execute() throws Exception {
        UserPortalConfig userPortalCfg = userPortalConfigService.getUserPortalConfig("classic", "root");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("classic", portalCfg.getName());
        assertEquals("en", portalCfg.getLocale());
        portalCfg.setLocale("fr");

        layoutService.save(portalCfg);

        userPortalCfg = userPortalConfigService.getUserPortalConfig("classic", "root");
        portalCfg = userPortalCfg.getPortalConfig();
        assertEquals("fr", portalCfg.getLocale());
      }
    }.execute("root");
  }

  public void testEnforcedReimporting() {
    new UnitTest() {
      public void execute() throws Exception {
        UserPortalConfig userPortalCfg = userPortalConfigService.getUserPortalConfig("classic", "root");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("classic", portalCfg.getName());
        assertEquals("en", portalCfg.getLocale());
        portalCfg.setLocale("fr");

        layoutService.save(portalCfg);

        userPortalCfg = userPortalConfigService.getUserPortalConfig("classic", "root");
        portalCfg = userPortalCfg.getPortalConfig();
        assertEquals("fr", portalCfg.getLocale());

        // Re-import site config from configuration
        userPortalConfigService.start();

        userPortalCfg = userPortalConfigService.getUserPortalConfig("classic", "root");
        portalCfg = userPortalCfg.getPortalConfig();
        assertEquals("en", portalCfg.getLocale());
      }
    }.execute("root");
  }

  public void testRootGetUserPortalConfig() {
    new UnitTest() {
      public void execute() throws Exception {
        UserPortalConfig userPortalCfg = userPortalConfigService.getUserPortalConfig("classic", "root");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("classic", portalCfg.getName());
        UserPortal userPortal = userPortalCfg.getUserPortal();
        assertNotNull(userPortal.getNavigations());
        Map<String, UserNavigation> navigations = toMap(userPortal);
        assertEquals("expected to have 5 navigations instead of " + navigations, 5, navigations.size());
        assertTrue(navigations.containsKey("portal::classic"));
        assertTrue(navigations.containsKey("portal::systemportal"));
        assertTrue(navigations.containsKey("group::/platform/administrators"));
        assertTrue(navigations.containsKey("group::/platform/users"));
        assertTrue(navigations.containsKey("group::/organization/management/executive-board"));
      }
    }.execute("root");
  }

  public void testGetGlobalUserPortalConfig() {
    new UnitTest() {
      public void execute() throws Exception {
        UserPortalConfig userPortalCfg = userPortalConfigService.getUserPortalConfig("classic", "john");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("classic", portalCfg.getName());
        UserPortal userPortal = userPortalCfg.getUserPortal();
        assertNotNull(userPortal.getNavigations());
        Map<String, UserNavigation> navigations = toMap(userPortal);
        assertTrue(navigations.containsKey("portal::classic"));
        assertTrue(navigations.containsKey("portal::" + userPortalConfigService.getGlobalPortal()));

        String originalGlobalPortal = userPortalConfigService.getGlobalPortal();
        userPortalConfigService.setGlobalPortal("system");
        try {
          userPortalCfg = userPortalConfigService.getUserPortalConfig("classic", "root");
          userPortal = userPortalCfg.getUserPortal();
          navigations = toMap(userPortal);

          assertTrue(navigations.containsKey("portal::classic"));
          assertTrue(navigations.containsKey("portal::system"));
        } finally {
          userPortalConfigService.setGlobalPortal(originalGlobalPortal);
        }
      }
    }.execute("root");
  }

  public void testGetGlobalUserNodes() {
    new UnitTest() {
      public void execute() throws Exception {
        UserNodeFilterConfig.Builder filterConfigBuilder = UserNodeFilterConfig.builder();
        filterConfigBuilder.withReadWriteCheck().withVisibility(Visibility.DISPLAYED, Visibility.TEMPORAL);
        filterConfigBuilder.withTemporalCheck();
        UserNodeFilterConfig filterConfig = filterConfigBuilder.build();

        UserPortalConfig userPortalCfg = userPortalConfigService.getUserPortalConfig("classic", "john");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("classic", portalCfg.getName());
        UserPortal userPortal = userPortalCfg.getUserPortal();
        Collection<UserNode> nodes = userPortal.getNodes(SiteType.PORTAL, Scope.ALL, filterConfig);
        assertNotNull(nodes);

        int initialNodesSize = nodes.size();
        assertTrue(initialNodesSize > 0);

        String originalGlobalPortal = userPortalConfigService.getGlobalPortal();
        userPortalConfigService.setGlobalPortal("systemtest");
        try {
          userPortalCfg = userPortalConfigService.getUserPortalConfig("classic", "root");
          portalCfg = userPortalCfg.getPortalConfig();
          userPortal = userPortalCfg.getUserPortal();
          nodes = userPortal.getNodes(SiteType.PORTAL, Scope.ALL, filterConfig);
          assertNotNull(nodes);

          assertEquals(initialNodesSize, nodes.size());
          UserNode homeNode = nodes.iterator().next();
          assertEquals("home", homeNode.getName());
          assertEquals("classic", homeNode.getNavigation().getKey().getName());

          UserNode lastUserNode = new ArrayList<>(nodes).get(initialNodesSize - 1);
          assertEquals("systemhome", lastUserNode.getName());
          assertEquals("systemtest", lastUserNode.getNavigation().getKey().getName());
        } finally {
          userPortalConfigService.setGlobalPortal(originalGlobalPortal);
        }
      }
    }.execute("root");
  }

  public void testGetUserNodesGlobalNotIncluded() {
    new UnitTest() {
      public void execute() throws Exception {
        UserNodeFilterConfig.Builder filterConfigBuilder = UserNodeFilterConfig.builder();
        filterConfigBuilder.withReadWriteCheck().withVisibility(Visibility.DISPLAYED, Visibility.TEMPORAL);
        filterConfigBuilder.withTemporalCheck();
        UserNodeFilterConfig filterConfig = filterConfigBuilder.build();

        UserPortalConfig userPortalCfg = userPortalConfigService.getUserPortalConfig("classic", "john");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("classic", portalCfg.getName());
        UserPortal userPortal = userPortalCfg.getUserPortal();
        Collection<UserNode> nodes = userPortal.getNodes(SiteType.PORTAL, Scope.ALL, filterConfig, false);
        assertNotNull(nodes);

        int initialNodesSize = nodes.size();
        assertTrue(initialNodesSize > 0);

        String originalGlobalPortal = userPortalConfigService.getGlobalPortal();
        userPortalConfigService.setGlobalPortal("systemtest");
        try {
          userPortalCfg = userPortalConfigService.getUserPortalConfig("classic", "root");
          portalCfg = userPortalCfg.getPortalConfig();
          userPortal = userPortalCfg.getUserPortal();
          nodes = userPortal.getNodes(SiteType.PORTAL, Scope.ALL, filterConfig, false);
          assertNotNull(nodes);

          assertEquals(initialNodesSize, nodes.size());
          UserNode homeNode = nodes.iterator().next();
          assertEquals("home", homeNode.getName());
          assertEquals("classic", homeNode.getNavigation().getKey().getName());

          UserNode lastUserNode = new ArrayList<>(nodes).get(nodes.size() - 1);
          assertEquals("webexplorer", lastUserNode.getName());
          assertEquals("classic", lastUserNode.getNavigation().getKey().getName());
        } finally {
          userPortalConfigService.setGlobalPortal(originalGlobalPortal);
        }
      }
    }.execute("root");
  }

  public void testJohnGetUserPortalConfig() {
    new UnitTest() {
      public void execute() throws Exception {
        UserPortalConfig userPortalCfg = userPortalConfigService.getUserPortalConfig("classic", "john");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("classic", portalCfg.getName());
        UserPortal userPortal = userPortalCfg.getUserPortal();
        assertNotNull(userPortal.getNavigations());
        Map<String, UserNavigation> navigations = toMap(userPortal);
        assertEquals("expected to have 5 navigations instead of " + navigations, 5, navigations.size());
        assertTrue(navigations.containsKey("portal::classic"));
        assertTrue(navigations.containsKey("group::/platform/administrators"));
        assertTrue(navigations.containsKey("group::/platform/users"));
      }
    }.execute("john");
  }

  public void testMaryGetUserPortalConfig() {
    new UnitTest() {
      public void execute() throws Exception {
        UserPortalConfig userPortalCfg = userPortalConfigService.getUserPortalConfig("classic", "mary");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("classic", portalCfg.getName());
        UserPortal userPortal = userPortalCfg.getUserPortal();
        assertNotNull(userPortal.getNavigations());
        Map<String, UserNavigation> navigations = toMap(userPortal);
        assertEquals(3, navigations.size());
        assertTrue(navigations.containsKey("portal::classic"));
        assertTrue(navigations.containsKey("group::/platform/users"));
      }
    }.execute("mary");
  }

  public void testGuestGetUserPortalConfig() {
    new UnitTest() {
      public void execute() throws Exception {
        UserPortalConfig userPortalCfg = userPortalConfigService.getUserPortalConfig("classic", null);
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("classic", portalCfg.getName());
        UserPortal userPortal = userPortalCfg.getUserPortal();
        assertNotNull(userPortal.getNavigations());
        Map<String, UserNavigation> navigations = toMap(userPortal);
        assertEquals("" + navigations, 2, navigations.size());
        assertTrue(navigations.containsKey("portal::classic"));
      }
    }.execute(null);
  }

  public void testGetMetaPortalConfig() {
    new UnitTest() {
      public void execute() throws Exception {
        PortalConfig portalConfig = userPortalConfigService.getMetaPortalConfig();
        assertNotNull(portalConfig);
        assertEquals(PortalConfig.PORTAL_TYPE, portalConfig.getType());
        assertEquals("classic", portalConfig.getName());
      }
    }.execute(null);
  }

  public void testNavigationOrder() {
    new UnitTest() {
      public void execute() throws Exception {
        UserPortalConfig userPortalCfg = userPortalConfigService.getUserPortalConfig("classic", "root");
        UserPortal userPortal = userPortalCfg.getUserPortal();
        List<UserNavigation> navigations = userPortal.getNavigations();
        assertEquals("expected to have 5 navigations instead of " + navigations, 5, navigations.size());
        assertEquals("classic", navigations.get(0).getKey().getName());
        assertEquals("/platform/administrators", navigations.get(1).getKey().getName());
        assertEquals("/organization/management/executive-board", navigations.get(2).getKey().getName());
        assertEquals("/platform/users", navigations.get(3).getKey().getName());
      }
    }.execute("root");
  }

  public void testCreateUserPortalConfig() {
    new UnitTest() {
      public void execute() throws Exception {
        userPortalConfigService.createUserPortalConfig(PortalConfig.PORTAL_TYPE, "jazz", "test");
        UserPortalConfig userPortalCfg = userPortalConfigService.getUserPortalConfig("jazz", "root");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("jazz", portalCfg.getName());
        UserPortal userPortal = userPortalCfg.getUserPortal();
        assertNotNull(userPortal.getNavigations());
        Map<String, UserNavigation> navigations = toMap(userPortal);
        assertEquals("expected to have 5 navigations instead of " + navigations, 5, navigations.size());
        assertTrue(navigations.containsKey("portal::jazz"));
        assertTrue(navigations.containsKey("group::/platform/administrators"));
        assertTrue(navigations.containsKey("group::/organization/management/executive-board"));
        assertTrue(navigations.containsKey("group::/platform/users"));

        queryPage();
      }

      private void queryPage() {
        try {
          pageStorage.findPages(0, 10, SiteType.PORTAL, null, null, null);
        } catch (Exception ex) {
          assertTrue("Exception while querying pages with new portal", false);
        }
      }

    }.execute("root");
  }

  public void testCanRestore() {
    assertTrue(userPortalConfigService.canRestore("portal", "classic"));
    String customSiteName = "customSiteName";
    userPortalConfigService.createUserPortalConfig(PortalConfig.PORTAL_TYPE, customSiteName, "test");
    assertFalse(userPortalConfigService.canRestore("portal", customSiteName));
  }

  public void testRestore() {
    assertTrue(userPortalConfigService.restoreSite("portal", "classic", ImportMode.CONSERVE, true, true, true));
    assertTrue(userPortalConfigService.restoreSite("portal", "classic", ImportMode.INSERT, true, false, true));
    assertTrue(userPortalConfigService.restoreSite("portal", "classic", ImportMode.MERGE, true, true, false));
    assertTrue(userPortalConfigService.restoreSite("portal", "classic", ImportMode.OVERWRITE, false, false, true));

    String customSiteName = "customSiteName";
    userPortalConfigService.createUserPortalConfig(PortalConfig.PORTAL_TYPE, customSiteName, "test");
    assertFalse(userPortalConfigService.restoreSite("portal", customSiteName, ImportMode.OVERWRITE, false, true, false));
  }

  public void testRootGetPage() {
    new UnitTest() {
      public void execute() throws Exception {
        assertEquals("group::/platform/administrators::newAccount",
                     userPortalConfigService.getPage(PageKey.parse("group::/platform/administrators::newAccount"), "root")
                                            .getKey()
                                            .format());
        assertEquals("group::/organization/management/executive-board::newStaff",
                     userPortalConfigService.getPage(PageKey.parse("group::/organization/management/executive-board::newStaff"),
                                                     "root")
                                            .getKey()
                                            .format());
      }
    }.execute("root");
  }

  public void testJohnGetPage() {
    new UnitTest() {
      public void execute() throws Exception {
        assertNull(userPortalConfigService.getPage(PageKey.parse("group::/platform/administrators::newAccount"), "john"));
        assertEquals("group::/organization/management/executive-board::newStaff",
                     userPortalConfigService.getPage(PageKey.parse("group::/organization/management/executive-board::newStaff"),
                                                     "john")
                                            .getKey()
                                            .format());
      }
    }.execute("john");
  }

  public void testMaryGetPage() {
    new UnitTest() {
      public void execute() throws Exception {
        assertNull(userPortalConfigService.getPage(PageKey.parse("group::/platform/administrators::newAccount"), "mary"));
        assertNull(userPortalConfigService.getPage(PageKey.parse("group::/organization/management/executive-board::newStaff"),
                                                   "mary"));
      }
    }.execute("mary");
  }

  public void testAnonymousGetPage() {
    new UnitTest() {
      public void execute() throws Exception {
        assertNull(userPortalConfigService.getPage(PageKey.parse("group::/platform/administrators::newAccount"), null));
        assertNull(userPortalConfigService.getPage(PageKey.parse("group::/organization/management/executive-board::newStaff"),
                                                   null));
      }
    }.execute(null);
  }

  public void testGetUserHomePage() {
    assertNull(userPortalConfigService.getUserHomePage(null));
    assertNull(userPortalConfigService.getUserHomePage("john"));
    assertNull(userPortalConfigService.getUserHomePage("NotExisting"));
    assertEquals("/portal/classic/home", userPortalConfigService.getDefaultPath("john"));

    userPortalConfigService.setUserHomePage("john", "/portal");
    assertEquals("/portal", userPortalConfigService.getUserHomePage("john"));
    assertEquals("/portal", userPortalConfigService.getDefaultPath("john"));
  }

  public void testGetDefaultSitePath() {
    assertEquals(DEFAULT_CLASSIC_HOME, userPortalConfigService.getDefaultSitePath("classic", null));
    assertEquals(DEFAULT_CLASSIC_HOME, userPortalConfigService.getDefaultSitePath("classic", "NotExisting"));
    assertEquals(DEFAULT_CLASSIC_HOME, userPortalConfigService.getDefaultSitePath("classic", "john"));
    assertEquals(DEFAULT_CLASSIC_HOME, userPortalConfigService.getDefaultSitePath("classic", "james"));
  }

  public void testGetSiteNodeOrGlobalNode() {
    UserNode userNode = userPortalConfigService.getDefaultSiteNode("classic", null);
    assertEquals("home", userNode.getURI());

    Page homePage = layoutService.getPage(userNode.getPageRef());
    String[] accessPermissions = homePage.getAccessPermissions();
    try {
      homePage.setAccessPermissions(new String[] {"*:/platform/administrators"});
      layoutService.save(new PageContext(homePage.getPageKey(), Utils.toPageState(homePage)));

      userNode = userPortalConfigService.getDefaultSiteNode("classic", null);
      assertEquals("home/subnode", userNode.getURI());
    } finally {
      homePage.setAccessPermissions(accessPermissions);
      layoutService.save(new PageContext(homePage.getPageKey(), Utils.toPageState(homePage)));
    }
  }

  public void testGetSiteNodeNoHomePageAccess() {
    UserNode userNode = userPortalConfigService.getSiteNodeOrGlobalNode(SiteType.PORTAL.getName(), "classic", "home", null);
    assertEquals("home", userNode.getURI());
    userNode = userPortalConfigService.getSiteNodeOrGlobalNode(SiteType.PORTAL.getName(), "classic", "Notfound", null);
    assertEquals("home", userNode.getURI());

    userNode = userPortalConfigService.getSiteNodeOrGlobalNode(SiteType.PORTAL.getName(), "classic", "webexplorer", null);
    assertEquals("home", userNode.getURI());

    userNode = userPortalConfigService.getSiteNodeOrGlobalNode(SiteType.PORTAL.getName(), "classic", "home", "NotExisting");
    assertEquals("home", userNode.getURI());

    userNode = userPortalConfigService.getSiteNodeOrGlobalNode(SiteType.PORTAL.getName(), "classic", "webexplorer", "NotExisting");
    assertEquals("home", userNode.getURI());

    userNode = userPortalConfigService.getSiteNodeOrGlobalNode(SiteType.PORTAL.getName(), "classic", "home", "john");
    assertEquals("home", userNode.getURI());
    userNode = userPortalConfigService.getSiteNodeOrGlobalNode(SiteType.PORTAL.getName(), "classic", "home/subnode", "john");
    assertEquals("home/subnode", userNode.getURI());

    userNode = userPortalConfigService.getSiteNodeOrGlobalNode(SiteType.PORTAL.getName(), "classic", "subnode", "john");
    assertEquals("home/subnode", userNode.getURI());

    userNode = userPortalConfigService.getSiteNodeOrGlobalNode(SiteType.PORTAL.getName(), "classic", "home/subnode2", "john");
    assertEquals("home/subnode2", userNode.getURI());

    userNode = userPortalConfigService.getSiteNodeOrGlobalNode(SiteType.PORTAL.getName(), "classic", "webexplorer", "john");
    assertEquals("webexplorer", userNode.getURI());

    userNode = userPortalConfigService.getSiteNodeOrGlobalNode(SiteType.PORTAL.getName(), "classic", "home/subnode233", "john");
    assertEquals("home", userNode.getURI());

    userNode = userPortalConfigService.getSiteNodeOrGlobalNode(SiteType.PORTAL.getName(), "classic", "home", "mary");
    assertEquals("home", userNode.getURI());
    userNode = userPortalConfigService.getSiteNodeOrGlobalNode(SiteType.PORTAL.getName(), "classic", "home/subnode", "mary");
    assertEquals("home/subnode", userNode.getURI());
    userNode = userPortalConfigService.getSiteNodeOrGlobalNode(SiteType.PORTAL.getName(), "classic", "home/subnode2", "mary");
    assertEquals("home/subnode2", userNode.getURI());

    userNode = userPortalConfigService.getSiteNodeOrGlobalNode(SiteType.PORTAL.getName(), "classic", "webexplorer", "john");
    assertEquals("webexplorer", userNode.getURI());

    userNode = userPortalConfigService.getSiteNodeOrGlobalNode(SiteType.PORTAL.getName(), "classic", "webexplorer", null);
    assertEquals("home", userNode.getURI());

    assertNull(userPortalConfigService.getSiteNodeOrGlobalNode(SiteType.PORTAL.getName(), "NotFound", "home/subnode233", "mary"));
  }

  private static Map<String, UserNavigation> toMap(UserPortal cfg) {
    return toMap(cfg.getNavigations());
  }

  private static Map<String, UserNavigation> toMap(List<UserNavigation> navigations) {
    Map<String, UserNavigation> map = new HashMap<>();
    for (UserNavigation nav : navigations) {
      map.put(nav.getKey().getType().getName() + "::" + nav.getKey().getName(), nav);
    }
    return map;
  }

  private abstract class UnitTest {

    /** . */
    protected final void execute(String userId) {
      Throwable failure = null;

      //
      begin();

      //
      ConversationState conversationState = null;
      if (userId != null) {
        try {
          conversationState = new ConversationState(authenticator.createIdentity(userId));
        } catch (Exception e) {
          failure = e;
        }
      }

      //
      if (failure == null) {
        //
        ConversationState.setCurrent(conversationState);
        try {
          execute();
        } catch (Exception e) {
          failure = e;
        } finally {
          ConversationState.setCurrent(null);
          end();
        }
      }

      // Report error as a junit assertion failure
      if (failure != null) {
        AssertionFailedError err = new AssertionFailedError();
        err.initCause(failure);
        throw err;
      }
    }

    protected abstract void execute() throws Exception;

  }
}
