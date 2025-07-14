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
package org.exoplatform.commons.container.test;

import org.picocontainer.Startable;

import org.exoplatform.commons.api.persistence.DataInitializer;
import org.exoplatform.container.BaseContainerLifecyclePlugin;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.services.naming.InitialContextInitializer;

/**
 * A class to start the database at first before starting any other component
 */
public class TestDatabaseInitializerContainerPlugin extends BaseContainerLifecyclePlugin {

  @Override
  public void initContainer(ExoContainer container) {
    InitialContextInitializer initialContextInitializer = container.getComponentInstanceOfType(InitialContextInitializer.class);
    if (initialContextInitializer instanceof Startable startable) {
      startable.start(); // Will probably do nothing but call it in case
                         // InitialContextInitializer2 is modified
    }
    DataInitializer dataInitializer = container.getComponentInstanceOfType(DataInitializer.class);
    if (dataInitializer != null) {
      if (container instanceof RootContainer) {
        // Attempt to start DataInitializer on PortalContainer or
        // StandaloneContainer only
        container.unregisterComponent(dataInitializer);
      } else if (dataInitializer instanceof Startable startable) {
        startable.start(); // Inject DBMS Schema before starting any other
                           // service
      }
    }
  }

}
