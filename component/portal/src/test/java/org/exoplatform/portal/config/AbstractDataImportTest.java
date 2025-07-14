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

import java.util.HashSet;
import java.util.Set;

import org.exoplatform.component.test.*;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.portal.mop.importer.ImportMode;

@ConfiguredBy({})
public abstract class AbstractDataImportTest extends AbstractConfigTest {
  private Set<String>     clearProperties = new HashSet<String>();

  private PortalContainer container;

  protected abstract ImportMode getMode();

  protected abstract String getConfig2();

  protected abstract String getConfig1();

  protected void afterOneBootWithExtention(PortalContainer container) throws Exception {
  }

  protected void afterFirstBoot(PortalContainer container) throws Exception {
  }

  protected abstract void afterSecondBoot(PortalContainer container) throws Exception;

  protected abstract void afterSecondBootWithOverride(PortalContainer container) throws Exception;

  protected abstract void afterSecondBootWithWantReimport(PortalContainer container) throws Exception;

  protected abstract void afterSecondBootWithNoMixin(PortalContainer container) throws Exception;

  protected void setSystemProperty(String key, String value) {
    clearProperties.add(key);
    System.setProperty(key, value);
  }

  @Override
  protected void setUp() throws Exception {
    // Avoid starting container
  }

  @Override
  protected void tearDown() throws Exception {
    for (String key : clearProperties) {
      System.clearProperty(key);
    }
    clearProperties.clear();
  }

  @Override
  protected void beforeClass() {
    // Avoid starting container
  }

  @Override
  protected void afterClass() {
    // Avoid starting container
  }

  @Override
  public PortalContainer getContainer() {
    return container;
  }

  public void testOneBootWithExtension() throws Exception {
    KernelBootstrap bootstrap = startContainer(false, true, true, false);

    setSystemProperty("override.1", "true");
    setSystemProperty("import.mode.1", getMode().toString());
    setSystemProperty("import.portal.1", getConfig1());
    setSystemProperty("override_2", "true");
    setSystemProperty("import.mode_2", getMode().toString());
    setSystemProperty("import.portal_2", getConfig2());

    bootContainer(bootstrap);

    begin();
    try {
      afterOneBootWithExtention(container);
    } finally {
      end();
    }
    stopContainer(bootstrap);
  }

  public void testOneBoot() throws Exception {
    KernelBootstrap bootstrap = startContainer(false, true, false, false);

    setSystemProperty("override.1", "false");
    setSystemProperty("import.mode.1", getMode().toString());
    setSystemProperty("import.portal.1", getConfig1());

    bootContainer(bootstrap);

    begin();
    try {
      afterFirstBoot(container);
    } finally {
      end();
    }
    stopContainer(bootstrap);
  }

  public void testTwoBoots() throws Exception {
    KernelBootstrap bootstrap = startContainer(false, true, false, false);

    //
    setSystemProperty("override.1", "false");
    setSystemProperty("import.mode.1", getMode().toString());
    setSystemProperty("import.portal.1", getConfig1());

    bootContainer(bootstrap);

    begin();
    try {
      afterFirstBoot(container);
    } finally {
      end();
    }
    stopContainer(bootstrap);

    setSystemProperty("import.portal.1", getConfig2());

    bootContainer(bootstrap);

    begin();
    try {
      afterSecondBoot(container);
    } finally {
      end();
    }
    stopContainer(bootstrap);
  }

  public void testTwoBootsWithOverride() throws Exception {
    KernelBootstrap bootstrap = startContainer(false, true, false, false);

    setSystemProperty("override.1", "true");
    setSystemProperty("import.mode.1", getMode().toString());
    setSystemProperty("import.portal.1", getConfig1());

    bootContainer(bootstrap);

    begin();
    try {
      afterFirstBoot(container);
    } finally {
      end();
    }
    stopContainer(bootstrap);

    setSystemProperty("import.portal.1", getConfig2());

    bootContainer(bootstrap);

    begin();
    try {
      afterSecondBootWithOverride(container);
    } finally {
      end();
    }
    stopContainer(bootstrap);
  }

  public void testTwoBootsWithPortalConfigOverrideFlag() throws Exception {
    KernelBootstrap bootstrap = startContainer(false, false, false, true);

    setSystemProperty("override.1", "true");
    setSystemProperty("override.2", "false");
    setSystemProperty("import.mode.1", getMode().toString());
    setSystemProperty("import.portal.1", getConfig1());

    bootContainer(bootstrap);

    begin();
    try {
      afterFirstBoot(container);
    } finally {
      end();
    }

    stopContainer(bootstrap);

    //
    setSystemProperty("import.portal.1", getConfig2());

    bootContainer(bootstrap);
    begin();
    try {
      afterSecondBoot(container);
    } finally {
      end();
    }

    stopContainer(bootstrap);
  }

  protected void stopContainer(KernelBootstrap bootstrap) {
    if (bootstrap != null) {
      bootstrap.dispose();
    }
    PortalContainer portalContainer = PortalContainer.getInstanceIfPresent();
    if (portalContainer != null) {
      RootContainer.getInstance().stop();
      ExoContainerContext.setCurrentContainer(null);
    }
  }

  protected void bootContainer(KernelBootstrap bootstrap) {
    bootstrap.boot();
    this.container = bootstrap.getContainer();
    ExoContainerContext.setCurrentContainer(this.container);
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
