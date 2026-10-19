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
package io.meeds.common;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.transaction.support.TransactionSynchronization;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.RollbackException;

/**
 * An aspect processing point that will allow to encapsulate two behavior:
 * - Set current container to PortalContainer
 * - Start the request lifecycle of the container, and a JPA transaction when
 *   the annotation is {@link ContainerTransactional#atomic()}
 */
@Aspect
public class ContainerTransactionalAspect {

  private static final Log LOG = ExoLogger.getLogger(ContainerTransactionalAspect.class);

  /**
   * The advice keeps this signature: the classes an addon wove at its own build
   * call it as is.
   */
  @Around("execution(* *(..)) && @annotation(io.meeds.common.ContainerTransactional)")
  public Object around(ProceedingJoinPoint point) throws Throwable {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    ExoContainer transactionContainer = container;
    if (container instanceof RootContainer) {
      transactionContainer = PortalContainer.getInstance();
      ExoContainerContext.setCurrentContainer(transactionContainer);
    }
    RequestLifeCycle.begin(transactionContainer);
    try {
      if (isAtomic(point)) {
        return proceedAtomically(point, transactionContainer.getComponentInstanceOfType(EntityManagerService.class));
      } else {
        return point.proceed();
      }
    } finally {
      RequestLifeCycle.end();
      if (transactionContainer != container) {
        ExoContainerContext.setCurrentContainer(container);
      }
    }
  }

  /**
   * Runs the advised method in the active JPA transaction of the thread, or in
   * a new one that is committed when the method returns and rolled back when it
   * throws. The kernel DAOs annotated with ExoTransactional join it without
   * committing it. When the method begins the transaction, the Spring
   * transactional methods (Spring Data repositories included) join it too,
   * unless a Spring transaction of its own is active on the thread.
   */
  Object proceedAtomically(ProceedingJoinPoint point, EntityManagerService entityManagerService) throws Throwable {
    if (entityManagerService == null) {
      // No JPA persistence in the container: there is no JPA write to group
      return point.proceed();
    }
    EntityManager entityManager = entityManagerService.getEntityManager();
    if (entityManager == null) {
      throw new IllegalStateException("No JPA EntityManager is started on the current thread: the method can't run atomically");
    }
    EntityTransaction transaction = entityManager.getTransaction();
    if (transaction.isActive()) {
      return proceedInActiveTransaction(point, transaction);
    } else {
      return proceedInNewTransaction(point,
                                     transaction,
                                     SpringTransactionScope.open(entityManagerService.getEntityManagerFactory(), entityManager));
    }
  }

  private Object proceedInActiveTransaction(ProceedingJoinPoint point, EntityTransaction transaction) throws Throwable {
    try {
      return point.proceed();
    } catch (Throwable e) { // NOSONAR the enclosing transaction can't be committed whatever the error
      if (transaction.isActive()) {
        transaction.setRollbackOnly();
        // The kernel rolls a transaction marked rollback-only back without
        // throwing: a caller that catches the error loses its writes silently
        LOG.warn("{} failed in a transaction it joined, which is marked rollback-only: none of its writes is committed. Cause: {}",
                 point.getSignature(),
                 e.toString());
      }
      throw e;
    }
  }

  private Object proceedInNewTransaction(ProceedingJoinPoint point,
                                         EntityTransaction transaction,
                                         SpringTransactionScope springScope) throws Throwable {
    int status = TransactionSynchronization.STATUS_UNKNOWN;
    boolean committing = false;
    try {
      transaction.begin();
      Object result = point.proceed();
      if (transaction.getRollbackOnly()) {
        springScope.beforeCompletion();
        springScope.close();
        transaction.rollback();
        status = TransactionSynchronization.STATUS_ROLLED_BACK;
        throw new RollbackException("The transaction of " + point.getSignature()
            + " was marked rollback-only by a call that failed: none of its writes is committed");
      }
      springScope.beforeCommit();
      springScope.close();
      committing = true;
      transaction.commit();
      status = TransactionSynchronization.STATUS_COMMITTED;
      springScope.afterCommit();
      return result;
    } catch (Throwable e) { // NOSONAR none of the method writes is committed whatever the error
      if (transaction.isActive()) {
        springScope.beforeCompletion();
        springScope.close();
        transaction.rollback();
        status = TransactionSynchronization.STATUS_ROLLED_BACK;
      } else if (committing && status == TransactionSynchronization.STATUS_UNKNOWN && e instanceof RollbackException) {
        // The JPA provider rolls the transaction back before throwing
        status = TransactionSynchronization.STATUS_ROLLED_BACK;
      }
      throw e;
    } finally {
      springScope.end(status);
    }
  }

  boolean isAtomic(ProceedingJoinPoint point) {
    if (point.getSignature() instanceof MethodSignature methodSignature) {
      ContainerTransactional containerTransactional = methodSignature.getMethod().getAnnotation(ContainerTransactional.class);
      return containerTransactional != null && containerTransactional.atomic();
    } else {
      return false;
    }
  }

}
