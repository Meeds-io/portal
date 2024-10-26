/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.exoplatform.portal.mop.user;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.exoplatform.portal.config.AbstractConfigTest;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

public class UserPortalTest extends AbstractConfigTest {

  private static final String     TEST_USER_PORTAL_GROUP    = "/platform";

  private static final SiteKey    TEST_USER_PORTAL_SITE_KEY = SiteKey.group(TEST_USER_PORTAL_GROUP);

  private UserPortal              userPortal;

  private NavigationService       navigationService;

  private UserPortalConfigService portalConfigService;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    begin();
    Identity identity = new Identity("root",
                                     Arrays.asList(
                                                   new MembershipEntry(TEST_USER_PORTAL_GROUP),
                                                   new MembershipEntry("/platform/users"),
                                                   new MembershipEntry("/platform/administrators")));
    ConversationState conversationState = new ConversationState(identity);
    ConversationState.setCurrent(conversationState);

    this.navigationService = getContainer().getComponentInstanceOfType(NavigationService.class);
    this.portalConfigService = getContainer().getComponentInstanceOfType(UserPortalConfigService.class);
    @SuppressWarnings("deprecation")
    UserPortalConfig userPortalConfig = portalConfigService.getUserPortalConfig("classic", "root"); // NOSONAR
    this.userPortal = userPortalConfig.getUserPortal();

    removeTestedNavigation();
  }

  @Override
  protected void tearDown() throws Exception {
    removeTestedNavigation();
    end();
    ConversationState.setCurrent(null);
    super.tearDown();
  }

  public void testCreate() throws Exception {
    List<UserNavigation> navs = userPortal.getNavigations();
    assertFalse(navs.isEmpty());
    int initialSize = navs.size();

    createTestedNavigation();
    userPortal.refresh();
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
    userPortal.refresh();

    navs = userPortal.getNavigations();
    assertFalse(navs.isEmpty());
    assertEquals(initialSize + 1, navs.size());

    UserNavigation userNavigation = userPortal.getNavigation(TEST_USER_PORTAL_SITE_KEY);
    assertNotNull(userNavigation);

    NodeContext root = navigationService.loadNode(NodeModel.SELF_MODEL, userNavigation.navigation, Scope.ALL, null);
    root.add(null, "foo");
    navigationService.saveNode(root, null);
    userPortal.refresh();

    navs = userPortal.getNavigations();
    assertEquals(initialSize + 1, navs.size());

    navigationService.destroyNavigation(TEST_USER_PORTAL_SITE_KEY);
    userPortal.refresh();

    navs = userPortal.getNavigations();
    assertEquals(initialSize, navs.size());
  }

  public void testDestroy() throws Exception {
    List<UserNavigation> navs = userPortal.getNavigations();
    assertFalse(navs.isEmpty());
    int initialSize = navs.size();

    createTestedNavigation();
    userPortal.refresh();

    navs = userPortal.getNavigations();
    assertFalse(navs.isEmpty());
    assertEquals(initialSize + 1, navs.size());

    UserNavigation userNavigation = userPortal.getNavigation(TEST_USER_PORTAL_SITE_KEY);
    assertNotNull(userNavigation);

    navigationService.destroyNavigation(TEST_USER_PORTAL_SITE_KEY);
    userPortal.refresh();

    navs = userPortal.getNavigations();
    assertEquals(initialSize, navs.size());
  }

  public void testGetSiteLabels() {
    assertEquals("Classic", userPortal.getPortalLabel(SiteKey.portal("classic")));
    assertEquals("Site classique", userPortal.getPortalLabel(SiteKey.portal("classic"), Locale.FRENCH));
    assertEquals("This is classic portal for testing", userPortal.getPortalDescription(SiteKey.portal("classic")));
    assertEquals("Un site pour test seulement", userPortal.getPortalDescription(SiteKey.portal("classic"), Locale.FRENCH));
  }

  private void createTestedNavigation() throws Exception {
    portalConfigService.createUserPortalConfig(PortalConfig.GROUP_TYPE, TEST_USER_PORTAL_GROUP, "group", "jar:/org/exoplatform/portal/config/conf");
    navigationService.saveNavigation(new NavigationContext(TEST_USER_PORTAL_SITE_KEY, new NavigationState(1)));
  }

  private void removeTestedNavigation() throws Exception {
    NavigationContext navigationContext = navigationService.loadNavigation(TEST_USER_PORTAL_SITE_KEY);
    if (navigationContext != null) {
      portalConfigService.removeUserPortalConfig(TEST_USER_PORTAL_SITE_KEY.getTypeName(), TEST_USER_PORTAL_SITE_KEY.getName());
      restartTransaction();
    }
  }

}
