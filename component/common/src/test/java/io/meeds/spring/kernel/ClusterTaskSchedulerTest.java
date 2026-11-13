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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.quartz.CronExpression;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import org.exoplatform.services.scheduler.JobInfo;
import org.exoplatform.services.scheduler.JobSchedulerService;
import org.exoplatform.services.scheduler.PeriodInfo;

/**
 * A scheduled method that names the cluster scheduler becomes a job of the
 * kernel's job scheduler; any other one keeps running on its node.
 */
class ClusterTaskSchedulerTest {

  private static final String               DAILY  = Jobs.class.getName() + ".daily";

  private static final String               HOURLY     = Jobs.class.getName() + ".hourly";

  private static final String               TWO_HOURLY = Jobs.class.getName() + ".twoHourly";

  private AnnotationConfigApplicationContext context;

  @AfterEach
  void tearDown() {
    if (context != null) {
      context.close();
    }
    ClusterTaskScheduler.TASKS.clear();
  }

  @Test
  void aMethodNamingTheClusterSchedulerBecomesAJobAndAnyOtherRunsOnItsNode() throws Exception {
    JobSchedulerService jobSchedulerService = mock(JobSchedulerService.class);
    Jobs jobs = start(jobSchedulerService);

    ArgumentCaptor<JobInfo> daily = ArgumentCaptor.forClass(JobInfo.class);
    verify(jobSchedulerService).addCronJob(daily.capture(), eq("0 15 5 * * ?"), any());
    assertEquals(DAILY, daily.getValue().getJobName());
    assertEquals(JobSchedulerService.RUNTIME_JOB_GROUP, daily.getValue().getGroupName());
    assertEquals(ClusterTaskJob.class, daily.getValue().getJob());
    ArgumentCaptor<JobInfo> periodJobs = ArgumentCaptor.forClass(JobInfo.class);
    ArgumentCaptor<PeriodInfo> periods = ArgumentCaptor.forClass(PeriodInfo.class);
    verify(jobSchedulerService, times(2)).addPeriodJob(periodJobs.capture(), periods.capture(), any());
    // A fixed rate, and a fixed delay run as a fixed rate
    List<String> periodJobNames = periodJobs.getAllValues().stream().map(JobInfo::getJobName).toList();
    assertEquals(TimeUnit.HOURS.toMillis(1), periods.getAllValues().get(periodJobNames.indexOf(HOURLY)).getRepeatInterval());
    assertEquals(TimeUnit.HOURS.toMillis(2), periods.getAllValues().get(periodJobNames.indexOf(TWO_HOURLY)).getRepeatInterval());
    // A method that names no scheduler runs on this node, on the local scheduler
    assertTrue(jobs.local.await(10, TimeUnit.SECONDS));
    assertTrue(jobs.localThread.get().startsWith("scheduling-"), jobs.localThread.get());
    assertEquals(0, jobs.dailyRuns.get());

    // The job fires on this node
    new ClusterTaskJob().execute(jobContext(DAILY, new JobDataMap(), mock(Scheduler.class)));

    assertEquals(1, jobs.dailyRuns.get());
  }

  @Test
  void aClosedContextNoLongerRunsItsMethods() throws Exception {
    start(mock(JobSchedulerService.class));
    assertTrue(ClusterTaskScheduler.TASKS.containsKey(DAILY));

    context.close();

    assertFalse(ClusterTaskScheduler.TASKS.containsKey(DAILY));
    assertFalse(ClusterTaskScheduler.TASKS.containsKey(HOURLY));
  }

  @Test
  void aRunThisNodeCannotDoIsFiredAgainOnce() throws Exception {
    Scheduler scheduler = mock(Scheduler.class);

    new ClusterTaskJob().execute(jobContext(DAILY, new JobDataMap(), scheduler));

    ArgumentCaptor<Trigger> retry = ArgumentCaptor.forClass(Trigger.class);
    verify(scheduler).scheduleJob(retry.capture());
    assertEquals(new JobKey(DAILY, "portal:runtime"), retry.getValue().getJobKey());
    // A string, which a store keeping the job data as properties accepts
    assertTrue(retry.getValue().getJobDataMap().get(ClusterTaskJob.RETRY_KEY) instanceof String);
    assertTrue(retry.getValue().getStartTime().getTime() > System.currentTimeMillis() + 50_000);

    // Fired again, with no node that declares the method
    Scheduler again = mock(Scheduler.class);
    new ClusterTaskJob().execute(jobContext(DAILY, retry.getValue().getJobDataMap(), again));

    verify(again, never()).scheduleJob(any(Trigger.class));
  }

