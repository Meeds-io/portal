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
package org.exoplatform.portal.mop.storage.cache.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class WindowData implements Serializable {

  public static final WindowData NULL_OBJECT      = new WindowData();

  private static final long serialVersionUID = -4708087284271337753L;

  @Getter
  private Long              id;

  @Getter
  private String            title;

  @Getter
  private String            icon;

  @Getter
  private String            description;

  @Getter
  private boolean           showInfoBar;

  @Getter
  private boolean           showApplicationState;

  @Getter
  private boolean           showApplicationMode;

  @Getter
  private String            theme;

  @Getter
  private String            width;

  @Getter
  private String            height;

  @Getter
  private String            properties;

  @Getter
  private String            appType;

  @Getter
  private String            contentId;

  @Getter
  private byte[]            customization;

  public boolean isNull() {
    return id == null;
  }

}
