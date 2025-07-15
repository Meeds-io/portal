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

import org.exoplatform.portal.config.model.ApplicationBackgroundStyle;
import org.exoplatform.portal.config.model.ModelStyle;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(force = true)
public class ContainerData extends ComponentData {

  private static final long                  serialVersionUID = 7328080023001711540L;

  /** . */
  private final String                       id;

  /** . */
  private final String                       name;

  /** . */
  private final String                       icon;

  /** . */
  private final String                       template;

  /** . */
  private final String                       factoryId;

  /** . */
  private final String                       title;

  /** . */
  private final String                       description;

  /** . */
  private final String                       width;

  /** . */
  private final String                       height;

  /** . */
  private final String                       cssClass;

  /** . */
  private final String                       profiles;

  /** . */
  private final List<String>                 accessPermissions;

  /** . */
  private final List<ComponentData>          children;

  @Getter
  protected final ApplicationBackgroundStyle appBackgroundStyle;

  public ContainerData(String storageId,
                       String id,
                       String name,
                       String icon,
                       String template,
                       String factoryId,
                       String title,
                       String description,
                       String width,
                       String height,
                       String cssClass,
                       String profiles,
                       ModelStyle cssStyle,
                       ApplicationBackgroundStyle appBackgroundStyle,
                       List<String> accessPermissions,
                       List<ComponentData> children) {
    super(storageId, null, cssStyle);

    //
    this.id = id;
    this.name = name;
    this.icon = icon;
    this.template = template;
    this.factoryId = factoryId;
    this.title = title;
    this.description = description;
    this.width = width;
    this.height = height;
    this.cssClass = cssClass;
    this.appBackgroundStyle = appBackgroundStyle;
    this.profiles = profiles;
    this.accessPermissions = accessPermissions;
    this.children = children;
  }

}
