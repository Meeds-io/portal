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
package org.exoplatform.commons.addons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.pom.data.ApplicationData;

public class AddOnServiceImpl implements AddOnService {

    private List<ApplicationDecorator> apps = new ArrayList<>();

    @Override
    public List<Application> getApplications(String containerName) {
      return this.apps.stream()
                      .filter(app -> app.getContainerName().equals(containerName))
                      .map(ApplicationDecorator::getApp)
                      .toList();
    }

    @Override
    public void addPlugin(AddOnPlugin plugin) {
      for (Application app : plugin.getApplications()) {
        if (StringUtils.isBlank(app.getId())) {
          app.setId(UUID.randomUUID().toString());
        }
        apps.add(new ApplicationDecorator(app, plugin.getPriority(), plugin.getContainerName()));
      }
      Collections.sort(apps, new Comparator<ApplicationDecorator>() { // NOSONAR
          @Override
          public int compare(ApplicationDecorator o1, ApplicationDecorator o2) {
            if (o1.getAppPriority() != o2.getAppPriority()) {
              return o1.getAppPriority() - o2.getAppPriority();
            }
            TransientApplicationState s1 = (TransientApplicationState)o1.getApp().getState();
            TransientApplicationState s2 = (TransientApplicationState)o2.getApp().getState();
            return s1.getContentId().compareTo(s2.getContentId());
          }
      });
    }
    
    class ApplicationDecorator extends Application {
      
      private Application app;
      private int appPriority;
      private String containerName;
      
      public ApplicationDecorator(ApplicationData appData) {
        super(appData);
      }

      public ApplicationDecorator(Application app, int priority, String containerName) {
        this.app = app;
        this.appPriority = priority;
        this.containerName = containerName;
      }

      public Application getApp() {
        return app;
      }

      public void setApp(Application app) {
        this.app = app;
      }

      public int getAppPriority() {
        return appPriority;
      }

      public void setAppPriority(int appPriority) {
        this.appPriority = appPriority;
      }

      public String getContainerName() {
        return containerName;
      }

      public void setContainerName(String containerName) {
        this.containerName = containerName;
      }
      
    }
}