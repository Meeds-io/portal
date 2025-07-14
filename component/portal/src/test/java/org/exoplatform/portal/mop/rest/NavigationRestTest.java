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
package org.exoplatform.portal.mop.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.navigation.NodeChangeListener;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.rest.NavigationRest.ResultUserNavigation;
import org.exoplatform.portal.mop.rest.model.UserNodeBreadcrumbItem;
import org.exoplatform.portal.mop.rest.model.UserNodeRestEntity;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.mop.user.UserPortalImpl;
import org.exoplatform.portal.rest.services.BaseRestServicesTestCase;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.test.mock.MockHttpServletRequest;

import jakarta.servlet.http.HttpServletRequest;

@RunWith(MockitoJUnitRunner.class)
public class NavigationRestTest extends BaseRestServicesTestCase {

  @Mock
  private UserPortalConfigService   portalConfigService;

  @Mock
  private LayoutService             layoutService;

  @Mock
  private OrganizationService       organizationService;

  @Mock
  private UserACL                   userACL;

  protected Class<?> getComponentClass() {
    return NavigationRest.class;
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    binder.addResource(new NavigationRest(portalConfigService, layoutService, organizationService, userACL), null);
  }

  @Override
  protected void registry(Class<?> resourceClass) throws Exception {
    // Nothing to do
  }

  @After
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetSiteNavigation() throws Exception {
    String path = "/v1/navigations/";
    String username = "testuser";
    String metaPortalName = "default";
    String globalPortalName = "global";
    String portalName = "test";

    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(path, null, 0, "GET", null);
    envctx.put(HttpServletRequest.class, httpRequest);

    UserPortalConfig userPortalConfig = mock(UserPortalConfig.class);
    UserPortalImpl userPortal = mock(UserPortalImpl.class);
    NavigationState navigationState = mock(NavigationState.class);
    List<UserNavigation> userNavigations = Arrays.asList(
                                                         new UserNavigation(userPortal,
                                                                            new NavigationContext(SiteKey.portal(globalPortalName),
                                                                                                  navigationState),
                                                                            false),
                                                         new UserNavigation(userPortal,
                                                                            new NavigationContext(SiteKey.portal(portalName),
                                                                                                  navigationState),
                                                                            false));

    startUserSession(username);
    when(portalConfigService.getMetaPortal()).thenReturn(metaPortalName);
    when(portalConfigService.getGlobalPortal()).thenReturn(globalPortalName);
    when(portalConfigService.getUserPortalConfig(eq(metaPortalName), eq(username))).thenReturn(userPortalConfig);
    when(userPortalConfig.getUserPortal()).thenReturn(userPortal);
    when(userPortal.getNavigations()).thenReturn(userNavigations);

    ContainerResponse resp = launcher.service("GET", path, "", null, null, envctx);

    assertEquals(200, resp.getStatus());
    Object entity = resp.getEntity();
    assertNotNull(entity);
    assertTrue(entity instanceof List);
    List<ResultUserNavigation> navigations = (List<ResultUserNavigation>) entity;
    assertEquals(1, navigations.size());
    assertEquals(portalName, navigations.get(0).getKey().getName());
    assertEquals(SiteType.PORTAL, navigations.get(0).getKey().getType());

    when(userPortal.getNavigations()).thenThrow(IllegalStateException.class);
    resp = launcher.service("GET", path, "", null, null, envctx);
    assertEquals(500, resp.getStatus());
  }

  @Test
  public void testGetSiteNavigationWithPageDetails() throws Exception {
    String path = "/v1/navigations/PORTAL?siteName=SiteName&expand=true";

    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(path, null, 0, "GET", null);
    envctx.put(HttpServletRequest.class, httpRequest);

    startUserSession("root1");
    Collection<UserNode> nodes = new ArrayList<>();
    Page nodePage = mock(Page.class);
    UserNode userNode = mock(UserNode.class);
    nodes.add(userNode);
    UserPortalConfig userPortalConfig = mock(UserPortalConfig.class);
    UserPortal userPortal = mock(UserPortal.class);
    PageKey pageKey = PageKey.parse("portal::page::ref");
    when(layoutService.getPage(pageKey)).thenReturn(nodePage);
    when(nodePage.getEditPermission()).thenReturn("*:/platform/users");
    when(nodePage.getAccessPermissions()).thenReturn(new String[] {"*:/platform/users"});
    when(nodePage.getType()).thenReturn("LINK");
    when(nodePage.getLink()).thenReturn("www.test.com");
    when(userNode.getPageRef()).thenReturn(pageKey);
    when(portalConfigService.getUserPortalConfig(anyString(), anyString())).thenReturn(userPortalConfig);
    when(userPortalConfig.getUserPortal()).thenReturn(userPortal);
    when(userPortal.getNodes(any(SiteType.class) , any(Scope.class), any(UserNodeFilterConfig.class),anyBoolean())).thenReturn(nodes);
    when(userACL.hasEditPermission(any(Page.class), any(Identity.class))).thenReturn(true);
    ContainerResponse resp = launcher.service("GET", path, "", null, null, envctx);
    Object entity = resp.getEntity();

    assertEquals(200, resp.getStatus());
    assertNotNull(entity);
    List<UserNodeRestEntity> resultUserNodes = (List<UserNodeRestEntity>) resp.getEntity();
    assertEquals(1, resultUserNodes.size());
    assertEquals("*:/platform/users", resultUserNodes.get(0).getPageEditPermission());
    assertEquals(1, resultUserNodes.get(0).getPageAccessPermissions().length);
    assertEquals("*:/platform/users", resultUserNodes.get(0).getPageAccessPermissions()[0]);

    when(nodePage.getEditPermission()).thenReturn("manager:/platform/users");
    when(nodePage.getAccessPermissions()).thenReturn(new String[] {"Everyone"});

    resp = launcher.service("GET", path, "", null, null, envctx);
    entity = resp.getEntity();

    assertEquals(200, resp.getStatus());
    assertNotNull(entity);
    resultUserNodes = (List<UserNodeRestEntity>) resp.getEntity();
    assertEquals(1, resultUserNodes.size());
    assertEquals(1, resultUserNodes.size());
    assertEquals("manager:/platform/users", resultUserNodes.get(0).getPageEditPermission());
    assertEquals(1, resultUserNodes.get(0).getPageAccessPermissions().length);
    assertEquals("Everyone", resultUserNodes.get(0).getPageAccessPermissions()[0]);
    assertEquals("www.test.com", resultUserNodes.get(0).getPageLink());
  }

