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

package io.meeds.portal.logging.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.exception.ObjectNotFoundException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import io.meeds.portal.logging.constant.LoggingLevel;
import io.meeds.portal.logging.job.LoggingLevelRefreshJob;
import io.meeds.portal.logging.model.LoggerLevel;
import io.meeds.portal.logging.model.LoggerLevelPage;
import io.meeds.portal.logging.model.StoredLevels;
import io.meeds.portal.logging.storage.LoggingLevelStorage;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

class LoggingLevelServiceTest {

  private static final String      PACKAGE = "io.meeds.test.logging.pkg";

  private static final String      OTHER   = "io.meeds.test.logging.other";

  private static final String      USER    = "admin";

  private final Map<String, Object> settings = new HashMap<>();

  private LoggingLevelStorage      storage;

  private LoggingLevelService      service;

  @BeforeEach
  void setUp() {
    SettingService settingService = mock(SettingService.class);
    org.mockito.Mockito.doAnswer(invocation -> {
      settings.put(invocation.getArgument(2), ((SettingValue<?>) invocation.getArgument(3)).getValue());
      return null;
    }).when(settingService).set(any(), any(), anyString(), any());
    when(settingService.get(any(), any(), anyString())).thenAnswer(invocation -> {
      Object value = settings.get(invocation.getArgument(2));
      return value == null ? null : SettingValue.create(value.toString());
    });
    storage = new LoggingLevelStorage(settingService);
    service = newService(storage);
    logger(PACKAGE).setLevel(null);
    logger(OTHER).setLevel(null);
  }

  @AfterEach
  void tearDown() {
    service.destroy();
    logger(PACKAGE).setLevel(null);
    logger(OTHER).setLevel(null);
  }

  @Test
  void setLevelAppliesToTheRunningLogger() throws Exception {
    service.setLevel(PACKAGE, "debug", USER);

    assertEquals(Level.DEBUG, logger(PACKAGE).getLevel());
    assertTrue(logger(PACKAGE).isDebugEnabled());
    assertEquals(LoggingLevel.DEBUG, storage.getOverrides().get(PACKAGE));
  }

  @Test
  void setLevelCreatesTheLoggerOfANewPackage() throws Exception {
    service.setLevel("io.meeds.test.logging.brandnew", "TRACE", USER);

    assertEquals(Level.TRACE, logger("io.meeds.test.logging.brandnew").getLevel());
    logger("io.meeds.test.logging.brandnew").setLevel(null);
  }

  @Test
  void setLevelChangesTheVersionOnEveryWrite() throws Exception {
    service.setLevel(PACKAGE, "DEBUG", USER);
    String first = storage.getVersion();
    service.setLevel(PACKAGE, "TRACE", USER);

    assertNotEquals(first, storage.getVersion());
  }

  @Test
  void setLevelRefusesAnInvalidNameOrLevel() {
    assertEquals("logging.name.invalid", assertThrows(IllegalArgumentException.class, () -> service.setLevel("a b", "DEBUG", USER)).getMessage());
    assertEquals("logging.name.invalid", assertThrows(IllegalArgumentException.class, () -> service.setLevel("a=b", "DEBUG", USER)).getMessage());
    assertEquals("logging.name.invalid", assertThrows(IllegalArgumentException.class, () -> service.setLevel("a\nb", "DEBUG", USER)).getMessage());
    assertEquals("logging.name.invalid", assertThrows(IllegalArgumentException.class, () -> service.setLevel("a..b", "DEBUG", USER)).getMessage());
    assertEquals("logging.name.invalid", assertThrows(IllegalArgumentException.class, () -> service.setLevel("", "DEBUG", USER)).getMessage());
    assertEquals("logging.name.invalid", assertThrows(IllegalArgumentException.class, () -> service.setLevel(null, "DEBUG", USER)).getMessage());
    assertEquals("logging.level.invalid", assertThrows(IllegalArgumentException.class, () -> service.setLevel(PACKAGE, "LOUD", USER)).getMessage());
    assertEquals("logging.level.invalid", assertThrows(IllegalArgumentException.class, () -> service.setLevel(PACKAGE, null, USER)).getMessage());
    assertTrue(storage.getOverrides().isEmpty());
  }

  @Test
  void resetLevelGivesBackTheLevelOfLogbackXml() throws Exception {
    logger(PACKAGE).setLevel(Level.WARN);
    service.setLevel(PACKAGE, "TRACE", USER);

    service.resetLevel(PACKAGE, USER);

    assertEquals(Level.WARN, logger(PACKAGE).getLevel());
    assertTrue(storage.getOverrides().isEmpty());
  }

  @Test
  void resetLevelOfALoggerWithoutLevelInheritsAgain() throws Exception {
    service.setLevel(PACKAGE, "TRACE", USER);

    service.resetLevel(PACKAGE, USER);

    assertNull(logger(PACKAGE).getLevel());
  }

