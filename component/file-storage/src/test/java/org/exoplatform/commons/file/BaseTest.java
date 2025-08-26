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
package org.exoplatform.commons.file;

import org.exoplatform.component.test.*;
import org.exoplatform.container.PortalContainer;


@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/components-configuration.xml"),
})
public abstract class BaseTest extends AbstractKernelTest {

  /** . */
  public static KernelBootstrap ownBootstrap = null;

  @Override
  public PortalContainer getContainer() {
    return ownBootstrap != null ? ownBootstrap.getContainer() : super.getContainer();
  }

  @Override
  protected void beforeRunBare() {
    if (ownBootstrap == null) {
      super.beforeRunBare();
    }
  }

  @Override
  protected void afterRunBare() {
    if (ownBootstrap == null && super.getContainer() != null) {
      super.afterRunBare();
    }
  }

  protected void setUp() {
    begin();
  }

  protected void tearDown() {
    end();
  }

  public <T> T getService(Class<T> clazz) {
    return (T) getContainer().getComponentInstanceOfType(clazz);
  }
}
