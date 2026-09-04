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

import java.lang.reflect.Member;
import java.util.EnumSet;
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;

public class PortableSequenceGenerator
    implements BeforeExecutionGenerator, OnExecutionGenerator, Configurable, PersistentIdentifierGenerator {

  private static final long            serialVersionUID = 2177731333709865647L;

  private final boolean                identity;

  private final IdentityGenerator      identityGenerator;

  private final SequenceStyleGenerator sequenceGenerator;

  private final String                 sequenceName;

  private final int                    startWith;

  private final int                    incrementBy;

  public PortableSequenceGenerator(PortableSequence config,
                                   Member annotatedMember, // NOSONAR
                                   GeneratorCreationContext context) {

    ServiceRegistry serviceRegistry = context.getServiceRegistry();

    Dialect dialect = serviceRegistry.requireService(JdbcEnvironment.class)
                                     .getDialect();
    this.identity = dialect instanceof MySQLDialect;
    this.sequenceName = config.name();
    this.startWith = config.startWith();
    this.incrementBy = config.incrementBy();

    this.identityGenerator = identity ? new IdentityGenerator() : null;
    this.sequenceGenerator = identity ? null : new SequenceStyleGenerator();
  }

  @Override
  public void configure(GeneratorCreationContext context, Properties parameters) throws MappingException {
    if (identity) {
      return;
    }

    Properties sequenceParams = new Properties();
    sequenceParams.putAll(parameters);
    sequenceParams.put(SequenceStyleGenerator.SEQUENCE_PARAM, sequenceName);
    sequenceParams.put(SequenceStyleGenerator.INITIAL_PARAM, String.valueOf(startWith)); // NOSONAR
    sequenceParams.put(SequenceStyleGenerator.INCREMENT_PARAM, String.valueOf(incrementBy));// NOSONAR
    sequenceGenerator.configure(context, sequenceParams);
  }

  @Override
  public void registerExportables(Database database) {
    if (!identity) {
      sequenceGenerator.registerExportables(database);
    }
  }

  @Override
  public void initialize(SqlStringGenerationContext context) {
    if (!identity) {
      sequenceGenerator.initialize(context);
    }
  }

  @Override
  public EnumSet<EventType> getEventTypes() {
    return EventTypeSets.INSERT_ONLY;
  }

  @Override
  public boolean generatedOnExecution() {
    return identity;
  }

  @Override
  public Object generate(SharedSessionContractImplementor session,
                         Object owner,
                         Object currentValue,
                         EventType eventType) {

    if (identity) {
      throw new UnsupportedOperationException("Identity generation happens during INSERT");
    } else {
      return sequenceGenerator.generate(session, owner, currentValue, eventType);
    }
  }

  @Override
  public Object generate(SharedSessionContractImplementor session, Object object) {
    if (identity) {
      throw new UnsupportedOperationException("Identity generation happens during INSERT");
    } else {
      return sequenceGenerator.generate(session, object);
    }
  }

  @Override
  public boolean referenceColumnsInSql(Dialect dialect) {
    return identity && identityGenerator.referenceColumnsInSql(dialect);
  }

  @Override
  public boolean writePropertyValue() {
    return identity && identityGenerator.writePropertyValue();
  }

  @Override
  public String[] getReferencedColumnValues(Dialect dialect) {
    return identity ? identityGenerator.getReferencedColumnValues(dialect) : new String[0];
  }

  @Override
  public Optimizer getOptimizer() {
    return identity ? null : sequenceGenerator.getOptimizer();
  }

  @Override
  @SuppressWarnings("removal")
  public InsertGeneratedIdentifierDelegate getGeneratedIdentifierDelegate(EntityPersister persister) { // NOSONAR
    return identity ? identityGenerator.getGeneratedIdentifierDelegate(persister) : null;
  }

}
