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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describe a method as transactional. This annotation will allow to:
 * - Set current container to PortalContainer
 * - Start the request lifecycle of the container: the IDM session transaction
 *   and the JPA EntityManager. The JPA writes made through kernel DAOs commit
 *   one DAO call at a time, unless {@link #atomic()} is set.
 *
 * This annotation is handled in io.meeds.common.ContainerTransactionalAspect.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ContainerTransactional {

  /**
   * @return true to run the method in one JPA transaction, committed when it
   *         returns and rolled back when it throws. The writes made through
   *         the kernel EntityManager (kernel DAOs, ExoTransactional methods)
   *         join it. When the method begins the transaction, the Spring
   *         transactional methods, Spring Data repositories included, join it
   *         too, unless a Spring transaction of its own is active on the
   *         thread, and the callbacks they register run once it is committed
   *         or rolled back, unless a Spring transaction synchronization is
   *         already active on the thread: their callbacks are then left to
   *         that synchronization. A Spring cache eviction (CacheEvict) made
   *         inside the method takes effect at once, before the commit. When a
   *         call that failed marked the transaction rollback-only, a method
   *         that returned throws a RollbackException after the rollback, and
   *         a method that threw keeps its exception, unless the rollback
   *         fails. When a JPA transaction is already
   *         active on the thread, the method joins it and marks it
   *         rollback-only when it throws.
   */
  boolean atomic() default false;

}
