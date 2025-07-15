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
package org.exoplatform.portal.webui.portal;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIContainer;

import lombok.Getter;
import lombok.Setter;

public class UIPortalComponent extends UIContainer {

  @Setter
  protected String template;

  @Getter
  @Setter
  protected String name;

  @Getter
  @Setter
  protected String factoryId;

  @Getter
  @Setter
  protected String width;

  @Getter
  @Setter
  protected String height;

  @Getter
  @Setter
  private String   title;

  @Getter
  @Setter
  private String[] accessPermissions = { UserACL.EVERYONE };

  @Override
  public String getTemplate() {
    if (StringUtils.isBlank(template)) {
      return getComponentConfig().getTemplate();
    }
    return template;
  }

  /**
   * @return
   * @deprecated Use {@link #hasAccessPermission()}
   */
  @Deprecated
  public boolean hasPermission() {
    return hasAccessPermission();
  }

  public boolean hasAccessPermission() {
    return ArrayUtils.isEmpty(accessPermissions) || ExoContainerContext.getService(UserACL.class)
                                                                       .hasPermission(ConversationState.getCurrent()
                                                                                                       .getIdentity(),
                                                                                      accessPermissions);
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    if (hasAccessPermission()) {
      super.processRender(context);
    }
  }

  @Override
  public void processDecode(WebuiRequestContext context) throws Exception {
    if (hasAccessPermission()) {
      super.processDecode(context);
    }
  }

  @Override
  public void processAction(WebuiRequestContext context) throws Exception {
    if (hasAccessPermission()) {
      super.processAction(context);
    }
  }
}
