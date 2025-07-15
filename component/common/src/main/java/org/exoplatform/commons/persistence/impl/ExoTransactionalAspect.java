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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Process the Transactional annotation. The only propagation implemented yet is
 * REQUIRED. Support a current transaction, create a new one if none exists.
 * Analogous to EJB or Spring transaction attribute of the same name.
 *
 * @see ExoTransactional
 */
@Aspect
public class ExoTransactionalAspect {
  private static final Log LOG = ExoLogger.getLogger(ExoTransactionalAspect.class);

  @Around("execution(* *(..)) && @annotation(org.exoplatform.commons.api.persistence.ExoTransactional)")
  public Object around(ProceedingJoinPoint point) throws Throwable {
    EntityManager entityManager;
    boolean begunTx = false;
    boolean emStarted = false;

    // Do we need to start EntityManager ?
    EntityManagerService service = getService();
    entityManager = service.getEntityManager();
    if (entityManager == null) {
      LOG.debug("Injecting new PersistenceContext");
      entityManager = service.createEntityManager();
      emStarted = true;
    } else {
      LOG.debug("Using existing PersistenceContext");
    }

    // Do we need to start Transaction ?
    EntityTransaction tx = entityManager.getTransaction();
    if (tx.isActive()) {
      LOG.debug("Using current transaction");
    } else {
      LOG.debug("Starting new transaction");
      try {
        tx.begin();
      } catch (PersistenceException e) {
        LOG.warn("An error occured while starting JPA transaction. The transaction will be restarted.", e);
        try {
          service.closeEntityManager();
        } catch (Exception e1) {
          LOG.warn("An error occured while closing current JPA entity manager. The transaction will be recreated.", e);
        }
        entityManager = service.createEntityManager();
        tx = entityManager.getTransaction();
        tx.begin();
      }
      begunTx = true;
    }
    try {
      return point.proceed();
    } finally {
      // Do we need to end Transaction ?
      try {
        if ((begunTx) && (tx.isActive())) {
          if (!tx.getRollbackOnly()) {
            LOG.debug("Committing current transaction");
            tx.commit();
          } else {
            LOG.debug("Rollback current transaction set as RollbackOnly");
            tx.rollback();
          }
        }
      } catch (RuntimeException ex) {
        try {
          if (tx != null && tx.isActive()) {
            tx.rollback();
          }
        } catch (RuntimeException rbEx) {
          LOG.error("Could not roll back transaction", rbEx);
        }
        throw ex;
      } finally {
        // Do we need to close EntityManager ?
        if (emStarted && (entityManager != null)) {
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
