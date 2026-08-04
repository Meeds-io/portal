/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package org.exoplatform.services.organization.idm.ldap;

import org.apache.commons.lang3.StringUtils;

/**
 * A single DNS SRV record, as returned for a name such as
 * {@code _ldap._tcp.example.com}, in the RFC 2782 wire format:
 * {@code <priority> <weight> <port> <target>}.
 */
public class SrvRecord {

  private final int    priority;

  private final int    weight;

  private final int    port;

  private final String target;

  public SrvRecord(int priority, int weight, int port, String target) {
    this.priority = priority;
    this.weight = weight;
    this.port = port;
    this.target = target;
  }

  /**
   * Parses a single SRV record value as returned by the JNDI DNS provider,
   * e.g. {@code "10 60 389 dc1.example.com."}.
   *
   * @param value the raw SRV attribute value
   * @return the parsed record, or {@code null} if the value could not be parsed
   */
  public static SrvRecord parse(String value) {
    if (StringUtils.isBlank(value)) {
      return null;
    }
    String[] parts = value.trim().split("\\s+");
    if (parts.length != 4) {
      return null;
    }
    try {
      int priority = Integer.parseInt(parts[0]);
      int weight = Integer.parseInt(parts[1]);
      int port = Integer.parseInt(parts[2]);
      String target = parts[3];
      // Strip the trailing root-zone dot from the FQDN, if present
      if (target.endsWith(".")) {
        target = target.substring(0, target.length() - 1);
      }
      if (StringUtils.isBlank(target)) {
        return null;
      }
      return new SrvRecord(priority, weight, port, target);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public int getPriority() {
    return priority;
  }

  public int getWeight() {
    return weight;
  }

  public int getPort() {
    return port;
  }

  public String getTarget() {
    return target;
  }

  public String toUrl(String scheme) {
    return scheme + "://" + target + ":" + port;
  }

  @Override
  public String toString() {
    return priority + " " + weight + " " + port + " " + target;
  }
}
