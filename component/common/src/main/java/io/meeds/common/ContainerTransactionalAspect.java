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

import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.component.RequestLifeCycle;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
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

  /**
   * Spring transactional methods, Spring Data repositories included, write
   * through the EntityManager that Spring's transaction synchronization holds
   * for the factory. The scope holds the thread's EntityManager there, as a
   * transaction already active, so that they join the transaction instead of
   * committing their own. When no Spring transaction synchronization is
   * active on the thread, the scope opens one and collects the callbacks they
   * register (a TransactionalEventListener event) so that they run with the
   * outcome of the transaction. The before-commit and before-completion
   * callbacks run inside the transaction. The EntityManager is then released
   * and the synchronization closed, before the transaction is committed or
   * rolled back: a Spring transactional method called by a callback of the
   * commit or of the rollback (a TransactionCallbacks callback), or by an
   * after-commit or after-completion callback, runs in a Spring transaction of
   * its own, with callbacks of its own, and one called while the commit
   * flushes the writes (an entity listener) doesn't join the transaction. When a
   * synchronization is already active, as in a Spring transaction without an
   * EntityManager of its own, the scope collects none of their callbacks: they
   * are left to that synchronization, which runs them with its own outcome and,
   * without an actual transaction, skips a TransactionalEventListener event
   * unless its fallback execution runs it at once.
   */
  private static final class SpringTransactionScope {

    private static final SpringTransactionScope NONE = new SpringTransactionScope(null, false);

    private final EntityManagerFactory          boundFactory;

    private final boolean                       synchronizing;

    private boolean                             completing;

    private List<TransactionSynchronization>    synchronizations;

    private SpringTransactionScope(EntityManagerFactory boundFactory, boolean synchronizing) {
      this.boundFactory = boundFactory;
      this.synchronizing = synchronizing;
    }

    static SpringTransactionScope open(EntityManagerFactory entityManagerFactory, EntityManager entityManager) {
      if (entityManagerFactory == null || TransactionSynchronizationManager.hasResource(entityManagerFactory)) {
        // A Spring transaction of its own holds an EntityManager for the
        // factory on the thread: the Spring writes stay in it
        return NONE;
      }
      TransactionSynchronizationManager.bindResource(entityManagerFactory, new ThreadEntityManagerHolder(entityManager));
      boolean synchronizing = !TransactionSynchronizationManager.isSynchronizationActive();
      if (synchronizing) {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
      }
      return new SpringTransactionScope(entityManagerFactory, synchronizing);
    }

    void beforeCommit() {
      if (synchronizing) {
        TransactionSynchronizationUtils.triggerBeforeCommit(false);
        beforeCompletion();
      }
    }

    void beforeCompletion() {
      if (synchronizing && !completing) {
        completing = true;
        TransactionSynchronizationUtils.triggerBeforeCompletion();
      }
    }

    void close() {
      release();
      if (synchronizing && synchronizations == null) {
        synchronizations = TransactionSynchronizationManager.getSynchronizations();
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
      }
    }

    void afterCommit() {
      close();
      if (synchronizing) {
        TransactionSynchronizationUtils.invokeAfterCommit(synchronizations);
      }
    }

    void end(int status) {
      close();
      if (synchronizing) {
        TransactionSynchronizationUtils.invokeAfterCompletion(synchronizations, status);
      }
    }

    private void release() {
      if (boundFactory != null) {
        TransactionSynchronizationManager.unbindResourceIfPossible(boundFactory);
      }
    }

  }

  private static class ThreadEntityManagerHolder extends EntityManagerHolder {

    ThreadEntityManagerHolder(EntityManager entityManager) {
      super(entityManager);
      // The EntityManager is given to the repositories as is, without joining
      // it to a transaction nor closing it with one
      setSynchronizedWithTransaction(true);
    }

    @Override
    protected boolean isTransactionActive() {
      return getEntityManager().getTransaction().isActive();
    }

  }

}
