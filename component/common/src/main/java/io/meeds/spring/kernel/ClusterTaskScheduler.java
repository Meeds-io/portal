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

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.quartz.JobDataMap;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;

import org.exoplatform.services.scheduler.JobInfo;
import org.exoplatform.services.scheduler.JobSchedulerService;
import org.exoplatform.services.scheduler.PeriodInfo;

/**
 * Runs a scheduled method once per cluster. The method names this scheduler,
 * {@code @Scheduled(cron = "...", scheduler = ClusterTaskScheduler.NAME)}, and
 * the scheduler registers it as a job of the kernel's job scheduler, whose
 * store the nodes of a cluster share: every node registers the same job, the
 * store fires each of its triggers on one node, and that node runs its own
 * copy of the method. A scheduled method that names no scheduler runs on every
 * node.
 * <p>
 * The job is named after the method, so that every node registers the same
 * one, and belongs to {@link JobSchedulerService#RUNTIME_JOB_GROUP}, which the
 * job scheduler keeps when a node restarts. A cron expression is read as Spring
 * reads it; a fixed delay runs at a fixed rate, one run at a time.
 */
public class ClusterTaskScheduler implements TaskScheduler {

  /** The name of the scheduler, which a scheduled method gives as its scheduler. */
  public static final String         NAME     = "clusterScheduler";

  /** The scheduled methods of this node, by the name of their job. */
  static final Map<String, Runnable> TASKS    = new ConcurrentHashMap<>();

  /** The key of the job's data that names its scheduled method. */
  static final String                TASK_KEY = "task";

  private final Supplier<JobSchedulerService> jobSchedulerService;

  /**
   * @param jobSchedulerService the kernel's job scheduler, looked up when a
   *          method is registered, so that a context with no scheduled method
   *          naming this scheduler starts without it
   */
  public ClusterTaskScheduler(Supplier<JobSchedulerService> jobSchedulerService) {
    this.jobSchedulerService = jobSchedulerService;
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
    if (!(trigger instanceof CronTrigger cronTrigger)) {
      throw new IllegalArgumentException("The scheduler '" + NAME + "' runs a cron or a fixed rate only, not " + trigger);
    }
    if (getZone(cronTrigger) != null) {
      throw new IllegalArgumentException("The scheduler '" + NAME + "' reads a cron expression in the server's time zone: "
          + task + " sets another one");
    }
    String name = register(task);
    try {
      getJobSchedulerService().addCronJob(jobInfo(name), toQuartzCron(cronTrigger.getExpression()), jobData(name));
    } catch (Exception e) {
      TASKS.remove(name, task);
      throw new IllegalStateException("Cannot register the scheduled method " + name, e);
    }
    return new ClusterTask(name, task);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
    String name = register(task);
    try {
      getJobSchedulerService().addPeriodJob(jobInfo(name),
                                       new PeriodInfo(Date.from(startTime), null, 0, period.toMillis()),
                                       jobData(name));
    } catch (Exception e) {
      TASKS.remove(name, task);
      throw new IllegalStateException("Cannot register the scheduled method " + name, e);
    }
    return new ClusterTask(name, task);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
    return scheduleAtFixedRate(task, getClock().instant(), period);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
    // The job runs one at a time: a fixed rate never starts a run while the
    // previous one runs
    return scheduleAtFixedRate(task, startTime, delay);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
    return scheduleAtFixedRate(task, getClock().instant(), delay);
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
    throw new UnsupportedOperationException("The scheduler '" + NAME + "' runs scheduled methods only");
  }

  /**
   * Converts a Spring cron expression to Quartz's, which wants a {@code ?} in
   * the day of the month or the day of the week. Spring numbers the days of the
   * week from Monday, Quartz from Sunday: a day of the week is written with its
   * name.
   *
   * @param expression a Spring cron expression, of six fields
   * @return the Quartz cron expression
   */
  static String toQuartzCron(String expression) {
    String[] fields = expression.trim().split("\\s+");
    if (fields.length != 6) {
      throw new IllegalArgumentException("A cron expression of six fields is expected: " + expression);
    }
    String dayOfMonth = fields[3];
    String dayOfWeek = fields[5];
    if (!"?".equals(dayOfWeek) && dayOfWeek.matches(".*\\d.*")) {
      throw new IllegalArgumentException("Write the days of the week by name (MON-FRI), which Spring and Quartz number differently: "
          + expression);
    }
    if (!"?".equals(dayOfMonth) && !"?".equals(dayOfWeek)) {
      if ("*".equals(dayOfWeek)) {
        fields[5] = "?";
      } else if ("*".equals(dayOfMonth)) {
        fields[3] = "?";
      } else {
        throw new IllegalArgumentException("Quartz cannot restrict both the day of the month and the day of the week: "
            + expression);
      }
    }
    return String.join(" ", fields);
  }

  private String register(Runnable task) {
    if (!(task instanceof SchedulingAwareRunnable schedulingAwareTask) || !NAME.equals(schedulingAwareTask.getQualifier())) {
      throw new IllegalArgumentException("The scheduler '" + NAME + "' runs the scheduled methods that name it only: " + task);
    }
    // The qualified name of the method, the same on every node
    String name = task.toString();
    TASKS.put(name, task);
    return name;
  }

  private JobSchedulerService getJobSchedulerService() {
    JobSchedulerService service = jobSchedulerService.get();
    if (service == null) {
      throw new IllegalStateException("The scheduler '" + NAME + "' needs the kernel's job scheduler, which this context has not");
    }
    return service;
  }

  private static JobInfo jobInfo(String name) {
    return new JobInfo(name, JobSchedulerService.RUNTIME_JOB_GROUP, ClusterTaskJob.class);
  }

  private static JobDataMap jobData(String name) {
    JobDataMap jobData = new JobDataMap();
    jobData.put(TASK_KEY, name);
    return jobData;
  }

  private static Object getZone(CronTrigger trigger) {
    try {
      Field zone = CronTrigger.class.getDeclaredField("zoneId");
      zone.setAccessible(true); // NOSONAR
      return zone.get(trigger);
    } catch (ReflectiveOperationException | RuntimeException e) {
      return null;
    }
  }

  /**
   * A scheduled method registered by this node. Cancelling it stops this node
   * from running it; the other nodes keep running the job.
   */
  static class ClusterTask implements ScheduledFuture<Object> {

    private final String   name;

    private final Runnable task;

    private volatile boolean cancelled;

    ClusterTask(String name, Runnable task) {
      this.name = name;
      this.task = task;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      cancelled = TASKS.remove(name, task) || cancelled;
      return cancelled;
    }

    @Override
    public boolean isCancelled() {
      return cancelled;
    }

    @Override
    public boolean isDone() {
      return cancelled;
    }

    @Override
    public Object get() {
      throw new UnsupportedOperationException("A scheduled method runs until it is cancelled");
    }

    @Override
    public Object get(long timeout, TimeUnit unit) {
      return get();
    }

    @Override
    public long getDelay(TimeUnit unit) {
      return 0;
    }

    @Override
    public int compareTo(Delayed other) {
      return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
    }
  }

}
