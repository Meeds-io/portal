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

import org.exoplatform.portal.pom.config.Utils;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PortalKey extends OwnerKey {

  private static final long serialVersionUID = -6089922297660724879L;

  public PortalKey(String type, String id) {
    super(type, id);
  }

  public static PortalKey create(String compositeId) {
    if (compositeId == null) {
      throw new NullPointerException();
    }
    String[] components = Utils.split("::", compositeId);
    if (components.length != 2) {
      throw new IllegalArgumentException("Wrong navigation id key format " + compositeId);
    }
    return new PortalKey(components[0], components[1]);
  }

  @Override
  public String toString() {
    return String.format("%s::%s", getType(), getId());
  }
}
