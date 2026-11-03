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
package io.meeds.common.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.LongStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.NoopOptimizer;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.enhanced.PooledLoOptimizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.meeds.common.persistence.entity.BlockSequenceEntity;
import io.meeds.common.persistence.entity.StepSequenceEntity;

/**
 * An add-on raising a sequence's increment through {@link PortableSequence}:
 * the platform starts on the first boot after the upgrade, while the database
 * still holds step 1, and no id is issued twice before, during or after the
 * change. The Hibernate settings come from the platform's
 * {@code META-INF/persistence.xml}, so the test runs with the mismatch
 * strategy it ships.
 */
class PortableSequenceGeneratorTest {

  private static final String   SEQUENCE = "SEQ_BLOCK_SEQUENCE_ITEM_ID";

  private String                url;

  private List<SessionFactory>  factories = new ArrayList<>();

  @BeforeEach
  void createSchemaAtStepOne() throws SQLException {
    url = "jdbc:hsqldb:mem:portable-sequence-" + UUID.randomUUID();
    execute("CREATE SEQUENCE " + SEQUENCE + " START WITH 1",
            "CREATE TABLE BLOCK_SEQUENCE_ITEM (ITEM_ID BIGINT NOT NULL PRIMARY KEY, LABEL VARCHAR(100))");
  }

  @AfterEach
  void closeFactories() throws SQLException {
    factories.forEach(SessionFactory::close);
    execute("SHUTDOWN");
  }

  @Test
  void firstBootAfterTheUpgradeStartsAtTheDatabaseStep() {
    SessionFactory before = open(StepSequenceEntity.class);
    insertAll(before, this::stepItem, 5);

    // The mapping says 50, the database still says 1: the factory starts
    SessionFactory firstBoot = open(BlockSequenceEntity.class);
    List<Long> ids = insertAll(firstBoot, this::blockItem, 3);

    assertEquals(List.of(6L, 7L, 8L), ids);
  }

  @Test
  void blocksAfterTheIncrementIsRaisedNeverReuseAnId() throws SQLException {
    insertAll(open(StepSequenceEntity.class), this::stepItem, 5);
    execute("ALTER SEQUENCE " + SEQUENCE + " INCREMENT BY 50");

    List<Long> ids = insertAll(open(BlockSequenceEntity.class), this::blockItem, 60);

    assertEquals(LongStream.rangeClosed(6, 65).boxed().toList(), ids);
    assertEquals(65, countRows());
    assertEquals(5, countRows("LABEL = 'step'"));
  }

  @Test
  void nodesAtBothStepsInsertAlternatelyWithoutDuplicates() throws SQLException {
    SessionFactory oldNode = open(StepSequenceEntity.class);
    SessionFactory nodeBootedAtStepOne = open(BlockSequenceEntity.class);
    insertAll(oldNode, this::stepItem, 3);
    insertAll(nodeBootedAtStepOne, this::blockItem, 3);
    execute("ALTER SEQUENCE " + SEQUENCE + " INCREMENT BY 50");
    SessionFactory nodeBootedAtFifty = open(BlockSequenceEntity.class);

    List<Long> ids = new ArrayList<>();
    for (int i = 0; i < 120; i++) {
      switch (i % 3) {
      case 0 -> ids.add(insert(oldNode, stepItem()));
      case 1 -> ids.add(insert(nodeBootedAtStepOne, blockItem()));
      default -> ids.add(insert(nodeBootedAtFifty, blockItem()));
      }
    }

    assertEquals(ids.size(), new HashSet<>(ids).size());
    assertEquals(126, countRows());
  }

  @Test
  void anIncrementAboveOneUsesThePooledLoOptimizer() throws SQLException {
    execute("ALTER SEQUENCE " + SEQUENCE + " INCREMENT BY 50");

    assertInstanceOf(PooledLoOptimizer.class, optimizer(open(BlockSequenceEntity.class), BlockSequenceEntity.class));
    assertInstanceOf(NoopOptimizer.class, optimizer(open(StepSequenceEntity.class), StepSequenceEntity.class));
  }

  @Test
  void theKernelTestUnitStartsAnUpgradedDatabaseAsThePlatformDoes() {
    String setting = "hibernate.id.sequence.increment_size_mismatch_strategy";
    assertEquals("FIX", platformSettings("meeds-jpa").get(setting));
    assertEquals("FIX", platformSettings("meeds-jpa-test").get(setting));
  }

