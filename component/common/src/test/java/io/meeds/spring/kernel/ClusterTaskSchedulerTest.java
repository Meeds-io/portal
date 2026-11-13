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
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

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
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

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

  private static final int                  POOL_SIZE  = 5;

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
    // A method that names no scheduler runs on this node, on the scheduler Spring Boot builds
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
    assertEquals("0 0 9 ? * MON,TUE,WED,THU,FRI", ClusterTaskScheduler.toQuartzCron("0 0 9 * * MON-FRI"));
    assertEquals("0 0 9 ? * SUN,MON,TUE,WED", ClusterTaskScheduler.toQuartzCron("0 0 9 * * 7-3"));
    assertEquals("0 0 9 ? * SUN,MON,TUE,WED,THU,FRI,SAT", ClusterTaskScheduler.toQuartzCron("0 0 9 * * SUN-SUN"));
    assertEquals("0 0 1 1 * ?", ClusterTaskScheduler.toQuartzCron("0 0 1 1 * ?"));
    assertEquals("0 15 5 ? * *", ClusterTaskScheduler.toQuartzCron("0 15 5 ? * *"));
    // Spring and Quartz number the days of the week differently: written by name
    assertEquals("0 0 9 ? * MON,TUE,WED,THU,FRI", ClusterTaskScheduler.toQuartzCron("0 0 9 * * 1-5"));
    assertEquals("0 0 9 ? * FRI#2", ClusterTaskScheduler.toQuartzCron("0 0 9 * * 5#2"));
    assertEquals("0 0 0 ? * SUN", ClusterTaskScheduler.toQuartzCron("@weekly"));
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
    Logger jobLogger = (Logger) LoggerFactory.getLogger(ClusterTaskJob.class);
    ListAppender<ILoggingEvent> failures = new ListAppender<>();
    failures.start();
    jobLogger.addAppender(failures);
    try {
      runAFailingMethod(failures);
    } finally {
      jobLogger.detachAppender(failures);
    }
  }

  private void runAFailingMethod(ListAppender<ILoggingEvent> failures) {
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
    // Logged as an error, which Quartz alone would log at INFO
    assertTrue(failures.list.stream()
                            .anyMatch(event -> event.getLevel() == Level.ERROR && event.getFormattedMessage().contains(DAILY)
                                && event.getThrowableProxy() != null),
               String.valueOf(failures.list));
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
    for (String expression : List.of("0 15 5 * * 1",
                                     "0 0 9 * * 1-5",
                                     "0 0 9 * * 0",
                                     "0 0 9 * * 7",
                                     "0 0 9 * * 5-7",
                                     "0 0 9 * * 7-3",
                                     "0 0 9 * * 7-7",
                                     "0 0 9 * * 0-3",
                                     "0 0 9 * * SUN-WED",
                                     "0 0 9 * * SUN-SAT/2",
                                     "0 0 9 * * SUN-SUN",
                                     "0 0 9 * * 7/2",
                                     "0 0 9 * * SUN/2",
                                     "0 0 9 * * */2",
                                     "0 0 9 * * 1/3",
                                     "0 0 9 * * MON/2",
                                     "0 0 9 * * 1,3,SAT",
                                     "0 0 9 ? * 5#2",
                                     "0 0 9 ? * FRI#2",
                                     "0 0 9 * * 5L",
                                     "@yearly",
                                     "@annually",
                                     "@monthly",
                                     "@weekly",
                                     "@daily",
                                     "@midnight",
                                     "@hourly",
                                     "0 15 5 * * *",
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

  @Test
  void aCronQuartzRejectsIsRefusedNamingItsMethod() {
    context = new AnnotationConfigApplicationContext();
    context.registerBean(JobSchedulerService.class, () -> mock(JobSchedulerService.class));
    context.register(KernelSchedulingConfiguration.class, SchedulingConfiguration.class, InvalidJobs.class);

    Exception e = assertThrows(Exception.class, context::refresh);

    Throwable cause = rootCause(e);
    assertTrue(cause instanceof IllegalArgumentException, String.valueOf(cause));
    assertTrue(cause.getMessage().contains(InvalidJobs.class.getName() + ".everyNinetySeconds"), cause.getMessage());
    assertTrue(ClusterTaskScheduler.TASKS.isEmpty());
  }

  @Test
  void aCronQuartzCannotWriteIsRefusedNamingItsMethod() {
    context = new AnnotationConfigApplicationContext();
    context.registerBean(JobSchedulerService.class, () -> mock(JobSchedulerService.class));
    context.register(KernelSchedulingConfiguration.class, SchedulingConfiguration.class, BothDaysJobs.class);

    Exception e = assertThrows(Exception.class, context::refresh);

    Throwable cause = rootCause(e);
    assertTrue(cause instanceof IllegalArgumentException, String.valueOf(cause));
    assertTrue(cause.getMessage().contains(BothDaysJobs.class.getName() + ".firstMondays"), cause.getMessage());
    assertTrue(ClusterTaskScheduler.TASKS.isEmpty());
  }

  @Test
  void springBootKeepsItsSchedulerAndItsTaskExecutor() {
    start(mock(JobSchedulerService.class));

    // Built by Spring Boot, which builds them only when the context holds none
    assertTrue(context.getBean("applicationTaskExecutor") instanceof java.util.concurrent.Executor);
    assertEquals(0, context.getBeansOfType(ClusterTaskScheduler.class).size());
    ThreadPoolTaskScheduler taskScheduler = context.getBean("taskScheduler", ThreadPoolTaskScheduler.class);
    assertEquals(POOL_SIZE, taskScheduler.getScheduledThreadPoolExecutor().getCorePoolSize());
  }

  private Jobs start(JobSchedulerService jobSchedulerService) {
    context = new AnnotationConfigApplicationContext();
    context.getEnvironment()
           .getPropertySources()
           .addFirst(new MapPropertySource("test", Map.of("spring.task.scheduling.pool.size", POOL_SIZE)));
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
  @ImportAutoConfiguration({ TaskExecutionAutoConfiguration.class, TaskSchedulingAutoConfiguration.class })
  static class SchedulingConfiguration {
  }

  static class InvalidJobs {

    @Scheduled(cron = "*/90 * * * * *", scheduler = ClusterTaskScheduler.NAME)
    public void everyNinetySeconds() {
      // Refused
    }
  }

  static class BothDaysJobs {

    // Restricts both the day of the month and the day of the week, which a
    // Quartz expression cannot
    @Scheduled(cron = "0 0 9 1 * MON", scheduler = ClusterTaskScheduler.NAME)
    public void firstMondays() {
      // Refused
    }
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
