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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;

import lombok.RequiredArgsConstructor;

/**
 * Tells {@link LoggingLevelService} that logback reset its levels, which a
 * reload of {@code logback.xml} does before it reads the file again. It only
 * runs a callback: logback calls it while it holds its own locks.
 */
@RequiredArgsConstructor
public class LoggerContextResetListener implements LoggerContextListener {

  private final Runnable onReset;

  @Override
  public boolean isResetResistant() {
    // a listener that does not resist is removed by the reset it reports
    return true;
  }

  @Override
  public void onReset(LoggerContext context) {
    onReset.run();
  }

  @Override
  public void onStart(LoggerContext context) {
    // nothing to do
  }

  @Override
  public void onStop(LoggerContext context) {
    // nothing to do
  }

  @Override
  public void onLevelChange(Logger logger, Level level) {
    // nothing to do
  }

}
