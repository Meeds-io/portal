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
package org.exoplatform.portal.webui.container;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.addons.AddOnService;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.webui.util.PortalDataMapper;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;

import lombok.SneakyThrows;

@ComponentConfig
public class UIAddOnContainer extends UIContainer {

    public static final String   ADDON_CONTAINER  = "addonContainer";

    private ThreadLocal<Boolean> childrenComputed = new ThreadLocal<>(); // NOSONAR, remove already called async

    private boolean              initialized      = false;

    @Override
    @SneakyThrows
    public List<UIComponent> getChildren() {
      if (!initialized
          || (PortalRequestContext.getCurrentInstance().isNoCache() && childrenComputed.get() == null)) {
        AddOnService addonService = getApplicationComponent(AddOnService.class);
        List<Application> apps = addonService.getApplications(this.getName());
        Container model = new Container();
        model.setChildren(new ArrayList<>(apps));
        UIContainer tmp = new UIContainer();
        PortalDataMapper.toUIContainer(tmp, model);
        for (UIComponent comp : tmp.getChildren()) {
          comp.setParent(this);
        }
        this.setChildren(tmp.getChildren());
        initialized = true;

        childrenComputed.set(true);
        PortalRequestContext.getCurrentInstance().addOnRequestEnd(childrenComputed::remove);
      }
      return super.getChildren();
    }

    @Override
    public ModelObject buildModelObject() {
        Container model = new Container(getStorageId());
        model.setId(getId());
        model.setName(getName());
        model.setTitle(getTitle());
        model.setIcon(getIcon());
        model.setDescription(getDescription());
        model.setHeight(getHeight());
        model.setWidth(getWidth());
        model.setTemplate(getTemplate());
        model.setFactoryId(getFactoryId());
        model.setAccessPermissions(getAccessPermissions());
        //Don't build children, we don't save them to database
        return model;
    }

}
