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
package io.meeds.common.persistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.picocontainer.Startable;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.management.annotations.Impact;
import org.exoplatform.management.annotations.ImpactType;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.annotations.ManagedName;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.management.rest.annotations.RESTEndpoint;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import lombok.SneakyThrows;

@Managed
@ManagedDescription("Hibernate Statistics Service")
@NameTemplate({ @Property(key = "service", value = "HibernateStatisticsService") })
@RESTEndpoint(path = "hibernateStatisticsService")
public class HibernateStatisticsService implements Startable, ComponentRequestLifecycle {

  private static final Log          LOG                    = ExoLogger.getLogger(HibernateStatisticsService.class);

  private ExoContainer              container;

  private EntityManagerService      entityManagerService;

  private ListenerService           listenerService;

  private ThreadLocal<Boolean>      collectionStarted      = new ThreadLocal<>();

  private List<String>              ignoredQueryMatch      = new ArrayList<>();

  private String[]                  ignoredQueryMatchArray = new String[0];

  private SessionFactoryImplementor sessionFactory;

  private boolean                   enabled;

  private boolean                   logResult;

  public HibernateStatisticsService(ExoContainer container,
                                    EntityManagerService entityManagerService,
                                    ListenerService listenerService) {
    this.entityManagerService = entityManagerService;
    this.listenerService = listenerService;
    this.container = container;
  }

  @Override
  public void start() {
    this.enabled = Boolean.parseBoolean(System.getProperty("meeds.hibernate.statistics.enabled", "false"));
    this.logResult = Boolean.parseBoolean(System.getProperty("meeds.hibernate.statistics.logResult", "true"));
    this.ignoredQueryMatch =
                           new ArrayList<>(Arrays.asList(StringUtils.split(System.getProperty("meeds.hibernate.statistics.ignoredQueryMatch",
                                                                                              ""),
                                                                           ";")));
    this.ignoredQueryMatchArray = ignoredQueryMatch.toArray(String[]::new);
    getStatistics().setStatisticsEnabled(enabled);
  }

  @Override
  public void startRequest(ExoContainer container) {
    if (this.enabled) {
      collectionStarted.set(true);
    }
  }

  @Override
  public void endRequest(ExoContainer container) {
    if (this.enabled && isStarted(container)) {
      try {
        StatisticsImplementor statistics = getStatistics();
        CompletableFuture.runAsync(() -> endStatisticsCollection(statistics));
      } finally {
        collectionStarted.remove();
      }
    }
  }

  @Override
  public boolean isStarted(ExoContainer container) {
    return collectionStarted.get() != null && collectionStarted.get().booleanValue();
  }

  @Managed
  @Impact(ImpactType.WRITE)
  @ManagedDescription("Enable/Disable Hibernate Queries Statistics Collection")
  public void setEnabled(
                         @ManagedName("enabled")
                         @ManagedDescription("Flag to enable (true) or disable (false) stats")
                         boolean enabled) {
    getStatistics().setStatisticsEnabled(enabled);
    this.enabled = enabled;
  }

  @Managed
  @Impact(ImpactType.READ)
  @ManagedDescription("Return the Hibernate Queries Statistics Collection Enablement Status")
  public boolean isEnabled() {
    return enabled;
  }

  @Managed
  @Impact(ImpactType.WRITE)
  @ManagedDescription("Whether to log results or broadcast performances throw ListenerService for external integrations")
  public void setLogResult(
                           @ManagedDescription("logResult")
                           @ManagedName("logResult")
                           boolean logResult) {
    this.logResult = logResult;
  }

  @Managed
  @Impact(ImpactType.READ)
  @ManagedDescription("Return the 'log results' flag value")
  public boolean isLogResult() {
    return logResult;
  }

  @Managed
  @Impact(ImpactType.WRITE)
  @ManagedDescription("Add an Hibernate Query part matched string to ignore in statistics collection")
  public void addIgnoredMatch(
                              @ManagedName("queryMatch")
                              @ManagedDescription("Query part matched string")
                              String queryMatch) {
    ignoredQueryMatch.add(queryMatch);
    ignoredQueryMatchArray = ignoredQueryMatch.toArray(String[]::new);
  }

  private void endStatisticsCollection(StatisticsImplementor statistics) {
    String[] queries = statistics.getQueries();
    if (ArrayUtils.isEmpty(queries)) {
      return;
    }
    List<String> performances = Arrays.stream(queries)
                                      .filter(q -> !Strings.CS.containsAny(q, ignoredQueryMatchArray))
                                      .map(q -> {
                                        QueryStatistics stats = statistics.getQueryStatistics(q);
                                        if (stats.getExecutionCount() == 0) {
                                          return null;
                                        } else {
                                          return String.format("%s;count=%s;dur=%s;max=%s;avg=%s;min=%s",
                                                               q.replace("\r\n", " ").replace("\n", " ").replace("\r", " "),
                                                               stats.getExecutionCount(),
                                                               stats.getExecutionTotalTime(),
                                                               stats.getExecutionMaxTime(),
                                                               stats.getExecutionAvgTime(),
                                                               stats.getExecutionMinTime());
                                        }
                                      })
                                      .filter(Objects::nonNull)
                                      .toList();
    if (this.logResult) {
      performances.forEach(LOG::info);
    } else {
      // In case an addon such as Analytics wants to ingest the statistics in a
      // different way than logs
      this.listenerService.broadcast("meeds.hibernate.statistics", performances, statistics);
    }
    getStatistics().clear();
  }

  private StatisticsImplementor getStatistics() {
    return getSessionFactory().getStatistics();
  }

  @SneakyThrows
  public SessionFactoryImplementor getSessionFactory() {
    if (sessionFactory == null) {
      boolean started = entityManagerService.isStarted(container);
      if (!started) {
        entityManagerService.startRequest(container);
      }
      try {
        Session session = (Session) entityManagerService.getEntityManager().getDelegate();
        sessionFactory = (SessionFactoryImplementor) session.getSessionFactory();
      } finally {
        if (!started) {
          entityManagerService.endRequest(container);
        }
      }
    }
    return sessionFactory;
  }

}
