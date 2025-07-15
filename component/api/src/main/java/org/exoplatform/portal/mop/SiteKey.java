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
package org.exoplatform.portal.mop;

import java.io.Serializable;

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.page.PageKey;

import lombok.Data;

@Data
public final class SiteKey implements Serializable {

  private static final long serialVersionUID = -33198391360501633L;

  private final SiteType    type;

  private final String      name;

  public SiteKey(SiteType type, String name) {
    if (type == null) {
      throw new NullPointerException("No null type can be provided");
    }
    if (name == null) {
      throw new NullPointerException("No null name can be provided");
    }

    //
    this.type = type;
    this.name = name;
  }

  public SiteKey(String type, String name) {
    if (PortalConfig.PORTAL_TYPE.equalsIgnoreCase(type)) {
      this.type = SiteType.PORTAL;
    } else if (PortalConfig.GROUP_TYPE.equalsIgnoreCase(type)) {
      this.type = SiteType.GROUP;
    } else if (PortalConfig.GROUP_TEMPLATE.equalsIgnoreCase(type)) {
      this.type = SiteType.GROUP_TEMPLATE;
    } else if (PortalConfig.PORTAL_TEMPLATE.equalsIgnoreCase(type)) {
      this.type = SiteType.PORTAL_TEMPLATE;
    } else if (PortalConfig.SPACE_TYPE.equalsIgnoreCase(type)) {
      this.type = SiteType.SPACE;
    } else if (PortalConfig.USER_TYPE.equalsIgnoreCase(type)) {
      this.type = SiteType.USER;
    } else if (PortalConfig.DRAFT.equalsIgnoreCase(type)) {
      this.type = SiteType.DRAFT;
    } else {
      throw new NullPointerException("No null name can be provided");
    }
    this.name = name;
  }

  public static SiteKey portal(String name) {
    return new SiteKey(SiteType.PORTAL, name);
  }

  public static SiteKey group(String name) {
    return new SiteKey(SiteType.GROUP, name);
  }

  public static SiteKey user(String name) {
    return new SiteKey(SiteType.USER, name);
  }

  public static SiteKey space(String name) {
    return new SiteKey(SiteType.SPACE, name);
  }

  public static SiteKey groupTemplate(String name) {
    return new SiteKey(SiteType.GROUP_TEMPLATE, name);
  }

  public static SiteKey portalTemplate(String name) {
    return new SiteKey(SiteType.PORTAL_TEMPLATE, name);
  }

  public static SiteKey draft(String name) {
    return new SiteKey(SiteType.DRAFT, name);
  }

  public String getTypeName() {
    return type.getName();
  }

  public PageKey page(String name) {
    return new PageKey(this, name);
  }
}
