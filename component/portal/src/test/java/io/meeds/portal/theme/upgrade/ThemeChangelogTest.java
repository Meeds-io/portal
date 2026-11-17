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
package io.meeds.portal.theme.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.OfflineConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * The theme changelog, run by the platform's own Liquibase: it applies on an
 * empty database, seeds the Default Theme and the state row, rolls back to
 * nothing (by date, never by a count, which would follow the changelog's
 * tail as changesets are appended) and applies again. The MySQL and PostgreSQL forms are generated
 * offline through Liquibase's own dialects, since the suite runs HSQLDB only.
 */
public class ThemeChangelogTest {

  private static final String CHANGELOG = "db/changelog/portal-theme.db.changelog-1.0.0.xml";

  @Test
  void appliesSeedsRollsBackAndReapplies() throws Exception {
    try (Connection connection = DriverManager.getConnection("jdbc:hsqldb:mem:theme-changelog" + System.nanoTime(), "sa", "")) {
      Liquibase liquibase = new Liquibase(CHANGELOG,
                                          new ClassLoaderResourceAccessor(),
                                          DatabaseFactory.getInstance()
                                                         .findCorrectDatabaseImplementation(new JdbcConnection(connection)));
      liquibase.update(new Contexts(), new LabelExpression());
      assertTrue(tableExists(connection, "PORTAL_THEMES"));
      assertTrue(tableExists(connection, "PORTAL_BRANDING_STATE"));
      assertEquals("1|Default Theme|default theme|DEFAULT|{}|{}|{}|FALSE",
                   singleRow(connection,
                             "SELECT THEME_ID, NAME, NAME_KEY, ORIGIN, SHARED_SETTINGS, LIGHT_SETTINGS, DARK_SETTINGS, NAME_SET_IN_UI FROM PORTAL_THEMES"));
      assertEquals("1|0|FALSE|null|null|0",
                   singleRow(connection,
                             "SELECT ID, VERSION, LEGACY_IMPORTED, THEME_IN_USE_ID, MODES_OFFERED, MODES_GENERATION FROM PORTAL_BRANDING_STATE"));
      assertEquals("2", singleRow(connection, "SELECT NEXT VALUE FOR SEQ_PORTAL_THEME_ID FROM (VALUES(0))"),
                   "the sequence never draws the seed's id");

      liquibase.rollback(new Date(0), "");
      assertFalse(tableExists(connection, "PORTAL_THEMES"), "the rollback drops the theme table");
      assertFalse(tableExists(connection, "PORTAL_BRANDING_STATE"), "and the state table");
      assertFalse(sequenceExists(connection, "SEQ_PORTAL_THEME_ID"), "and the sequence");

      liquibase.update(new Contexts(), new LabelExpression());
      assertEquals("1", singleRow(connection, "SELECT COUNT(*) FROM PORTAL_THEMES"), "the changesets apply again after their rollback");
    }
  }

  @Test
  void onMySqlTheIdStartsAtTwoAndTheTextColumnsAreUtf8mb4() throws Exception {
    String sql = offlineSql("mysql?version=8.0.17");
    Matcher createThemes = Pattern.compile("CREATE TABLE PORTAL_THEMES \\(.*?\\);", Pattern.DOTALL).matcher(sql);
    assertTrue(createThemes.find(), sql);
    assertTrue(createThemes.group().contains("AUTO_INCREMENT"), createThemes.group());
    assertTrue(createThemes.group().contains("AUTO_INCREMENT=2"), "the MySQL generator starts after the seed: " + createThemes.group());
    assertTrue(sql.contains("MODIFY NAME_KEY VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL"));
    assertTrue(sql.contains("MODIFY NAME VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL"));
    assertTrue(sql.contains("MODIFY COMPANY_NAME VARCHAR(200) CHARACTER SET utf8mb4"));
    assertFalse(sql.contains("SEQ_PORTAL_THEME_ID"), "MySQL uses an auto-increment id, no sequence");
  }

  @Test
  void onPostgresqlTheIdHasItsSequenceStartingAtTwo() throws Exception {
    String sql = offlineSql("postgresql?version=15");
    Matcher sequence = Pattern.compile("CREATE SEQUENCE[^;]*SEQ_PORTAL_THEME_ID[^;]*;").matcher(sql);
    assertTrue(sequence.find(), sql);
    assertTrue(sequence.group().contains("START WITH 2"), sequence.group());
    assertFalse(sql.contains("utf8mb4"), "the MySQL-only changeset stays on MySQL");
  }

  private static String offlineSql(String engine) throws Exception {
    // Each engine gets its own offline history: a shared one would hold the other engine's checksums
    Path history = Files.createTempFile("theme-changelog-" + engine.substring(0, engine.indexOf('?')), ".csv");
    Files.delete(history);
    Database database = DatabaseFactory.getInstance()
                                       .findCorrectDatabaseImplementation(new OfflineConnection("offline:" + engine
                                           + "&changeLogFile=" + history.toAbsolutePath(), new ClassLoaderResourceAccessor()));
    Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database);
    StringWriter writer = new StringWriter();
    liquibase.update(new Contexts(), new LabelExpression(), writer);
    return writer.toString();
  }

  private static String singleRow(Connection connection, String query) throws Exception {
    try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(query)) {
      assertTrue(resultSet.next(), query);
      StringBuilder row = new StringBuilder();
      for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
        if (i > 1) {
          row.append('|');
        }
        row.append(resultSet.getString(i));
      }
      assertFalse(resultSet.next(), "one row expected: " + query);
      return row.toString();
    }
  }

  private static boolean tableExists(Connection connection, String table) throws Exception {
    try (ResultSet resultSet = connection.getMetaData().getTables(null, null, table, null)) {
      return resultSet.next();
    }
  }

  private static boolean sequenceExists(Connection connection, String sequence) throws Exception {
    try (Statement statement = connection.createStatement();
         ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_NAME = '"
             + sequence + "'")) {
      resultSet.next();
      return resultSet.getInt(1) > 0;
    }
  }
}
