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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.exoplatform.commons.persistence.impl.EntityManagerService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.RollbackException;

@ExtendWith(MockitoExtension.class)
class ContainerTransactionalAspectTest {

  private static final Object          RESULT = new Object();

  @Mock
  private ProceedingJoinPoint          point;

  @Mock
  private EntityManagerService         entityManagerService;

  @Mock
  private EntityManagerFactory         entityManagerFactory;

  @Mock
  private EntityManager                entityManager;

  @Mock
  private EntityTransaction            transaction;

  private ContainerTransactionalAspect aspect = new ContainerTransactionalAspect();

  @BeforeEach
  void setUp() {
    lenient().when(entityManagerService.getEntityManager()).thenReturn(entityManager);
    lenient().when(entityManagerService.getEntityManagerFactory()).thenReturn(entityManagerFactory);
    lenient().when(entityManager.getTransaction()).thenReturn(transaction);
  }

  private boolean isSpringScopeClosed() {
    return !TransactionSynchronizationManager.hasResource(entityManagerFactory)
        && !TransactionSynchronizationManager.isSynchronizationActive()
        && !TransactionSynchronizationManager.isActualTransactionActive();
  }

  @AfterEach
  void tearDown() {
    TransactionSynchronizationManager.unbindResourceIfPossible(entityManagerFactory);
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
    TransactionSynchronizationManager.setActualTransactionActive(false);
  }

  @Test
  void testCommitWhenTheMethodReturns() throws Throwable {
    when(point.proceed()).thenReturn(RESULT);

    assertSame(RESULT, aspect.proceedAtomically(point, entityManagerService));

    InOrder order = inOrder(transaction, point);
    order.verify(transaction).begin();
    order.verify(point).proceed();
    order.verify(transaction).commit();
    verify(transaction, never()).rollback();
    assertTrue(isSpringScopeClosed());
  }

  @Test
  void testRollbackWhenTheMethodThrows() throws Throwable {
    IllegalStateException error = new IllegalStateException("write failed");
    when(point.proceed()).thenAnswer(invocation -> {
      when(transaction.isActive()).thenReturn(true);
      throw error;
    });

    assertSame(error, assertThrows(IllegalStateException.class, () -> aspect.proceedAtomically(point, entityManagerService)));

    verify(transaction).begin();
    verify(transaction).rollback();
    verify(transaction, never()).commit();
    assertTrue(isSpringScopeClosed());
  }

  @Test
  void testCloseTheSpringScopeWhenTheTransactionCantBegin() throws Throwable {
    IllegalStateException error = new IllegalStateException("begin failed");
    doThrow(error).when(transaction).begin();

    assertSame(error, assertThrows(IllegalStateException.class, () -> aspect.proceedAtomically(point, entityManagerService)));

    verify(point, never()).proceed();
    verify(transaction, never()).commit();
    verify(transaction, never()).rollback();
    assertTrue(isSpringScopeClosed());
  }

  @Test
  void testRollbackAndThrowWhenTheTransactionIsMarkedRollbackOnly() throws Throwable {
    when(point.proceed()).thenAnswer(invocation -> {
      when(transaction.isActive()).thenReturn(true);
      when(transaction.getRollbackOnly()).thenReturn(true);
      return RESULT;
    });
    boolean[] closedBeforeRollback = new boolean[1];
    doAnswer(invocation -> {
      closedBeforeRollback[0] = isSpringScopeClosed();
      return when(transaction.isActive()).thenReturn(false);
    }).when(transaction).rollback();

    assertThrows(RollbackException.class, () -> aspect.proceedAtomically(point, entityManagerService));

    verify(transaction).rollback();
    verify(transaction, never()).commit();
    assertTrue(closedBeforeRollback[0]);
  }

  @Test
  void testJoinTheActiveTransaction() throws Throwable {
    when(transaction.isActive()).thenReturn(true);
    when(point.proceed()).thenReturn(RESULT);

    assertSame(RESULT, aspect.proceedAtomically(point, entityManagerService));

    verify(transaction, never()).begin();
    verify(transaction, never()).commit();
    verify(transaction, never()).rollback();
    verify(transaction, never()).setRollbackOnly();
  }

  @Test
  void testMarkTheJoinedTransactionRollbackOnlyWhenTheMethodThrows() throws Throwable {
    IllegalStateException error = new IllegalStateException("write failed");
    when(transaction.isActive()).thenReturn(true);
    when(point.proceed()).thenThrow(error);

    assertSame(error, assertThrows(IllegalStateException.class, () -> aspect.proceedAtomically(point, entityManagerService)));

    verify(transaction).setRollbackOnly();
    verify(transaction, never()).begin();
    verify(transaction, never()).commit();
    verify(transaction, never()).rollback();
  }

