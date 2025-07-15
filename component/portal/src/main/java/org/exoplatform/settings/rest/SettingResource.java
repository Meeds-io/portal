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
package org.exoplatform.settings.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/settings")
@Tag(name = "/v1/settings", description = "Managing settings" )
public class SettingResource implements ResourceContainer {

  private SettingService settingService;

  private UserACL userACL;

  public SettingResource(SettingService settingService, UserACL userACL) {
    this.settingService = settingService;
    this.userACL = userACL;
  }

  @Path("/{context}/{scope}/{settingKey}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(
          summary = "Gets a specific setting value",
          description = "Gets a specific setting value",
          method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "User does not have permissions to get it"),
          @ApiResponse(responseCode = "404", description = "Setting does not exist"),
          @ApiResponse(responseCode = "500", description = "Internal server error")})
  public Response getSetting(@Parameter(description = "Context - Format 'contextName,contextId' where 'contextId' is optional. Example: GLOBAL or USER,john", required = true) @PathParam("context") String contextParams,
                             @Parameter(description = "Scope - Format 'scopeName,scopeId' where 'scopeId' is optional. Example: GLOBAL or APPLICATION,wiki or SPACE,marketing", required = true) @PathParam("scope") String scopeParams,
                             @Parameter(description = "Setting key", required = true) @PathParam("settingKey") String settingKey) {
    if(StringUtils.isEmpty(contextParams) || StringUtils.isEmpty(scopeParams) || StringUtils.isEmpty(settingKey)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Context, scope and setting key are mandatory").build();
    }

    Context context = extractContext(contextParams);

    // only the user can get its own settings
    if (!hasPermissions(context)) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    Scope scope = extractScope(scopeParams);

    SettingValue<?> settingValue = settingService.get(context, scope, settingKey);

    if(settingValue == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    SettingEntity settingEntity = new SettingEntity(context, scope, settingKey, settingValue.getValue().toString());

    return Response.ok(settingEntity, MediaType.APPLICATION_JSON).build();
  }

  @Path("/{context}/{scope}/{settingKey}")
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(
          summary = "Sets a specific setting value",
          description = "Sets a specific setting value",
          method = "PUT")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "401", description = "User does not have permissions to update it"),
          @ApiResponse(responseCode = "500", description = "Internal server error")})
  public Response setSetting(@Parameter(description = "Context - Format 'contextName,contextId' where 'contextId' is optional. Example: GLOBAL or USER,john", required = true) @PathParam("context") String contextParams,
                             @Parameter(description = "Scope - Format 'scopeName,scopeId' where 'scopeId' is optional. Example: GLOBAL or APPLICATION,wiki or SPACE,marketing", required = true) @PathParam("scope") String scopeParams,
                             @Parameter(description = "Setting key", required = true) @PathParam("settingKey") String settingKey,
                             @Parameter(description = "Setting value", required = true) SettingValueEntity settingValue) {
    if(StringUtils.isEmpty(contextParams) || StringUtils.isEmpty(scopeParams) || StringUtils.isEmpty(settingKey) || settingValue == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Context, scope and setting key and value are mandatory").build();
    }

    Context context = extractContext(contextParams);

    // only the user can update its own settings
    if (!hasPermissions(context)) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    Scope scope = extractScope(scopeParams);

    settingService.set(context, scope, settingKey, SettingValue.create(settingValue.getValue()));

    return Response.ok().build();
  }

  /**
   * Check if user has permissions to read/write settings.
   * An user can read/write its own settings only.
   * The super user can do anything.
   * @param context The context of the setting
   * @return true if the operation is authorized for the current user
   */
  private boolean hasPermissions(Context context) {
    Identity identity = ConversationState.getCurrent().getIdentity();
    String currentUser = identity.getUserId();
    if(currentUser != null) {
      return (context.getName().toUpperCase().equals(Context.USER.getName())
              && (context.getId() == null || currentUser.equals(context.getId())))
             || userACL.isAdministrator(identity);
    }
    return false;
  }

  /**
   * Extract Scope object from REST input param. The input param has the following pattern : {scopeName},{scopeId}
   * The scopeId is optional.
   * Examples :
   * * APPLICATION,wiki
   * * SPACE,marketing
   * * GLOBAL
   * @param scopeParams REST input param for scope
   * @return The Scope object
   */
  private Scope extractScope(String scopeParams) {
    String scopeName = scopeParams;
    String scopeId = null;
    int scopeIndex = scopeParams.indexOf(",");
    if(scopeIndex > 0) {
      scopeName = scopeParams.substring(0, scopeIndex);
      scopeId = scopeParams.substring(scopeIndex + 1);
    }
    return new Scope(scopeName, scopeId);
  }

  /**
   * Extract Context object from REST input param. The input param has the following pattern : {contextName},{contextId}
   * The contextId is optional.
   * Examples :
   * * USER,john
   * * GLOBAL
   * @param contextParams REST input param for context
   * @return The Context object
   */
  private Context extractContext(String contextParams) {
    String contextName = contextParams;
    String contextId = null;
    int contextIndex = contextParams.indexOf(",");
    if(contextIndex > 0) {
      contextName = contextParams.substring(0, contextIndex);
      contextId = contextParams.substring(contextIndex + 1);
    }
    if(contextName.equals("GLOBAL")) {
      return Context.GLOBAL;
    }
    return new Context(contextName, contextId);
  }
}
