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
package io.meeds.spring;

import org.springframework.stereotype.Service;

/**
 * A class that group available Spring integrations made for Meeds Portal and
 * Meeds Kernel applications. Those integrations aren't imported automatically
 * to let each addon choose which Beans and integrations to adopt which is
 * convinient to not force initialise not needed Beans in certain contexts
 */
public class AvailableIntegration {

  private AvailableIntegration() {
  }

  /**
   * Used to bring Kernel components into Spring Context and vice versa add
   * Spring {@link Service} layer Beans into Kernel and other Spring contexts to
   * share the service layer
   */
  public static final String   KERNEL_MODULE          = "io.meeds.spring.kernel";

  /**
   * see {@link AvailableIntegration#KERNEL_MODULE} Use in Test Scope only
   */
  public static final String   KERNEL_TEST_MODULE     = "io.meeds.spring.kernel.test";

  /**
   * Used when JPA entities are needed to be managed by Spring Data JPA and
   * allows to reuse JPA PersistenceUnit defined globally in Kernel.
   */
  public static final String   JPA_MODULE             = "io.meeds.spring.jpa";

  /**
   * Has to be used when Liquibase configuration and annotation are needed in an
   * addon, otherwise, the parameter 'exclude =
   * LiquibaseAutoConfiguration.class' has to be added in SpringBootApplication
   * annotation to disable auto configuration of liquibase.
   */
  public static final String   LIQUIBASE_MODULE       = "io.meeds.spring.liquibase";

  /**
   * Used to inject Meeds Portal Authentication and Authorization contexts to
   * apply security on Spring REST/Controller endpoints. At the same time, this
   * will allow to inject ConversationState.setCurrent as made in regular Portal
   * REST endpoints.
   */
  public static final String   WEB_MODULE             = "io.meeds.spring.web";

  /**
   * Used to inject Meeds Portal Authentication and Authorization contexts to
   * apply security on Spring REST/Controller endpoints. At the same time, this
   * will allow to inject ConversationState.setCurrent as made in regular Portal
   * REST endpoints.
   * 
   * @deprecated use WEB_MODULE instead to integrate all web integrations
   */
  @Deprecated(forRemoval = true)
  public static final String   WEB_SECURITY_MODULE    = "io.meeds.spring.web.security";    // NOSONAR

  /**
   * Used to start and end a transaction at each Spring REST/Controller call,
   * same as used to be in regular Portal REST endpoints.
   * 
   * @deprecated use WEB_MODULE instead
   */
  @Deprecated(forRemoval = true)
  public static final String   WEB_TRANSACTION_MODULE = "io.meeds.spring.web.transaction"; // NOSONAR

  /**
   * Shortcut to list all available Meeds Portal and Kernel integration modules
   * with Spring
   */
  public static final String[] ALL_MODULES            = {                                  // NOSONAR
    KERNEL_MODULE,
    JPA_MODULE,
    LIQUIBASE_MODULE,
    WEB_MODULE
  };

  /**
   * Shortcut to list all available Meeds Portal and Kernel integration modules
   * with Spring in Test scope
   */
  public static final String[] ALL_TEST_MODULES       = {                                  // NOSONAR
    KERNEL_TEST_MODULE,
    JPA_MODULE,
    LIQUIBASE_MODULE,
    WEB_MODULE
  };

}
