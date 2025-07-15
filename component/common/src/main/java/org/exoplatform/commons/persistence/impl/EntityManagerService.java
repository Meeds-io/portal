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
package org.exoplatform.commons.persistence.impl;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JdbcSettings;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * This service is responsible to create a single EntityManagerFactory, with the
 * persistence unit name <code>meeds-jpa</code>.
 * <p>
 * The service is also bound to use of the RequestLifecycle that there is only
 * one EntityManager will be created at beginning of the request lifecycle. The
 * EntityManager instance will be maintained open and shared with all
 * applications relying on this through to the end of lifecycle.
 *
 */
public class EntityManagerService implements ComponentRequestLifecycle {
  public static final String          PERSISTENCE_UNIT_NAME       = "meeds-jpa";
  private static final Log            LOGGER                      = ExoLogger.getLogger(EntityManagerService.class);
  private static final String         EXO_JPA_DATASOURCE_NAME     = "exo.jpa.datasource.name";
  private static final String         EXO_PREFIX_FOR_HIB_SETTINGS = "exo.jpa.";

  @Getter
  protected EntityManagerFactory        entityManagerFactory;

  protected Properties properties;

  private ThreadLocal<EntityManager>  instance                    = new ThreadLocal<>();

  public EntityManagerService() {
    this(null, null);
  }

  public EntityManagerService(EntityManagerFactory managerFactory, Properties props) {
    if (managerFactory != null && props != null) {
      this.entityManagerFactory = managerFactory;
      this.properties = props;
      return;
    }
    properties = new Properties();

    // Setting datasource JNDI name. Get it directly from eXo global properties so it is not overridable by addons.
    String datasourceName = PropertyManager.getProperty(EXO_JPA_DATASOURCE_NAME);
    if (StringUtils.isNotBlank(datasourceName)) {
      properties.put(JdbcSettings.JAKARTA_NON_JTA_DATASOURCE, datasourceName);
      LOGGER.info("EntityManagerFactory [{}] - Creating with datasource {}.", getPersistenceUnitName(), datasourceName);
    } else {
      LOGGER.info("EntityManagerFactory [{}] - Creating with default datasource.", getPersistenceUnitName());
    }

    // Get Hibernate properties in eXo global properties
    for (String propertyName : getHibernateAvailableSettings()) {
      String propertyValue = PropertyManager.getProperty(EXO_PREFIX_FOR_HIB_SETTINGS + propertyName);
      if (StringUtils.isNotBlank(propertyValue)) {
        properties.put(propertyName, propertyValue);
        LOGGER.info("EntityManagerFactory [{}] - Setting [{}] to [{}]", getPersistenceUnitName(), propertyName, propertyValue);
      }
    }

    entityManagerFactory = Persistence.createEntityManagerFactory(getPersistenceUnitName(), properties);
    LOGGER.info("EntityManagerFactory [{}] - Created.", getPersistenceUnitName());
  }

  public String getDatasourceName() {
    return (String) properties.get(JdbcSettings.JAKARTA_NON_JTA_DATASOURCE);
  }

  public void setDatasourceName(String datasourceName) {
    properties.put(JdbcSettings.JAKARTA_NON_JTA_DATASOURCE, datasourceName);
  }

  private List<String> getHibernateAvailableSettings() {
    List<String> result = new ArrayList<>();
    for (Field field : AvailableSettings.class.getDeclaredFields()) {
      try {
        result.add((String) field.get(null));
      } catch (IllegalAccessException e) {
        // Noting to do: we log and continue
        LOGGER.error("Error while getting Hibernate available settings.", e);
      }
    }
    return result;
  }

  /**
   * @return the EntityManager instance available in current request lifecycle.
   *         Otherwise, it returns NULL.
   */
  public EntityManager getEntityManager() {
    if (instance.get() == null) {
      return null;
    } else {
      return instance.get();
    }
  }

  /**
   * Return a completely new instance of EntityManager. The EntityManager
   * instance is put in the threadLocal for further use.
   *
   * @return return a completely new instance of EntityManager.
   */
  EntityManager createEntityManager() {
    EntityManager em = entityManagerFactory.createEntityManager();
    instance.set(em);
    return em;
  }

  @Override
  public void startRequest(ExoContainer container) {
    if(instance.get() != null) {
      endRequest(container);
    }

    createEntityManager();
  }

  @Override
  public void endRequest(ExoContainer container) {
    closeEntityManager();
  }

  @Override
  public boolean isStarted(ExoContainer container) {
    return instance.get() != null && instance.get().getTransaction() != null;
  }

  public String getPersistenceUnitName() {
    return PERSISTENCE_UNIT_NAME;
  }

  void closeEntityManager() {
    EntityManager em = getEntityManager();
    if (em != null) {
      EntityTransaction tx = null;
      try {
        tx = em.getTransaction();
        if (tx.isActive()) {
          // Rollback transaction when it's marked as RollbackOnly
          if (tx.getRollbackOnly()) {
            tx.rollback();
          } else {
            tx.commit();
          }
        }
      } finally {
        instance.remove();
        em.close();

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Ended a request lifecycle of {} component service", EntityManagerService.class.getName());
        }
      }
    }
  }
}
