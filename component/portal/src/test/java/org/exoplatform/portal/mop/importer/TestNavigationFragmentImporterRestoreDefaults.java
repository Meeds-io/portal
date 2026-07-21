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
package org.exoplatform.portal.mop.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.model.NavigationFragment;
import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.config.model.PageNode;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.navigation.Node;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.storage.DescriptionStorage;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.PortalData;

/**
 * Reproduces the reported "Restore default pages" scenario against the real
 * admin-site navigation shape, where every section ("general", "organisation"...)
 * is nested one level below a single "home" wrapper node:
 * <ul>
 * <li>a copy/paste leftover living inside an already-default section must be
 * removed by {@link ImportMode#RESTORE_DEFAULTS};</li>
 * <li>a page cut out of one default section and pasted into another default
 * section must be removed from where it landed, and recreated at its default
 * location;</li>
 * <li>a brand-new custom section added as a sibling of "general" (i.e. also
 * nested under "home", exactly like every other admin section) must survive
 * untouched.</li>
 * </ul>
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/test.navigation.configuration.xml") })
public class TestNavigationFragmentImporterRestoreDefaults extends AbstractKernelTest {

  private NavigationService   service;

  private DescriptionStorage  descriptionStorage;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    PortalContainer container = PortalContainer.getInstance();
    this.service = container.getComponentInstanceOfType(NavigationService.class);
    this.descriptionStorage = container.getComponentInstanceOfType(DescriptionStorage.class);
    begin();
  }

  // Each test uses its own site name: they share no state (in-memory tree
  // caching keyed by site+navigation collides across tests otherwise).
  private void createSite(String siteName) throws Exception {
    ContainerData containerData = new ContainerData(null,
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
                                       SiteType.PORTAL.getName(),
                                       null,
                                       null,
                                       null,
                                       new ArrayList<>(),
                                       null,
                                       null,
                                       null,
                                       containerData,
                                       true,
                                       9,
                                       0);
    PortalContainer.getInstance().getComponentInstanceOfType(org.exoplatform.portal.mop.storage.SiteStorage.class).create(portal);

    NavigationContext nav = new NavigationContext(SiteKey.portal(siteName), new NavigationState(1));
    service.saveNavigation(nav);
    restartTransaction();
  }

  public void testStrayNestedDuplicateIsRemovedButCustomSectionIsKept() throws Exception {
    String siteName = "restore_defaults_stray_test";
    createSite(siteName);

    // Build the corrupted live tree, mirroring the real admin site: everything
    // lives under a single "home" wrapper. "general" is a default section whose
    // "notification" child was copy/pasted, leaving a stray duplicate behind.
    // "myCustomSection" is a brand-new admin-added section, a sibling of
    // "general" under "home" -- exactly where a real admin would add one.
    NavigationContext nav = service.loadNavigation(SiteKey.portal(siteName));
    NodeContext<?> root = service.loadNode(Node.MODEL, nav, Scope.ALL, null);
    NodeContext<?> home = root.add(null, "home");
    home.add(null, "myCustomSection");
    NodeContext<?> general = home.add(null, "general");
    general.add(null, "notification");
    general.add(null, "notification-copy-12345");
    service.saveNode(root, null);
    restartTransaction();

    // The default tree only ever declared "home" > "general" > "notification".
    PageNode notification = new PageNode();
    notification.setName("notification");
    notification.setLabel("Notification");

    PageNode general2 = new PageNode();
    general2.setName("general");
    general2.setLabel("General");
    general2.getNodes().add(notification);

    PageNode home2 = new PageNode();
    home2.setName("home");
    home2.setLabel("Home");
    home2.getNodes().add(general2);

    NavigationFragment fragment = new NavigationFragment();
    fragment.setParentURI(null);
    fragment.getNodes().add(home2);

    PageNavigation src = new PageNavigation(SiteType.PORTAL.getName(), siteName);
    src.setPriority(1);
    src.addFragment(fragment);

    new NavigationImporter(Locale.ENGLISH, ImportMode.RESTORE_DEFAULTS, src, service, descriptionStorage).perform();
    restartTransaction();

    nav = service.loadNavigation(SiteKey.portal(siteName));
    Node reloadedRoot = service.loadNode(Node.MODEL, nav, Scope.ALL, null).getNode();
    Node reloadedHome = reloadedRoot.getChild("home");
    assertNotNull("the default 'home' wrapper must still exist", reloadedHome);

    Node reloadedGeneral = reloadedHome.getChild("general");
    assertNotNull("the default 'general' section must still exist", reloadedGeneral);
    assertNotNull("the default 'notification' child must still exist",
                  reloadedGeneral.getChild("notification"));
    assertNull("the copy/paste leftover nested under a default section must be removed by RESTORE_DEFAULTS",
               reloadedGeneral.getChild("notification-copy-12345"));
    assertNotNull("a brand-new custom section, nested under 'home' exactly like every real admin section, "
        + "must never be touched by RESTORE_DEFAULTS just because it isn't a top-level fragment child",
                  reloadedHome.getChild("myCustomSection"));
  }

  public void testCutItemBetweenTwoDefaultSectionsIsRestoredToItsDefaultLocation() throws Exception {
    String siteName = "restore_defaults_cut_test";
    createSite(siteName);

    // Build the corrupted live tree: "notification" was cut out of "general"
    // (its default section) and pasted into "organisation" (a different
    // default section), so "general" is missing it and "organisation" has it
    // as a stray extra.
    NavigationContext nav = service.loadNavigation(SiteKey.portal(siteName));
    NodeContext<?> root = service.loadNode(Node.MODEL, nav, Scope.ALL, null);
    NodeContext<?> home = root.add(null, "home");
    home.add(null, "general");
    NodeContext<?> organisation = home.add(null, "organisation");
    organisation.add(null, "notification");
    service.saveNode(root, null);
    restartTransaction();

    // The default tree declares "notification" under "general", not under "organisation".
    PageNode notification = new PageNode();
    notification.setName("notification");
    notification.setLabel("Notification");

    PageNode general = new PageNode();
    general.setName("general");
    general.setLabel("General");
    general.getNodes().add(notification);

    PageNode organisation2 = new PageNode();
    organisation2.setName("organisation");
    organisation2.setLabel("Organisation");

    PageNode home2 = new PageNode();
    home2.setName("home");
    home2.setLabel("Home");
    home2.getNodes().add(general);
    home2.getNodes().add(organisation2);

    NavigationFragment fragment = new NavigationFragment();
    fragment.setParentURI(null);
    fragment.getNodes().add(home2);

    PageNavigation src = new PageNavigation(SiteType.PORTAL.getName(), siteName);
    src.setPriority(1);
    src.addFragment(fragment);

    new NavigationImporter(Locale.ENGLISH, ImportMode.RESTORE_DEFAULTS, src, service, descriptionStorage).perform();
    restartTransaction();

    nav = service.loadNavigation(SiteKey.portal(siteName));
    Node reloadedHome = service.loadNode(Node.MODEL, nav, Scope.ALL, null).getNode().getChild("home");

    Node reloadedGeneral = reloadedHome.getChild("general");
    assertNotNull("the default 'general' section must still exist", reloadedGeneral);
    assertNotNull("the cut item must be recreated at its default location ('general')",
                  reloadedGeneral.getChild("notification"));

    Node reloadedOrganisation = reloadedHome.getChild("organisation");
    assertNotNull("the default 'organisation' section must still exist", reloadedOrganisation);
    assertNull("the stray left behind by the cut, inside a different default section ('organisation'), "
        + "must be removed by RESTORE_DEFAULTS",
               reloadedOrganisation.getChild("notification"));
  }
}
