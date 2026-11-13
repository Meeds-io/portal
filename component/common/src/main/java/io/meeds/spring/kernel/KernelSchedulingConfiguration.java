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
package io.meeds.spring.kernel;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import org.exoplatform.services.scheduler.JobSchedulerService;

/**
 * The schedulers of the scheduled methods: {@link ClusterTaskScheduler} for a
 * method that names it, which runs once per cluster, and the local scheduler
 * every other method runs on, on each node.
 * <p>
 * Spring runs a method that names no scheduler on the bean named
 * {@value #LOCAL_SCHEDULER} when the context holds several schedulers, and
 * Spring Boot builds its own local scheduler only when the context holds none:
 * the local one is therefore declared here, as Spring Boot would build it.
 */
@Configuration
public class KernelSchedulingConfiguration {

  /** The name Spring looks up for the scheduler of a method that names none. */
  public static final String LOCAL_SCHEDULER = "taskScheduler";

  @Bean(ClusterTaskScheduler.NAME)
  public TaskScheduler clusterScheduler(ObjectProvider<JobSchedulerService> jobSchedulerService) {
    return new ClusterTaskScheduler(jobSchedulerService::getIfAvailable);
  }

  @Bean(LOCAL_SCHEDULER)
  public ThreadPoolTaskScheduler taskScheduler(@Value("${spring.task.scheduling.pool.size:1}") int poolSize,
                                               @Value("${spring.task.scheduling.thread-name-prefix:scheduling-}") String threadNamePrefix) {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(poolSize);
    scheduler.setThreadNamePrefix(threadNamePrefix);
    return scheduler;
  }

}