  @Test
  void resetLevelOfALoggerWithoutOverrideIsNotFound() {
    assertThrows(ObjectNotFoundException.class, () -> service.resetLevel(PACKAGE, USER));
  }

  @Test
  void resetAllRestoresEveryLogger() throws Exception {
    logger(OTHER).setLevel(Level.ERROR);
    service.setLevel(PACKAGE, "DEBUG", USER);
    service.setLevel(OTHER, "TRACE", USER);

    service.resetAll(USER);

    assertNull(logger(PACKAGE).getLevel());
    assertEquals(Level.ERROR, logger(OTHER).getLevel());
    assertTrue(storage.getOverrides().isEmpty());
  }

  @Test
  void refreshAppliesTheOverridesAnotherNodeStored() throws Exception {
    LoggingLevelService otherNode = newService(storage);
    otherNode.setLevel(PACKAGE, "DEBUG", USER);
    logger(PACKAGE).setLevel(null);

    service.refresh();

    assertEquals(Level.DEBUG, logger(PACKAGE).getLevel());
  }

  @Test
  void refreshRestoresTheLoggerAnotherNodeReset() throws Exception {
    logger(PACKAGE).setLevel(Level.WARN);
    service.setLevel(PACKAGE, "DEBUG", USER);
    LoggingLevelService otherNode = newService(storage);
    otherNode.refresh();
    otherNode.resetLevel(PACKAGE, USER);

    service.refresh();

    assertEquals(Level.WARN, logger(PACKAGE).getLevel());
  }

  @Test
  void refreshAppliesTheOverrideAgainAfterALogbackReload() throws Exception {
    service.setLevel(PACKAGE, "DEBUG", USER);
    logger(PACKAGE).setLevel(Level.INFO); // what a reload of logback.xml does

    service.refresh();

    assertEquals(Level.DEBUG, logger(PACKAGE).getLevel());
  }

  @Test
  void resetAfterALogbackReloadGivesTheReloadedLevel() throws Exception {
    service.setLevel(PACKAGE, "DEBUG", USER);
    logger(PACKAGE).setLevel(Level.INFO); // the reloaded logback.xml gives INFO
    service.contextWasReset(); // logback reports its reset before it reads the file
    service.refresh();

    service.resetLevel(PACKAGE, USER);

    assertEquals(Level.INFO, logger(PACKAGE).getLevel());
  }

  @Test
  void initAppliesTheStoredOverridesAfterARestart() {
    storage.saveOverrides(new java.util.LinkedHashMap<>(Map.of(PACKAGE, LoggingLevel.TRACE)));

    LoggingLevelService restarted = newService(storage);
    restarted.init();

    assertEquals(Level.TRACE, logger(PACKAGE).getLevel());
    restarted.destroy();
  }

  @Test
  void getLoggersListsTheOverriddenOnesFirstThenByName() throws Exception {
    logger(OTHER).setLevel(Level.WARN);
    service.setLevel(PACKAGE, "DEBUG", USER);

    LoggerLevelPage page = service.getLoggers("io.meeds.test.logging", true, 0, 10);

    assertEquals(2, page.total());
    LoggerLevel overridden = page.loggers().get(0);
    assertEquals(PACKAGE, overridden.name()); // after OTHER by name, first because it is overridden
    assertTrue(overridden.overridden());
    assertEquals(LoggingLevel.DEBUG, overridden.level());
    assertNull(overridden.configuredLevel());
    LoggerLevel configured = page.loggers().get(1);
    assertEquals(OTHER, configured.name());
    assertFalse(configured.overridden());
    assertEquals(LoggingLevel.WARN, configured.configuredLevel());
  }

  @Test
  void getLoggersFiltersAndPages() throws Exception {
    logger(PACKAGE).setLevel(Level.WARN);
    logger(OTHER).setLevel(Level.ERROR);

    LoggerLevelPage first = service.getLoggers("IO.MEEDS.TEST.LOGGING", true, 0, 1);
    assertEquals(2, first.total());
    assertEquals(OTHER, first.loggers().get(0).name());

    LoggerLevelPage second = service.getLoggers("io.meeds.test.logging", true, 1, 1);
    assertEquals(1, second.loggers().size());
    assertEquals(PACKAGE, second.loggers().get(0).name());
    assertEquals(2, second.total());
  }

  @Test
  void getLoggersWithoutExplicitOnlyIncludesInheritingLoggers() throws Exception {
    logger(PACKAGE);

    assertEquals(0, service.getLoggers(PACKAGE, true, 0, 10).total());
    assertEquals(1, service.getLoggers(PACKAGE, false, 0, 10).total());
  }

