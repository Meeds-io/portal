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
package org.exoplatform.commons.file.services;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 */
public class NameSpacePlugin extends BaseComponentPlugin {

  private Map<String, String> nameSpaceList = new HashMap<>();

  public NameSpacePlugin(InitParams initParams) {
    if (initParams != null) {
      PropertiesParam params = initParams.getPropertiesParam("fileNameSpace.params");
      if (params != null && !params.getProperty("name").isEmpty())
        nameSpaceList.put(params.getProperty("name"), params.getProperty("description"));
    }
  }

  public Map<String, String> getNameSpaceList() {
    return nameSpaceList;
  }

  public void setNameSpaceList(Map<String, String> nameSpaceList) {
    this.nameSpaceList = nameSpaceList;
  }

}
