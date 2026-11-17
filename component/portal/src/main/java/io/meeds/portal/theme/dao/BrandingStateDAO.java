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
package io.meeds.portal.theme.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import io.meeds.portal.theme.entity.BrandingStateEntity;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

/**
 * The branding state row. Every writer reads it first with
 * {@link #findForUpdate(long)}, inside its transaction: the row lock
 * serialises the writers, so the version each compares and increments covers
 * everything committed before it.
 */
public interface BrandingStateDAO extends JpaRepository<BrandingStateEntity, Long> {

  /** The longest wait for the state row, in milliseconds. */
  String LOCK_TIMEOUT_MILLIS = "10000";

  /**
   * Waits at most {@link #LOCK_TIMEOUT_MILLIS} for the row: its holder may be
   * another node's configuration synchroniser, which trial-compiles every
   * configured theme under the lock, and PostgreSQL waits forever by default
   * (lock_timeout 0). Hibernate applies it as the connection's lock_timeout on
   * PostgreSQL; InnoDB keeps its own innodb_lock_wait_timeout (50 s by
   * default). A timed-out writer fails, and its caller writes nothing.
   *
   * @param id the state row id
   * @return the row, locked until the transaction ends
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = LOCK_TIMEOUT_MILLIS))
  @Query("SELECT s FROM PortalBrandingState s WHERE s.id = :id")
  BrandingStateEntity findForUpdate(@Param("id") long id);

}
