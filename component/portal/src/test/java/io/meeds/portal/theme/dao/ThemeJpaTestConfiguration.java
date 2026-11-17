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

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import io.meeds.portal.theme.entity.ThemeEntity;

/**
 * The minimal Spring slice of the theme tables' real-SQL tests: their two
 * entities and repositories, over Boot's in-memory HSQLDB whose schema is built
 * by the theme changelog (never by Hibernate), so the tests also prove the
 * changesets and the mapping agree, the {@code @PortableSequence} name and the
 * seed's id included.
 */
@Configuration
@EntityScan(basePackageClasses = ThemeEntity.class)
@EnableJpaRepositories(basePackageClasses = ThemeDAO.class)
public class ThemeJpaTestConfiguration {

  /** The schema comes from the theme changelog. */
  public static final String LIQUIBASE_CHANGELOG = "spring.liquibase.change-log=classpath:db/changelog/portal-theme.db.changelog-1.0.0.xml";

  /** And never from Hibernate. */
  public static final String NO_DDL              = "spring.jpa.hibernate.ddl-auto=none";

}
