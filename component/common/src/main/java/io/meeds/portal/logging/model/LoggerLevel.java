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
package io.meeds.portal.logging.model;

import io.meeds.portal.logging.constant.LoggingLevel;

/**
 * A logger as the administration page shows it.
 *
 * @param name           the logger name, a package or a class, or
 *                       {@code ROOT}
 * @param level          the level set on the logger itself, {@code null}
 *                       when it inherits the one of its parent
 * @param effectiveLevel the level the logger applies, inherited or not
 * @param configuredLevel the level {@code logback.xml} gave the logger,
 *                       {@code null} when it gave none; only differs from
 *                       {@code level} on an overridden logger
 * @param overridden     whether an administrator set the level at runtime
 * @param revertsAt      when the level is given back to the one of
 *                       {@code logback.xml}, in milliseconds since the
 *                       epoch; {@code null} when it is not
 * @param editable       whether the name can be changed here; a logger of
 *                       the container whose name is not a package or a class
 *                       is listed, never changed
 */
public record LoggerLevel(String name,
                          LoggingLevel level,
                          LoggingLevel effectiveLevel,
                          LoggingLevel configuredLevel,
                          boolean overridden,
                          Long revertsAt,
                          boolean editable) {

}
