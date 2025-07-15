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
package org.exoplatform.portal.config.serialize.model;

import org.exoplatform.portal.config.model.Container;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Used for deserializing from pages.xml using JibX
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BannerSection extends Container {

  protected boolean topBar;

  protected boolean stickyBeahvior;

  @Override
  public String getCssClass() {
    StringBuilder cssClasses = new StringBuilder();
    if (cssStyle != null) {
      cssClasses.append(cssStyle.getCssClass(cssClass, true, 0));
      cssClasses.append(" ");
    }
    if (cssClass != null) {
      cssClasses.append(cssClass);
    }
    if (stickyBeahvior) {
      cssClasses.append(" layout-sticky-section");
    }
    if (!cssClasses.toString().contains("layout-banner-top-section")
        && !cssClasses.toString().contains("layout-banner-bottom-section")) {
      if (topBar) {
        cssClasses.append(" layout-banner-top-section");
      } else {
        cssClasses.append(" layout-banner-bottom-section");
      }
    }
    return cssClasses.toString();
  }

  @Override
  public String getTemplate() {
    return "Banner";
  }

}