  @Test
  void aSecondChangeStillGivesBackTheLevelOfLogbackXml() throws Exception {
    logger(PACKAGE).setLevel(Level.INFO);
    service.setLevel(PACKAGE, "DEBUG", USER);
    service.setLevel(PACKAGE, "TRACE", USER);

    service.resetLevel(PACKAGE, USER);

    assertEquals(Level.INFO, logger(PACKAGE).getLevel());
  }

  @Test
  void anOverrideEqualToTheLevelOfLogbackXmlStillGivesItBack() throws Exception {
    logger(PACKAGE).setLevel(Level.INFO);
    service.setLevel(PACKAGE, "INFO", USER);
    assertEquals(LoggingLevel.INFO, service.getLoggers(PACKAGE, true, 0, 10).loggers().get(0).configuredLevel());

    service.resetLevel(PACKAGE, USER);

    assertEquals(Level.INFO, logger(PACKAGE).getLevel());
  }

  @Test
  void afterAReloadTheLevelOfTheNewFileIsTheOneGivenBack() throws Exception {
    logger(PACKAGE).setLevel(Level.INFO);
    service.init();
    service.setLevel(PACKAGE, "DEBUG", USER);
    // the reloaded logback.xml gives the logger the very level of the override
    logger(PACKAGE).setLevel(Level.DEBUG);
    service.contextWasReset();
    service.refresh();

    service.resetLevel(PACKAGE, USER);

    assertEquals(Level.DEBUG, logger(PACKAGE).getLevel());
  }

  @Test
  void theListenerOfALogbackResetIsRegisteredAndRemoved() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    service.init();
    assertEquals(1, context.getCopyOfListenerList().stream().filter(LoggerContextResetListener.class::isInstance).count());

    service.destroy();
    service.destroy();

