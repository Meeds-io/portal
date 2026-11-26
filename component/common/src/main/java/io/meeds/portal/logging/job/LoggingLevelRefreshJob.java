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
package io.meeds.portal.logging.job;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.meeds.portal.logging.service.LoggingLevelService;

import lombok.RequiredArgsConstructor;

/**
 * Makes each node apply what another node changed, the levels again after a
 * reload of {@code logback.xml}, and the changes that are due to be given back.
 * It is not a cluster task: the unqualified {@code @Scheduled} runs on every
 * node, which is what the propagation needs.
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
public class LoggingLevelRefreshJob {

  private final LoggingLevelService loggingLevelService;

  @Scheduled(fixedDelayString = "${meeds.logging.refreshIntervalMs:10000}",
             initialDelayString = "${meeds.logging.refreshIntervalMs:10000}")
  public void refresh() {
    loggingLevelService.refresh();
  }

}
