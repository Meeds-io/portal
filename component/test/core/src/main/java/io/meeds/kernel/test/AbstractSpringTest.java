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
package io.meeds.kernel.test;

import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.component.test.KernelBootstrap;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.component.RequestLifeCycle;

import lombok.Getter;
import lombok.Setter;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
})
public abstract class AbstractSpringTest {

  @Setter
  @Getter
  private static Class<?>        testClass;

  private static KernelBootstrap bootstrap;

  public static PortalContainer bootContainer(Class<?> clazz) {
    Class<? extends Object> bootTestClass = ObjectUtils.firstNonNull(clazz,
                                                                     testClass,
                                                                     AbstractSpringTest.class);
    if (isPortalContainerPresent()) {
      if (isPortalContainerIncludingSpring(bootTestClass)) {
        PortalContainer container = PortalContainer.getInstanceIfPresent();
        ExoContainerContext.setCurrentContainer(container);
        return container;
      } else {
        RootContainer rootContainer = RootContainer.getInstance();
        rootContainer.stop();
        rootContainer.dispose();
        RootContainer.setInstance(null);
        PortalContainer.setInstance(null);
      }
    }
    bootstrap = new KernelBootstrap(Thread.currentThread().getContextClassLoader());
    bootstrap.addConfiguration(bootTestClass);
    bootstrap.boot();
    PortalContainer container = bootstrap.getContainer();
    ExoContainerContext.setCurrentContainer(container);
    container.registerComponentInstance(SpringLoadFlag.class);
    return container;
  }

  public PortalContainer getContainer() {
    return bootstrap == null ? bootContainer() : bootstrap.getContainer();
  }

  protected PortalContainer bootContainer() {
    if (isPortalContainerPresent()) {
      PortalContainer container = PortalContainer.getInstanceIfPresent();
      ExoContainerContext.setCurrentContainer(container);
      return container;
    } else {
      PortalContainer container = bootContainer(getClass());
      ExoContainerContext.setCurrentContainer(container);
      return container;
    }
  }

  protected void begin() {
    PortalContainer container = getContainer();
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
  }

  protected void end() {
    RequestLifeCycle.end();
  }

  protected void restartTransaction() {
    int i = 0;
    // Close transactions until no encapsulated transaction
    boolean success = true;
    do {
      try {
        end();
        i++;
      } catch (IllegalStateException e) {
        success = false;
      }
    } while (success);

    // Restart transactions with the same number of encapsulations
    for (int j = 0; j < i; j++) {
      begin();
    }
  }

  protected static boolean isPortalContainerPresent() {
    return ExoContainerContext.getCurrentContainerIfPresent() != null && PortalContainer.getInstanceIfPresent() != null;
  }

  protected static boolean isPortalContainerIncludingSpring(Class<?> testClass) {
    SpringLoadFlag springLoadFlag = isPortalContainerPresent() ? PortalContainer.getInstance()
                                                                                .getComponentInstanceOfType(SpringLoadFlag.class) :
                                                               null;
    return springLoadFlag != null && Objects.equals(testClass, springLoadFlag.getTestClass());
  }

  public class SpringLoadFlag { // NOSONAR

    @Getter
    @Setter
    private Class<?> testClass;

  }
}
