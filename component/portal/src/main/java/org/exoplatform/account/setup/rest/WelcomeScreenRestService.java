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
package org.exoplatform.account.setup.rest;

import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.rest.resource.ResourceContainer;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/welcomeScreen")
public class WelcomeScreenRestService implements ResourceContainer {

  private static final Log LOG = ExoLogger.getLogger(WelcomeScreenRestService.class);

  /**
   * This method checks if username entered by user in Account Setup Screen
   * already exists
   */
  @GET
  @Path("/checkUsername")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response checkUsername(@QueryParam("username") String username) {

    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);

    boolean userExists = false;
    JSONObject jsonObject = new JSONObject();
    OrganizationService orgService = (OrganizationService) PortalContainer.getInstance()
                                                                          .getComponentInstanceOfType(OrganizationService.class);
    UserHandler userHandler = orgService.getUserHandler();
    try {
      if (userHandler.findUserByName(username) != null) {
        userExists = true;
      }

    } catch (Exception e) {
      LOG.error("An error occurred while checking if username exists.", e);
    }
    try {
      jsonObject.put("userExists", userExists);
    } catch (JSONException e) {
      LOG.error("An error occurred while creating JSONObject that will be returned to identify if username exists.", e);
    }
    return Response.ok(jsonObject.toString()).cacheControl(cacheControl).build();
  }
}