  @Test
  public void testGetSiteNavigationWithBreadcrumbs() throws Exception {

    String path = "/v1/navigations/PORTAL?siteName=SiteName&expandBreadcrumb=true";

    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(path, null, 0, "GET", null);
    envctx.put(HttpServletRequest.class, httpRequest);

    startUserSession("root1");
    Collection<UserNode> nodes = new ArrayList<>();
    Page nodePage = mock(Page.class);
    UserNode parentNode = mock(UserNode.class);
    UserNode userNode = mock(UserNode.class);
    nodes.add(userNode);
    UserNavigation userNavigation = mock(UserNavigation.class);
    UserPortalConfig userPortalConfig = mock(UserPortalConfig.class);
    UserPortal userPortal = mock(UserPortal.class);
    PageKey pageKey = PageKey.parse("portal::page::ref");
    when(layoutService.getPage(pageKey)).thenReturn(nodePage);
    when(nodePage.getType()).thenReturn("PAGE");
    when(userNode.getPageRef()).thenReturn(pageKey);
    when(userNode.getURI()).thenReturn("homepage/usernode");
    when(userNode.getId()).thenReturn("1");
    when(userNode.getName()).thenReturn("usernode");
    when(userNode.getResolvedLabel()).thenReturn("user node label");
    when(userNode.getTarget()).thenReturn("SAME_TAB");
    when(userNode.getNavigation()).thenReturn(userNavigation);
    SiteKey siteKey = mock(SiteKey.class);
    when(userNavigation.getKey()).thenReturn(siteKey);
    when(siteKey.getName()).thenReturn("siteName");
    when(userNode.getParent()).thenReturn(parentNode);
    when(parentNode.getName()).thenReturn("default");
    when(parentNode.getChildren()).thenReturn(nodes);
    when(portalConfigService.getUserPortalConfig(anyString(), anyString())).thenReturn(userPortalConfig);
    when(userPortalConfig.getUserPortal()).thenReturn(userPortal);
    when(userPortal.getNodes(any(SiteType.class),
                             any(Scope.class),
                             any(UserNodeFilterConfig.class),
                             anyBoolean())).thenReturn(nodes);
    when(userPortal.getNode(any(UserNavigation.class),
                            any(Scope.class),
                            any(UserNodeFilterConfig.class),
                            nullable(NodeChangeListener.class))).thenReturn(parentNode);
    ContainerResponse resp = launcher.service("GET", path, "", null, null, envctx);
    Object entity = resp.getEntity();
    assertEquals(200, resp.getStatus());
    assertNotNull(entity);
    List<UserNodeRestEntity> resultUserNodes = (List<UserNodeRestEntity>) resp.getEntity();
    assertEquals(1, resultUserNodes.size());
    List<UserNodeBreadcrumbItem> userNodeBreadcrumbItemList = resultUserNodes.get(0).getUserNodeBreadcrumbItemList();
    assertNotNull(userNodeBreadcrumbItemList);
    assertEquals(1, userNodeBreadcrumbItemList.size());
    assertEquals("1", userNodeBreadcrumbItemList.get(0).getNodeId());
    assertEquals("usernode", userNodeBreadcrumbItemList.get(0).getName());
    assertEquals("SAME_TAB", userNodeBreadcrumbItemList.get(0).getTarget());
    assertEquals("user node label", userNodeBreadcrumbItemList.get(0).getLabel());
    assertEquals("/portal/siteName/homepage/usernode", userNodeBreadcrumbItemList.get(0).getUri());

    when(userNode.getTarget()).thenReturn("NEW_TAB");
    when(nodePage.getType()).thenReturn("LINK");
    when(nodePage.getLink()).thenReturn("www.test.com");

    resp = launcher.service("GET", path, "", null, null, envctx);
    entity = resp.getEntity();
    assertEquals(200, resp.getStatus());
    assertNotNull(entity);
    resultUserNodes = (List<UserNodeRestEntity>) resp.getEntity();
    assertEquals(1, resultUserNodes.size());
    userNodeBreadcrumbItemList = resultUserNodes.get(0).getUserNodeBreadcrumbItemList();
    assertNotNull(userNodeBreadcrumbItemList);
    assertTrue(userNodeBreadcrumbItemList.size() > 0);
    assertEquals("NEW_TAB", userNodeBreadcrumbItemList.get(0).getTarget());
    assertNotNull("www.test.com", userNodeBreadcrumbItemList.get(0).getUri());
  }
}
