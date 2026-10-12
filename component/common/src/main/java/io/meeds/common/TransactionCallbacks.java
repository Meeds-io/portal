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

import org.hibernate.Transaction;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Synchronization;

/**
 * Callbacks run once the JPA transaction of the current thread completes
 */
public class TransactionCallbacks {

  private static final Log LOG = ExoLogger.getLogger(TransactionCallbacks.class);

  private TransactionCallbacks() {
    // Utility class
  }

  /**
   * Registers a callback run once the JPA transaction active on the current
   * thread completes, whether it's committed or rolled back. A cache eviction
   * made inside a transaction is repeated this way: a concurrent read made
   * before the commit caches the previous state again.
   *
   * @param callback the callback to run, its errors are logged
   * @return true when the callback is registered, false when no JPA
   *         transaction is active on the thread: the callback isn't run then
   */
  public static boolean afterCompletion(Runnable callback) {
    return afterCompletion(ExoContainerContext.getService(EntityManagerService.class), callback);
  }

  static boolean afterCompletion(EntityManagerService entityManagerService, Runnable callback) {
    EntityManager entityManager = entityManagerService == null ? null : entityManagerService.getEntityManager();
    if (entityManager == null
        || !(entityManager.getTransaction() instanceof Transaction transaction)
        || !transaction.isActive()) {
      return false;
    }
    transaction.registerSynchronization(new Synchronization() {
      @Override
      public void beforeCompletion() {
        // Nothing to run before the transaction completes
      }

      @Override
      public void afterCompletion(int status) {
        try {
          callback.run();
        } catch (RuntimeException e) {
          LOG.warn("Error while running a callback after the completion of a transaction", e);
        }
      }
    });
    return true;
  }

}
