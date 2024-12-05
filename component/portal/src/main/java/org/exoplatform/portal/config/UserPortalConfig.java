/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.config;

import java.util.Locale;

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.mop.user.UserPortalImpl;

public class UserPortalConfig {

  private PortalConfig            portal;

  private String                  portalName;

  private String                  accessUser;

  private UserPortal              userPortal;

  private Locale                  locale;

  public UserPortalConfig(PortalConfig portal,
                          String portalName,
                          String accessUser,
                          Locale locale) {
    this.portal = portal;
    this.portalName = portalName;
    this.accessUser = accessUser;
    this.locale = locale;
  }

  public UserPortal getUserPortal() {
    return getUserPortal(false);
  }

  public UserPortal getUserPortal(boolean isNewlyCreated) {
    if (isNewlyCreated || userPortal == null) {
      userPortal = new UserPortalImpl(portalName, portal, accessUser, locale);
    }
    return userPortal;
  }

  public PortalConfig getPortalConfig() {
    return portal;
  }

  public void setPortalConfig(PortalConfig portal) {
    this.portal = portal;
  }

  public String getPortalName() {
    return portalName;
  }

}
