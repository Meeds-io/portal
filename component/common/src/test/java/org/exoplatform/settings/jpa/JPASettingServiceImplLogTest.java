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
package org.exoplatform.settings.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.event.impl.EventManagerImpl;
import org.exoplatform.commons.api.persistence.DataInitializer;

/**
 * A setting's value never reaches the logs of the settings service.
 */
public class JPASettingServiceImplLogTest {

  private static final String SECRET = "the-setting-value";

  private Logger                  logger;

  private Level                   previousLevel;

  private ListAppender<ILoggingEvent> appender;

  private SettingContextDAO       contextDAO;

  private JPASettingServiceImpl   service;

  @Before
  public void setUp() {
    logger = (Logger) LoggerFactory.getLogger(JPASettingServiceImpl.class.getName());
    previousLevel = logger.getLevel();
    logger.setLevel(Level.DEBUG);
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    contextDAO = mock(SettingContextDAO.class);
    service = new JPASettingServiceImpl(mock(SettingsDAO.class),
                                        contextDAO,
                                        mock(SettingScopeDAO.class),
                                        mock(EventManagerImpl.class),
                                        mock(DataInitializer.class));
  }

  @After
  public void tearDown() {
    logger.detachAppender(appender);
    logger.setLevel(previousLevel);
  }

  @Test
  public void testASaveDoesNotLogTheValue() {
    when(contextDAO.getContextByTypeAndName(anyString(), anyString())).thenReturn(null);
    when(contextDAO.create(any())).thenThrow(new IllegalStateException("stop"));

    service.set(Context.USER.id("john"), Scope.APPLICATION.id("app"), "key", SettingValue.create(SECRET));

    assertTrue(appender.list.stream().anyMatch(e -> e.getLevel() == Level.DEBUG));
    assertNoLogCarriesTheValue();
  }

  @Test
  public void testAFailedSaveDoesNotLogTheValue() {
    when(contextDAO.getContextByTypeAndName(anyString(), anyString())).thenThrow(new IllegalStateException("store down"));

    service.set(Context.USER.id("john"), Scope.APPLICATION.id("app"), "key", SettingValue.create(SECRET));

    assertTrue(appender.list.stream().anyMatch(e -> e.getLevel() == Level.ERROR));
    assertNoLogCarriesTheValue();
  }

  private void assertNoLogCarriesTheValue() {
    assertFalse(appender.list.isEmpty());
    appender.list.forEach(event -> {
      assertFalse(event.getFormattedMessage(), event.getFormattedMessage().contains(SECRET));
      assertEquals(0, java.util.Arrays.stream(event.getArgumentArray() == null ? new Object[0] : event.getArgumentArray())
                                      .filter(a -> SECRET.equals(String.valueOf(a)))
                                      .count());
    });
  }

}
