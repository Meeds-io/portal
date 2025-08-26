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
package org.exoplatform.commons.file.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.api.persistence.DataInitializer;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.file.model.NameSpace;
import org.exoplatform.commons.file.services.NameSpacePlugin;
import org.exoplatform.commons.file.services.NameSpaceService;
import org.exoplatform.commons.file.storage.DataStorage;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

import java.util.ArrayList;
import java.util.List;

public class NameSpaceServiceImpl implements NameSpaceService, Startable {
  private static final Log      LOG                    = ExoLogger.getLogger(NameSpaceService.class);

  private static final String   FILE_DEFAULT_NAMESPACE = "default.namespace";

  private List<NameSpacePlugin> nameSpacePlugins       = new ArrayList<NameSpacePlugin>();

  private String                defaultNamespace;

  private DataStorage           dataStorage;

  public NameSpaceServiceImpl(DataStorage dataStorage, InitParams initParams, DataInitializer dataInitializer) {
    this.dataStorage = dataStorage;
    ValueParam defaultNameSpaceParam = initParams.getValueParam(FILE_DEFAULT_NAMESPACE);
    if (defaultNameSpaceParam != null && defaultNameSpaceParam.getValue() != null) {
      defaultNamespace = defaultNameSpaceParam.getValue();
    } else {
      defaultNamespace = "file";
    }
  }

  /**
   * Add a nameSpace plugin
   * 
   * @param nameSpacePlugin nameSpace plugin to add
   */
  public void addNameSpacePlugin(NameSpacePlugin nameSpacePlugin) {
    this.nameSpacePlugins.add(nameSpacePlugin);
  }

  @ExoTransactional
  private void initNameSpace() {
    LOG.info("Start Init Files nameSpaces ");
    List<NameSpace> list = new ArrayList<NameSpace>();

    /* Default File namespace */
    NameSpace nameSpace = dataStorage.getNameSpace(defaultNamespace);
    if (nameSpace == null) {
      NameSpace add = new NameSpace(defaultNamespace, "Default Files NameSpace");
      list.add(add);
    }
    /* Application File namespace */
    for (NameSpacePlugin nameSpacePlugin : this.nameSpacePlugins) {
      for (String name : nameSpacePlugin.getNameSpaceList().keySet()) {
        LOG.info("{} declares the namespace {}", nameSpacePlugin.getName(), name);

        nameSpace = dataStorage.getNameSpace(name);
        if (nameSpace == null) {
          NameSpace add = new NameSpace(name, nameSpacePlugin.getNameSpaceList().get(name));
          list.add(add);
        }
      }
    }
    if (!list.isEmpty()) {
      dataStorage.createNameSpaces(list);
    }
    LOG.info("End Init Files nameSpaces ");
  }

  @Override
  @ExoTransactional
  public void createNameSpace(String name, String description) {
    if (!StringUtils.isEmpty(name)) {
      NameSpace nameSpace = dataStorage.getNameSpace(name);
      if (nameSpace == null) {
        NameSpace add = new NameSpace(name, description);
        dataStorage.createNameSpace(add);
      }
    }
  }

  public String getDefaultNameSpace() {
    return defaultNamespace;
  }

  @Override
  public void start() {
    initNameSpace();
  }

  @Override
  public void stop() {

  }
}
