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

  /** The optimizer used for a sequence whose increment is greater than 1. */
  public static final String           POOLED_LO_OPTIMIZER = "pooled-lo";

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

  /**
   * Configures the sequence generator from the annotation. When
   * {@link PortableSequence#incrementBy()} is greater than 1, the
   * {@value #POOLED_LO_OPTIMIZER} optimizer is used: each {@code nextval} is
   * the lowest id of a block of {@code incrementBy} ids. Hibernate's default,
   * {@code pooled}, takes {@code nextval} as the highest id of the block, which
   * overlaps ids already issued on an engine whose sequence returns its stored
   * next value right after an {@code ALTER SEQUENCE ... INCREMENT BY} (HSQLDB).
   * <p>
   * Upgrade safety, together with
   * {@code hibernate.id.sequence.increment_size_mismatch_strategy=FIX} in
   * {@code META-INF/persistence.xml}: the platform's EntityManagerFactory is
   * built before an add-on's changelog alters its sequence, so on the first
   * boot after an upgrade the database still reports step 1, and FIX makes
   * Hibernate issue one id per {@code nextval}. Once the sequence is altered,
   * every {@code nextval} is a fresh value spaced by {@code incrementBy}, so
   * a block that starts at one never contains another: there is no overlap
   * between those two modes, including a cluster where nodes run both at
   * once.
   *
   * @param context the generator creation context
   * @param parameters the parameters Hibernate passes to the generator
   * @throws MappingException when the sequence generator rejects the
   *           configuration
   */
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
    if (incrementBy > 1) {
      sequenceParams.putIfAbsent(SequenceStyleGenerator.OPT_PARAM, POOLED_LO_OPTIMIZER);
    }
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
