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

import org.exoplatform.portal.config.model.ModelStyle;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BodyData extends ComponentData {

  private static final long serialVersionUID = 4438136697346439120L;

  protected String          width;

  protected String          height;

  protected String          cssClass;

  private final BodyType    type;

  public BodyData(String storageId,
                  BodyType type,
                  ModelStyle cssStyle) {
    super(storageId, null, cssStyle);
    this.type = type;
  }

  public BodyData(String storageId,
                  String storageName,
                  ModelStyle cssStyle,
                  String width,
                  String height,
                  String cssClass,
                  BodyType type) {
    super(storageId, storageName, cssStyle);
    this.type = type;
    this.width = width;
    this.height = height;
    this.cssClass = cssClass;
  }

}
