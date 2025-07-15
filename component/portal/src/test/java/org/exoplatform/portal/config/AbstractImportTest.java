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

import java.util.List;

import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.component.test.KernelBootstrap;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.importer.Status;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.Node;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;

@ConfiguredBy({})
public abstract class AbstractImportTest extends AbstractConfigTest {

  private static PortalContainer container;

  protected abstract ImportMode getMode();

  protected abstract String getConfig2();

  protected abstract String getConfig1();

  protected abstract void afterOnePhaseBoot(NodeContext<?> root);

  protected abstract void afterTwoPhasesBoot(NodeContext<?> root);

  protected abstract void afterTwoPhaseOverrideReboot(NodeContext<?> root);

  @Override
  protected void setUp() {// NOSONAR
    // Avoid starting container
  }

  @Override
  protected void tearDown() {// NOSONAR
    // Avoid starting container
  }

  @Override
  protected void beforeClass() {
    if (container != null) {
      RootContainer.getInstance().stop();
      ExoContainerContext.setCurrentContainer(null);
    }
  }

  @Override
  protected void afterClass() {
    if (container != null) {
      RootContainer.getInstance().stop();
      ExoContainerContext.setCurrentContainer(null);
    }
  }

  @Override
  public PortalContainer getContainer() {
    return container;
  }

  public void testOnePhase() throws Exception {// NOSONAR
    KernelBootstrap bootstrap = startContainer(false, true, true, false);

    //
    System.setProperty("override.1", "true");
    System.setProperty("import.mode.1", "overwrite");
    System.setProperty("import.portal.1", getConfig1());
    System.setProperty("override_2", "true");
    System.setProperty("import.mode_2", getMode().toString());
    System.setProperty("import.portal_2", getConfig2());

    bootContainer(bootstrap);

    NavigationService service = container.getComponentInstanceOfType(NavigationService.class);
    begin();
    try {
      NavigationContext nav = service.loadNavigation(SiteKey.portal("classic"));
      NodeContext<?> root = service.loadNode(Node.MODEL, nav, Scope.ALL, null);
      afterOnePhaseBoot(root);
    } finally {
      restartTransaction();
      clearPortalData(container);
      end();
      bootstrap.dispose();
    }
  }

  public void testTwoPhasesOverride() throws Exception {// NOSONAR
    KernelBootstrap bootstrap = startContainer(false, true, false, false);

    //
    System.setProperty("override.1", "true");
    System.setProperty("import.mode.1", getMode().toString());

    //
    System.setProperty("import.portal.1", getConfig1());

    bootContainer(bootstrap);

    NavigationService service = container.getComponentInstanceOfType(NavigationService.class);
    begin();
    try {
      NavigationContext nav = service.loadNavigation(SiteKey.portal("classic"));
      NodeContext<?> root = service.loadNode(Node.MODEL, nav, Scope.ALL, null);
      afterTwoPhasesBoot(root);
    } finally {
      restartTransaction();
      end();
      stopContainer(bootstrap);
    }

    //
    System.setProperty("import.portal.1", getConfig2());

    bootContainer(bootstrap);

    service = container.getComponentInstanceOfType(NavigationService.class);
    begin();
    try {
      NavigationContext nav = service.loadNavigation(SiteKey.portal("classic"));
      NodeContext<?> root = service.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);
      afterTwoPhaseOverrideReboot(root);
    } finally {
      restartTransaction();
      clearPortalData(container);
      end();
      stopContainer(bootstrap);
    }
  }

  private void clearPortalData(PortalContainer container) throws Exception {
    LayoutService dataStorage = container.getComponentInstanceOfType(LayoutService.class);
    List<String> portalNames = dataStorage.getSiteNames(SiteType.GROUP, 0, 200);
    for (String portalName : portalNames) {
      PortalConfig portalConfig = dataStorage.getPortalConfig(PortalConfig.GROUP_TYPE, portalName);
      dataStorage.remove(portalConfig);
    }
    portalNames = dataStorage.getSiteNames(SiteType.PORTAL, 0, 200);
    for (String portalName : portalNames) {
      PortalConfig portalConfig = dataStorage.getPortalConfig(PortalConfig.PORTAL_TYPE, portalName);
      dataStorage.remove(portalConfig);
    }
    dataStorage.saveImportStatus(Status.WANT_REIMPORT);
  }

  protected void stopContainer(KernelBootstrap bootstrap) {
    if (bootstrap != null) {
      bootstrap.dispose();
    }
    PortalContainer portalContainer = PortalContainer.getInstanceIfPresent();
    if (portalContainer != null) {
      RootContainer.getInstance().stop();
      ExoContainerContext.setCurrentContainer(null);
      container = null;
    }
  }

  protected void bootContainer(KernelBootstrap bootstrap) {
    bootstrap.boot();
    container = bootstrap.getContainer();
    ExoContainerContext.setCurrentContainer(container);
  }

  protected KernelBootstrap startContainer(boolean importFile0, boolean importFile1, boolean importFile2, boolean importFile3) {
    PortalContainer portalContainer = PortalContainer.getInstanceIfPresent();
    if (portalContainer != null) {
      RootContainer.getInstance().stop();
      ExoContainerContext.setCurrentContainer(null);
    }

    KernelBootstrap bootstrap = new KernelBootstrap();
    bootstrap.addConfiguration(ContainerScope.ROOT, "conf/configuration.xml");
    bootstrap.addConfiguration(ContainerScope.PORTAL, "conf/portal/configuration.xml");
    bootstrap.addConfiguration(ContainerScope.PORTAL, "conf/exo.portal.component.portal-configuration-local.xml");
    if (importFile0) {
      bootstrap.addConfiguration(ContainerScope.PORTAL, "org/exoplatform/portal/config/TestImport0-configuration.xml");
    }
    if (importFile1) {
      bootstrap.addConfiguration(ContainerScope.PORTAL, "org/exoplatform/portal/config/TestImport1-configuration.xml");
    }
    if (importFile2) {
      bootstrap.addConfiguration(ContainerScope.PORTAL, "org/exoplatform/portal/config/TestImport2-configuration.xml");
    }
    if (importFile3) {
      bootstrap.addConfiguration(ContainerScope.PORTAL, "org/exoplatform/portal/config/TestImport3-configuration.xml");
    }
    return bootstrap;
  }

}
