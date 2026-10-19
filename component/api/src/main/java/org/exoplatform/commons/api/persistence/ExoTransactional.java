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
package org.exoplatform.commons.api.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describe a method as transactional. The only propagation implemented yet is
 * REQUIRED. Support a current transaction, create a new one if none exists.
 * Analogous to EJB or Spring transaction attribute of the same name.
 *
 * When a call that failed marked the transaction that the method began
 * rollback-only, the transaction is rolled back: a method that returned then
 * throws a jakarta.persistence.RollbackException, and a method that threw
 * keeps its exception, unless ending the transaction fails. A method that
 * joined an active transaction never throws it: the method that began the
 * transaction does.
 *
 * This annotation is handled in org.exoplatform.commons.persistence.impl.ExoTransactionalAspect.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExoTransactional {
}
