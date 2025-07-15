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
package io.meeds.spring.liquibase;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.exoplatform.commons.persistence.impl.LiquibaseDataInitializer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.integration.spring.SpringLiquibase;

@Configuration
@EnableConfigurationProperties(LiquibaseProperties.class)
public class LiquibaseIntegration {

  private static final Log           LOG      = ExoLogger.getLogger(LiquibaseIntegration.class);

  @Bean
  public SpringLiquibase liquibase(LiquibaseProperties liquibaseProperties) {
    LiquibaseDataInitializer liquibaseDataInitializer = PortalContainer.getInstance()
                                                                       .getComponentInstanceOfType(LiquibaseDataInitializer.class);
    SpringLiquibase liquibase = new SpringLiquibase();
    DataSource datasource = liquibaseDataInitializer.getDatasource();
    liquibase.setDataSource(datasource);
    liquibase.setContexts(liquibaseDataInitializer.getContexts());
    String schema = getSchema(datasource, liquibaseProperties);
    liquibase.setDefaultSchema(schema);
    liquibase.setLiquibaseSchema(schema);
    liquibase.setChangeLog(liquibaseProperties.getChangeLog());
    liquibase.setDropFirst(liquibaseProperties.isDropFirst());
    liquibase.setShouldRun(liquibaseProperties.isEnabled());
    return liquibase;
  }

  private String getSchema(DataSource datasource, LiquibaseProperties liquibaseProperties) {
    try (Database database = DatabaseFactory.getInstance()
                                            .findCorrectDatabaseImplementation(new JdbcConnection(datasource.getConnection()))) {
      return database.getDefaultSchemaName();
    } catch (DatabaseException | SQLException e) {
      LOG.warn("Error while retrieving default schema name of datasource, attept to use default schema from settings 'spring.liquibase.default-schema'",
               e);
      return liquibaseProperties.getDefaultSchema();
    }
  }

}
