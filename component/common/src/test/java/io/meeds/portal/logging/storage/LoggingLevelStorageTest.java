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

package io.meeds.portal.logging.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;

import io.meeds.portal.logging.constant.LoggingLevel;
import io.meeds.portal.logging.model.StoredLevels;

class LoggingLevelStorageTest {

  private final Map<String, Object> settings = new HashMap<>();

  private LoggingLevelStorage       storage;

  @BeforeEach
  void setUp() {
    SettingService settingService = mock(SettingService.class);
    Mockito.doAnswer(invocation -> {
      settings.put(invocation.getArgument(2), ((SettingValue<?>) invocation.getArgument(3)).getValue());
      return null;
    }).when(settingService).set(any(), any(), anyString(), any());
    when(settingService.get(any(), any(), anyString())).thenAnswer(invocation -> {
      Object value = settings.get(invocation.getArgument(2));
      return value == null ? null : SettingValue.create(value.toString());
    });
    storage = new LoggingLevelStorage(settingService);
  }

  @Test
  void nothingStoredReadsAsNothing() {
    assertNull(storage.read());
    assertNull(storage.getVersion());
    assertTrue(storage.getOverrides().isEmpty());
  }

  @Test
  void theLevelsAndTheirVersionAreOneSettingRead() {
    Map<String, LoggingLevel> levels = new LinkedHashMap<>();
    levels.put("org.exoplatform.portal", LoggingLevel.DEBUG);
    levels.put("version", LoggingLevel.OFF); // a logger named like the first line
    levels.put("ROOT", LoggingLevel.WARN);

    String version = storage.saveOverrides(levels);

    assertEquals(1, settings.size()); // one write: a reader never sees a version with the levels of another write
    StoredLevels stored = storage.read();
    assertEquals(version, stored.version());
    assertEquals(levels, stored.overrides());
    assertEquals(List.of("org.exoplatform.portal", "version", "ROOT"), List.copyOf(stored.overrides().keySet()));
  }

  @Test
  void everyWriteChangesTheVersion() {
    String first = storage.saveOverrides(new LinkedHashMap<>());
    String second = storage.saveOverrides(new LinkedHashMap<>());

    assertNotEquals(first, second);
    assertEquals(second, storage.getVersion());
    assertTrue(storage.getOverrides().isEmpty());
  }

  @Test
  void aLineThatIsNotALevelIsSkipped() {
    settings.put("state", "version=v1\norg.a=DEBUG\norg.b=LOUD\nnot a line\n=INFO\norg.c=trace\n");

    StoredLevels stored = storage.read();

    assertEquals("v1", stored.version());
    assertEquals(List.of("org.a", "org.c"), List.copyOf(stored.overrides().keySet()));
    assertEquals(LoggingLevel.TRACE, stored.overrides().get("org.c"));
  }

  @Test
  void aStateWithoutAVersionLineIsNotRead() {
    settings.put("state", "org.a=DEBUG\n");

    assertNull(storage.read());
  }

  @Test
  void theTimeOfAChangeIsStoredWithItsLevel() {
    Map<String, LoggingLevel> levels = new LinkedHashMap<>();
    levels.put("org.a", LoggingLevel.DEBUG);
    levels.put("org.b", LoggingLevel.WARN);

    storage.saveOverrides(levels, Map.of("org.a", 1_732_540_000_000L));

    StoredLevels stored = storage.read();
    assertEquals(Map.of("org.a", 1_732_540_000_000L), stored.changedAt()); // org.b has no known time
    assertEquals(levels, stored.overrides());
  }

  @Test
  void aTimeThatIsNotANumberOrNotPositiveIsNoTime() {
    settings.put("state", "version=v1\norg.a=DEBUG@abc\norg.b=INFO@-5\norg.c=WARN@0\norg.d=ERROR@12\n");

    StoredLevels stored = storage.read();

    assertEquals(List.of("org.a", "org.b", "org.c", "org.d"), List.copyOf(stored.overrides().keySet()));
    assertEquals(Map.of("org.d", 12L), stored.changedAt());
  }

  @Test
  void aLevelWithoutATimeStillReadsAsBefore() {
    storage.saveOverrides(new LinkedHashMap<>(Map.of("org.a", LoggingLevel.INFO)));

    assertEquals(LoggingLevel.INFO, storage.read().overrides().get("org.a"));
    assertTrue(storage.read().changedAt().isEmpty());
  }
}
