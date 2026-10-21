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
package org.exoplatform.commons.persistence.impl;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.RollbackException;

@ExtendWith(MockitoExtension.class)
class ExoTransactionalAspectTest {

  private static final Object    RESULT = new Object();

  @Mock
  private ProceedingJoinPoint    point;

  @Mock
  private EntityManagerService   entityManagerService;

  @Mock
  private EntityManager          entityManager;

  @Mock
  private EntityTransaction      transaction;

  private ExoTransactionalAspect aspect = new ExoTransactionalAspect();

  @BeforeEach
  void setUp() {
    lenient().when(entityManagerService.getEntityManager()).thenReturn(entityManager);
    lenient().when(entityManager.getTransaction()).thenReturn(transaction);
  }

  @Test
  void testCommitWhenTheMethodReturns() throws Throwable {
    when(transaction.isActive()).thenReturn(false, true);
    when(point.proceed()).thenReturn(RESULT);

    assertSame(RESULT, aspect.proceed(point, entityManagerService));

    verify(transaction).begin();
    verify(transaction).commit();
    verify(transaction, never()).rollback();
  }

  @Test
  void testCommitWhenTheMethodThrows() throws Throwable {
    IllegalStateException error = new IllegalStateException("failure");
    when(transaction.isActive()).thenReturn(false, true);
    when(point.proceed()).thenThrow(error);

    assertSame(error, assertThrows(IllegalStateException.class, () -> aspect.proceed(point, entityManagerService)));

    verify(transaction).commit();
    verify(transaction, never()).rollback();
  }

  @Test
  void testRollbackAndThrowWhenTheMethodReturnsAndTheTransactionIsMarkedRollbackOnly() throws Throwable {
    when(transaction.isActive()).thenReturn(false, true);
    when(transaction.getRollbackOnly()).thenReturn(true);
    when(point.proceed()).thenReturn(RESULT);

    assertThrows(RollbackException.class, () -> aspect.proceed(point, entityManagerService));

    verify(transaction).rollback();
    verify(transaction, never()).commit();
  }

  @Test
  void testRollbackAndKeepTheErrorWhenTheMethodThrowsAndTheTransactionIsMarkedRollbackOnly() throws Throwable {
    IllegalStateException error = new IllegalStateException("failure");
    when(transaction.isActive()).thenReturn(false, true);
    when(transaction.getRollbackOnly()).thenReturn(true);
    when(point.proceed()).thenThrow(error);

    assertSame(error, assertThrows(IllegalStateException.class, () -> aspect.proceed(point, entityManagerService)));

    verify(transaction).rollback();
    verify(transaction, never()).commit();
  }

  @Test
  void testLeaveAJoinedTransactionToTheMethodThatBeganIt() throws Throwable {
    when(transaction.isActive()).thenReturn(true);
    lenient().when(transaction.getRollbackOnly()).thenReturn(true);
    when(point.proceed()).thenReturn(RESULT);

    assertSame(RESULT, aspect.proceed(point, entityManagerService));

    verify(transaction, never()).begin();
    verify(transaction, never()).commit();
    verify(transaction, never()).rollback();
  }

  @Test
  void testCloseTheEntityManagerItStartedWhenTheTransactionIsRolledBack() throws Throwable {
    when(entityManagerService.getEntityManager()).thenReturn(null);
    when(entityManagerService.createEntityManager()).thenReturn(entityManager);
    when(transaction.isActive()).thenReturn(false, true);
    when(transaction.getRollbackOnly()).thenReturn(true);
    when(point.proceed()).thenReturn(RESULT);

    assertThrows(RollbackException.class, () -> aspect.proceed(point, entityManagerService));

    verify(entityManagerService).closeEntityManager();
  }

}
