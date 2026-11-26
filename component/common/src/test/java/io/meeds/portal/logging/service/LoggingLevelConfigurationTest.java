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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoggingLevelConfigurationTest {

  @Test
  void theFlagsAndTheRoleAreTheOnesOfTheProperties() {
    LoggingLevelConfiguration configuration = new LoggingLevelConfiguration(false, true, "loggingAdmins", 0);

    assertFalse(configuration.isEnabled());
    assertTrue(configuration.isReadOnly());
    assertArrayEquals(new String[] { "loggingAdmins" }, configuration.getRoles());
  }

  @Test
  void rolesAreSeparatedByCommasAndTrimmed() {
    assertArrayEquals(new String[] { "administrators", "loggingAdmins", "ops" },
                      new LoggingLevelConfiguration(true, false, " administrators , loggingAdmins,, ops ,", 0).getRoles());
  }

  @Test
  void aListWithNoRoleWouldLockEverybodyOutSoTheDefaultApplies() {
    assertArrayEquals(new String[] { "administrators" }, new LoggingLevelConfiguration(true, false, "", 0).getRoles());
    assertArrayEquals(new String[] { "administrators" }, new LoggingLevelConfiguration(true, false, " , ,", 0).getRoles());
    assertArrayEquals(new String[] { "administrators" }, new LoggingLevelConfiguration(true, false, null, 0).getRoles());
  }

  @Test
  void theDelayToGiveAChangeBackIsOffByDefaultAndNeverNegative() {
    assertEquals(0, new LoggingLevelConfiguration(true, false, "administrators", 0).getRevertAfterMinutes());
    assertEquals(0, new LoggingLevelConfiguration(true, false, "administrators", -5).getRevertAfterMinutes());
    assertEquals(60, new LoggingLevelConfiguration(true, false, "administrators", 60).getRevertAfterMinutes());
  }

  @Test
  void theRolesCannotBeChangedFromOutside() {
    LoggingLevelConfiguration configuration = new LoggingLevelConfiguration(true, false, "administrators", 0);

    configuration.getRoles()[0] = "everybody";

    assertArrayEquals(new String[] { "administrators" }, configuration.getRoles());
  }

}