  @Test
  void aSpringCronBecomesAQuartzCron() {
    assertEquals("0 15 5 * * ?", ClusterTaskScheduler.toQuartzCron("0 15 5 * * *"));
    assertEquals("0 0 9 ? * MON-FRI", ClusterTaskScheduler.toQuartzCron("0 0 9 * * MON-FRI"));
    assertEquals("0 0 1 1 * ?", ClusterTaskScheduler.toQuartzCron("0 0 1 1 * ?"));
    assertEquals("0 15 5 ? * *", ClusterTaskScheduler.toQuartzCron("0 15 5 ? * *"));
    // Spring and Quartz number the days of the week differently
    assertThrows(IllegalArgumentException.class, () -> ClusterTaskScheduler.toQuartzCron("0 0 9 * * 1-5"));
    assertThrows(IllegalArgumentException.class, () -> ClusterTaskScheduler.toQuartzCron("0 0 9 1 * MON"));
  }

  @Test
  void aTaskThatDoesNotNameTheClusterSchedulerIsRefused() {
    ClusterTaskScheduler scheduler = new ClusterTaskScheduler(() -> mock(JobSchedulerService.class));

    assertThrows(IllegalArgumentException.class,
                 () -> scheduler.scheduleAtFixedRate(() -> {
                 }, java.time.Duration.ofMinutes(1)));
    SchedulingAwareRunnable otherScheduler = new SchedulingAwareRunnable() {
      @Override
      public void run() {
        // Never run
      }

      @Override
      public String getQualifier() {
        return "otherScheduler";
      }
    };
    assertThrows(IllegalArgumentException.class,
                 () -> scheduler.scheduleAtFixedRate(otherScheduler, java.time.Duration.ofMinutes(1)));
    assertTrue(ClusterTaskScheduler.TASKS.isEmpty());
  }

  @Test
  void aContextWithoutTheJobSchedulerStartsUnlessAMethodNamesTheClusterScheduler() {
    context = new AnnotationConfigApplicationContext();
    context.register(KernelSchedulingConfiguration.class, SchedulingConfiguration.class, LocalJobs.class);
    context.refresh();
    assertTrue(context.getBean(ClusterTaskScheduler.NAME) instanceof ClusterTaskScheduler);
    context.close();

    context = new AnnotationConfigApplicationContext();
    context.register(KernelSchedulingConfiguration.class, SchedulingConfiguration.class, Jobs.class);
    Exception e = assertThrows(Exception.class, context::refresh);
    assertTrue(rootCause(e) instanceof IllegalStateException, String.valueOf(rootCause(e)));
    assertTrue(ClusterTaskScheduler.TASKS.isEmpty());
  }

  private static Throwable rootCause(Throwable e) {
    Throwable cause = e;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    return cause;
  }

  @Test
  void aFailedRunIsReportedAndNotFiredAgainAtOnce() {
    // One run at a time across the cluster
    assertTrue(ClusterTaskJob.class.isAnnotationPresent(DisallowConcurrentExecution.class));
    ClusterTaskScheduler.TASKS.put(DAILY, () -> {
      throw new IllegalStateException("failed");
    });

    JobExecutionException e = assertThrows(JobExecutionException.class,
                                           () -> new ClusterTaskJob().execute(jobContext(DAILY,
                                                                                         new JobDataMap(),
                                                                                         mock(Scheduler.class))));

    assertFalse(e.refireImmediately());
    assertTrue(e.getCause() instanceof IllegalStateException);
  }

  @Test
  void aCronWithItsOwnZoneAndAOneTimeTaskAreRefused() {
    context = new AnnotationConfigApplicationContext();
    context.registerBean(JobSchedulerService.class, () -> mock(JobSchedulerService.class));
    context.register(KernelSchedulingConfiguration.class, SchedulingConfiguration.class, ZonedJobs.class);
    Exception e = assertThrows(Exception.class, context::refresh);
    assertTrue(rootCause(e) instanceof IllegalArgumentException, String.valueOf(rootCause(e)));
    assertTrue(ClusterTaskScheduler.TASKS.isEmpty());

    ClusterTaskScheduler scheduler = new ClusterTaskScheduler(() -> mock(JobSchedulerService.class));
    assertThrows(UnsupportedOperationException.class, () -> scheduler.schedule(() -> {
    }, Instant.now()));
  }

