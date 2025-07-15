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
package org.exoplatform.commons.utils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TimeZoneUtils {

  private TimeZoneUtils() {
    // Class with static methods only
  }

  public static List<TimeZone> getTimeZones() {
    String[] ids = TimeZone.getAvailableIDs();
    Map<Integer, TimeZone> timeZoneByOffset = new HashMap<>();
    for (String id : ids) {
      TimeZone timeZone = TimeZone.getTimeZone(id);
      if (timeZone.getDisplayName().contains("GMT")) {
        continue;
      }
      timeZoneByOffset.put(timeZone.getRawOffset(), timeZone);
    }
    List<Integer> offsets = new ArrayList<>(timeZoneByOffset.keySet());
    Collections.sort(offsets);
    return offsets.stream().map(offset -> timeZoneByOffset.get(offset)).collect(Collectors.toList());
  }

  public static String getTimeZoneDisplay(TimeZone timeZone, Locale locale) {
    long hours = TimeUnit.MILLISECONDS.toHours(timeZone.getRawOffset());
    long minutes = TimeUnit.MILLISECONDS.toMinutes(timeZone.getRawOffset())
        - TimeUnit.HOURS.toMinutes(hours);
    minutes = Math.abs(minutes);
    String result = "";
    if (hours > 0) {
      result = String.format("(GMT +%02d:%02d) %s", hours, minutes, timeZone.getDisplayName(locale));
    } else {
      result = String.format("(GMT %02d:%02d) %s", hours, minutes, timeZone.getDisplayName(locale));
    }
    return result;
  }

}
