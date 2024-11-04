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

package org.exoplatform.portal.application;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.web.application.Application;
import org.exoplatform.web.application.ApplicationLifecycle;
import org.exoplatform.webui.application.WebuiRequestContext;

public class UserProfileLifecycle implements ApplicationLifecycle<WebuiRequestContext> {

  public static final String  USER_PROFILE_ATTRIBUTE_NAME = "PortalUserProfile";

  private OrganizationService organizationService;

  @Override
  public void onStartRequest(final Application app, final WebuiRequestContext context) throws Exception {
    String username = context.getRemoteUser();
    if (username == null) {
      context.setAttribute(UserProfileLifecycle.USER_PROFILE_ATTRIBUTE_NAME, null);
    } else {
      UserProfile userProfile = getOrganizationService().getUserProfileHandler()
                                                        .findUserProfileByName(username);
      context.setAttribute(UserProfileLifecycle.USER_PROFILE_ATTRIBUTE_NAME, userProfile);
    }
  }

  public OrganizationService getOrganizationService() {
    if (organizationService == null) {
      organizationService = ExoContainerContext.getService(OrganizationService.class);
    }
    return organizationService;
  }
}
