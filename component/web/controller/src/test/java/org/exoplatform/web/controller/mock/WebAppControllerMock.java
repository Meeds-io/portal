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
package org.exoplatform.web.controller.mock;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.controller.router.RouterConfigException;

public class WebAppControllerMock extends WebAppController {

  private ExoContainer container;

  public WebAppControllerMock(ExoContainer container, InitParams params) throws Exception {
    super(params);
    this.container = container;
  }

  @Override
  public void reloadConfiguration() throws RouterConfigException, IOException {
    try {
      super.reloadConfiguration();
    } catch (Exception e) {
      URL resource = null;
      String configurationPath = getConfigurationPath();
      if (StringUtils.contains(configurationPath, "classpath:")
          || StringUtils.contains(configurationPath, "jar:")) {
        configurationPath = configurationPath.replace("classpath:/", "")
                                             .replace("classpath:", "")
                                             .replace("jar:/", "")
                                             .replace("jar:", "");
      }
      resource = getResource(configurationPath);
      if (resource == null) {
        resource = getResource("conf/controller.xml");
        if (resource == null) {
          resource = getResource("controller.xml");
          if (resource == null) {
            throw new RouterConfigException("File wasn't found in paths 'conf/controller.xml', 'controller.xml' nor "
                + getConfigurationPath());
          }
        }
      }
      loadConfiguration(resource);
    }
  }

  private URL getResource(String relativePath) {
    if (StringUtils.isBlank(relativePath)) {
      return null;
    }
    URL resource;
    if (this.container instanceof PortalContainer portalContainer) {
      resource = portalContainer.getPortalClassLoader().getResource(relativePath);
    } else {
      resource = this.getClass().getClassLoader().getResource(relativePath);
    }
    return resource;
  }
}