  /**
   * Builds a session factory for one entity on the test database, with the
   * Hibernate settings of the platform's persistence unit.
   */
  private SessionFactory open(Class<?> entityClass) {
    StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder().applySetting("hibernate.connection.url", url)
                                                                                 .applySetting("hibernate.connection.username",
                                                                                               "sa")
                                                                                 .applySetting("hibernate.connection.password",
                                                                                               "");
    platformSettings("meeds-jpa").forEach(builder::applySetting);
    StandardServiceRegistry registry = builder.build();
    try {
      SessionFactory factory = new MetadataSources(registry).addAnnotatedClass(entityClass)
                                                            .buildMetadata()
                                                            .buildSessionFactory();
      factories.add(factory);
      return factory;
    } catch (RuntimeException e) {
      StandardServiceRegistryBuilder.destroy(registry);
      throw e;
    }
  }

  /**
   * Reads the {@code hibernate.*} properties of a persistence unit on the
   * classpath, but its entity scanner.
   */
  private Map<String, String> platformSettings(String unitName) {
    try {
      Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/persistence.xml");
      while (resources.hasMoreElements()) {
        try (InputStream input = resources.nextElement().openStream()) {
          Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
          NodeList units = document.getElementsByTagName("persistence-unit");
          for (int i = 0; i < units.getLength(); i++) {
            Element unit = (Element) units.item(i);
            if (unitName.equals(unit.getAttribute("name"))) {
              Map<String, String> settings = new LinkedHashMap<>();
              NodeList properties = unit.getElementsByTagName("property");
              for (int j = 0; j < properties.getLength(); j++) {
                Element property = (Element) properties.item(j);
                String name = property.getAttribute("name");
                if (name.startsWith("hibernate.") && !name.equals("hibernate.archive.scanner")) {
                  settings.put(name, property.getAttribute("value"));
                }
              }
              return settings;
            }
          }
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Cannot read the platform's persistence unit", e);
    }
    throw new IllegalStateException("No " + unitName + " persistence unit on the classpath");
  }

  /** Persists {@code count} new entities, one transaction each, and returns their ids in order. */
  private List<Long> insertAll(SessionFactory factory, Supplier<Object> entities, int count) {
    List<Long> ids = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      ids.add(insert(factory, entities.get()));
    }
    return ids;
  }

  /** Persists one entity in its own transaction and returns its id. */
  private long insert(SessionFactory factory, Object entity) {
    factory.inTransaction(session -> session.persist(entity));
    return (Long) factory.getPersistenceUnitUtil().getIdentifier(entity);
  }

  /** A row written with the step-1 mapping. */
  private Object stepItem() {
    StepSequenceEntity entity = new StepSequenceEntity();
    entity.setLabel("step");
    return entity;
  }

  /** A row written with the block mapping. */
  private Object blockItem() {
    BlockSequenceEntity entity = new BlockSequenceEntity();
    entity.setLabel("block");
    return entity;
  }

  /** The optimizer of the entity's id generator. */
  private Optimizer optimizer(SessionFactory factory, Class<?> entityClass) {
    PortableSequenceGenerator generator = (PortableSequenceGenerator) factory.unwrap(SessionFactoryImplementor.class)
                                                                             .getMappingMetamodel()
                                                                             .getEntityDescriptor(entityClass)
                                                                             .getGenerator();
    return generator.getOptimizer();
  }

  /** Counts every row of the table. */
  private long countRows() throws SQLException {
    return countRows("1 = 1");
  }

  /** Counts the rows of the table matching a condition. */
  private long countRows(String condition) throws SQLException {
    try (Connection connection = DriverManager.getConnection(url, "sa", "");
         Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM BLOCK_SEQUENCE_ITEM WHERE " + condition)) {
      result.next();
      return result.getLong(1);
    }
  }

  /** Runs statements on the test database outside Hibernate, as a changelog would. */
  private void execute(String... sqls) throws SQLException {
    try (Connection connection = DriverManager.getConnection(url, "sa", "");
         Statement statement = connection.createStatement()) {
      for (String sql : sqls) {
        statement.execute(sql);
      }
    }
  }

}
