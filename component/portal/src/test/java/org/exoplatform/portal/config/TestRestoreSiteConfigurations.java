/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.storage.PageStorage;

/**
 * Restores the site "contributed", defined by three configurations: the first
 * one holds pages and no navigation, the two others hold pages and the nodes
 * home > section > node2 and home > section > node3, the second one also holds
 * home > section > node23, which references a page of the third one, and the
 * third one attaches home > attached3 > leaf3 to the home node. The site "partial" is defined by a
 * configuration and by another one whose pages and navigation can't be read.
 * The site "attached" is defined by a configuration that attaches section6 >
 * node6 to the node section6, which no configuration defines. The site
 * "startup" is defined by a configuration imported in insert mode and by
 * another one that overwrites the site at each startup.
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration-local.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/config/conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/config/restore/configuration.xml") })
public class TestRestoreSiteConfigurations extends AbstractConfigTest {

  private static final String     SITE_NAME         = "contributed";

  private static final SiteKey    SITE_KEY          = SiteKey.portal(SITE_NAME);

  private static final String     PARTIAL_SITE_NAME = "partial";

  private static final String     ATTACHED_SITE_NAME = "attached";

  private static final String     STARTUP_SITE_NAME  = "startup";

  private static final String     CUSTOM_NAME       = "custom";

  private static final String     HOME              = "home";

  private static final String     SECTION           = "section";

  private static final String     ATTACHED          = "attached3";

  private UserPortalConfigService userPortalConfigService;

  private NavigationService       navigationService;

  private PageStorage             pageStorage;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    PortalContainer container = getContainer();
    userPortalConfigService = container.getComponentInstanceOfType(UserPortalConfigService.class);
    navigationService = container.getComponentInstanceOfType(NavigationService.class);
    pageStorage = container.getComponentInstanceOfType(PageStorage.class);
  }

  public void testOverwriteRestoresThePagesAndNavigationOfEveryConfiguration() {
    addCustomPageAndNodes();
    pageStorage.destroyPage(new PageKey(SITE_KEY, "page3"));
    SiteKey attachedSiteKey = SiteKey.portal(ATTACHED_SITE_NAME);
    addCustomNode(getRootNode(attachedSiteKey), new PageKey(SITE_KEY, "page1"));
    try {
      assertTrue(userPortalConfigService.restoreSite(PortalConfig.PORTAL_TYPE, SITE_NAME, ImportMode.OVERWRITE, true, true, true));
      restartTransaction();

      assertEquals(Set.of("page1", "page2", "page3"), getPageNames(SITE_KEY));
      NodeContext<?> rootNode = getRootNode(SITE_KEY);
      assertEquals(Set.of(HOME), getNodeNames(rootNode));
      assertEquals(Set.of(SECTION, ATTACHED), getNodeNames(rootNode.get(HOME)));
      NodeContext<?> sectionNode = rootNode.get(HOME).get(SECTION);
      assertEquals(Set.of("node2", "node23", "node3"), getNodeNames(sectionNode));
      assertEquals(new PageKey(SITE_KEY, "page2"), sectionNode.get("node2").getState().getPageRef());
      assertEquals("A node references the page of a later configuration, imported before any navigation",
                   new PageKey(SITE_KEY, "page3"),
                   sectionNode.get("node23").getState().getPageRef());
      assertEquals("A node references the removed default page that the restore creates again",
                   new PageKey(SITE_KEY, "page3"),
                   sectionNode.get("node3").getState().getPageRef());
      NodeContext<?> attachedNode = rootNode.get(HOME).get(ATTACHED);
      assertEquals(Set.of("leaf3"), getNodeNames(attachedNode));
      assertEquals(new PageKey(SITE_KEY, "page3"), attachedNode.get("leaf3").getState().getPageRef());
      assertEquals("A node of another site keeps the restored page it references",
                   new PageKey(SITE_KEY, "page1"),
                   getRootNode(attachedSiteKey).get(CUSTOM_NAME).getState().getPageRef());
    } finally {
      removeCustomNode(attachedSiteKey);
    }
  }

  public void testRestorePagesImportsThePagesOfASiteThatTheStartupDidntImport() {
    PageKey pageKey = new PageKey(SITE_KEY, "page3");
    pageStorage.destroyPage(pageKey);
    restartTransaction();
    // A startup that imports no configuration of the site, as a restart does
    // when none of them overrides it
    Set<String> createdOwners = userPortalConfigService.getNewPortalConfigListener().createdOwners;
    Set<String> startupOwners = new HashSet<>(createdOwners);
    createdOwners.clear();
    try {
      assertTrue(userPortalConfigService.restoreSite(PortalConfig.PORTAL_TYPE,
                                                     SITE_NAME,
                                                     ImportMode.RESTORE_DEFAULTS,
                                                     false,
                                                     true,
                                                     true));
      restartTransaction();

      assertNotNull(pageStorage.getPage(pageKey));
    } finally {
      createdOwners.addAll(startupOwners);
    }
  }

  public void testRestoreDefaultsKeepsTheDefaultNodesOfEveryConfiguration() {
    addCustomPageAndNodes();
    try {
      assertTrue(userPortalConfigService.restoreSite(PortalConfig.PORTAL_TYPE,
                                                     SITE_NAME,
                                                     ImportMode.RESTORE_DEFAULTS,
                                                     true,
                                                     true,
                                                     true));
      restartTransaction();

      assertEquals("A restore of the default pages keeps the custom pages",
                   Set.of("page1", "page2", "page3", CUSTOM_NAME),
                   getPageNames(SITE_KEY));
      NodeContext<?> rootNode = getRootNode(SITE_KEY);
      assertEquals("A custom node of a fragment attachment point is kept",
                   Set.of(HOME, CUSTOM_NAME),
                   getNodeNames(rootNode));
      assertEquals("A custom node of a top-level node of a fragment is kept",
                   Set.of(SECTION, ATTACHED, CUSTOM_NAME),
                   getNodeNames(rootNode.get(HOME)));
      assertEquals("The nodes of each configuration are kept and the custom nested node is removed",
                   Set.of("node2", "node23", "node3"),
                   getNodeNames(rootNode.get(HOME).get(SECTION)));
      assertEquals("A custom node of a top-level node of a fragment attached to another node is kept",
                   Set.of("leaf3", CUSTOM_NAME),
                   getNodeNames(rootNode.get(HOME).get(ATTACHED)));
    } finally {
      userPortalConfigService.restoreSite(PortalConfig.PORTAL_TYPE, SITE_NAME, ImportMode.OVERWRITE, false, true, true);
      restartTransaction();
    }
  }

  public void testOverwriteKeepsTheNodesWhenANavigationConfigurationCantBeRead() {
    SiteKey partialSiteKey = SiteKey.portal(PARTIAL_SITE_NAME);
    addCustomNode(getRootNode(partialSiteKey), null);
    try {
      assertTrue(userPortalConfigService.restoreSite(PortalConfig.PORTAL_TYPE,
                                                     PARTIAL_SITE_NAME,
                                                     ImportMode.OVERWRITE,
                                                     false,
                                                     false,
                                                     true));
      restartTransaction();

      assertEquals(Set.of("node4", CUSTOM_NAME), getNodeNames(getRootNode(partialSiteKey)));
    } finally {
      removeCustomNode(partialSiteKey);
    }
  }

  public void testOverwriteKeepsThePagesWhenAPagesConfigurationCantBeRead() {
    PageKey customPageKey = new PageKey(SiteKey.portal(PARTIAL_SITE_NAME), CUSTOM_NAME);
    pageStorage.savePage(new PageContext(customPageKey, null));
    restartTransaction();
    try {
      assertTrue(userPortalConfigService.restoreSite(PortalConfig.PORTAL_TYPE,
                                                     PARTIAL_SITE_NAME,
                                                     ImportMode.OVERWRITE,
                                                     false,
                                                     true,
                                                     false));
      restartTransaction();

      assertEquals(Set.of("page4", CUSTOM_NAME), getPageNames(customPageKey.getSite()));
    } finally {
      pageStorage.destroyPage(customPageKey);
      restartTransaction();
    }
  }

  public void testStartupOverwriteImportsTheSiteFromEveryConfiguration() {
    SiteKey startupSiteKey = SiteKey.portal(STARTUP_SITE_NAME);
    assertEquals("An overwrite at startup keeps the pages of the other configurations",
                 Set.of("pageA", "pageB"),
                 getPageNames(startupSiteKey));
    NodeContext<?> startupRootNode = getRootNode(startupSiteKey);
    assertEquals("An overwrite at startup keeps the nodes of the other configurations",
                 Set.of("nodeA", "nodeB"),
                 getNodeNames(startupRootNode));
    assertEquals(new PageKey(startupSiteKey, "pageA"), startupRootNode.get("nodeA").getState().getPageRef());
    assertEquals(new PageKey(startupSiteKey, "pageB"), startupRootNode.get("nodeB").getState().getPageRef());

    PageKey customPageKey = new PageKey(startupSiteKey, CUSTOM_NAME);
    pageStorage.savePage(new PageContext(customPageKey, null));
    addCustomNode(getRootNode(startupSiteKey), customPageKey);
    SiteKey attachedSiteKey = SiteKey.portal(ATTACHED_SITE_NAME);
    addCustomNode(getRootNode(attachedSiteKey), new PageKey(startupSiteKey, "pageB"));
    try {
      userPortalConfigService.start();
      restartTransaction();

      assertEquals("An overwrite at startup removes the pages that no configuration defines only",
                   Set.of("pageA", "pageB"),
                   getPageNames(startupSiteKey));
      assertEquals("An overwrite at startup removes the nodes that no configuration defines only",
                   Set.of("nodeA", "nodeB"),
                   getNodeNames(getRootNode(startupSiteKey)));
      assertEquals("A node of another site keeps the page that an overwrite at startup imports",
                   new PageKey(startupSiteKey, "pageB"),
                   getRootNode(attachedSiteKey).get(CUSTOM_NAME).getState().getPageRef());
    } finally {
      removeCustomNode(attachedSiteKey);
    }
  }

  public void testOverwriteKeepsTheNodesOutsideTheFragmentAttachmentPoints() {
    SiteKey attachedSiteKey = SiteKey.portal(ATTACHED_SITE_NAME);
    addCustomNode(getRootNode(attachedSiteKey), null);
    addCustomNode(getRootNode(attachedSiteKey).get("section6"), null);
    try {
      assertTrue(userPortalConfigService.restoreSite(PortalConfig.PORTAL_TYPE,
                                                     ATTACHED_SITE_NAME,
                                                     ImportMode.OVERWRITE,
                                                     false,
                                                     false,
                                                     true));
      restartTransaction();

      NodeContext<?> rootNode = getRootNode(attachedSiteKey);
      assertEquals("A node outside the fragment attachment point is kept",
                   Set.of("section6", CUSTOM_NAME),
                   getNodeNames(rootNode));
      assertEquals(Set.of("node6"), getNodeNames(rootNode.get("section6")));
    } finally {
      removeCustomNode(attachedSiteKey);
    }
  }

  private void addCustomPageAndNodes() {
    PageKey customPageKey = new PageKey(SITE_KEY, CUSTOM_NAME);
    pageStorage.savePage(new PageContext(customPageKey, null));
    addCustomNode(getRootNode(SITE_KEY), customPageKey);
    addCustomNode(getRootNode(SITE_KEY).get(HOME), customPageKey);
    addCustomNode(getRootNode(SITE_KEY).get(HOME).get(SECTION), customPageKey);
    addCustomNode(getRootNode(SITE_KEY).get(HOME).get(ATTACHED), customPageKey);
  }

  private void addCustomNode(NodeContext<?> parentNode, PageKey pageKey) {
    NodeContext<?> customNode = parentNode.add(null, CUSTOM_NAME);
    customNode.setState(customNode.getState().builder().pageRef(pageKey).build());
    navigationService.saveNode(parentNode, null);
    restartTransaction();
  }

  private void removeCustomNode(SiteKey siteKey) {
    NodeContext<?> rootNode = getRootNode(siteKey);
    if (rootNode.get(CUSTOM_NAME) != null) {
      rootNode.get(CUSTOM_NAME).removeNode();
      navigationService.saveNode(rootNode, null);
    }
    restartTransaction();
  }

  private NodeContext<?> getRootNode(SiteKey siteKey) {
    NavigationContext navigation = navigationService.loadNavigation(siteKey);
    return navigationService.loadNode(NodeModel.SELF_MODEL, navigation, Scope.ALL, null);
  }

  private Set<String> getNodeNames(NodeContext<?> parentNode) {
    return parentNode.getNodes().stream().map(node -> ((NodeContext<?>) node).getName()).collect(Collectors.toSet());
  }

  private Set<String> getPageNames(SiteKey siteKey) {
    return pageStorage.loadPages(siteKey).stream().map(page -> page.getKey().getName()).collect(Collectors.toSet());
  }

}
