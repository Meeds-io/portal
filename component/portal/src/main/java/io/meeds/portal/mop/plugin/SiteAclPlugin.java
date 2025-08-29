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
package io.meeds.portal.mop.plugin;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.security.Identity;

import io.meeds.portal.plugin.AclPlugin;

import jakarta.annotation.PostConstruct;

@Component
public class SiteAclPlugin implements AclPlugin {

  public static final String OBJECT_TYPE = "site";

  @Autowired
  private PortalContainer    container;

  private LayoutService      layoutService;

  private UserACL            userAcl;

  @PostConstruct
  public void init() {
    layoutService = container.getComponentInstanceOfType(LayoutService.class);
    userAcl = container.getComponentInstanceOfType(UserACL.class);
    userAcl.addAclPlugin(this);
  }

  @Override
  public String getObjectType() {
    return OBJECT_TYPE;
  }

  @Override
  public boolean hasPermission(String siteId,
                               String permissionType,
                               Identity identity) {
    if (!StringUtils.isNumeric(siteId)) {
      throw new IllegalArgumentException(String.format("SiteId '%s' isn't numeric", siteId));
    }
    PortalConfig portalConfig = layoutService.getPortalConfig(Long.parseLong(siteId));
    if (portalConfig == null) {
      return userAcl.isAdministrator(identity);
    } else if (StringUtils.equals(permissionType, VIEW_PERMISSION_TYPE)) {
      return hasAccessPermission(portalConfig, identity)
             || hasEditPermission(portalConfig, identity);
    } else if (StringUtils.equalsAny(permissionType, EDIT_PERMISSION_TYPE, DELETE_PERMISSION_TYPE)) {
      return hasEditPermission(portalConfig, identity);
    } else {
      return false;
    }
  }

  private boolean hasEditPermission(PortalConfig portalConfig, Identity identity) {
    return userAcl.hasEditPermission(identity,
                                     portalConfig.getType(),
                                     portalConfig.getName(),
                                     portalConfig.getEditPermission());
  }

  private boolean hasAccessPermission(PortalConfig portalConfig, Identity identity) {
    return userAcl.hasAccessPermission(identity,
                                       portalConfig.getType(),
                                       portalConfig.getName(),
                                       portalConfig.getAccessPermissions());
  }

}
