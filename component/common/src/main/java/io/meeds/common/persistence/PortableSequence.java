/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package io.meeds.common.persistence;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.annotations.IdGeneratorType;

@IdGeneratorType(PortableSequenceGenerator.class)
@Target({ FIELD, METHOD })
@Retention(RUNTIME)
public @interface PortableSequence {

  String name();

  int startWith() default 1;

  /**
   * The number of ids Hibernate takes from one {@code nextval}, on the engines
   * that use a sequence (MySQL and MariaDB use an identity column and ignore
   * it). Above 1, ids are issued in blocks (see
   * {@link PortableSequenceGenerator#configure}). The add-on that owns the
   * sequence sets the same increment on the database sequence in a new
   * changeset: the first boot after the upgrade still works at step 1, and
   * blocks start from the next boot.
   *
   * @return the sequence increment, 1 by default
   */
  int incrementBy() default 1;
}