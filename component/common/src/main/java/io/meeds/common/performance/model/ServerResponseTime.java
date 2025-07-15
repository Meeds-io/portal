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
package io.meeds.common.performance.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;

@Data
public class ServerResponseTime {

  public static final boolean                 DEVELOPPING           = PropertyManager.isDevelopping();

  public static final boolean                 SERVER_TIMING_ENABLED = DEVELOPPING
                                                                      || StringUtils.equals(System.getProperty("meeds.server.timing.api.enabled",
                                                                                                               "false"),
                                                                                            "true");

  private static final Log                    LOG                   = ExoLogger.getLogger("ServerTimingAPI");

  private static final String                 OVERALL_KEY           = "overall";

  private List<String>                        serverTimeNames       = new Vector<>();                                                            // NOSONAR

  private Map<String, ServerResponseTimeItem> serverTimes           = new LinkedHashMap<>();

  public ServerResponseTime() {
    if (!SERVER_TIMING_ENABLED) {
      LOG.warn("Server Timing API Not enabled, please consider use it conditionally when (ServerResponseTime.SERVER_TIMING_ENABLED == true) only");
    }
  }

  public boolean startServerTime(String name) {
    if (!serverTimeNames.contains(name)) { // NOSONAR
      long start = System.currentTimeMillis();
      ServerResponseTimeItem timeItem = serverTimes.computeIfAbsent(name,
                                                                    k -> new ServerResponseTimeItem(name, serverTimes.size()));
      timeItem.setStartTime(start);
      if (!serverTimeNames.isEmpty()) {
        String lastName = serverTimeNames.getLast();
        serverTimes.get(lastName).setPausedTime(start);
      }
      serverTimeNames.add(name);
      return true;
    }
    return false;
  }

  public void endServerTime(String name) {
    long end = System.currentTimeMillis();
    ServerResponseTimeItem timeItem = serverTimes.get(name);
    timeItem.setDuration(end - timeItem.getStartTime() - timeItem.getCumulativePausedTime());
    serverTimeNames.remove(name);
    if (!serverTimeNames.isEmpty()) {
      String lastName = serverTimeNames.getLast();
      ServerResponseTimeItem lastTimeItem = serverTimes.get(lastName);
      lastTimeItem.setCumulativePausedTime(lastTimeItem.getCumulativePausedTime() + end - lastTimeItem.getPausedTime());
    }
  }

  public void addHttpHeader(HttpServletResponse response) {
    if (response.isCommitted()) {
      LOG.warn("Can't add Server Timing Header since the response is already committed");
    } else {
      ServerResponseTimeItem overallTimeItem = new ServerResponseTimeItem(OVERALL_KEY, serverTimes.size());
      serverTimeNames.reversed().forEach(this::endServerTime);
      overallTimeItem.setDuration(serverTimes.values().stream().mapToLong(ServerResponseTimeItem::getDuration).sum());
      serverTimes.put(OVERALL_KEY, overallTimeItem);
      List<String> times = serverTimes.values()
                                      .stream()
                                      .map(e -> e.getName() + ";dur=" + e.getDuration())
                                      .toList();
      response.setHeader("Server-Timing", StringUtils.join(times, ","));
    }
  }

}
