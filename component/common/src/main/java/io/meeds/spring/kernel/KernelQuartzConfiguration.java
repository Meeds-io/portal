/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package io.meeds.spring.kernel;

import org.quartz.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import org.exoplatform.services.scheduler.impl.QuartzSheduler;

/**
 * A configuration setting for Spring in order to reuse the PortalContainer
 * Quartz Scheduler instance
 */
@Configuration
public class KernelQuartzConfiguration {

  @Bean
  @Primary
  public SchedulerFactoryBean quartzSchedulerFactoryBean(QuartzSheduler quartzSheduler) {
    return new SchedulerFactoryBean() {
      @Override
      public Scheduler getScheduler() {
        return quartzSheduler.getQuartzSheduler();
      }

      @Override
      public void afterPropertiesSet() {
        // no new Quartz scheduler creation
      }

      @Override
      public void start() {
        // Kernel owns startup
      }

      @Override
      public void destroy() {
        // Kernel owns shutdown
      }
    };
  }

  @Bean
  @Primary
  public Scheduler scheduler(QuartzSheduler quartzSheduler) {
    return quartzSheduler.getQuartzSheduler();
  }

}
