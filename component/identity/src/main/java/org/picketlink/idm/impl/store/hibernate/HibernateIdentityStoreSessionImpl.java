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
package org.picketlink.idm.impl.store.hibernate;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.SessionFactory;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.spi.store.IdentityStoreSession;

/**
 * Wrapper around HibernateEntityManager
 *
 */
public class HibernateIdentityStoreSessionImpl implements IdentityStoreSession {
  private static Logger              log               = Logger.getLogger(HibernateIdentityStoreImpl.class.getName());

  private final SessionFactory       sessionFactory;

  private final boolean              lazyStartOfHibernateTransaction;

  // Tracking status of Hibernate transaction. Value "true" means that Hibernate
  // transaction has been started for this client
  private final ThreadLocal<Boolean> hibernateTxStatus = new ThreadLocal<Boolean>();

  public HibernateIdentityStoreSessionImpl(SessionFactory sessionFactory, boolean lazyStartOfHibernateTransaction) {
    this.sessionFactory = sessionFactory;
    this.lazyStartOfHibernateTransaction = lazyStartOfHibernateTransaction;
  }

  public Object getSessionContext() {
    return sessionFactory.getCurrentSession();
  }

  public void close() throws IdentityException {
    sessionFactory.getCurrentSession().close();
  }

  public void save() throws IdentityException {
    // In non-lazy setup we always need flush
    Boolean flushNeeded = !lazyStartOfHibernateTransaction;

    // Now check if Hibernate transaction was started. We need flush in that
    // case
    if (!flushNeeded) {
      Boolean txStatus = hibernateTxStatus.get();
      flushNeeded = txStatus != null && txStatus;
    }

    if (flushNeeded) {
      sessionFactory.getCurrentSession().flush();
    }
  }

  public void clear() throws IdentityException {
    // In non-lazy setup we always need clear
    Boolean clearNeeded = !lazyStartOfHibernateTransaction;

    // Now check if Hibernate transaction was started. We need clear in that
    // case
    if (!clearNeeded) {
      Boolean txStatus = hibernateTxStatus.get();
      clearNeeded = txStatus != null && txStatus;
    }

    if (clearNeeded) {
      sessionFactory.getCurrentSession().clear();
    }
  }

  public boolean isOpen() {
    return sessionFactory.getCurrentSession().isOpen();
  }

  public boolean isTransactionSupported() {
    return true;
  }

  public void startTransaction() {
    // Init ThreadLocal but don't start real Hibernate transaction if option
    // lazyStartOfHibernateTransaction is enabled
    if (lazyStartOfHibernateTransaction) {
      if (hibernateTxStatus.get() == null) {
        hibernateTxStatus.set(Boolean.FALSE);
      }
    } else {
      startHibernateTransaction();
    }
  }

  public void commitTransaction() {
    if (lazyStartOfHibernateTransaction) {
      Boolean hbTxStatus = hibernateTxStatus.get();

      // Commit hibernate transaction only if it has really been started
      if (hbTxStatus != null && hbTxStatus) {
        commitHibernateTransaction();
        hibernateTxStatus.remove();
      }
    } else {
      commitHibernateTransaction();
    }
  }

  public void rollbackTransaction() {
    if (lazyStartOfHibernateTransaction) {
      Boolean hbTxStatus = hibernateTxStatus.get();
      // Rollback hibernate transaction only if it has really been started
      if (hbTxStatus != null && hbTxStatus) {
        rollbackHibernateTransaction();
        hibernateTxStatus.remove();
      }
    } else {
      rollbackHibernateTransaction();
    }
  }

  public boolean isTransactionActive() {
    if (lazyStartOfHibernateTransaction) {
      return hibernateTxStatus.get() != null;
    } else {
      return isHibernateTransactionActive();
    }
  }

  void startHibernateTransactionIfNotStartedYet() {
    Boolean txStatus = hibernateTxStatus.get();

    // Now we really need to start Hibernate transaction at this point if not
    // already started
    if (txStatus == null || !txStatus) {
      startHibernateTransaction();
      hibernateTxStatus.set(Boolean.TRUE);
    }
  }

  private void startHibernateTransaction() {
    if (log.isLoggable(Level.FINER)) {
      log.log(Level.FINER, "Going to start Hibernate transaction");
    }

    sessionFactory.getCurrentSession().getTransaction().begin();
  }

  private void commitHibernateTransaction() {
    if (log.isLoggable(Level.FINER)) {
      log.log(Level.FINER, "Going to commit Hibernate transaction");
    }

    sessionFactory.getCurrentSession().getTransaction().commit();
  }

  private void rollbackHibernateTransaction() {
    if (log.isLoggable(Level.FINER)) {
      log.log(Level.FINER, "Going to rollback Hibernate transaction");
    }

    sessionFactory.getCurrentSession().getTransaction().rollback();
  }

  private boolean isHibernateTransactionActive() {
    return sessionFactory.getCurrentSession().getTransaction().isActive();
  }

  Boolean getHibernateTxStatus() {
    return hibernateTxStatus.get();
  }
}