    assertEquals(0, context.getCopyOfListenerList().stream().filter(LoggerContextResetListener.class::isInstance).count());
  }

  @Test
  void aResetOfLogbackIsReportedToTheService() throws Exception {
    LoggerContextResetListener listener = new LoggerContextResetListener(service::contextWasReset);
    logger(PACKAGE).setLevel(Level.WARN);
    service.setLevel(PACKAGE, "DEBUG", USER);
    logger(PACKAGE).setLevel(Level.ERROR);

    listener.onReset((LoggerContext) LoggerFactory.getILoggerFactory());
    service.refresh();

    assertEquals(LoggingLevel.ERROR, service.getLoggers(PACKAGE, true, 0, 10).loggers().get(0).configuredLevel());
    assertTrue(listener.isResetResistant());
  }

  @Test
  void twoNodesDoNotEraseEachOthersChange() throws Exception {
    LoggingLevelService otherNode = newService(storage);
    otherNode.setLevel(OTHER, "DEBUG", USER);

    service.setLevel(PACKAGE, "TRACE", USER); // this node did not refresh since

    assertEquals(LoggingLevel.DEBUG, storage.getOverrides().get(OTHER));
    assertEquals(LoggingLevel.TRACE, storage.getOverrides().get(PACKAGE));
    assertEquals(Level.DEBUG, logger(OTHER).getLevel());
  }

  @Test
  void aPageHasBetweenOneAndFiveHundredLoggers() throws Exception {
    assertEquals(1, service.getLoggers("", false, 0, 0).limit());
    assertEquals(1, service.getLoggers("", false, 0, -5).limit());
    assertEquals(500, service.getLoggers("", false, 0, 100000).limit());
    assertEquals(20, service.getLoggers("", false, 0, 20).limit());
  }

  @Test
  void refreshKeepsTheLevelsWhenTheStoreDoesNotAnswer() {
    SettingService failing = mock(SettingService.class);
    when(failing.get(any(), any(), anyString())).thenThrow(new IllegalStateException("database down"));
    LoggingLevelService node = newService(new LoggingLevelStorage(failing));
    logger(PACKAGE).setLevel(Level.WARN);

    node.refresh();
    node.refresh();

    assertEquals(Level.WARN, logger(PACKAGE).getLevel());
  }

  @Test
  void atMostFiveHundredLoggersAreChanged() throws Exception {
    for (int i = 0; i < 500; i++) {
      service.setLevel("io.meeds.test.logging.many.n" + i, "DEBUG", USER);
    }

    assertEquals("logging.overrides.tooMany",
                 assertThrows(IllegalArgumentException.class, () -> service.setLevel(PACKAGE, "DEBUG", USER)).getMessage());
    service.setLevel("io.meeds.test.logging.many.n7", "TRACE", USER); // a change of one already changed stays possible
    service.resetAll(USER);
  }

  @Test
  void aSecondChangeOfALoggerWithoutLevelStillInheritsAfterAReset() throws Exception {
    service.setLevel(PACKAGE, "DEBUG", USER);
    service.setLevel(PACKAGE, "TRACE", USER);

    service.resetLevel(PACKAGE, USER);

    assertNull(logger(PACKAGE).getLevel());
  }

  @Test
  void rootIsRootWhateverTheCase() throws Exception {
    service.setLevel("root", "INFO", USER);
    service.setLevel("Root", "INFO", USER);

    assertEquals(1, storage.getOverrides().size());
    assertEquals(LoggingLevel.INFO, storage.getOverrides().get("ROOT"));
  }

  @Test
  void aWriteThatFailsChangesNothing() throws Exception {
    SettingService failing = mock(SettingService.class);
    doThrow(new IllegalStateException("database down")).when(failing).set(any(), any(), anyString(), any());
    LoggingLevelService node = newService(new LoggingLevelStorage(failing));
    logger(PACKAGE).setLevel(Level.WARN);

    assertThrows(IllegalStateException.class, () -> node.setLevel(PACKAGE, "DEBUG", USER));

    assertEquals(Level.WARN, logger(PACKAGE).getLevel());
    assertFalse(node.getLoggers(PACKAGE, true, 0, 10).loggers().get(0).overridden());
  }

  @Test
  void aResetJustAfterAReloadKeepsTheLevelOfTheNewFile() throws Exception {
    logger(PACKAGE).setLevel(Level.INFO);
    service.setLevel(PACKAGE, "DEBUG", USER);
    logger(PACKAGE).setLevel(Level.WARN); // the reloaded logback.xml gives WARN
    service.contextWasReset();

    service.resetLevel(PACKAGE, USER); // before the next refresh

    assertEquals(Level.WARN, logger(PACKAGE).getLevel());
  }

  @Test
  void aContextThatIsGoneGivesBackTheLevelsItChanged() throws Exception {
    logger(PACKAGE).setLevel(Level.INFO);
    service.init();
    service.setLevel(PACKAGE, "DEBUG", USER);

    service.destroy();

    assertEquals(Level.INFO, logger(PACKAGE).getLevel());
    assertEquals(LoggingLevel.DEBUG, storage.getOverrides().get(PACKAGE)); // stored: the next context applies it again
  }

  @Test
  void aNameIsDottedJavaIdentifiers() throws Exception {
    for (String invalid : new String[] { ".a", "a.", "a.1b", "1a", "a-.b c", "a/b", "a\\b", "a;b", "a%b",
        "a".repeat(100) + "." + "b".repeat(101) }) {
      assertEquals("logging.name.invalid",
                   assertThrows(IllegalArgumentException.class, () -> service.setLevel(invalid, "DEBUG", USER)).getMessage(),
                   invalid);
    }
    // a long name is checked without recursion: it neither overflows the stack nor takes long
    String longest = "a".repeat(100) + "." + "b".repeat(99); // 200 characters
    service.setLevel(longest, "DEBUG", USER);
    assertEquals(LoggingLevel.DEBUG, storage.getOverrides().get(longest));
    String longName = ("a.".repeat(99)) + "a";
    service.setLevel(longName, "DEBUG", USER);
    assertEquals(LoggingLevel.DEBUG, storage.getOverrides().get(longName));
    service.setLevel("io.meeds.test.logging.Outer$Inner", "DEBUG", USER);
    service.setLevel("io.meeds.test.logging.my-lib_2", "DEBUG", USER);
    service.resetAll(USER);
  }

  @Test
  void anotherNodeChangingAnOverrideKeepsTheLevelOfLogbackXmlToGiveBack() throws Exception {
    logger(PACKAGE).setLevel(Level.INFO);
    service.setLevel(PACKAGE, "DEBUG", USER);
    // another node stored WARN for it; on this node the logger is still at DEBUG
    storage.saveOverrides(new java.util.LinkedHashMap<>(Map.of(PACKAGE, LoggingLevel.WARN)));

    service.refresh();
    assertEquals(Level.WARN, logger(PACKAGE).getLevel());
    assertEquals(LoggingLevel.INFO, service.getLoggers(PACKAGE, true, 0, 10).loggers().get(0).configuredLevel());
    service.resetLevel(PACKAGE, USER);

    assertEquals(Level.INFO, logger(PACKAGE).getLevel());
  }

  @Test
  void aRefreshThatReadBeforeALocalWriteDoesNotRevertIt() throws Exception {
    service.setLevel(PACKAGE, "DEBUG", USER);
    StoredLevels readBeforeTheWrite = new StoredLevels("older", new java.util.LinkedHashMap<>(Map.of(OTHER, LoggingLevel.TRACE)));

    service.applyStored(readBeforeTheWrite, null); // this node had applied nothing when it read

    assertEquals(Level.DEBUG, logger(PACKAGE).getLevel());
    assertNull(logger(OTHER).getLevel());
  }

  @Test
  void whenDisabledNothingIsListedChangedOrApplied() {
    storage.saveOverrides(new java.util.LinkedHashMap<>(Map.of(PACKAGE, LoggingLevel.TRACE)));
    LoggingLevelService disabled = newService(storage, false, false);
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    disabled.init();
    disabled.refresh();

    assertNull(logger(PACKAGE).getLevel()); // the stored levels are ignored, not deleted
    assertEquals(0, context.getCopyOfListenerList().stream().filter(LoggerContextResetListener.class::isInstance).count());
    assertEquals(LoggingLevel.TRACE, storage.getOverrides().get(PACKAGE));
    assertEquals("logging.disabled", assertThrows(IllegalAccessException.class, () -> disabled.getLoggers("", true, 0, 10)).getMessage());
    assertEquals("logging.disabled", assertThrows(IllegalAccessException.class, () -> disabled.setLevel(OTHER, "DEBUG", USER)).getMessage());
    assertEquals("logging.disabled", assertThrows(IllegalAccessException.class, () -> disabled.resetLevel(PACKAGE, USER)).getMessage());
    assertEquals("logging.disabled", assertThrows(IllegalAccessException.class, () -> disabled.resetAll(USER)).getMessage());
    assertNull(logger(OTHER).getLevel());
    assertEquals(LoggingLevel.TRACE, storage.getOverrides().get(PACKAGE));
    assertFalse(disabled.getStatus().enabled());
  }

  @Test
  void whenReadOnlyTheLevelsAreListedAndAppliedButNotChanged() throws Exception {
    storage.saveOverrides(new java.util.LinkedHashMap<>(Map.of(PACKAGE, LoggingLevel.TRACE)));
    LoggingLevelService readOnly = newService(storage, true, true);

    readOnly.init();

    assertEquals(Level.TRACE, logger(PACKAGE).getLevel()); // what is stored is still in force
    assertEquals(1, readOnly.getLoggers(PACKAGE, true, 0, 10).total());
    assertEquals("logging.readOnly", assertThrows(IllegalAccessException.class, () -> readOnly.setLevel(OTHER, "DEBUG", USER)).getMessage());
    assertEquals("logging.readOnly", assertThrows(IllegalAccessException.class, () -> readOnly.resetLevel(PACKAGE, USER)).getMessage());
    assertEquals("logging.readOnly", assertThrows(IllegalAccessException.class, () -> readOnly.resetAll(USER)).getMessage());
    assertNull(logger(OTHER).getLevel());
    assertEquals(Level.TRACE, logger(PACKAGE).getLevel());
    assertEquals(LoggingLevel.TRACE, storage.getOverrides().get(PACKAGE));
    assertTrue(readOnly.getStatus().enabled());
    assertTrue(readOnly.getStatus().readOnly());
    readOnly.destroy();
  }

  @Test
  void whenEnabledTheStatusSaysTheLevelsCanBeChanged() {
    assertTrue(service.getStatus().enabled());
    assertFalse(service.getStatus().readOnly());
  }

  @Test
  void aChangeIsGivenBackAfterTheDelayAndNotBefore() throws Exception {
    long[] now = { 1_000_000L };
    LoggingLevelService node = newService(storage, true, false, 60);
    node.clock = () -> now[0];
    logger(PACKAGE).setLevel(Level.INFO);
    node.setLevel(PACKAGE, "DEBUG", USER);
    String version = storage.getVersion();

    now[0] += 59 * 60_000L;
    node.refresh();
    assertEquals(Level.DEBUG, logger(PACKAGE).getLevel());
    assertEquals(version, storage.getVersion());

    now[0] += 60_000L; // 60 minutes
    node.refresh();
    assertEquals(Level.INFO, logger(PACKAGE).getLevel()); // the level of logback.xml
    assertTrue(storage.getOverrides().isEmpty());
    assertNotEquals(version, storage.getVersion());
    assertFalse(node.getLoggers(PACKAGE, true, 0, 10).loggers().get(0).overridden());
    node.destroy();
  }

  @Test
  void aNewChangeStartsTheDelayAgain() throws Exception {
    long[] now = { 1_000_000L };
    LoggingLevelService node = newService(storage, true, false, 60);
    node.clock = () -> now[0];
    node.setLevel(PACKAGE, "DEBUG", USER);
    now[0] += 50 * 60_000L;
    node.setLevel(PACKAGE, "TRACE", USER);
    // stored too: another node, or a restart, reads this time, not the first one
    assertEquals(1_000_000L + 50 * 60_000L, storage.read().changedAt().get(PACKAGE));

    now[0] += 20 * 60_000L; // 70 minutes after the first change, 20 after the last
    node.refresh();
    assertEquals(Level.TRACE, logger(PACKAGE).getLevel());

    now[0] += 40 * 60_000L; // 60 minutes after the last
    node.refresh();
    assertNull(logger(PACKAGE).getLevel());
    node.destroy();
  }

  @Test
  void withoutALimitAChangeStaysForEver() throws Exception {
    long[] now = { 1_000_000L };
    LoggingLevelService node = newService(storage, true, false, 0);
    node.clock = () -> now[0];
    node.setLevel(PACKAGE, "DEBUG", USER);

    now[0] += 365L * 24 * 60 * 60_000L;
    node.refresh();

    assertEquals(Level.DEBUG, logger(PACKAGE).getLevel());
    assertNull(node.getLoggers(PACKAGE, true, 0, 10).loggers().get(0).revertsAt());
    node.destroy();
  }

  @Test
  void aChangeWithNoKnownTimeIsNeverGivenBack() {
    long[] now = { 1_000_000L };
    storage.saveOverrides(new java.util.LinkedHashMap<>(Map.of(PACKAGE, LoggingLevel.TRACE))); // no time stored
    LoggingLevelService node = newService(storage, true, false, 1);
    node.clock = () -> now[0];
    node.init();

    now[0] += 24L * 60 * 60_000L;
    node.refresh();

    assertEquals(Level.TRACE, logger(PACKAGE).getLevel());
    node.destroy();
  }

  @Test
  void theTimeAChangeIsGivenBackIsListedAndInTheStatus() throws Exception {
    long[] now = { 5_000_000L };
    LoggingLevelService node = newService(storage, true, false, 30);
    node.clock = () -> now[0];
    node.setLevel(PACKAGE, "DEBUG", USER);
    logger(OTHER).setLevel(Level.WARN);

    assertEquals(5_000_000L + 30 * 60_000L, node.getLoggers(PACKAGE, true, 0, 10).loggers().get(0).revertsAt());
    assertNull(node.getLoggers(OTHER, true, 0, 10).loggers().get(0).revertsAt()); // not changed
    assertEquals(30, node.getStatus().revertAfterMinutes());
    node.destroy();
  }

  @Test
  void aReadOnlyServerStillGivesBackAForgottenChange() throws Exception {
    long[] now = { 1_000_000L };
    LoggingLevelService writer = newService(storage, true, false, 0);
    writer.clock = () -> now[0];
    writer.setLevel(PACKAGE, "DEBUG", USER);
    writer.destroy();
    LoggingLevelService readOnly = newService(storage, true, true, 10);
    readOnly.clock = () -> now[0];
    readOnly.init();
    assertEquals(Level.DEBUG, logger(PACKAGE).getLevel());

    now[0] += 11 * 60_000L;
    readOnly.refresh();

    assertNull(logger(PACKAGE).getLevel());
    assertTrue(storage.getOverrides().isEmpty());
    readOnly.destroy();
  }

  @Test
  void whenDisabledNothingIsGivenBack() {
    long[] now = { 1_000_000L };
    storage.saveOverrides(new java.util.LinkedHashMap<>(Map.of(PACKAGE, LoggingLevel.TRACE)), Map.of(PACKAGE, 1L));
    LoggingLevelService disabled = newService(storage, false, false, 1);
    disabled.clock = () -> now[0];

    disabled.refresh();

    assertEquals(LoggingLevel.TRACE, storage.getOverrides().get(PACKAGE));
  }

  @Test
  void anotherNodeFollowsWhatTheFirstOneGaveBack() throws Exception {
    long[] now = { 1_000_000L };
    logger(PACKAGE).setLevel(Level.INFO);
    LoggingLevelService first = newService(storage, true, false, 60);
    first.clock = () -> now[0];
    first.setLevel(PACKAGE, "DEBUG", USER);
    // another node, which has its own logger, still at the level of logback.xml, and takes the override at its next refresh
    logger(PACKAGE).setLevel(Level.INFO);
    LoggingLevelService second = newService(storage, true, false, 60);
    second.clock = () -> now[0];
    second.refresh();
    assertEquals(Level.DEBUG, logger(PACKAGE).getLevel());

    now[0] += 61 * 60_000L;
    first.refresh();
    second.refresh();

    assertEquals(Level.INFO, logger(PACKAGE).getLevel());
    assertTrue(storage.getOverrides().isEmpty());
    first.destroy();
    second.destroy();
  }

  @Test
  void theAuditLineOfAChangeThatSilencesTheServiceIsWrittenNonetheless() throws Exception {
    Logger own = logger(LoggingLevelService.class.getName());
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    own.addAppender(appender);
    own.setLevel(Level.INFO); // what the platform logs at by default
    try {
      service.setLevel(LoggingLevelService.class.getName(), "OFF", USER);

      assertTrue(appender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("set to OFF at runtime by 'admin'")));
      appender.list.clear();
      service.resetLevel(LoggingLevelService.class.getName(), USER); // the logger is OFF until the restore: the line follows it
      assertTrue(appender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("back to its logback.xml level by 'admin'")));
    } finally {
      own.detachAppender(appender);
      own.setLevel(null);
    }
  }

  @Test
  void theAuditLineOfAChangeThatGivesTheLogsBackIsWrittenAfterIt() throws Exception {
    String own = LoggingLevelService.class.getName();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger(own).addAppender(appender);
    try {
      service.setLevel(own, "OFF", USER);
      appender.list.clear();

      service.setLevel(own, "INFO", USER); // written while the logger is OFF: the line follows the change

      assertEquals(1, appender.list.stream().filter(event -> event.getFormattedMessage().contains("set to INFO at runtime by 'admin'")).count());
    } finally {
      logger(own).detachAppender(appender);
      logger(own).setLevel(null);
    }
  }

  @Test
  void theAuditLineOfAResetToALevelThatSilencesTheServiceIsWrittenBeforeIt() throws Exception {
    String own = LoggingLevelService.class.getName();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger(own).addAppender(appender);
    logger(own).setLevel(Level.WARN); // what a logback.xml may give
    try {
      service.setLevel(own, "DEBUG", USER);
      appender.list.clear();
      service.resetLevel(own, USER);
      assertEquals(1, appender.list.stream().filter(event -> event.getFormattedMessage().contains("back to its logback.xml level by 'admin'")).count());

      service.setLevel(own, "DEBUG", USER);
      appender.list.clear();
      service.resetAll(USER);
      assertEquals(1, appender.list.stream().filter(event -> event.getFormattedMessage().contains("Every logger back to its logback.xml level by 'admin'")).count());
    } finally {
      logger(own).detachAppender(appender);
      logger(own).setLevel(null);
    }
  }

  @Test
  void theAuditLineOfAnAutoRevertToALevelThatSilencesTheServiceIsWrittenBeforeIt() throws Exception {
    String own = LoggingLevelService.class.getName();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger(own).addAppender(appender);
    logger(own).setLevel(Level.WARN);
    long[] now = { 1_000_000L };
    LoggingLevelService node = newService(storage, true, false, 60);
    node.clock = () -> now[0];
    try {
      node.setLevel(own, "DEBUG", USER);
      appender.list.clear();
      now[0] += 60 * 60_000L;

      node.refresh();

      assertEquals(1, appender.list.stream().filter(event -> event.getFormattedMessage().contains("minutes after it was changed")).count());
    } finally {
      node.destroy();
      logger(own).detachAppender(appender);
      logger(own).setLevel(null);
    }
  }

  @Test
  void theAuditLineIsWrittenOnceWhenItIsEnabledBeforeTheChange() throws Exception {
    String own = LoggingLevelService.class.getName();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger(own).addAppender(appender);
    logger(own).setLevel(Level.INFO);
    try {
      service.setLevel(own, "DEBUG", USER);

      assertEquals(1, appender.list.stream().filter(event -> event.getFormattedMessage().contains("set to DEBUG at runtime by 'admin'")).count());
    } finally {
      logger(own).detachAppender(appender);
      logger(own).setLevel(null);
    }
  }

  /**
   * Kills the mutants that remove one of: {@code @PostConstruct} on init,
   * {@code @PreDestroy} on destroy, {@code @Service} on the service,
   * {@code @Scheduled} on the job's refresh (or change its interval property),
   * {@code @EnableScheduling} and {@code @Component} on the job.
   */
  @Test
  void theAnnotationsThatMakeTheServiceWorkArePinned() throws Exception {
    assertTrue(LoggingLevelService.class.getMethod("init").isAnnotationPresent(PostConstruct.class));
    assertTrue(LoggingLevelService.class.getMethod("destroy").isAnnotationPresent(PreDestroy.class));
    assertTrue(LoggingLevelService.class.isAnnotationPresent(org.springframework.stereotype.Service.class));
    Method refresh = LoggingLevelRefreshJob.class.getMethod("refresh");
    Scheduled scheduled = refresh.getAnnotation(Scheduled.class);
    assertNotNull(scheduled);
    assertEquals("${meeds.logging.refreshIntervalMs:10000}", scheduled.fixedDelayString());
    assertEquals(scheduled.fixedDelayString(), scheduled.initialDelayString());
    assertTrue(LoggingLevelRefreshJob.class.isAnnotationPresent(EnableScheduling.class));
    assertTrue(LoggingLevelRefreshJob.class.isAnnotationPresent(org.springframework.stereotype.Component.class));
  }

  @Test
  void theJobRunsTheRefreshOfTheService() {
    LoggingLevelService mocked = mock(LoggingLevelService.class);

    new LoggingLevelRefreshJob(mocked).refresh();

    verify(mocked).refresh();
  }

  @Test
  void anAutoRevertDoesNotEraseAChangeAnotherNodeStoredSinceItRead() throws Exception {
    long[] now = { 1_000_000L };
    LoggingLevelService node = newService(storage, true, false, 60);
    node.clock = () -> now[0];
    node.setLevel(PACKAGE, "DEBUG", USER);
    StoredLevels readBefore = storage.read();
    // another node stores a change of OTHER after this node read the store
    storage.saveOverrides(new java.util.LinkedHashMap<>(java.util.Map.of(PACKAGE, LoggingLevel.DEBUG, OTHER, LoggingLevel.TRACE)),
                          Map.of(PACKAGE, 1_000_000L, OTHER, 1_000_000L + 59 * 60_000L));

    now[0] += 60 * 60_000L; // the change of PACKAGE is due, the one of OTHER is not
    node.applyStored(readBefore, readBefore.version());

    assertEquals(Map.of(OTHER, LoggingLevel.TRACE), storage.getOverrides()); // OTHER is still there
    node.destroy();
  }

  @Test
  void anAutoRevertDoesNotGiveBackALoggerAnotherNodeChangedAgainSinceItRead() throws Exception {
    long[] now = { 1_000_000L };
    LoggingLevelService node = newService(storage, true, false, 60);
    node.clock = () -> now[0];
    node.setLevel(PACKAGE, "DEBUG", USER);
    StoredLevels readBefore = storage.read();
    now[0] += 60 * 60_000L; // the change of this node is due
    // another node changed the same logger again a minute ago: it is not due any more
    storage.saveOverrides(new java.util.LinkedHashMap<>(java.util.Map.of(PACKAGE, LoggingLevel.TRACE)),
                          Map.of(PACKAGE, now[0] - 60_000L));

    node.applyStored(readBefore, readBefore.version());

    assertEquals(Map.of(PACKAGE, LoggingLevel.TRACE), storage.getOverrides());
    assertEquals(Level.TRACE, logger(PACKAGE).getLevel());
    node.destroy();
  }

  @Test
  void anAutoRevertGivesNothingBackWhenTheStoreCannotBeReadAgain() throws Exception {
    long[] now = { 1_000_000L };
    LoggingLevelStorage failing = spy(storage);
    LoggingLevelService node = newService(failing, true, false, 60);
    node.clock = () -> now[0];
    node.setLevel(PACKAGE, "DEBUG", USER);
    StoredLevels readBefore = storage.read();
    now[0] += 60 * 60_000L;
    doThrow(new IllegalStateException("store down")).when(failing).read();

    String readVersion = readBefore.version();
    assertThrows(IllegalStateException.class, () -> node.applyStored(readBefore, readVersion));

    assertEquals(Map.of(PACKAGE, LoggingLevel.DEBUG), storage.getOverrides());
    assertEquals(Level.DEBUG, logger(PACKAGE).getLevel());
    org.mockito.Mockito.doCallRealMethod().when(failing).read();
    node.destroy();
  }

  @Test
  void theLoggersOfTheContainerAreListedButNotEditable() throws Exception {
    String container = "org.apache.catalina.core.ContainerBase.[Catalina].[localhost].[/portal]";
    logger(container);
    logger(PACKAGE).setLevel(Level.WARN);

    LoggerLevelPage page = service.getLoggers("", false, 0, 500);

    assertFalse(page.loggers().stream().filter(l -> l.name().equals(container)).findFirst().orElseThrow().editable());
    assertTrue(page.loggers().stream().filter(l -> l.name().equals(PACKAGE)).findFirst().orElseThrow().editable());
    assertTrue(page.loggers().stream().filter(l -> l.name().equals("ROOT")).findFirst().orElseThrow().editable());
  }

  @Test
  void aLoggerWithASpaceAroundItsNameIsNotEditableSinceTheTrimmedNameIsAnotherLogger() throws Exception {
    String padded = " io.meeds.test.logging.padded";
    logger(padded);

    assertFalse(service.getLoggers("padded", false, 0, 10).loggers().stream().filter(l -> l.name().equals(padded)).findFirst().orElseThrow().editable());
  }

  @Test
  void aServiceThatIsStoppedDoesNotApplyWhatIsStoredAfterwards() throws Exception {
    service.init();
    service.setLevel(PACKAGE, "DEBUG", USER);
    service.destroy();
    storage.saveOverrides(new java.util.LinkedHashMap<>(java.util.Map.of(OTHER, LoggingLevel.TRACE))); // another node

    service.refresh(); // a refresh that was already under way when the context closed

    assertNull(logger(OTHER).getLevel());
    assertNull(logger(PACKAGE).getLevel()); // given back by destroy, and not put back
  }

  private static LoggingLevelService newService(LoggingLevelStorage storage) {
    return newService(storage, true, false);
  }

  private static LoggingLevelService newService(LoggingLevelStorage storage, boolean enabled, boolean readOnly) {
    return newService(storage, enabled, readOnly, 0);
  }

  private static LoggingLevelService newService(LoggingLevelStorage storage, boolean enabled, boolean readOnly, int revertAfterMinutes) {
    return new LoggingLevelService(storage, new LoggingLevelConfiguration(enabled, readOnly, "administrators", revertAfterMinutes));
  }

  private static Logger logger(String name) {
    return ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(name);
  }

}