  @Test
  void testHoldTheEntityManagerForSpringRepositoriesDuringTheMethod() throws Throwable {
    when(point.proceed()).thenAnswer(invocation -> {
      EntityManagerHolder holder = (EntityManagerHolder) TransactionSynchronizationManager.getResource(entityManagerFactory);
      assertSame(entityManager, holder.getEntityManager());
      assertTrue(holder.isSynchronizedWithTransaction());
      throw new IllegalStateException("write failed");
    });

    assertThrows(IllegalStateException.class, () -> aspect.proceedAtomically(point, entityManagerService));

    assertFalse(TransactionSynchronizationManager.hasResource(entityManagerFactory));
  }

  @Test
  void testKeepTheEntityManagerOfASpringTransaction() throws Throwable {
    EntityManagerHolder springHolder = new EntityManagerHolder(mock(EntityManager.class));
    TransactionSynchronizationManager.bindResource(entityManagerFactory, springHolder);
    when(point.proceed()).thenAnswer(invocation -> {
      assertSame(springHolder, TransactionSynchronizationManager.getResource(entityManagerFactory));
      return RESULT;
    });

    assertSame(RESULT, aspect.proceedAtomically(point, entityManagerService));

    assertSame(springHolder, TransactionSynchronizationManager.getResource(entityManagerFactory));
  }

  @Test
  void testRunTheSpringCallbacksOnceTheTransactionIsCommitted() throws Throwable {
    TransactionSynchronization synchronization = mock(TransactionSynchronization.class);
    when(point.proceed()).thenAnswer(invocation -> {
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      TransactionSynchronizationManager.registerSynchronization(synchronization);
      return RESULT;
    });

    assertSame(RESULT, aspect.proceedAtomically(point, entityManagerService));

    InOrder order = inOrder(synchronization, transaction);
    order.verify(synchronization).beforeCommit(false);
    order.verify(synchronization).beforeCompletion();
    order.verify(transaction).commit();
    order.verify(synchronization).afterCommit();
    order.verify(synchronization).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
    assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
  }

  @Test
  void testRunTheSpringCallbacksOnceTheTransactionIsRolledBack() throws Throwable {
    TransactionSynchronization synchronization = mock(TransactionSynchronization.class);
    IllegalStateException error = new IllegalStateException("write failed");
    when(point.proceed()).thenAnswer(invocation -> {
      when(transaction.isActive()).thenReturn(true);
      TransactionSynchronizationManager.registerSynchronization(synchronization);
      throw error;
    });

    assertSame(error, assertThrows(IllegalStateException.class, () -> aspect.proceedAtomically(point, entityManagerService)));

    InOrder order = inOrder(synchronization, transaction);
    order.verify(synchronization).beforeCompletion();
    order.verify(transaction).rollback();
    order.verify(synchronization).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
    verify(synchronization, never()).afterCommit();
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
  }

