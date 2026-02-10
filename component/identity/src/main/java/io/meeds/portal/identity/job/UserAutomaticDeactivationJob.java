/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.portal.identity.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import org.exoplatform.services.organization.OrganizationService;

import io.meeds.common.ContainerTransactional;

import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled job responsible for automatically deactivating inactive users.
 * <p>
 * This job periodically scans enabled user accounts and automatically disables
 * those that have not logged into the platform for a configurable number of
 * days.
 * </p>
 * <p>
 * The inactivity threshold is configurable using the property
 * {@code social.user.inactive.period.days} (default value: 180 days).
 * </p>
 * <p>
 * The job execution schedule is configurable using the cron expression
 * {@code social.UserAutomaticDeactivationJob.expression}. By default, the job
 * is executed once per day.
 * </p>
 * <p>
 * Users deactivated by this job are flagged as automatically deactivated in
 * order to distinguish them from users manually disabled by administrators.
 * </p>
 */
@Configuration
@EnableScheduling
@Slf4j
public class UserAutomaticDeactivationJob {

  @Autowired
  private OrganizationService organizationService;

  @Value("${social.user.inactive.period.days:180}")
  private int                 inactiveDays;

  @Value("${social.user.inactive.groupId:}")
  private String              groupId;

  @Scheduled(cron = "${social.UserAutomaticDeactivationJob.expression:0 15 23 ? * *}")
  @ContainerTransactional
  public void deactivateUsersInactive() {
    log.info("Starting automatic user deactivation job (inactiveDays={})", inactiveDays);
    try {
      int deactivatedCount = organizationService.getUserHandler()
                                                .disableInactiveUsers(groupId, inactiveDays);
      if (deactivatedCount != 0) {
        log.info("Automatic user deactivation job finished. {} users disabled",
                 deactivatedCount);
      }
    } catch (Exception e) {
      log.error("Error while executing automatic user deactivation job", e);
    }
  }

}
