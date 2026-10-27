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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.jpa.CommonsDAOJPAImplTest;
import org.exoplatform.settings.jpa.dao.SettingContextDAO;
import org.exoplatform.settings.jpa.entity.ContextEntity;

import io.meeds.spring.module.dao.TestJpaDao;
import io.meeds.spring.module.entity.TestJpaEntity;

import jakarta.persistence.EntityManager;

@SpringJUnitConfig(CommonsDAOJPAImplTest.class)
public class ContainerTransactionalRepositoryTest extends CommonsDAOJPAImplTest { // NOSONAR

  @Autowired
  private TestJpaDao testJpaDao;

  @Test
  public void repositoryWritesRollBackWithTheAtomicMethod() throws Throwable {
    long entitiesCount = testJpaDao.count();
    IllegalStateException error = new IllegalStateException("Failure after the repository write");
    ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
    when(point.proceed()).thenAnswer(invocation -> {
      testJpaDao.save(new TestJpaEntity(null, "rolled back"));
      throw error;
    });

    assertSame(error, assertThrows(IllegalStateException.class, () -> proceedAtomically(point)));

    assertEquals(entitiesCount, testJpaDao.count());
  }

  @Test
  public void repositoryWritesCommitWithTheAtomicMethod() throws Throwable {
    long entitiesCount = testJpaDao.count();
    ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
    when(point.proceed()).thenAnswer(invocation -> testJpaDao.save(new TestJpaEntity(null, "committed")));

    proceedAtomically(point);

    assertEquals(entitiesCount + 1, testJpaDao.count());
  }

  @Test
  public void kernelWritesAfterARepositoryWriteCommitWithTheAtomicMethod() throws Throwable {
    SettingContextDAO settingContextDAO = getService(SettingContextDAO.class);
    long entitiesCount = testJpaDao.count();
    long contextsCount = countContexts(settingContextDAO);
    ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
    when(point.proceed()).thenAnswer(invocation -> {
      EntityManager entityManager = getService(EntityManagerService.class).getEntityManager();
      TestJpaEntity entity = testJpaDao.save(new TestJpaEntity(null, "committed with a kernel write"));
      assertTrue(entityManager.isOpen());
      assertTrue(entityManager.getTransaction().isActive());
      ContextEntity context = settingContextDAO.create(new ContextEntity().setType("atomic").setName("committed"));
      return List.of(entity, context);
    });

    List<?> written = (List<?>) proceedAtomically(point);

    assertEquals(entitiesCount + 1, testJpaDao.count());
    assertEquals(contextsCount + 1, countContexts(settingContextDAO));
    testJpaDao.delete((TestJpaEntity) written.get(0));
    RequestLifeCycle.begin(getContainer());
    try {
      settingContextDAO.delete((ContextEntity) written.get(1));
    } finally {
      RequestLifeCycle.end();
    }
  }

  @Test
  public void kernelWritesAfterARepositoryWriteRollBackWithTheAtomicMethod() throws Throwable {
    SettingContextDAO settingContextDAO = getService(SettingContextDAO.class);
    long entitiesCount = testJpaDao.count();
    long contextsCount = countContexts(settingContextDAO);
    IllegalStateException error = new IllegalStateException("Failure after the kernel write");
    ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
    when(point.proceed()).thenAnswer(invocation -> {
      testJpaDao.save(new TestJpaEntity(null, "rolled back with a kernel write"));
      settingContextDAO.create(new ContextEntity().setType("atomic").setName("rolled back"));
      throw error;
    });

    assertSame(error, assertThrows(IllegalStateException.class, () -> proceedAtomically(point)));

    assertEquals(entitiesCount, testJpaDao.count());
    assertEquals(contextsCount, countContexts(settingContextDAO));
  }

  @Test
  public void repositoryWritesOfATransactionCallbackCommitInATransactionOfTheirOwn() throws Throwable {
    long entitiesCount = testJpaDao.count();
    ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
    when(point.proceed()).thenAnswer(invocation -> TransactionCallbacks.afterCompletion(getService(EntityManagerService.class),
                                                                                         () -> testJpaDao.save(new TestJpaEntity(null,
                                                                                                                                 "saved by a callback"))));

    assertEquals(Boolean.TRUE, proceedAtomically(point));

    assertEquals(entitiesCount + 1, testJpaDao.count());
  }

  private long countContexts(SettingContextDAO settingContextDAO) {
    RequestLifeCycle.begin(getContainer());
    try {
      return settingContextDAO.count();
    } finally {
      RequestLifeCycle.end();
    }
  }

  private Object proceedAtomically(ProceedingJoinPoint point) throws Throwable {
    RequestLifeCycle.begin(getContainer());
    try {
      return new ContainerTransactionalAspect().proceedAtomically(point, getService(EntityManagerService.class));
    } finally {
      RequestLifeCycle.end();
    }
  }

}
