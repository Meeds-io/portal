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
package org.exoplatform.portal.rest;

import java.util.Collection;
import java.util.Date;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.impl.MembershipTypeImpl;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

@Path("v1/membershipTypes")
@Tag(name = "v1/membershipTypes", description = "Manage membership types operations")
public class MembershipTypeRestResourcesV1 implements ResourceContainer {

  private OrganizationService organizationService;

  private UserACL             userACL;

  public MembershipTypeRestResourcesV1(OrganizationService organizationService, UserACL userACL) {
    this.organizationService = organizationService;
    this.userACL = userACL;
  }

  @GET
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Gets membership types",
      description = "Gets membership types",
      method = "GET"
  )
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "401", description = "User not authorized to call this endpoint"),
          @ApiResponse(responseCode = "500", description = "Internal server error")
      }
  )
  public Response getMembershipType() throws Exception {
    Collection<MembershipType> membershipTypes = organizationService.getMembershipTypeHandler().findMembershipTypes();
    return Response.ok(membershipTypes).build();
  }

  @POST
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Creates a new membership type",
      description = "Creates a new membership type",
      method = "POST"
  )
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "204", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "User not authorized to call this endpoint"),
          @ApiResponse(responseCode = "500", description = "Internal server error")
      }
  )
  public Response createMembershipType(@RequestBody(description = "Membership type object")
                                         MembershipTypeImpl membershipType) throws Exception {
    if (membershipType == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Membership type object is required").build();
    }
    if (StringUtils.isBlank(membershipType.getName())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("NAME:MANDATORY").build();
    } else if (membershipType.getName().length() > 30 || membershipType.getName().length()<3) {
      return Response.status(Response.Status.BAD_REQUEST).entity("NAME:LENGTH_INVALID").build();
    }
    if (StringUtils.isBlank(membershipType.getDescription())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("DESCRIPTION:MANDATORY").build();
    } else if (membershipType.getDescription().length() > 255 || membershipType.getDescription().length()<3) {
      return Response.status(Response.Status.BAD_REQUEST).entity("DESCRIPTION:LENGTH_INVALID").build();
    }
    if (organizationService.getMembershipTypeHandler().findMembershipType(membershipType.getName()) != null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("NAME:ALREADY_EXISTS")
                     .build();
    }
    membershipType.setCreatedDate(new Date());
    membershipType.setModifiedDate(new Date());
    membershipType.setOwner(ConversationState.getCurrent().getIdentity().getUserId());

    organizationService.getMembershipTypeHandler().createMembershipType(membershipType, true);
    return Response.noContent().build();
  }

  @PUT
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Updates an existing membership type",
      description = "Updates an existing membership type",
      method = "PUT"
  )
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "204", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "User not authorized to call this endpoint"),
          @ApiResponse(responseCode = "500", description = "Internal server error")
      }
  )
  public Response updateMembershipType(@RequestBody(description = "Membership type object")
                                          MembershipTypeImpl membershipType) throws Exception {
    if (membershipType == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Membership type object is required").build();
    }
    if (StringUtils.isBlank(membershipType.getName())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("NAME:MANDATORY").build();
    }
    if (organizationService.getMembershipTypeHandler().findMembershipType(membershipType.getName()) == null) {
      return Response.status(Response.Status.NOT_FOUND)
                     .entity("NAME:NOT_FOUND")
                     .build();
    }
    membershipType.setModifiedDate(new Date());
    membershipType.setOwner(ConversationState.getCurrent().getIdentity().getUserId());
    organizationService.getMembershipTypeHandler().saveMembershipType(membershipType, true);
    return Response.noContent().build();
  }

  @DELETE
  @Path("{membershipType}")
  @RolesAllowed("administrators")
  @Operation(
      summary = "Deletes an existing membership type",
      description = "Deletes an existing membership type",
      method = "DELETE")
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "204", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "User not authorized to call this endpoint"),
          @ApiResponse(responseCode = "500", description = "Internal server error")
      }
  )
  public Response deleteMembershipType(@Parameter(description = "Membership type name", required = true)
                                       @PathParam("membershipType") String membershipType) throws Exception {
    if (StringUtils.isBlank(membershipType)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("NAME:MANDATORY").build();
    }
    if ((userACL.getMandatoryMSTypes() != null && userACL.getMandatoryMSTypes().contains(membershipType))
        || StringUtils.equals(userACL.getMakableMT(), membershipType)) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("MandatoryMembershipType")
                     .build();
    }
    if (organizationService.getMembershipTypeHandler().findMembershipType(membershipType) == null) {
      return Response.status(Response.Status.NOT_FOUND)
                     .entity("NAME:NOT_FOUND")
                     .build();
    }
    organizationService.getMembershipTypeHandler().removeMembershipType(membershipType, true);
    return Response.noContent().build();
  }

}
