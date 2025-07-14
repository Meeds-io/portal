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
package org.exoplatform.commons.persistence.impl;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import jakarta.persistence.EntityManager;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Proxy that returns an EntityManager instance. If there is an entityManager
 * already started, it will return it. If no entityManager is found in the
 * context, it will create a new one and close it at the end of the execution of
 * the method.
 *
 */
public class EntityManagerHolder {
  private static final Log           LOG      = ExoLogger.getLogger(EntityManagerHolder.class);

  private final static EntityManager EM_PROXY = (EntityManager) Proxy.newProxyInstance(
          ExoEntityManagerInvocationHandler.class.getClassLoader(),
          new Class[] { EntityManager.class },
          new ExoEntityManagerInvocationHandler());

  public static EntityManager get() {
    return EM_PROXY;
  }

  private static class ExoEntityManagerInvocationHandler implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      boolean emStarted = false;
      EntityManager entityManager = null;

      EntityManagerService service = getService();
      try {
        entityManager = service.getEntityManager();
        if (entityManager == null) {
          LOG.debug("Injecting new EntityManager");
          entityManager = service.createEntityManager();
          emStarted = true;
        } else {
          LOG.debug("Using existing EntityManager");
        }
        return method.invoke(entityManager, args);

      } finally {
        if (emStarted && (entityManager != null) && (!entityManager.getTransaction().isActive())) {
          LOG.debug("Closing new EntityManager");
          service.closeEntityManager();
        }
      }
    }
  }

  private static EntityManagerService getService() {
    EntityManagerService service = ExoContainerContext.getService(EntityManagerService.class);
    if (service == null) {
      PortalContainer container = PortalContainer.getInstanceIfPresent();
      if (container != null) {
        service = container.getComponentInstanceOfType(EntityManagerService.class);
      }
      if (service == null) {
        throw new IllegalStateException("Cannot find EntityManagerService instance. This may happen when ExoContainerContext.getCurrentContainer is not set in current thread.");
      }
    }
    return service;
  }

}
