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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TaskSchedulerRouter;

import org.exoplatform.services.scheduler.JobSchedulerService;

/**
 * Routes a scheduled method that names {@link ClusterTaskScheduler#NAME} to the
 * {@link ClusterTaskScheduler}, which runs it once per cluster. Every other
 * scheduled method runs as Spring routes it, on each node: on the scheduler
 * Spring Boot builds for the context ({@code spring.task.scheduling.*}), never
 * on the kernel's job scheduler.
 * <p>
 * The cluster scheduler is not a bean: Spring Boot builds its own scheduler,
 * and its own task executor, only when the context holds no bean of their
 * kind, so a scheduler bean declared here would take them from every context.
 */
@Configuration
public class KernelSchedulingConfiguration implements SchedulingConfigurer, BeanFactoryAware, DisposableBean {

  private BeanFactory           beanFactory;

  private ClusterSchedulerRouter router;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar registrar) {
    ClusterTaskScheduler clusterScheduler =
                                          new ClusterTaskScheduler(() -> beanFactory.getBeanProvider(JobSchedulerService.class)
                                                                                    .getIfAvailable());
    router = new ClusterSchedulerRouter(clusterScheduler);
    router.setBeanFactory(beanFactory);
    registrar.setTaskScheduler(router);
  }

  @Override
  public void destroy() {
    if (router != null) {
      router.destroy();
    }
  }

  /**
   * Spring's router of the scheduled methods, which resolves the cluster
   * scheduler by its name.
   */
  static class ClusterSchedulerRouter extends TaskSchedulerRouter {

    private final TaskScheduler clusterScheduler;

    ClusterSchedulerRouter(TaskScheduler clusterScheduler) {
      this.clusterScheduler = clusterScheduler;
    }

    @Override
    protected TaskScheduler determineQualifiedScheduler(String qualifier) {
      return ClusterTaskScheduler.NAME.equals(qualifier) ? clusterScheduler : super.determineQualifiedScheduler(qualifier);
    }
  }

}
