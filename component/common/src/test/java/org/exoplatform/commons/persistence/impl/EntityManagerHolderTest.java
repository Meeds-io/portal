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

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.Test;

import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.jpa.CommonsDAOJPAImplTest;
import org.exoplatform.settings.jpa.entity.ContextEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;

/**
 * A kernel DAO reaches its EntityManager through the {@link EntityManagerHolder}
 * proxy, so an exception the EntityManager throws must reach the DAO's caller
 * as itself, for a catch of that exception around a DAO call to fire. Each
 * failure below is the engine's own, and arrives as an
 * UndeclaredThrowableException when the proxy lets the reflection exception
 * through.
 */
public class EntityManagerHolderTest extends CommonsDAOJPAImplTest {

  @Test
  public void testFindWithANullIdThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> settingContextDAO.find(null));
  }

  @Test
  public void testPersistOfADetachedEntityThrowsPersistenceException() {
    ContextEntity context = settingContextDAO.create(newContext());
    restartRequest();

    assertThrows(PersistenceException.class, () -> settingContextDAO.create(context));
    restartRequest();
  }

  @Test
  public void testRefreshOfARemovedRowThrowsEntityNotFoundException() {
    ContextEntity context = settingContextDAO.create(newContext());
    EntityManager entityManager = EntityManagerHolder.get();
    entityManager.getTransaction().begin();
    entityManager.createQuery("DELETE FROM SettingsContextEntity c WHERE c.id = :id")
                 .setParameter("id", context.getId())
                 .executeUpdate();
    entityManager.getTransaction().commit();

    assertThrows(EntityNotFoundException.class, () -> entityManager.refresh(context));
    restartRequest();
  }

  @Test
  public void testFailedCallOutsideARequestClosesTheEntityManagerItOpened() {
    end();
    try {
      assertThrows(IllegalArgumentException.class, () -> settingContextDAO.find(null));
      assertNull(getService(EntityManagerService.class).getEntityManager());
    } finally {
      begin();
    }
  }

  private ContextEntity newContext() {
    return new ContextEntity().setType(Context.USER.getName()).setName("EntityManagerHolderTest");
  }

  private void restartRequest() {
    end();
    begin();
  }

}
