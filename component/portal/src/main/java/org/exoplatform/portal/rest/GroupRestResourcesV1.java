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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.rest.model.GroupRestEntity;
import org.exoplatform.portal.rest.model.MembershipRestEntity;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.organization.idm.MembershipImpl;
import org.exoplatform.services.organization.impl.GroupImpl;
import org.exoplatform.services.organization.search.GroupSearchService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;


@Path("v1/groups")
@Tag(name = "v1/groups", description = "Manages groups operations")
public class GroupRestResourcesV1 implements ResourceContainer {

  public static final int     DEFAULT_LIMIT  = 20;

  public static final int     DEFAULT_OFFSET = 0;

  private GroupSearchService  groupSearchService;

  private OrganizationService organizationService;

  private UserACL             userACL;

  public GroupRestResourcesV1(OrganizationService organizationService, GroupSearchService groupSearchService, UserACL userACL) {
    this.organizationService = organizationService;
    this.groupSearchService = groupSearchService;
    this.userACL = userACL;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @Operation(
      summary = "Gets groups",
      description = "Gets groups",
      method = "GET")
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "401", description = "User not authorized to call this endpoint"),
          @ApiResponse(responseCode = "500", description = "Internal server error") }
  )
  public Response getGroups(@Context UriInfo uriInfo,
                            @Parameter(description = "Search text to filter groups") @QueryParam(
                              "q"
                            ) String q,
                            @Parameter(description = "Offset") @QueryParam(
                              "offset"
                            ) int offset,
                            @Parameter(description = "Limit") @QueryParam(
                              "limit"
                            ) int limit,
                            @Parameter(description = "Whether build tree until results or not") @Schema(defaultValue = "false")
                            @QueryParam(
                              "tree"
                            ) boolean buildTree,
                            @QueryParam(
                              "returnSize"
                            ) boolean returnSize,
                            @QueryParam("expand") String expand) throws Exception {
    offset = offset > 0 ? offset : DEFAULT_OFFSET;
    limit = limit > 0 ? limit : DEFAULT_LIMIT;

    int size = groupSearchService.searchGroups(q).getSize();
    limit = limit < size ? limit : size;
    Group[] groups = groupSearchService.searchGroups(q).load(offset, limit);
    List<GroupRestEntity> listAllGroups = Arrays.stream(groups)
                                                .map(group -> new GroupRestEntity(group))
                                                .collect(Collectors.toList());
    CollectionEntity<GroupRestEntity> result = null;
    if (buildTree) {
      List<GroupRestEntity> rootGroups = new ArrayList<>();
      Map<String, GroupRestEntity> groupsById = new HashMap<>();
      for (GroupRestEntity groupRestEntity : listAllGroups) {
        buildTree(rootGroups, groupsById, groupRestEntity);
      }
      result = new CollectionEntity<>(rootGroups,
                                      offset,
                                      limit,
                                      rootGroups.size());
    } else {
      result = new CollectionEntity<>(listAllGroups,
                                      offset,
                                      limit,
                                      size);
    }
    return Response.ok(result).build();
  }

  @GET
  @Path("tree")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @Operation(
      summary = "Gets groups tree",
      description = "Gets groups tree",
      method = "GET")
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "401", description = "User not authorized to call this endpoint"),
          @ApiResponse(responseCode = "500", description = "Internal server error") }
  )
  public Response getGroupsTree(@Context UriInfo uriInfo,
                                @Parameter(description = "Parent groupId to search") @QueryParam(
                                  "parentId"
                                ) String parentId,
                                @Parameter(description = "Search text to filter groups") @QueryParam(
                                  "q"
                                ) String q,
                                @Parameter(description = "Offset") @QueryParam(
                                  "offset"
                                ) int offset,
                                @Parameter(description = "Limit") @QueryParam(
                                  "limit"
                                ) int limit,
                                @QueryParam(
                                  "returnSize"
                                ) boolean returnSize) throws Exception {
    offset = offset > 0 ? offset : DEFAULT_OFFSET;
    limit = limit > 0 ? limit : DEFAULT_LIMIT;

    Group parentGroup = null;
    if (StringUtils.isNotBlank(parentId)) {
      parentGroup = organizationService.getGroupHandler().findGroupById(parentId);
      if (parentGroup == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
    }

    ListAccess<Group> childrenGroupsListAccess = organizationService.getGroupHandler().findGroupChildren(parentGroup, q);

    int totalSize = childrenGroupsListAccess.getSize();
    int limitToFetch = limit;
    if (totalSize < (offset + limitToFetch)) {
      limitToFetch = totalSize - offset;
    }
    Group[] groups = null;
    if (limitToFetch <= 0) {
      groups = new Group[0];
    } else {
      groups = childrenGroupsListAccess.load(offset, limitToFetch);
      if (!returnSize) {
        totalSize = 0;
      }
    }
    List<Group> groupsList = Arrays.asList(groups);
    CollectionEntity<Group> result = new CollectionEntity<>(groupsList,
                                                            offset,
                                                            limit,
                                                            totalSize);
    return Response.ok(result).build();
  }

  @POST
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Creates a new group",
      description = "Creates a new group",
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
  public Response createGroup(GroupImpl group) throws Exception {
    String regex = "^[a-zA-Z0-9-_]+$";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(group.getGroupName());
    boolean isValid =  matcher.matches();
    if (group == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Group object is required").build();
    }
    if (StringUtils.isBlank(group.getGroupName())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("NAME:MANDATORY").build();
    }
    if (!isValid) {
      return Response.status(Response.Status.BAD_REQUEST).entity("NAME:INVALID").build();
    }
    if (StringUtils.isBlank(group.getLabel())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("LABEL:MANDATORY").build();
    }
    String groupId = (group.getParentId() == null ? "" : (group.getParentId() + "/")) + group.getGroupName();
    if (organizationService.getGroupHandler().findGroupById(groupId) != null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("NAME:ALREADY_EXISTS")
                     .build();
    }
    Group parent = null;
    if (group.getParentId() != null) {
      parent = organizationService.getGroupHandler().findGroupById(group.getParentId());
      if (parent == null) {
        return Response.status(Response.Status.BAD_REQUEST)
                       .entity("PARENT:NOT_FOUND")
                       .build();
      }
    }

    organizationService.getGroupHandler().addChild(parent, group, true);
    if (parent != null && CollectionUtils.isNotEmpty(group.getEnclosingMemberships())) {
      for (NestedMembership enclosingMembership : group.getEnclosingMemberships()) {
        enclosingMembership.setNestedGroupId(groupId);
        organizationService.getGroupHandler().linkGroups(enclosingMembership);
      }
    }
    return Response.noContent().build();
  }

  @PUT
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Updates an existing Group",
      description = "Updates an existing Group",
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
  public Response updateGroup(GroupImpl group) throws Exception {
    if (group == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Group object is required").build();
    }
    if (StringUtils.isBlank(group.getId()) || organizationService.getGroupHandler().findGroupById(group.getId()) == null) {
      return Response.status(Response.Status.NOT_FOUND)
                     .entity("ID:NOT_FOUND")
                     .build();
    }
    organizationService.getGroupHandler().saveGroup(group, true);
    return Response.noContent().build();
  }

  @DELETE
  @RolesAllowed("administrators")
  @Operation(
      summary = "Deletes an existing Group",
      description = "Deletes an existing Group",
      method = "DELETE"
  )
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "204", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "User not authorized to call this endpoint"),
          @ApiResponse(responseCode = "500", description = "Internal server error")
      }
  )
  public Response deleteGroup(@Parameter(description = "Group id", required = true) @QueryParam(
    "groupId"
  ) String groupId) throws Exception {
    if (StringUtils.isBlank(groupId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("ID:MANDATORY").build();
    }
    if (userACL.getMandatoryGroups() != null && userACL.getMandatoryGroups().contains(groupId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("MandatoryGroup").build();
    }
    Group group = organizationService.getGroupHandler().findGroupById(groupId);
    if (group == null) {
      return Response.status(Response.Status.NOT_FOUND)
                     .entity("ID:NOT_FOUND")
                     .build();
    }
    organizationService.getGroupHandler().removeGroup(group, true);
    return Response.noContent().build();
  }

  @GET
  @Path("memberships")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @Operation(
      summary = "Gets Group memberships list",
      description = "Gets Group memberships list",
      method = "GET"
  )
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "404", description = "Group not found"),
          @ApiResponse(responseCode = "500", description = "Internal server error due to data encoding"),
      }
  )
  public Response getGroupMemberships(
                                      @Parameter(description = "Group identifier", required = true) @QueryParam(
                                        "groupId"
                                      ) String groupId,
                                      @Parameter(description = "Offset", required = false)
                                      @Schema(defaultValue = "0")
                                      @QueryParam(
                                        "offset"
                                      ) int offset,
                                      @Parameter(description = "Limit", required = false)
                                      @Schema(defaultValue = "20")
                                      @QueryParam(
                                        "limit"
                                      ) int limit,
                                      @Parameter(description = "Returning the number of users found or not")
                                      @Schema(defaultValue = "false")
                                      @QueryParam(
                                        "returnSize"
                                      ) boolean returnSize) throws Exception {

    offset = offset > 0 ? offset : 0;
    limit = limit > 0 ? limit : DEFAULT_LIMIT;

    Group group = organizationService.getGroupHandler().findGroupById(groupId);
    if (group == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    ListAccess<Membership> membershipsByGroup = organizationService.getMembershipHandler().findAllMembershipsByGroup(group);
    int totalSize = membershipsByGroup.getSize();
    Membership[] memberships;
    int limitToFetch = limit;
    if (totalSize < (offset + limitToFetch)) {
      limitToFetch = totalSize - offset;
    }
    List<MembershipRestEntity> membershipEntities = new ArrayList<>();
    if (limitToFetch > 0) {
      memberships = membershipsByGroup.load(offset, limitToFetch);
      for (Membership membership : memberships) {
        User user = organizationService.getUserHandler().findUserByName(membership.getUserName(), UserStatus.ANY);
        membershipEntities.add(new MembershipRestEntity(membership, group, user));
      }
      if (!returnSize) {
        totalSize = 0;
      }
    }

    return Response.ok(new CollectionEntity<>(membershipEntities, offset, limit, totalSize)).build();
  }

  @POST
  @Path("memberships")
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Creates a new membership",
      description = "Creates a new membership",
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
  public Response createMembership(@RequestBody(description = "Membership Object") MembershipImpl membership) throws Exception {
    if (membership == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Membership object is required").build();
    }
    if (StringUtils.isBlank(membership.getGroupId())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("GROUP_ID:MANDATORY").build();
    }
    if (StringUtils.isBlank(membership.getUserName())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("USER:MANDATORY").build();
    }
    if (StringUtils.isBlank(membership.getMembershipType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("MEMBERSHIP_TYPE:MANDATORY").build();
    }
    if (organizationService.getMembershipHandler().findMembership(membership.getId()) != null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("MEMBERSHIP:ALREADY_EXISTS")
                     .build();
    }
    User user = organizationService.getUserHandler().findUserByName(membership.getUserName(), UserStatus.ANY);
    if (user == null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("USER:NOT_FOUND")
                     .build();
    }
    Group group = organizationService.getGroupHandler().findGroupById(membership.getGroupId());
    if (group == null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("GROUP:NOT_FOUND")
                     .build();
    }
    MembershipType membershipType = organizationService.getMembershipTypeHandler()
                                                       .findMembershipType(membership.getMembershipType());
    if (membershipType == null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("MEMBERSHIP_TYPE:NOT_FOUND")
                     .build();
    }

    organizationService.getMembershipHandler().linkMembership(user, group, membershipType, true);
    return Response.noContent().build();
  }

  @POST
  @Path("memberships/bulk")
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
          summary = "Creates new memberships",
          description = "Creates new memberships",
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
  public Response createMultipleMembership(@RequestBody(description = "List of membership objects")
                                                          List<MembershipImpl> memberships) throws Exception {
    for (MembershipImpl membership : memberships) {
      if (membership == null) {
        return Response.status(Response.Status.BAD_REQUEST).entity("Membership object is required").build();
      }
      if (StringUtils.isBlank(membership.getGroupId())) {
        return Response.status(Response.Status.BAD_REQUEST).entity("GROUP_ID:MANDATORY").build();
      }
      if (StringUtils.isBlank(membership.getUserName())) {
        return Response.status(Response.Status.BAD_REQUEST).entity("USER:MANDATORY").build();
      }
      if (StringUtils.isBlank(membership.getMembershipType())) {
        return Response.status(Response.Status.BAD_REQUEST).entity("MEMBERSHIP_TYPE:MANDATORY").build();
      }
      if (organizationService.getMembershipHandler().findMembership(membership.getId()) != null) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity("MEMBERSHIP:ALREADY_EXISTS")
                .build();
      }
      User user = organizationService.getUserHandler().findUserByName(membership.getUserName(), UserStatus.ANY);
      if (user == null) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity("USER:NOT_FOUND")
                .build();
      }
      Group group = organizationService.getGroupHandler().findGroupById(membership.getGroupId());
      if (group == null) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity("GROUP:NOT_FOUND")
                .build();
      }
      MembershipType membershipType = organizationService.getMembershipTypeHandler()
              .findMembershipType(membership.getMembershipType());
      if (membershipType == null) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity("MEMBERSHIP_TYPE:NOT_FOUND")
                .build();
      }

      organizationService.getMembershipHandler().linkMembership(user, group, membershipType, true);
    }
    return Response.noContent().build();
  }

  @PUT
  @Path("memberships")
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Updates an existing membership",
      description = "Updates an existing membership",
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
  public Response updateMembership(@Parameter(description = "Membership identifier with format: MEMBERSHIP_TYPE:GROUP_ID:USER_NAME", required = true)
                                   @QueryParam("membershipId") String membershipId,
                                   @RequestBody(description = "Membership object") MembershipImpl membership) throws Exception {
    if (membership == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Membership object is required").build();
    }
    if (StringUtils.isBlank(membershipId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Membership identifier object is required").build();
    }
    if (StringUtils.isBlank(membership.getGroupId())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("GROUP_ID:MANDATORY").build();
    }
    if (StringUtils.isBlank(membership.getUserName())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("USER:MANDATORY").build();
    }
    if (StringUtils.isBlank(membership.getMembershipType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("MEMBERSHIP_TYPE:MANDATORY").build();
    }
    Membership oldMembership = organizationService.getMembershipHandler().findMembership(membershipId);
    if (oldMembership == null) {
      return Response.status(Response.Status.NOT_FOUND)
                     .entity("MEMBERSHIP:NOT_FOUND")
                     .build();
    }
    if (membershipId.equals(membership.getId())) {
      return Response.noContent().build();
    }
    if (organizationService.getMembershipHandler().findMembership(membership.getId()) != null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("MEMBERSHIP:ALREADY_EXISTS")
                     .build();
    }
    User user = organizationService.getUserHandler().findUserByName(membership.getUserName(), UserStatus.ANY);
    if (user == null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("USER:NOT_FOUND")
                     .build();
    }
    Group group = organizationService.getGroupHandler().findGroupById(membership.getGroupId());
    if (group == null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("GROUP:NOT_FOUND")
                     .build();
    }
    MembershipType membershipType = organizationService.getMembershipTypeHandler()
                                                       .findMembershipType(membership.getMembershipType());
    if (membershipType == null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("MEMBERSHIP_TYPE:NOT_FOUND")
                     .build();
    }

    organizationService.getMembershipHandler().removeMembership(membershipId, true);
    organizationService.getMembershipHandler().linkMembership(user, group, membershipType, true);
    return Response.noContent().build();
  }

  @DELETE
  @Path("memberships")
  @RolesAllowed("administrators")
  @Operation(
      summary = "Deletes an existing membership",
      description = "Deletes an existing membership",
      method = "DELETE")
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "204", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "User not authorized to call this endpoint"),
          @ApiResponse(responseCode = "500", description = "Internal server error")
      }
  )
  public Response deleteMembership(@Parameter(
      description = "Membership identifier with format: MEMBERSHIP_TYPE:USER_NAME:GROUP_ID", required = true
  ) @QueryParam(
    "membershipId"
  ) String membershipId) throws Exception {
    if (StringUtils.isBlank(membershipId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("MEMBERSHIP:MANDATORY").build();
    }
    if (organizationService.getMembershipHandler().findMembership(membershipId) == null) {
      return Response.status(Response.Status.NOT_FOUND)
                     .entity("NAME:NOT_FOUND")
                     .build();
    }
    organizationService.getMembershipHandler().removeMembership(membershipId, true);
    return Response.noContent().build();
  }

  @GET
  @Path("treeMembers")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Schema()
  @Operation(
          summary = "Gets groups tree",
          description = "Gets groups tree",
          method = "GET")
  @ApiResponses(value = {
          @ApiResponse(
                  responseCode = "200",
                  description = "Request fulfilled",
                  content = @Content(
                          schema = @Schema(implementation = CollectionEntity.class))),
         @ApiResponse(responseCode = "401", description = "User not authorized to call this endpoint"),
         @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getGroupsTreeMembers(@Context
  UriInfo uriInfo,
                                       @Parameter(description = "Search text to filter groups")
                                       @QueryParam("q")
                                       String q,
                                       @Parameter(description = "Group member")
                                       @QueryParam("groupMember")
                                       String groupMember,
                                       @Parameter(description = "Group type")
                                       @QueryParam("groupType")
                                       String groupType,
                                       @Parameter(description = "Offset")
                                       @QueryParam("offset")
                                       int offset,
                                       @Parameter(description = "Limit")
                                       @QueryParam("limit")
                                       int limit,
                                       @QueryParam("returnSize")
                                       boolean returnSize,
                                       @Parameter(description = "allGroupsForAdmin") 
                                       @QueryParam("allGroupsForAdmin")
                                       boolean allGroupsForAdmin,
                                       @Parameter(description = "List of excluded parent/type groups")
                                       @QueryParam("excludeParentGroup")
                                       List<String> excludeParentGroup) throws Exception {

    Identity identity;
    try {
      identity = ConversationState.getCurrent().getIdentity();
    } catch (Exception e) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }
    if (!userACL.isAdministrator(identity) && !identity.isMemberOf(groupMember)) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }
    if (StringUtils.isNotBlank(q)) {
      q = q.replace("#", " ").replace("$", " ").replace("_", " ").replace(".", " ");
    }

    offset = offset > 0 ? offset : DEFAULT_OFFSET;
    limit = limit > 0 ? limit : DEFAULT_LIMIT;
    Group[] groups = null;
    int totalSize = 0;
    Collection<Group> userGroupsList = null;
    if (allGroupsForAdmin && userACL.isAdministrator(identity)) {
      userGroupsList  = organizationService.getGroupHandler().findAllGroupsByKeyword(q, excludeParentGroup);
    } else {
      userGroupsList = organizationService.getGroupHandler()
              .findGroupsOfUserByKeyword(identity.getUserId(), q, excludeParentGroup);
    }
    totalSize = userGroupsList.size();
    int limitToFetch = limit;
    if (totalSize < (offset + limitToFetch)) {
      limitToFetch = totalSize - offset;
    }
    if (limitToFetch <= 0) {
      groups = new Group[0];
    } else {
      groups = userGroupsList.toArray(new Group[0]);
      if (!returnSize) {
        totalSize = 0;
      }
    }
    List<Group> groupsList = Arrays.asList(groups);
    CollectionEntity<Group> result = new CollectionEntity<>(groupsList, offset, limit, totalSize);
    return Response.ok(result).build();
  }

  @GET
  @Path("nested")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @Operation(
      summary = "Get nested groups",
      description = "Returns paginated nested groups of a parent group",
      method = "GET")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled successfully"),
      @ApiResponse(responseCode = "404", description = "Parent group not found"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public Response getNestedGroups(@Context UriInfo uriInfo,
                                  @Parameter(description = "Parent group id", required = true)
                                  @QueryParam("groupId")
                                  String groupId,
                                  @Parameter(description = "Pagination offset")
                                  @DefaultValue("0")
                                  @QueryParam("offset")
                                  int offset,
                                  @Parameter(description = "Pagination limit")
                                  @DefaultValue("20")
                                  @QueryParam("limit")
                                  int limit,
                                  @Parameter(description = "Return total size")
                                  @QueryParam("returnSize")
                                  boolean returnSize) throws Exception {

    offset = Math.max(offset, DEFAULT_OFFSET);
    limit = limit > 0 ? limit : DEFAULT_LIMIT;
    if (StringUtils.isBlank(groupId)) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    Group parentGroup = organizationService.getGroupHandler().findGroupById(groupId);
    if (parentGroup == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    List<Group> groups = organizationService.getGroupHandler()
                                            .getNestedMemberships(groupId)
                                            .stream()
                                            .map(NestedMembership::getNestedGroupId)
                                            .sorted(String.CASE_INSENSITIVE_ORDER)
                                            .map(id -> {
                                              try {
                                                return organizationService.getGroupHandler().findGroupById(id);
                                              } catch (Exception e) {
                                                return null;
                                              }
                                            })
                                            .filter(Objects::nonNull)
                                            .toList();
    int totalSize = groups.size();
    List<Group> paginatedGroups = groups.stream().skip(offset).limit(limit).toList();
    CollectionEntity<Group> result = new CollectionEntity<>(paginatedGroups, offset, limit, returnSize ? totalSize : 0);
    return Response.ok(result).build();
  }

  private void buildTree(List<GroupRestEntity> rootGroups,
                         Map<String, GroupRestEntity> groupsById,
                         GroupRestEntity groupRestEntity) throws Exception {
    groupsById.put(groupRestEntity.getId(), groupRestEntity);

    String parentId = groupRestEntity.getParentId();
    if (parentId == null) {
      if (!rootGroups.contains(groupRestEntity)) {
        rootGroups.add(groupRestEntity);
      }
      return;
    }
    GroupRestEntity parentGroupEntity = groupsById.get(parentId);
    if (parentGroupEntity == null) {
      Group parentGroup = organizationService.getGroupHandler().findGroupById(parentId);
      parentGroupEntity = new GroupRestEntity(parentGroup);
      groupsById.put(parentId, parentGroupEntity);
    }
    parentGroupEntity.addChild(groupRestEntity);
    buildTree(rootGroups, groupsById, parentGroupEntity);
  }
}
