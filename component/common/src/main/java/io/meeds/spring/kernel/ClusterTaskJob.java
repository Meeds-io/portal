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

import java.util.Date;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The job of a scheduled method that {@link ClusterTaskScheduler} runs once
 * per cluster: it runs the method on the node its trigger fires on, one run at
 * a time across the cluster.
 * <p>
 * A node can fire it before its own Spring contexts registered the method, as
 * it starts, or after they closed, as it stops: the run is then fired again
 * once, {@link #RETRY_DELAY} seconds later, on whichever node takes it. A
 * method no node declares any more is reported at each of its runs, until its
 * job is removed.
 */
@DisallowConcurrentExecution
public class ClusterTaskJob implements Job {

  /** Delay, in seconds, before a run this node could not do is fired again. */
  static final long        RETRY_DELAY = 60;

  /** The key of the trigger's data that marks a run fired again. */
  static final String      RETRY_KEY   = "retry";

  private static final Log LOG         = ExoLogger.getLogger(ClusterTaskJob.class);

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    String name = context.getMergedJobDataMap().getString(ClusterTaskScheduler.TASK_KEY);
    Runnable task = name == null ? null : ClusterTaskScheduler.TASKS.get(name);
    if (task != null) {
      try {
        task.run();
      } catch (RuntimeException e) {
        throw new JobExecutionException("The scheduled method " + name + " failed", e, false);
      }
    } else if (context.getTrigger().getJobDataMap().containsKey(RETRY_KEY)) {
      LOG.warn("The scheduled method {} of job {} was not run: no node that took it declares it. A method removed from the code keeps its job until the job is removed",
               name,
               context.getJobDetail().getKey());
    } else {
      fireAgain(context);
    }
  }

  private void fireAgain(JobExecutionContext context) throws JobExecutionException {
    JobKey jobKey = context.getJobDetail().getKey();
    try {
      context.getScheduler()
             .scheduleJob(TriggerBuilder.newTrigger()
                                        .forJob(jobKey)
                                        .usingJobData(RETRY_KEY, Boolean.TRUE)
                                        .startAt(new Date(System.currentTimeMillis() + RETRY_DELAY * 1000))
                                        .build());
    } catch (SchedulerException e) {
      throw new JobExecutionException("Cannot fire the job " + jobKey + " again", e, false);
    }
  }

}
