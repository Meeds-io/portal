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
package org.exoplatform.portal.pom.data;

import java.util.List;
import java.util.Map;

import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.ModelStyle;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApplicationData extends ComponentData {

  private static final long         serialVersionUID = -9136595444062927185L;

  /** . */
  private final ApplicationState    state;

  /** . */
  private final String              id;

  /** . */
  private final String              title;

  /** . */
  private final String              icon;

  /** . */
  private final String              description;

  /** . */
  private final boolean             showInfoBar;

  /** . */
  private final boolean             showApplicationState;

  /** . */
  private final boolean             showApplicationMode;

  /** . */
  private final String              theme;

  /** . */
  private final String              width;

  /** . */
  private final String              height;

  /** . */
  private final String              cssClass;

  /** . */
  private final Map<String, String> properties;

  /** . */
  private final List<String>        accessPermissions;

  public ApplicationData(String storageId, // NOSONAR
                         String storageName,
                         ApplicationState state,
                         String id,
                         String title,
                         String icon,
                         String description,
                         boolean showInfoBar,
                         boolean showApplicationState,
                         boolean showApplicationMode,
                         String theme,
                         String width,
                         String height,
                         String cssClass,
                         ModelStyle cssStyle,
                         Map<String, String> properties,
                         List<String> accessPermissions) {
    super(storageId, storageName, cssStyle);

    //
    this.state = state;
    this.id = id;
    this.title = title;
    this.icon = icon;
    this.description = description;
    this.showInfoBar = showInfoBar;
    this.showApplicationState = showApplicationState;
    this.showApplicationMode = showApplicationMode;
    this.theme = theme;
    this.width = width;
    this.height = height;
    this.cssClass = cssClass;
    this.properties = properties;
    this.accessPermissions = accessPermissions;
  }
}