  @Test
  void testReleaseTheEntityManagerBeforeTheSpringCallbacks() throws Throwable {
    boolean[] releasedBeforeAfterCommit = new boolean[1];
    boolean[] scopeClosedBeforeAfterCommit = new boolean[1];
    boolean[] scopeClosedBeforeAfterCompletion = new boolean[1];
    when(point.proceed()).thenAnswer(invocation -> {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          releasedBeforeAfterCommit[0] = !TransactionSynchronizationManager.hasResource(entityManagerFactory);
          scopeClosedBeforeAfterCommit[0] = !TransactionSynchronizationManager.isSynchronizationActive()
              && !TransactionSynchronizationManager.isActualTransactionActive();
        }

        @Override
        public void afterCompletion(int status) {
          scopeClosedBeforeAfterCompletion[0] = !TransactionSynchronizationManager.isSynchronizationActive();
        }
      });
      return RESULT;
    });

    aspect.proceedAtomically(point, entityManagerService);

    assertTrue(releasedBeforeAfterCommit[0]);
    assertTrue(scopeClosedBeforeAfterCommit[0], "A Spring transactional call of an after-commit callback has callbacks of its own");
    assertTrue(scopeClosedBeforeAfterCompletion[0]);
  }

  @Test
  void testReleaseTheEntityManagerBeforeTheSpringCallbacksOfARollback() throws Throwable {
    boolean[] releasedBeforeAfterCompletion = new boolean[1];
    boolean[] scopeClosedBeforeAfterCompletion = new boolean[1];
    when(point.proceed()).thenAnswer(invocation -> {
      when(transaction.isActive()).thenReturn(true);
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCompletion(int status) {
          releasedBeforeAfterCompletion[0] = !TransactionSynchronizationManager.hasResource(entityManagerFactory);
          scopeClosedBeforeAfterCompletion[0] = !TransactionSynchronizationManager.isSynchronizationActive()
              && !TransactionSynchronizationManager.isActualTransactionActive();
        }
      });
      throw new IllegalStateException("write failed");
    });

    assertThrows(IllegalStateException.class, () -> aspect.proceedAtomically(point, entityManagerService));

    assertTrue(releasedBeforeAfterCompletion[0]);
    assertTrue(scopeClosedBeforeAfterCompletion[0], "A Spring transactional call of an after-completion callback has callbacks of its own");
  }

  @Test
  void testLeaveTheActiveSpringSynchronizationToItsTransaction() throws Throwable {
    TransactionSynchronization outerSynchronization = mock(TransactionSynchronization.class);
    TransactionSynchronizationManager.initSynchronization();
    TransactionSynchronizationManager.registerSynchronization(outerSynchronization);
    when(point.proceed()).thenAnswer(invocation -> {
      EntityManagerHolder holder = (EntityManagerHolder) TransactionSynchronizationManager.getResource(entityManagerFactory);
      assertSame(entityManager, holder.getEntityManager());
      return RESULT;
    });

    assertSame(RESULT, aspect.proceedAtomically(point, entityManagerService));

    verify(transaction).commit();
    assertFalse(TransactionSynchronizationManager.hasResource(entityManagerFactory));
    assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
    assertEquals(List.of(outerSynchronization), TransactionSynchronizationManager.getSynchronizations());
    verifyNoInteractions(outerSynchronization);
  }

  @Test
  void testCloseTheSpringScopeBeforeTheCommit() throws Throwable {
    boolean[] closedBeforeCommit = new boolean[1];
    when(point.proceed()).thenReturn(RESULT);
    doAnswer(invocation -> closedBeforeCommit[0] = isSpringScopeClosed()).when(transaction).commit();

    assertSame(RESULT, aspect.proceedAtomically(point, entityManagerService));

    assertTrue(closedBeforeCommit[0], "A Spring transactional call of a callback of the commit runs in a Spring transaction of its own");
  }

  @Test
  void testCloseTheSpringScopeBeforeTheRollback() throws Throwable {
    boolean[] closedBeforeRollback = new boolean[1];
    when(point.proceed()).thenAnswer(invocation -> {
      when(transaction.isActive()).thenReturn(true);
      throw new IllegalStateException("write failed");
    });
    doAnswer(invocation -> closedBeforeRollback[0] = isSpringScopeClosed()).when(transaction).rollback();

    assertThrows(IllegalStateException.class, () -> aspect.proceedAtomically(point, entityManagerService));

    assertTrue(closedBeforeRollback[0], "A Spring transactional call of a callback of the rollback runs in a Spring transaction of its own");
  }

  @Test
  void testReportARollbackWhenTheCommitFails() throws Throwable {
    TransactionSynchronization synchronization = mock(TransactionSynchronization.class);
    RollbackException error = new RollbackException("flush failed");
    when(point.proceed()).thenAnswer(invocation -> {
      TransactionSynchronizationManager.registerSynchronization(synchronization);
      return RESULT;
    });
    doThrow(error).when(transaction).commit();

    assertSame(error, assertThrows(RollbackException.class, () -> aspect.proceedAtomically(point, entityManagerService)));

    verify(synchronization).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
    verify(synchronization, never()).afterCommit();
  }

  @Test
  void testOpenNoSpringScopeInAJoinedTransaction() throws Throwable {
    when(transaction.isActive()).thenReturn(true);
    when(point.proceed()).thenAnswer(invocation -> {
      assertFalse(TransactionSynchronizationManager.hasResource(entityManagerFactory));
      assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
      return RESULT;
    });

    assertSame(RESULT, aspect.proceedAtomically(point, entityManagerService));
  }

  @Test
  void testProceedWithoutJpaPersistence() throws Throwable {
    when(point.proceed()).thenReturn(RESULT);

    assertSame(RESULT, aspect.proceedAtomically(point, null));
  }

  @Test
  void testRefuseToRunWithoutEntityManager() throws Throwable {
    when(entityManagerService.getEntityManager()).thenReturn(null);

    assertThrows(IllegalStateException.class, () -> aspect.proceedAtomically(point, entityManagerService));

    verify(point, never()).proceed();
  }

  @Test
  void testIsAtomic() throws Exception {
    assertTrue(aspect.isAtomic(pointOf("atomicMethod")));
    assertFalse(aspect.isAtomic(pointOf("containerMethod")));
    assertFalse(aspect.isAtomic(pointOf("plainMethod")));
    ProceedingJoinPoint constructorPoint = mock(ProceedingJoinPoint.class);
    when(constructorPoint.getSignature()).thenReturn(mock(Signature.class));
    assertFalse(aspect.isAtomic(constructorPoint));
  }

  @Test
  void testAtomicIsOptIn() throws Exception {
    assertEquals(Boolean.FALSE, ContainerTransactional.class.getMethod("atomic").getDefaultValue());
  }

  private ProceedingJoinPoint pointOf(String methodName) throws NoSuchMethodException {
    MethodSignature signature = mock(MethodSignature.class);
    when(signature.getMethod()).thenReturn(AnnotatedMethods.class.getDeclaredMethod(methodName));
    ProceedingJoinPoint methodPoint = mock(ProceedingJoinPoint.class);
    when(methodPoint.getSignature()).thenReturn(signature);
    return methodPoint;
  }

  @SuppressWarnings("unused")
  private static class AnnotatedMethods {

    @ContainerTransactional(atomic = true)
    void atomicMethod() {
      // Annotation holder
    }

    @ContainerTransactional
    void containerMethod() {
      // Annotation holder
    }

    void plainMethod() {
      // Annotation holder
    }
  }

}
