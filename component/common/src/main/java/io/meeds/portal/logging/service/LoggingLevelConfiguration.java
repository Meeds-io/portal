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
package io.meeds.portal.logging.service;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * What a system administrator allows, in {@code exo.properties}, read once at
 * startup:
 * <ul>
 * <li>{@code meeds.logging.levels.enabled} (default {@code true}): when
 * {@code false}, nothing is listed, changed or applied, and the stored
 * levels are ignored (kept, not deleted): {@code logback.xml} is the only
 * authority;</li>
 * <li>{@code meeds.logging.levels.readOnly} (default {@code false}): the
 * levels are listed, no change is accepted; the stored levels are still
 * applied;</li>
 * <li>{@code meeds.logging.levels.roles} (default {@code administrators}): the
 * roles of the users who may use it, separated by commas. The roles of a
 * user come from the groups they are a member of: a group under
 * {@code /platform} gives its name ({@code /platform/administrators} gives
 * {@code administrators}), and any other group gives its first segment
 * ({@code /spaces/x} gives {@code spaces}, {@code /organization/support} gives
 * {@code organization}), so {@code spaces} or {@code organization} would match
 * every member of those trees;</li>
 * <li>{@code meeds.logging.levels.revertAfterMinutes} (default {@code 0}, no
 * limit): a change is given back to the level of {@code logback.xml} that
 * many minutes after it was made, so that a forgotten DEBUG does not stay on
 * after a restart. It applies to the changes already stored too, and runs
 * whatever the read only mode.</li>
 * </ul>
 */
@Component
public class LoggingLevelConfiguration {

  private static final String   DEFAULT_ROLE = "administrators";

  private final boolean         enabled;

  private final boolean         readOnly;

  private final String[]        roles;

  private final int             revertAfterMinutes;

  public LoggingLevelConfiguration(@Value("${meeds.logging.levels.enabled:true}")
                                   boolean enabled,
                                   @Value("${meeds.logging.levels.readOnly:false}")
                                   boolean readOnly,
                                   @Value("${meeds.logging.levels.roles:administrators}")
                                   String roles,
                                   @Value("${meeds.logging.levels.revertAfterMinutes:0}")
                                   int revertAfterMinutes) {
    this.enabled = enabled;
    // a negative limit is no limit
    this.revertAfterMinutes = Math.max(revertAfterMinutes, 0);
    this.readOnly = readOnly;
    String[] configured = roles == null ? new String[0]
                                        : Arrays.stream(roles.split(",")).map(String::trim).filter(role -> !role.isEmpty()).toArray(String[]::new);
    // a list with no role would lock everybody out: the default applies
    this.roles = configured.length == 0 ? new String[] { DEFAULT_ROLE } : configured;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  /**
   * @return the minutes after which a change is given back, {@code 0} for no
   *         limit
   */
  public int getRevertAfterMinutes() {
    return revertAfterMinutes;
  }

  /**
   * Read by the access check of the REST endpoints, as
   * {@code hasAnyAuthority(@loggingLevelConfiguration.roles)}.
   *
   * @return the roles of the users who may use it
   */
  public String[] getRoles() {
    return roles.clone();
  }

}
