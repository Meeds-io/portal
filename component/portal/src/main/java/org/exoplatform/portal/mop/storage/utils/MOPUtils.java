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
package org.exoplatform.portal.mop.storage.utils;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import org.gatein.common.io.IOTools;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;

public class MOPUtils {

  /*
   * This is a hack and should be removed, it is only used temporarily. This is
   * because the objects are loaded from files and don't have name. (this is
   * clone from POMDataStorage)
   */
  public static void generateStorageName(ModelObject obj) {
    if (obj instanceof Container container) {
      for (ModelObject child : container.getChildren()) {
        generateStorageName(child);
      }
    } else if (obj instanceof Application) {
      obj.setStorageName(UUID.randomUUID().toString());
    }
  }

  public static JSONArray parseJsonArray(String content) {
    try {
      JSONParser parser = new JSONParser();
      return (JSONArray) parser.parse(content);
    } catch (ParseException e) {
      throw new IllegalStateException("Error parsing JSON content: " + content, e);
    }
  }

  public static JSONObject parseJsonObject(String content) {
    try {
      JSONParser parser = new JSONParser();
      return (JSONObject) parser.parse(content);
    } catch (ParseException e) {
      throw new IllegalStateException("Error parsing JSON content: " + content, e);
    }
  }

  public static String toJSONString(Map<?, ?> properties) {
    JSONObject json = new JSONObject(properties);
    return json.toJSONString();
  }

  public static Serializable unserialize(byte[] bytes) {
    try {
      return IOTools.unserialize(bytes);
    } catch (Exception e) {
      throw new IllegalStateException("Error unserializing bytes", e);
    }
  }

  public static byte[] serialize(Serializable obj) {
    try {
      return IOTools.serialize(obj);
    } catch (Exception e) {
      throw new IllegalStateException("Error serializing object: " + obj, e);
    }
  }

  private MOPUtils() {
    // Utils class
  }
}
