/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.picocontainer.Startable;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
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

  private PortalContainer           portalContainer;

  private EntityManagerService      entityManagerService;

  private ListenerService           listenerService;

  private ThreadLocal<Boolean>      collectionStarted      = new ThreadLocal<>();

  private List<String>              ignoredQueryMatch      = new ArrayList<>();

  private String[]                  ignoredQueryMatchArray = new String[0];

  private SessionFactoryImplementor sessionFactory;

  private boolean                   enabled;

  private boolean                   logResult;

  public HibernateStatisticsService(PortalContainer portalContainer,
                                    EntityManagerService entityManagerService,
                                    ListenerService listenerService) {
    this.entityManagerService = entityManagerService;
    this.listenerService = listenerService;
    this.portalContainer = portalContainer;
    this.enabled = Boolean.parseBoolean(System.getProperty("meeds.hibernate.statistics.enabled", "false"));
    this.logResult = Boolean.parseBoolean(System.getProperty("meeds.hibernate.statistics.logResult", "true"));
  }

  @Override
  public void start() {
    getStatistics().setStatisticsEnabled(enabled);
  }

  @Override
  public void startRequest(ExoContainer container) {
    if (this.enabled) {
      startStatisticsCollection();
      collectionStarted.set(true);
    }
  }

  @Override
  public void endRequest(ExoContainer container) {
    if (this.enabled && isStarted(container)) {
      try {
        endStatisticsCollection();
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
  public void setEnabled(
                         @ManagedDescription("enabled")
                         @ManagedName("enabled")
                         boolean enabled) {
    getStatistics().setStatisticsEnabled(enabled);
    this.enabled = enabled;
  }

  @Managed
  @Impact(ImpactType.READ)
  public boolean isEnabled() {
    return enabled;
  }

  @Managed
  @Impact(ImpactType.WRITE)
  public void setLogResult(
                           @ManagedDescription("logResult")
                           @ManagedName("logResult")
                           boolean logResult) {
    this.logResult = logResult;
  }

  @Managed
  @Impact(ImpactType.READ)
  public boolean isLogResult() {
    return logResult;
  }

  @Managed
  @Impact(ImpactType.WRITE)
  public void addIgnoredMatch(
                              @ManagedDescription("queryMatch")
                              @ManagedName("queryMatch")
                              String queryMatch) {
    ignoredQueryMatch.add(queryMatch);
    ignoredQueryMatchArray = ignoredQueryMatch.toArray(String[]::new);
  }

  private void startStatisticsCollection() {
    getStatistics().clear();
  }

  private void endStatisticsCollection() {
    StatisticsImplementor statistics = getStatistics();
    String[] queries = statistics.getQueries();
    if (ArrayUtils.isEmpty(queries)) {
      return;
    }
    List<String> performances = Arrays.stream(queries)
                                      .filter(q -> !StringUtils.containsAny(q, ignoredQueryMatchArray))
                                      .map(q -> {
                                        QueryStatistics stats = statistics.getQueryStatistics(q);
                                        return String.format("%s;count=%s;dur=%s;max=%s;avg=%s;min=%s",
                                                             q.replace("\r\n", " ").replace("\n", " ").replace("\r", " "),
                                                             stats.getExecutionCount(),
                                                             stats.getExecutionTotalTime(),
                                                             stats.getExecutionMaxTime(),
                                                             stats.getExecutionAvgTime(),
                                                             stats.getExecutionMinTime());
                                      })
                                      .toList();
    if (this.logResult) {
      performances.forEach(LOG::info);
    } else {
      // In case an addon such as Analytics wants to ingest the statistics in a
      // different way than logs
      this.listenerService.broadcast("meeds.hibernate.statistics", performances, statistics);
    }
  }

  private StatisticsImplementor getStatistics() {
    return getSessionFactory().getStatistics();
  }

  @SneakyThrows
  public SessionFactoryImplementor getSessionFactory() {
    if (sessionFactory == null) {
      boolean started = entityManagerService.isStarted(portalContainer);
      if (!started) {
        entityManagerService.startRequest(portalContainer);
      }
      try {
        Session session = (Session) entityManagerService.getEntityManager().getDelegate();
        sessionFactory = (SessionFactoryImplementor) session.getSessionFactory();
      } finally {
        if (!started) {
          entityManagerService.endRequest(portalContainer);
        }
      }
    }
    return sessionFactory;
  }

}
