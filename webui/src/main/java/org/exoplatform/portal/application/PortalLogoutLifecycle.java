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
package org.exoplatform.portal.application;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.web.application.Application;
import org.exoplatform.web.application.ApplicationLifecycle;
import org.exoplatform.web.application.RequestFailure;
import org.exoplatform.web.login.LoginError;
import org.exoplatform.web.login.LogoutControl;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.gatein.wci.ServletContainerFactory;

public class PortalLogoutLifecycle implements ApplicationLifecycle<WebuiRequestContext> {
    public void onInit(Application app) throws Exception {
    }

    public void onStartRequest(Application app, WebuiRequestContext context) throws Exception {
        LogoutControl.cancelLogout();

        String uid = context.getRemoteUser();
        User user = null;
        if (uid != null) {
            ExoContainer exoContainer = app.getApplicationServiceContainer();
            if (exoContainer != null) {
                OrganizationService organizationService = (OrganizationService) exoContainer
                        .getComponentInstanceOfType(OrganizationService.class);
                user = organizationService.getUserHandler().findUserByName(uid, UserStatus.ANY);
            }

            // If user is not existed OR disabled
            if (user == null || !user.isEnabled()) {
                logout(user, context);
            }
        }
    }

    private void logout(User user, WebuiRequestContext context) throws Exception {
        LogoutControl.wantLogout();

        Map<String, String> param = null;
        if(user != null) {
            LoginError error = new LoginError(LoginError.DISABLED_USER_ERROR, user.getUserName());
            param = new HashMap<String, String>();
            param.put(LoginError.ERROR_PARAM, error.toString());
        }

        PortalRequestContext.getCurrentInstance().requestAuthenticationLogin(param);
    }

    public void onFailRequest(Application app, WebuiRequestContext context, RequestFailure failureType) {
    }

    public void onEndRequest(Application app, WebuiRequestContext context) throws Exception {
        if (LogoutControl.isLogoutRequired()) {
            PortalRequestContext prContext = Util.getPortalRequestContext();
            HttpServletRequest request = prContext.getRequest();
            HttpServletResponse response = prContext.getResponse();

            if (request.getRemoteUser() != null) {
                ServletContainerFactory.getServletContainer().logout(request, response);
            }
        }
    }

    public void onDestroy(Application app) throws Exception {
    }
}
