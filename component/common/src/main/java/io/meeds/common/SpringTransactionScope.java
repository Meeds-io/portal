/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

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
final class SpringTransactionScope {

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
