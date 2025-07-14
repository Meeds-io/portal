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
package org.exoplatform.portal.jdbc.entity;

import java.io.Serializable;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public abstract class ComponentEntity implements Serializable {
  private static final long serialVersionUID = 1181255637761644181L;

  public JSONObject toJSON() {
    JSONObject obj = new JSONObject();
    obj.put("type", getType().name());
    obj.put("id", getId());
    return obj;
  }

  public abstract Long getId();

  public abstract void setId(Long id);

  public abstract TYPE getType();

  public enum TYPE {
    SITE,
    PAGE,
    CONTAINER,
    WINDOW
  }

  protected static final String getJSONString(JSONObject jsonObject) {
    return jsonObject.toJSONString();
  }

  protected static final String getJSONString(JSONArray jsonArray) {
    return jsonArray.toJSONString();
  }
}
