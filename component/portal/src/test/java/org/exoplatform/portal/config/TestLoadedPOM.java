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

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.storage.PageStorage;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

public class TestLoadedPOM extends AbstractConfigTest {

  /** . */
  private LayoutService           layoutService;

  /** . */
  private PageStorage             pageStorage;

  /** . */
  private NavigationService       navService;

  public void setUp() throws Exception {
    super.setUp();
    PortalContainer container = getContainer();
    layoutService = container.getComponentInstanceOfType(LayoutService.class);
    pageStorage = container.getComponentInstanceOfType(PageStorage.class);
    navService = container.getComponentInstanceOfType(NavigationService.class);
  }

  public void testLegacyGroupWithNormalizedName() throws Exception {
    SiteKey key = SiteKey.group("/test/legacy");
    NavigationContext nav = navService.loadNavigation(key);
    assertNotNull(nav);
    NodeContext<?> root = navService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);
    NodeContext<?> node = root.get(0);
    assertEquals(SiteKey.group("/test/legacy").page("register"), node.getState().getPageRef());

    Page page = layoutService.getPage("group::/test/legacy::register");
    assertNotNull(page);
    assertEquals("group::/test/legacy::register", page.getPageId());
    assertEquals("/test/legacy", page.getOwnerId());
  }

  public void testGroupWithNormalizedName() throws Exception {
    SiteKey key = SiteKey.group("/test/normalized");
    restartTransaction();
    NavigationContext nav = navService.loadNavigation(key);
    assertNotNull(nav);
    NodeContext<?> root = navService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);
    NodeContext<?> node = root.get(0);
    assertEquals(SiteKey.group("/test/normalized").page("register"), node.getState().getPageRef());

    Page page = layoutService.getPage("group::/test/normalized::register");
    assertNotNull(page);
    assertEquals("group::/test/normalized::register", page.getPageId());
    assertEquals("/test/normalized", page.getOwnerId());
  }

  public void testPageProfiles() throws Exception {
    try (MockedStatic<ExoContainer> exoContainer = Mockito.mockStatic(ExoContainer.class);) {
      exoContainer.when(ExoContainer::getProfiles).thenReturn(Stream.of("test").collect(Collectors.toSet()));

      SiteKey key = SiteKey.portal("profiles");
      NavigationContext nav = navService.loadNavigation(key);
      assertNotNull(nav);
      NodeContext<?> root = navService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);
      NodeContext<?> node = root.get(0);
      assertEquals("register", node.getName());
      assertNull(node.getState().getPageRef());

      node = root.get(1);
      assertEquals("register2", node.getName());
      assertEquals(SiteKey.portal("profiles").page("register2"), node.getState().getPageRef());

      Page page = layoutService.getPage("portal::profiles::register");
      assertNotNull(page);
      assertEquals("portal::profiles::register", page.getPageId());
      assertEquals("profiles", page.getOwnerId());
      assertEquals("unknown", page.getProfiles());

      page = layoutService.getPage("portal::profiles::register2");
      assertNotNull(page);
      assertEquals("portal::profiles::register2", page.getPageId());
      assertEquals("profiles", page.getOwnerId());
      assertEquals("test", page.getProfiles());

      node = root.get(0);
      NodeContext<?> subNode = node.get("subpage");
      assertNotNull(subNode);
      assertEquals("subpage", subNode.getName());
      assertEquals(SiteKey.portal("profiles").page("subpage"), subNode.getState().getPageRef());
    }
  }

  @SuppressWarnings("unchecked")
  public void testPageWithSections() throws Exception {
    Page page = layoutService.getPage("portal::profiles::sections");
    assertNotNull(page);
    assertEquals(1, page.getChildren().size());
    Container sectionsParent = (Container) page.getChildren().get(0);
    assertEquals("system:/groovy/portal/webui/container/UIPageLayout.gtmpl", sectionsParent.getTemplate());

    assertEquals(2, sectionsParent.getChildren().size());

    Container columnsSection = (Container) sectionsParent.getChildren().get(0);
    assertEquals(2, columnsSection.getChildren().size());
    assertEquals("FlexContainer", columnsSection.getTemplate());
    assertNotNull(columnsSection.getCssClass());
    assertTrue("'grid-cols-md-12' not found", columnsSection.getCssClass().contains("grid-cols-md-12"));
    assertTrue("'grid-cols-lg-12' not found", columnsSection.getCssClass().contains("grid-cols-lg-12"));
    assertTrue("'grid-cols-xl-12' not found", columnsSection.getCssClass().contains("grid-cols-xl-12"));
    assertTrue("'layout-mobile-columns' not found", columnsSection.getCssClass().contains("layout-mobile-columns"));
    assertTrue("'layout-sticky-application' not found", columnsSection.getCssClass().contains("layout-sticky-application"));
    assertFalse("'grid-rows' should not be present", columnsSection.getCssClass().contains("grid-rows"));
    assertTrue("'TEST-columns-section-class' not found", columnsSection.getCssClass().contains("TEST-columns-section-class"));

    Container column = (Container) columnsSection.getChildren().get(0);
    assertEquals(1, column.getChildren().size());
    assertEquals("CellContainer", column.getTemplate());
    assertNotNull(column.getCssClass());
    assertTrue("'flex-cell' not found", column.getCssClass().contains("flex-cell"));
    assertTrue("'grid-cell-colspan-md-9' not found", column.getCssClass().contains("grid-cell-colspan-md-9"));
    assertTrue("'grid-cell-colspan-lg-9' not found", column.getCssClass().contains("grid-cell-colspan-lg-9"));
    assertTrue("'grid-cell-colspan-xl-9' not found", column.getCssClass().contains("grid-cell-colspan-xl-9"));
    assertTrue("'grid-cell-rowspan-md-1' not found", column.getCssClass().contains("grid-cell-rowspan-md-1"));
    assertTrue("'grid-cell-rowspan-lg-1' not found", column.getCssClass().contains("grid-cell-rowspan-lg-1"));
    assertTrue("'grid-cell-rowspan-xl-1' not found", column.getCssClass().contains("grid-cell-rowspan-xl-1"));
    assertTrue("'TEST-column-class' custom class not found", column.getCssClass().contains("TEST-column-class"));

    Application columnApplication = (Application) column.getChildren().get(0);
    assertNotNull(columnApplication.getCssClass());
    assertTrue("'mt-n1' not found", columnApplication.getCssClass().contains("mt-n1"));
    assertTrue("'mb-n3' not found", columnApplication.getCssClass().contains("mb-n3"));
    assertTrue("'me-n4' not found", columnApplication.getCssClass().contains("me-n4"));
    assertTrue("'ms-n5' not found", columnApplication.getCssClass().contains("ms-n5"));
    assertTrue("'brtr-4' not found", columnApplication.getCssClass().contains("brtr-4"));
    assertTrue("'brtl-2' not found", columnApplication.getCssClass().contains("brtl-2"));
    assertTrue("'brbr-1' not found", columnApplication.getCssClass().contains("brbr-1"));
    assertTrue("'brbl-0' not found", columnApplication.getCssClass().contains("brbl-0"));
    assertTrue("'hidden-sm-and-down' not found", columnApplication.getCssClass().contains("hidden-sm-and-down"));

    Container gridSection = (Container) sectionsParent.getChildren().get(1);
    assertEquals(3, gridSection.getChildren().size());
    assertEquals("GridContainer", gridSection.getTemplate());
    assertNotNull(gridSection.getCssClass());
    assertTrue("'grid-cols-md-4' not found", gridSection.getCssClass().contains("grid-cols-md-4"));
    assertTrue("'grid-cols-lg-4' not found", gridSection.getCssClass().contains("grid-cols-lg-4"));
    assertTrue("'grid-cols-xl-4' not found", gridSection.getCssClass().contains("grid-cols-xl-4"));
    assertTrue("'grid-rows-md-2' not found", gridSection.getCssClass().contains("grid-rows-md-2"));
    assertTrue("'grid-rows-lg-2' not found", gridSection.getCssClass().contains("grid-rows-lg-2"));
    assertTrue("'grid-rows-xl-2' not found", gridSection.getCssClass().contains("grid-rows-xl-2"));
    assertTrue("'TEST-grid-section-class' not found", gridSection.getCssClass().contains("TEST-grid-section-class"));

    Container cell = (Container) gridSection.getChildren().get(0);
    assertEquals(1, cell.getChildren().size());
    assertEquals("CellContainer", cell.getTemplate());
    assertNotNull(cell.getCssClass());
    assertTrue("'grid-cell' not found", cell.getCssClass().contains("grid-cell"));
    assertTrue("'grid-cell-colspan-md-2' not found", cell.getCssClass().contains("grid-cell-colspan-md-2"));
    assertTrue("'grid-cell-colspan-lg-2' not found", cell.getCssClass().contains("grid-cell-colspan-lg-2"));
    assertTrue("'grid-cell-colspan-xl-2' not found", cell.getCssClass().contains("grid-cell-colspan-xl-2"));
    assertTrue("'grid-cell-rowspan-md-3' not found", cell.getCssClass().contains("grid-cell-rowspan-md-3"));
    assertTrue("'grid-cell-rowspan-lg-3' not found", cell.getCssClass().contains("grid-cell-rowspan-lg-3"));
    assertTrue("'grid-cell-rowspan-xl-3' not found", cell.getCssClass().contains("grid-cell-rowspan-xl-3"));
    assertTrue("'TEST-grid-cell-class' custom class not found", cell.getCssClass().contains("TEST-grid-cell-class"));
    assertTrue("'mt-n5' not found", cell.getCssClass().contains("mt-n5"));
    assertTrue("'mb-n4' not found", cell.getCssClass().contains("mb-n4"));
    assertTrue("'me-n3' not found", cell.getCssClass().contains("me-n3"));
    assertTrue("'ms-n1' not found", cell.getCssClass().contains("ms-n1"));
    assertTrue("'brtr-0' not found", cell.getCssClass().contains("brtr-0"));
    assertTrue("'brtl-1' not found", cell.getCssClass().contains("brtl-1"));
    assertTrue("'brbr-2' not found", cell.getCssClass().contains("brbr-2"));
    assertTrue("'brbl-4' not found", cell.getCssClass().contains("brbl-4"));
    assertTrue("'hidden-sm-and-down' not found", cell.getCssClass().contains("hidden-sm-and-down"));
  }

  public void testNavigation() throws Exception {
    SiteKey key = SiteKey.portal("test");
    NavigationContext nav = navService.loadNavigation(key);
    assertNotNull(nav);

    //
    assertEquals(1, (int) nav.getState().getPriority());

    //
    NodeContext<?> root = navService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);
    assertEquals(5, root.getNodeCount());

    //
    NodeContext<?> nodeNavigation = root.get(0);
    assertEquals(0, nodeNavigation.getNodeCount());
    assertEquals("node_name", nodeNavigation.getName());
    assertEquals("node_label", nodeNavigation.getState().getLabel());
    assertEquals("node_icon", nodeNavigation.getState().getIcon());
    GregorianCalendar start = new GregorianCalendar(2000, 2, 21, 1, 33, 0);
    start.setTimeZone(TimeZone.getTimeZone("UTC"));
    assertEquals(start.getTime().getTime(), nodeNavigation.getState().getStartPublicationTime());
    GregorianCalendar end = new GregorianCalendar(2009, 2, 21, 1, 33, 0);
    end.setTimeZone(TimeZone.getTimeZone("UTC"));
    assertEquals(end.getTime().getTime(), nodeNavigation.getState().getEndPublicationTime());
    assertEquals(Visibility.TEMPORAL, nodeNavigation.getState().getVisibility());
  }

  public void testLoadNode() throws Exception {
    NodeContext<?> root = navService.loadNode(SiteKey.portal("test"));
    assertEquals(5, root.getNodeCount());

    NodeContext<?> nodeNavigation = navService.loadNode(SiteKey.portal("test"), "node_name");
    assertEquals(0, nodeNavigation.getNodeCount());
    assertEquals("node_name", nodeNavigation.getName());
    assertEquals("node_label", nodeNavigation.getState().getLabel());
    assertEquals("node_icon", nodeNavigation.getState().getIcon());
    GregorianCalendar start = new GregorianCalendar(2000, 2, 21, 1, 33, 0);
    start.setTimeZone(TimeZone.getTimeZone("UTC"));
    assertEquals(start.getTime().getTime(), nodeNavigation.getState().getStartPublicationTime());
    GregorianCalendar end = new GregorianCalendar(2009, 2, 21, 1, 33, 0);
    end.setTimeZone(TimeZone.getTimeZone("UTC"));
    assertEquals(end.getTime().getTime(), nodeNavigation.getState().getEndPublicationTime());
    assertEquals(Visibility.TEMPORAL, nodeNavigation.getState().getVisibility());
  }

  public void testPortal() throws Exception {
    PortalConfig portal = layoutService.getPortalConfig("test");
    assertNotNull(portal);

    assertEquals("test", portal.getName());
    assertEquals("en", portal.getLocale());
    assertArrayEquals(new String[] { "test_portal_access_permissions" }, portal.getAccessPermissions());
    assertEquals("test_edit_permission", portal.getEditPermission());
    assertEquals("test_skin", portal.getSkin());
    assertEquals("test_prop_value", portal.getProperty("prop_key"));
  }

  public void testPageWithoutPageId() throws Exception {
    Page page = layoutService.getPage("portal::test::test2");
    assertNotNull(page);
    assertEquals("portal::test::test2", page.getPageId());
    assertEquals("test", page.getOwnerId());
    assertEquals("portal", page.getOwnerType());
    assertEquals("test2", page.getName());
  }

  public void testPage() throws Exception {
    Page page = layoutService.getPage("portal::test::test1");
    assertNotNull(page);

    PageContext pageContext = pageStorage.loadPage(page.getPageKey());
    assertNotNull(pageContext);

    //
    assertEquals("test_title", pageContext.getState().getDisplayName());
    assertEquals("test_factory_id", pageContext.getState().getFactoryId());
    assertEquals(Arrays.<String> asList("test_access_permissions"), pageContext.getState().getAccessPermissions());
    assertEquals("test_edit_permission", pageContext.getState().getEditPermission());
    assertEquals(true, pageContext.getState().isShowMaxWindow());

    //
    List<ModelObject> children = page.getChildren();
    assertEquals(2, children.size());

    //
    Container container1 = (Container) children.get(0);
    assertEquals("container_1", container1.getName());
    assertEquals("container_1_title", container1.getTitle());
    assertEquals("container_1_icon", container1.getIcon());
    assertEquals("container_1_template", container1.getTemplate());
    assertTrue(Arrays.equals(new String[] { "container_1_access_permissions" }, container1.getAccessPermissions()));
    assertEquals("container_1_factory_id", container1.getFactoryId());
    assertEquals("container_1_description", container1.getDescription());
    assertEquals("container_1_width", container1.getWidth());
    assertEquals("container_1_height", container1.getHeight());

    //
    Application application1 = (Application) children.get(1);
    assertEquals("application_1_theme", application1.getTheme());
    assertEquals("application_1_title", application1.getTitle());
    assertTrue(Arrays.equals(new String[] { "application_1_access_permissions" }, application1.getAccessPermissions()));
    assertEquals(true, application1.getShowInfoBar());
    assertEquals(true, application1.getShowApplicationState());
    assertEquals(true, application1.getShowApplicationMode());
    assertEquals("application_1_description", application1.getDescription());
    assertEquals("application_1_icon", application1.getIcon());
    assertEquals("application_1_width", application1.getWidth());
    assertEquals("application_1_height", application1.getHeight());
    // assertEquals("portal#test:/web/BannerPortlet/banner",
    // application1.getInstanceState().getWeakReference());
  }
}
