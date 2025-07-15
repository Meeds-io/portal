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
package org.exoplatform.web.portal;

import java.util.Arrays;
import java.util.List;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.mop.user.UserPortalImpl;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.application.URLBuilder;
import org.exoplatform.web.url.PortalURL;
import org.exoplatform.web.url.ResourceType;
import org.exoplatform.web.url.URLFactory;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/test.mop.portal.configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/web/portal/configuration.xml")
})
public class TestRefreshCurrentUserPortal extends AbstractKernelTest { // NOSONAR

  private static final String     TEST_USER_PORTAL_GROUP    = "/platform";

  private static final SiteKey    TEST_USER_PORTAL_SITE_KEY = SiteKey.group(TEST_USER_PORTAL_GROUP);

  private UserPortal              userPortal;

  private NavigationService       navigationService;

  private UserPortalConfigService portalConfigService;

  private LayoutService           layoutService;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    begin();

    this.navigationService = getContainer().getComponentInstanceOfType(NavigationService.class);
    this.portalConfigService = getContainer().getComponentInstanceOfType(UserPortalConfigService.class);
    this.layoutService = getContainer().getComponentInstanceOfType(LayoutService.class);
    getContainer().getComponentInstanceOfType(IdentityRegistry.class)
                  .register(new Identity("root",
                                         Arrays.asList(
                                                       new MembershipEntry(TEST_USER_PORTAL_GROUP),
                                                       new MembershipEntry("/platform/users"),
                                                       new MembershipEntry("/platform/administrators"))));

    UserPortalConfig userPortalConfig = portalConfigService.getUserPortalConfig("classic", "root"); // NOSONAR
    this.userPortal = userPortalConfig.getUserPortal();
    RequestContext.setCurrentInstance(new RequestContext(null) {
      @Override
      public URLFactory getURLFactory() {
        throw new UnsupportedOperationException();
      }

      @Override
      public <R, U extends PortalURL<R, U>> U newURL(ResourceType<R, U> resourceType, URLFactory urlFactory) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Orientation getOrientation() {
        throw new UnsupportedOperationException();
      }

      @Override
      public String getRequestParameter(String name) {
        throw new UnsupportedOperationException();
      }

      @Override
      public String[] getRequestParameterValues(String name) {
        throw new UnsupportedOperationException();
      }

      @Override
      public URLBuilder<?> getURLBuilder() {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean useAjax() {
        throw new UnsupportedOperationException();
      }

      @Override
      public UserPortal getUserPortal() {
        return userPortal;
      }
    });

    removeTestedNavigation();
  }

  @Override
  protected void tearDown() throws Exception {
    getContainer().getComponentInstanceOfType(IdentityRegistry.class).unregister("root");
    removeTestedNavigation();
    RequestContext.setCurrentInstance(null);
    end();
    ConversationState.setCurrent(null);
    super.tearDown();
  }

  public void testCreate() throws Exception {
    List<UserNavigation> navs = userPortal.getNavigations();
    assertFalse(navs.isEmpty());
    int initialSize = navs.size();

    createTestedNavigation();

    navs = userPortal.getNavigations();
    assertEquals(initialSize + 1, navs.size());
  }

  @SuppressWarnings({
    "rawtypes", "unchecked"
  })
  public void testUpdate() throws Exception {
    List<UserNavigation> navs = userPortal.getNavigations();
    assertFalse(navs.isEmpty());
    int initialSize = navs.size();

    createTestedNavigation();

    navs = userPortal.getNavigations();
    assertFalse(navs.isEmpty());
    assertEquals(initialSize + 1, navs.size());

    UserNavigation userNavigation = userPortal.getNavigation(TEST_USER_PORTAL_SITE_KEY);
    assertNotNull(userNavigation);

    NavigationContext navigationContext = navigationService.loadNavigation(TEST_USER_PORTAL_SITE_KEY);
    NodeContext root = navigationService.loadNode(NodeModel.SELF_MODEL, navigationContext, Scope.ALL, null);
    root.add(null, "foo");
    navigationService.saveNode(root, null);

    navs = userPortal.getNavigations();
    assertEquals(initialSize + 1, navs.size());

    navigationService.destroyNavigation(TEST_USER_PORTAL_SITE_KEY);

    navs = userPortal.getNavigations();
    assertEquals(initialSize, navs.size());
  }

  public void testDestroy() throws Exception {
    List<UserNavigation> navs = userPortal.getNavigations();
    assertFalse(navs.isEmpty());
    int initialSize = navs.size();

    createTestedNavigation();

    navs = userPortal.getNavigations();
    assertFalse(navs.isEmpty());
    assertEquals(initialSize + 1, navs.size());

    UserNavigation userNavigation = userPortal.getNavigation(TEST_USER_PORTAL_SITE_KEY);
    assertNotNull(userNavigation);

    navigationService.destroyNavigation(TEST_USER_PORTAL_SITE_KEY);

    navs = userPortal.getNavigations();
    assertEquals(initialSize, navs.size());
  }

  private void createTestedNavigation() {
    portalConfigService.createUserPortalConfig(PortalConfig.GROUP_TYPE,
                                               TEST_USER_PORTAL_GROUP,
                                               "group",
                                               "jar:/org/exoplatform/portal/config/conf");
    navigationService.saveNavigation(new NavigationContext(TEST_USER_PORTAL_SITE_KEY, new NavigationState(1)));
  }

  private void removeTestedNavigation() {
    NavigationContext navigationContext = navigationService.loadNavigation(TEST_USER_PORTAL_SITE_KEY);
    if (navigationContext != null) {
      layoutService.remove(layoutService.getPortalConfig(TEST_USER_PORTAL_SITE_KEY));
      restartTransaction();
    }
  }

}
