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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.commons.persistence.impl.EntityManagerService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

@ExtendWith(MockitoExtension.class)
class TransactionCallbacksTest {

  @Mock
  private EntityManagerService entityManagerService;

  @Mock
  private EntityManager        entityManager;

  @Mock
  private Transaction          transaction;

  private AtomicInteger        callbackRuns = new AtomicInteger();

  private Runnable             callback     = callbackRuns::incrementAndGet;

  @BeforeEach
  void setUp() {
    lenient().when(entityManagerService.getEntityManager()).thenReturn(entityManager);
    lenient().when(entityManager.getTransaction()).thenReturn(transaction);
  }

  @Test
  void testRunTheCallbackOnceTheActiveTransactionCompletes() {
    when(transaction.isActive()).thenReturn(true);

    assertTrue(TransactionCallbacks.afterCompletion(entityManagerService, callback));
    assertEquals(0, callbackRuns.get(), "The callback runs once the transaction completes");

    ArgumentCaptor<Synchronization> synchronization = ArgumentCaptor.forClass(Synchronization.class);
    verify(transaction).registerSynchronization(synchronization.capture());
    synchronization.getValue().beforeCompletion();
    assertEquals(0, callbackRuns.get());
    synchronization.getValue().afterCompletion(Status.STATUS_COMMITTED);
    assertEquals(1, callbackRuns.get());
    synchronization.getValue().afterCompletion(Status.STATUS_ROLLEDBACK);
    assertEquals(2, callbackRuns.get());
  }

  @Test
  void testLogTheCallbackErrors() {
    when(transaction.isActive()).thenReturn(true);

    assertTrue(TransactionCallbacks.afterCompletion(entityManagerService, () -> {
      throw new IllegalStateException("eviction failed");
    }));

    ArgumentCaptor<Synchronization> synchronization = ArgumentCaptor.forClass(Synchronization.class);
    verify(transaction).registerSynchronization(synchronization.capture());
    synchronization.getValue().afterCompletion(Status.STATUS_COMMITTED);
  }

  @Test
  void testNoCallbackWithoutActiveTransaction() {
    when(transaction.isActive()).thenReturn(false);

    assertFalse(TransactionCallbacks.afterCompletion(entityManagerService, callback));

    verify(transaction, never()).registerSynchronization(any());
    assertEquals(0, callbackRuns.get());
  }

  @Test
  void testNoCallbackWithoutEntityManager() {
    when(entityManagerService.getEntityManager()).thenReturn(null);

    assertFalse(TransactionCallbacks.afterCompletion(entityManagerService, callback));
    assertFalse(TransactionCallbacks.afterCompletion(null, callback));
    assertEquals(0, callbackRuns.get());
  }

  @Test
  void testNoCallbackWithAnotherTransactionImplementation() {
    EntityTransaction jpaTransaction = mock(EntityTransaction.class);
    lenient().when(jpaTransaction.isActive()).thenReturn(true);
    when(entityManager.getTransaction()).thenReturn(jpaTransaction);

    assertFalse(TransactionCallbacks.afterCompletion(entityManagerService, callback));
    assertEquals(0, callbackRuns.get());
  }

}
