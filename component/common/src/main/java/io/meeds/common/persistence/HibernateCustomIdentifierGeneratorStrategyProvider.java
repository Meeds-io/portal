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
package io.meeds.common.persistence;

import java.util.Collections;
import java.util.Map;

import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.factory.spi.GenerationTypeStrategyRegistration;
import org.hibernate.jpa.spi.IdentifierGeneratorStrategyProvider;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import jakarta.persistence.GenerationType;

/**
 * This is used as workaround for Hibernate 6 change behavior with MySQL Dialect to
 * interpret {@link GenerationType#AUTO} as {@link SequenceStyleGenerator}
 * instead of {@link IdentityGenerator}. When supporting multiple RDBMS, this
 * behavior is inconvenient, thus it's simply overridden here.
 * 
 * This method of customization was deprecated while in Hibernate 6.3 the replacement using
 * {@link GenerationTypeStrategyRegistration} doesn't work yet.
 * 
 * Thus it will be considered when the Hibernate Engine will support the proposed replacement.
 */
public class HibernateCustomIdentifierGeneratorStrategyProvider implements IdentifierGeneratorStrategyProvider {

  private static final Log     LOG      = ExoLogger.getLogger(HibernateCustomIdentifierGeneratorStrategyProvider.class);

  private static final boolean IS_MYSQL =
                                        Boolean.parseBoolean(System.getProperty("io.meeds.hibernate.legacy_mysql_sequence_generator",
                                                                                "true"));

  @Override
  public Map<String, Class<?>> getStrategies() {
    if (isMySQLDriver()) {
      if (PropertyManager.isDevelopping()) {
        LOG.info("Using Old Strategy generator. This might be deleted by hibernate new versions.");
      }
      return Collections.singletonMap(SequenceStyleGenerator.class.getName(), IdentityGenerator.class);
    } else {
      return Collections.singletonMap(SequenceStyleGenerator.class.getName(), SequenceStyleGenerator.class);
    }
  }

  private boolean isMySQLDriver() {
    try {
      return IS_MYSQL && getClass().getClassLoader().loadClass("com.mysql.cj.jdbc.Driver") != null;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

}
