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
package org.exoplatform.commons.persistence.impl;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Changelog plugin to add Liquibase changelog path during the data initialization
 */
public class ChangeLogsPlugin extends BaseComponentPlugin {

  public static final String CHANGELOGS_PARAM_NAME = "changelogs";

  public static final String DATASOURCE_PARAM_NAME = "datasource";

  private List<String>       changelogPaths        = new ArrayList<String>();

  private String             datasourceName        = null;

  public ChangeLogsPlugin(InitParams initParams) {
    if(initParams != null) {
      ValuesParam changelogs = initParams.getValuesParam(CHANGELOGS_PARAM_NAME);
      if (changelogs != null) {
        changelogPaths.addAll(changelogs.getValues());
      }
      ValueParam datasourceNameValue = initParams.getValueParam(DATASOURCE_PARAM_NAME);
      if (datasourceNameValue != null) {
        this.datasourceName = datasourceNameValue.getValue();
      }
    }
  }

  public List<String> getChangelogPaths() {
    return changelogPaths;
  }

  public void setChangelogPaths(List<String> changelogPaths) {
    this.changelogPaths = changelogPaths;
  }

  public String getDatasourceName() {
    return datasourceName;
  }
}