  @Test
  void aMethodRegisteredTwiceUnderOneNameIsRefused() {
    ClusterTaskScheduler scheduler = new ClusterTaskScheduler(() -> mock(JobSchedulerService.class));
    NamedTask first = new NamedTask();
    scheduler.scheduleAtFixedRate(first, java.time.Duration.ofMinutes(1));

    assertThrows(IllegalStateException.class,
                 () -> scheduler.scheduleAtFixedRate(new NamedTask(), java.time.Duration.ofMinutes(1)));
    assertTrue(ClusterTaskScheduler.TASKS.get(NamedTask.NAME) == first);
  }

  @Test
  void aConvertedCronIsValidForQuartzAndFiresWhenSpringWould() throws Exception {
    ZoneId zone = ZoneId.systemDefault();
    for (String expression : List.of("0 15 5 * * *",
                                     "0 15 5 ? * *",
                                     "0 15 23 ? * *",
                                     "0 0 9 * * MON-FRI",
                                     "0 0 1 1 * ?",
                                     "15 */2 * * * ?",
                                     "0 */5 * * * *",
                                     "0 * * * * ?")) {
      String quartzExpression = ClusterTaskScheduler.toQuartzCron(expression);
      assertTrue(CronExpression.isValidExpression(quartzExpression), quartzExpression);
      CronExpression quartz = new CronExpression(quartzExpression);
      quartz.setTimeZone(java.util.TimeZone.getTimeZone(zone));
      org.springframework.scheduling.support.CronExpression spring =
                                                                   org.springframework.scheduling.support.CronExpression.parse(expression);
      ZonedDateTime springTime = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, zone);
      Date quartzTime = Date.from(springTime.toInstant());
      for (int i = 0; i < 10; i++) {
        springTime = spring.next(springTime);
        quartzTime = quartz.getNextValidTimeAfter(quartzTime);
        assertEquals(springTime.toInstant(), quartzTime.toInstant(), expression);
      }
    }
  }

  private Jobs start(JobSchedulerService jobSchedulerService) {
    context = new AnnotationConfigApplicationContext();
    context.registerBean(JobSchedulerService.class, () -> jobSchedulerService);
    context.register(KernelSchedulingConfiguration.class, SchedulingConfiguration.class, Jobs.class);
    context.refresh();
    return context.getBean(Jobs.class);
  }

  private static JobExecutionContext jobContext(String task, JobDataMap triggerData, Scheduler scheduler) {
    JobExecutionContext jobContext = mock(JobExecutionContext.class);
    JobDetail jobDetail = mock(JobDetail.class);
    Trigger trigger = mock(Trigger.class);
    JobDataMap merged = new JobDataMap(triggerData);
    merged.put(ClusterTaskScheduler.TASK_KEY, task);
    when(jobContext.getMergedJobDataMap()).thenReturn(merged);
    when(jobContext.getJobDetail()).thenReturn(jobDetail);
    when(jobDetail.getKey()).thenReturn(new JobKey(task, "portal:runtime"));
    when(jobContext.getTrigger()).thenReturn(trigger);
    when(trigger.getJobDataMap()).thenReturn(triggerData);
    when(jobContext.getScheduler()).thenReturn(scheduler);
    return jobContext;
  }

  @Configuration
  @EnableScheduling
  static class SchedulingConfiguration {
  }

  static class ZonedJobs {

    @Scheduled(cron = "0 15 5 * * *", zone = "Pacific/Auckland", scheduler = ClusterTaskScheduler.NAME)
    public void zoned() {
      // Refused
    }
  }

  static class NamedTask implements SchedulingAwareRunnable {

    static final String NAME = "io.meeds.Example.run";

    @Override
    public void run() {
      // Registered only
    }

    @Override
    public String getQualifier() {
      return ClusterTaskScheduler.NAME;
    }

    @Override
    public String toString() {
      return NAME;
    }
  }

  static class LocalJobs {

    @Scheduled(fixedDelay = 60_000)
    public void local() {
      // Runs on this node
    }
  }

  static class Jobs {

    final AtomicInteger           dailyRuns   = new AtomicInteger();

    final CountDownLatch          local       = new CountDownLatch(1);

    final AtomicReference<String> localThread = new AtomicReference<>();

    @Scheduled(cron = "0 15 5 * * *", scheduler = ClusterTaskScheduler.NAME)
    public void daily() {
      dailyRuns.incrementAndGet();
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS, scheduler = ClusterTaskScheduler.NAME)
    public void hourly() {
      // Registered only
    }

    @Scheduled(fixedDelay = 2, timeUnit = TimeUnit.HOURS, scheduler = ClusterTaskScheduler.NAME)
    public void twoHourly() {
      // Registered only
    }

    @Scheduled(fixedDelay = 50)
    public void local() {
      localThread.compareAndSet(null, Thread.currentThread().getName());
      local.countDown();
    }
  }

}
