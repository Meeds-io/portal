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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.rest.model.MembershipRestEntity;
import org.exoplatform.portal.rest.model.UserRestEntity;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.organization.search.UserSearchService;
import org.exoplatform.services.rest.http.PATCH;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.web.login.recovery.ChangePasswordConnector;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Path("v1/users")
@Tag(name = "v1/users", description = "Manage User operations")
public class UserRestResourcesV1 implements ResourceContainer {

  public static final int                DEFAULT_LIMIT                  = 10;

  public static final String             PASSWORD_UNKNOWN_ERROR_CODE    = "PASSWORD_UNKNOWN_ERROR";

  public static final String             USER_NOT_FOUND_ERROR_CODE      = "USER_NOT_FOUND";

  public static final String             WRONG_USER_PASSWORD_ERROR_CODE = "WRONG_USER_PASSWORD";
  
  public static final String             CHANGE_PASSWORD_NOT_ALLOWED = "CHANGE_PASSWORD_NOT_ALLOWED";

  private static final Log               LOG                               = ExoLogger.getLogger(UserRestResourcesV1.class);

  private static final String            ADMINISTRATOR_GROUP            = "/platform/administrators";

  private static final String            GROUP_OBJECT_TYPE = "group";

  private static final String            GROUP_LIST_MEMBERS_PERMISSION_TYPE = "listMembers";

  public static final String             UNCHANGED_NEW_PASSWORD_ERROR_CODE = "UNCHANGED_NEW_PASSWORD";

  public static final String             CREATION_SOURCE_UI             = UserAccountService.CREATION_SOURCE_UI;

  public static final String             OTP_CODE_INVALID               = "OTP_CODE_INVALID";

  public static final UserFieldValidator USERNAME_VALIDATOR             = new UserFieldValidator("userName", true, false);

  public static final UserFieldValidator EMAIL_VALIDATOR                = new UserFieldValidator("email", false, false);

  public static final UserFieldValidator LASTNAME_VALIDATOR             = new UserFieldValidator("lastName", false, true);

  public static final UserFieldValidator FIRSTNAME_VALIDATOR            = new UserFieldValidator("firstName", false, true);

  public static final UserFieldValidator PASSWORD_VALIDATOR             = new UserFieldValidator("password", false, false, 8, 255);

  private OrganizationService            organizationService;

  private UserSearchService              userSearchService;

  private UserACL                        userACL;
  
  private PasswordRecoveryService        passwordRecoveryService;

  private UserAccountService             userAccountService;

  public UserRestResourcesV1(OrganizationService organizationService,
                             UserSearchService userSearchService,
                             UserACL userACL,
                             PasswordRecoveryService passwordRecoveryService) {
    this.organizationService = organizationService;
    this.userSearchService = userSearchService;
    this.userACL = userACL;
    this.passwordRecoveryService = passwordRecoveryService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @Operation(
      summary = "Gets all users",
      description = "Gets all users",
      method = "GET"
  )
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "500", description = "Internal server error due to data encoding"),
      }
  )
  public Response getUsers(
                           @Parameter(
                               description = "User name information to filter, ex: user name, last name, first name or full name",
                               required = false
                           ) @QueryParam("q") String q,
                           @Parameter(description = "User status : ANY, ENABLED or DISABLED", required = false) @Schema(defaultValue = "ANY")
                           @QueryParam("status") String status,
                           @Parameter(description = "Offset", required = false) @Schema(defaultValue = "0")
                           @QueryParam("offset") int offset,
                           @Parameter(description = "Limit", required = false) @Schema(defaultValue = "10")
                           @QueryParam("limit") int limit,
                           @Parameter(description = "Returning the number of users found or not") @Schema(defaultValue = "false")
                           @QueryParam("returnSize") boolean returnSize) throws Exception {

    offset = offset > 0 ? offset : 0;
    limit = limit > 0 ? limit : DEFAULT_LIMIT;

    UserStatus userStatus = StringUtils.isBlank(status) ||
        StringUtils.equalsIgnoreCase("ALL", status) ||
        StringUtils.equalsIgnoreCase("ANY", status) ? UserStatus.ANY : UserStatus.valueOf(status.toUpperCase());

    User[] users;
    int totalSize = 0;
    if (StringUtils.isBlank(q)) {
      ListAccess<User> allUsersListAccess = organizationService.getUserHandler().findAllUsers(userStatus);
      totalSize = allUsersListAccess.getSize();
      int limitToFetch = limit;
      if (totalSize < (offset + limitToFetch)) {
        limitToFetch = totalSize - offset;
      }
      if (limitToFetch <= 0) {
        users = new User[0];
      } else {
        users = allUsersListAccess.load(offset, limitToFetch);
        if (!returnSize) {
          totalSize = 0;
        }
      }
    } else {
      ListAccess<User> usersListAccess = userSearchService.searchUsers(q, userStatus);
      totalSize = usersListAccess.getSize();
      int limitToFetch = limit;
      if (totalSize < (offset + limitToFetch)) {
        limitToFetch = totalSize - offset;
      }
      if (limitToFetch <= 0) {
        users = new User[0];
      } else {
        users = usersListAccess.load(offset, limitToFetch);
        if (!returnSize) {
          totalSize = 0;
        }
      }
    }
    List<UserRestEntity> userEntities = Arrays.stream(users)
                                              .map(this::toEntity)
                                              .toList();
    CollectionEntity<UserRestEntity> result = new CollectionEntity<>(userEntities,
                                                                     offset,
                                                                     limit,
                                                                     totalSize);
    return Response.ok(result).build();
  }

  @POST
  @RolesAllowed("administrators")
  @Operation(
      summary = "Create new user",
      description = "Creates an account: the login is the given one or a generated one, the password is optional,"
          + " the role (INTERNAL, EXTERNAL or ADMINISTRATOR) decides the memberships, the language is the preferred one",
      method = "POST")
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "201", description = "Account created, the body carries its login"),
          @ApiResponse(responseCode = "400", description = "A refused field, the body being its code: USERNAME:*, PASSWORD:*,"
              + " FIRSTNAME:*, LASTNAME:*, EMAIL:*, ROLE:INVALID, LANGUAGE:INVALID"),
          @ApiResponse(responseCode = "403", description = "The session has not been confirmed with a one-time password: OTP_REQUIRED"),
          @ApiResponse(responseCode = "500", description = "The store did not complete the creation, the body being the code:"
              + " USER:CREATION_FAILED and USER:USERNAME_GENERATION_EXHAUSTED (nothing created),"
              + " USER:CREATED_INCOMPLETE:<login>:<parts> (created, then deactivated),"
              + " USER:CREATED_INCOMPLETE_ENABLED:<login>:<parts> (created, still active)"),
      }
  )
  public Response createUser(@Context HttpServletRequest request,
                             @RequestBody(description = "User Object") UserRestEntity userEntity) throws Exception {
    if (!UserAccountConfirmation.isConfirmed(request)) {
      return UserAccountConfirmation.otpRequired();
    }
    if (userEntity == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("empty user object").build();
    }
    Locale locale = request == null ? Locale.ENGLISH : request.getLocale();
    UserAccount account = new UserAccount(userEntity.getUserName(),
                                          userEntity.getFirstName(),
                                          userEntity.getLastName(),
                                          userEntity.getEmail(),
                                          userEntity.getPassword(),
                                          userEntity.getLanguage(),
                                          userEntity.getRole(),
                                          userEntity.isEnabled());
    String userName;
    try {
      userName = getUserAccountService().createUser(account, locale);
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (UserAccountCreationException e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
    // The login may have been generated: the client needs it to send the
    // onboarding email
    return Response.created(URI.create("/portal/rest/v1/users/" + userName))
                   .entity(Collections.singletonMap("userName", userName))
                   .type(MediaType.APPLICATION_JSON)
                   .build();
  }

  @GET
  @Path("onboardingSettings")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @Operation(
      summary = "Gets the onboarding settings",
      description = "Gets the validity of an onboarding link, in days and in hours, as configured on the platform",
      method = "GET")
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      }
  )
  public Response getOnboardingSettings() {
    long validityMillis = passwordRecoveryService.getOnboardingTokenValidity();
    long days = TimeUnit.MILLISECONDS.toDays(validityMillis);
    // The hours and the whole-days flag let a client state the validity the
    // way the email states it: in days when it is a whole number of days
    // from two up, in whole hours otherwise. The rule mirrors
    // PasswordRecoveryServiceImpl.isWholeDays, out of reach of this module;
    // the two are pinned by their tests
    Map<String, Object> settings = new LinkedHashMap<>();
    settings.put("linkValidityDays", days);
    settings.put("linkValidityHours", TimeUnit.MILLISECONDS.toHours(validityMillis));
    settings.put("linkValidityWholeDays", days >= 2 && validityMillis == TimeUnit.DAYS.toMillis(days));
    return Response.ok(settings)
                   .cacheControl(privateNoStore())
                   .build();
  }

  @PUT
  @RolesAllowed("users")
  @Operation(
      summary = "Update an existing user",
      description = "Update an existing user: names, email, password when given, enabled flag, and the preferred"
          + " language when given, a blank one leaving the stored one unchanged",
      method = "PUT")
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input, or a language that is not a supported"
              + " locale: LANGUAGE:INVALID"),
          @ApiResponse(responseCode = "404", description = "Unknown user"),
          @ApiResponse(responseCode = "403", description = "User not administrator, or the session has not been confirmed with a one-time password: OTP_REQUIRED"),
          @ApiResponse(responseCode = "500", description = "The update could not be proven: USER:PASSWORD_NOT_STORED or"
              + " USER:PASSWORD_NOT_VERIFIABLE, nothing written after the password; USER:UPDATE_INCOMPLETE, a field read back"
              + " other than requested"),
      }
  )
  public Response updateUser(@Context HttpServletRequest request,
                             @RequestBody(description = "User Object") UserRestEntity userEntity) throws Exception {
    Identity identity = ConversationState.getCurrent().getIdentity();
    if (!userACL.isAdministrator(identity)) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }
    if (!UserAccountConfirmation.isConfirmed(request)) {
      return UserAccountConfirmation.otpRequired();
    }
    if (userEntity == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("empty user object").build();
    }

    User user = organizationService.getUserHandler().findUserByName(userEntity.getUserName(), UserStatus.ANY);
    if (user == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    UserAccount account = new UserAccount();
    account.setUserName(userEntity.getUserName());
    account.setFirstName(userEntity.getFirstName());
    account.setLastName(userEntity.getLastName());
    account.setEmail(userEntity.getEmail());
    account.setPassword(userEntity.getPassword());
    account.setLanguage(userEntity.getLanguage());
    account.setEnabled(userEntity.isEnabled());
    try {
      getUserAccountService().updateUser(account, request == null ? Locale.ENGLISH : request.getLocale(), getCurrentUsername());
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (UserAccountUpdateException e) {
      return Response.serverError().entity(e.getMessage()).build();
    }
    return Response.noContent().build();
  }

  @DELETE
  @RolesAllowed("administrators")
  @Path("{id}")
  @Operation(
          summary = "Deletes a user identified by its id",
          description = "Deletes a user identified by its id",
          method = "DELETE")
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "204", description = "Request fulfilled"),
          @ApiResponse(responseCode = "401", description = "User not authorized to call this endpoint"),
          @ApiResponse(responseCode = "403", description = "The session has not been confirmed with a one-time password: OTP_REQUIRED"),
          @ApiResponse(responseCode = "404", description = "User not found"),
          @ApiResponse(responseCode = "500", description = "Internal server error"),
      }
  )
  public Response deleteUser(@Context HttpServletRequest request,
                             @Parameter(description = "User name identifier", required = true) @PathParam(
    "id"
  ) String userName) throws Exception {
    if (!UserAccountConfirmation.isConfirmed(request)) {
      return UserAccountConfirmation.otpRequired();
    }
    User user = organizationService.getUserHandler().findUserByName(userName, UserStatus.ANY);
    if (user == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    String currentUsername = getCurrentUsername();
    if (StringUtils.equals(currentUsername, user.getUserName())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("SelfDelete").build();
    }
    if (StringUtils.equals(userACL.getSuperUser(), user.getUserName())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("DeleteSuperUser").build();
    }
    organizationService.getUserHandler().removeUser(userName, true);
    return Response.noContent().build();
  }

  @GET
  @RolesAllowed("users")
  @Path("{id}")
  @Operation(
          summary = "Gets a user identified by its id",
          description = "Gets a user identified by its id",
          method = "GET")
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "401", description = "User not authorized to call this endpoint"),
          @ApiResponse(responseCode = "404", description = "User not found"),
          @ApiResponse(responseCode = "500", description = "Internal server error"),
      }
  )
  public Response getUser(@Context UriInfo uriInfo,
                          @Parameter(description = "User name identifier", required = true) @PathParam(
                            "id"
                          ) String id) throws Exception {
    Identity identity = ConversationState.getCurrent().getIdentity();
    if (!userACL.isAdministrator(identity) && !identity.getUserId().equals(id)) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    User user = organizationService.getUserHandler().findUserByName(id, UserStatus.ANY);

    if (user == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    UserRestEntity userRestEntity = toEntity(user);
    try {
      userRestEntity.setLanguage(getUserAccountService().getLanguage(id));
    } catch (IllegalStateException e) {
      // The account is read without its language, which an edit then
      // leaves unchanged
      LOG.warn("The preferred language of {} could not be read", id, e);
    }
    userRestEntity.setIsInternal(user.isInternalStore());

    Collection<Membership> memberships = organizationService.getMembershipHandler().findMembershipsByUser(id);
    if (memberships != null) {
      userRestEntity.setPlatformAdministrator(memberships.stream()
                                                         .anyMatch(membership -> membership.getGroupId()
                                                                                           .equals(userACL.getAdminGroups())));
    }

    return Response.ok(userRestEntity, MediaType.APPLICATION_JSON).build();
  }

  @PATCH
  @RolesAllowed("users")
  @Path("{id}/changePassword")
  @Operation(
          summary = "Changes user password",
          description = "Changes user password",
          method = "POST")
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "401", description = "User not authorized to call this endpoint"),
          @ApiResponse(responseCode = "404", description = "User not found"),
          @ApiResponse(responseCode = "500", description = "Internal server error")
      }
  )
  public Response changePassword(
                                 @Context HttpServletRequest request,
                                 @Parameter(description = "username to change his password", required = true) @PathParam(
                                   "id"
                                 ) String username,
                                 @Parameter(description = "Current user password", required = true) @FormParam(
                                   "currentPassword"
                                 ) String currentPassword,
                                 @Parameter(description = "New user password", required = true) @FormParam(
                                   "newPassword"
                                 ) String newPassword) {
    Identity identity = ConversationState.getCurrent().getIdentity();
    boolean isAdmin = userACL.isAdministrator(identity);
    boolean isSameUser = identity.getUserId().equals(username);
    if (!isAdmin && !isSameUser) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }
    if (!isSameUser && !UserAccountConfirmation.isConfirmed(request)) {
      return UserAccountConfirmation.otpRequired();
    }

    try {
      UserHandler userHandler = organizationService.getUserHandler();
      User user = userHandler.findUserByName(username, UserStatus.ANY);
      if (user == null) {
        return Response.serverError().entity(USER_NOT_FOUND_ERROR_CODE).build();
      }
  
      if (!passwordRecoveryService.allowChangePassword(user.getUserName())) {
        return Response.serverError().entity(CHANGE_PASSWORD_NOT_ALLOWED).build();
      }
  
      if (isSameUser && !userHandler.authenticate(username, currentPassword)) {
        return Response.serverError().entity(WRONG_USER_PASSWORD_ERROR_CODE).build();
      }

      if (isSameUser && userHandler.authenticate(username, newPassword))  {
        return Response.serverError().entity(UNCHANGED_NEW_PASSWORD_ERROR_CODE).build();
      }
  
      Locale locale = request.getLocale();
  
      String errorMessage = PASSWORD_VALIDATOR.validate(locale, newPassword);
      if (StringUtils.isNotBlank(errorMessage)) {
        return Response.serverError().entity(errorMessage).build();
      }
  
      ChangePasswordConnector activeChangePasswordConnector = passwordRecoveryService.getActiveChangePasswordConnector();
      activeChangePasswordConnector.changePassword(user.getUserName(),newPassword);
      return Response.noContent().build();
    } catch (Exception e) {
      return Response.serverError().entity(PASSWORD_UNKNOWN_ERROR_CODE + ":" + e.getMessage()).build();
    }
  }

  @POST
  @Path("confirmation")
  @RolesAllowed("users")
  @Operation(
      summary = "Confirms the current session with a one-time password",
      description = "Validates the one-time password sent by email to the authenticated user and marks the current"
          + " session as confirmed, so that the administrator actions on user accounts are applied from it."
          + " The code validates once. The confirmation proves the caller's identity only: each guarded"
          + " endpoint keeps its own permission check.",
      method = "POST")
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "204", description = "Session confirmed"),
          @ApiResponse(responseCode = "401", description = "Wrong, expired, already used or throttled code: OTP_CODE_INVALID"),
      }
  )
  public Response confirmSession(@Context HttpServletRequest request,
                                 @Parameter(description = "One-time password received by email", required = true)
                                 @FormParam("otpCode") String otpCode) {
    String username = getCurrentUsername();
    try {
      getUserAccountService().confirmSession(username, otpCode);
    } catch (IllegalAccessException e) {
      return Response.status(Response.Status.UNAUTHORIZED).entity(OTP_CODE_INVALID).build();
    }
    UserAccountConfirmation.confirm(request, username);
    return Response.noContent().build();
  }

  @GET
  @Path("{id}/memberships")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(
      summary = "Gets User memberships list",
      description = "Gets User memberships list",
      method = "GET")
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "403", description = "User not allowed to list the group memberships"),
          @ApiResponse(responseCode = "404", description = "User not found"),
          @ApiResponse(responseCode = "500", description = "Internal server error due to data encoding"),
      }
  )
  public Response getUserMemberships(
                                     @Parameter(description = "User name identifier", required = true) @PathParam("id") String userName,
                                     @Parameter(description = "Group id to filter only its memberships, ex: /platform")
                                     @QueryParam("groupId")
                                     String groupId,
                                     @Parameter(description = "When filtering by groupId, whether to also include memberships on groups nested, at any level, inside the given group")
                                     @Schema(defaultValue = "false")
                                     @QueryParam("includeNestedGroups")
                                     boolean includeNestedGroups,
                                     @Parameter(description = "Offset", required = false) @Schema(defaultValue = "0")
                                     @QueryParam("offset") int offset,
                                     @Parameter(description = "Limit", required = false) @Schema(defaultValue = "20")
                                     @QueryParam("limit") int limit,
                                     @Parameter(description = "Returning the number of users found or not") @Schema(defaultValue = "false")
                                     @QueryParam("returnSize")
                                     boolean returnSize) throws Exception {

    boolean isAdmin = isMemberOfAdminGroup();
    if (!isAdmin && !canListGroupMemberships(groupId)) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }
    offset = offset > 0 ? offset : 0;
    limit = limit > 0 ? limit : DEFAULT_LIMIT;

    User user = organizationService.getUserHandler().findUserByName(userName, UserStatus.ANY);
    if (user == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    List<MembershipRestEntity> membershipEntities = new ArrayList<>();
    int totalSize = 0;
    if (StringUtils.isBlank(groupId)) {
      ListAccess<Membership> membershipsByUser = organizationService.getMembershipHandler().findAllMembershipsByUser(user);
      totalSize = membershipsByUser.getSize();
      Membership[] memberships;
      int limitToFetch = limit;
      if (totalSize < (offset + limitToFetch)) {
        limitToFetch = totalSize - offset;
      }
      if (limitToFetch > 0) {
        memberships = membershipsByUser.load(offset, limitToFetch);
        for (Membership membership : memberships) {
          Group group = organizationService.getGroupHandler().findGroupById(membership.getGroupId());
          membershipEntities.add(new MembershipRestEntity(membership, group, user));
        }
        if (!returnSize) {
          totalSize = 0;
        }
      }
    } else {
      List<Membership> memberships = organizationService.getMembershipHandler().findMembershipsByUser(userName)
                                                      .stream()
                                                      .filter(m -> StringUtils.equals(groupId, m.getGroupId())
                                                                   || (includeNestedGroups
                                                                       && isNestedInGroup(m.getGroupId(), groupId)))
                                                      .toList();
      totalSize = memberships.size();
      memberships = memberships.stream()
                                .skip(offset)
                                .limit(limit)
                                .toList();
      memberships.forEach(m -> {
        try {
          Group group = organizationService.getGroupHandler().findGroupById(m.getGroupId());
          membershipEntities.add(new MembershipRestEntity(m.getMembershipType(), group, user));
        } catch (Exception e) {
          LOG.warn("Error retrieving group {}", m.getGroupId(), e);
        }
      });
    }
    return Response.ok(new CollectionEntity<>(membershipEntities, offset, limit, totalSize)).build();
  }

  @GET
  @Path("isSuperUser")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(
      summary = "Check if current user is a superUser",
      description = "Check if current user is a superUser",
      method = "GET")
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "500", description = "Internal server error due to data encoding"),
      }
  )
  public Response isSuperUser() {
    return Response.ok().entity("{\"isSuperUser\":\"" + userACL.isSuperUser(ConversationState.getCurrent().getIdentity()) + "\"}").build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("isSynchronizedUserAllowedToChangePassword")
  @RolesAllowed("users")
  @Operation(
          summary = "Check if synchronized user is allowed to change his password",
          description = "Check if synchronized user is allowed to change his password",
          method = "GET")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "500", description = "Internal server error due to data encoding")})
  public Response isSynchronizedUserAllowedToChangePassword(@Context UriInfo uriInfo) throws Exception {

    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    User user = organizationService.getUserHandler().findUserByName(userId, UserStatus.ANY);
    if (user == null) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
    boolean allowChangePassword = user.isInternalStore() || (System.getProperty("exo.portal.allow.change.external.password") != null && Boolean.valueOf(System.getProperty("exo.portal.allow.change.external.password").toString()));
    return Response.ok().entity("{\"isSynchronizedUserAllowedToChangePassword\":\"" + allowChangePassword + "\"}").build();
  }

  private UserRestEntity toEntity(User user) {
    return new UserRestEntity(user.getUserName(),
                              user.getFirstName(),
                              user.getLastName(),
                              user.getDisplayName(),
                              user.getEmail(),
                              user.isEnabled(),
                              false);
  }

  public static String getCurrentUsername() {
    org.exoplatform.services.security.Identity currentIdentity =
                                                               ConversationState.getCurrent() == null ? null
                                                                                                      : ConversationState.getCurrent()
                                                                                                                         .getIdentity();
    if (currentIdentity == null) {
      return null;
    }
    return currentIdentity.getUserId();
  }

  public boolean isMemberOfAdminGroup() {
    return ConversationState.getCurrent().getIdentity().isMemberOf(ADMINISTRATOR_GROUP);
  }

  /**
   * The service is implemented in the web security module, above this one in
   * the module graph, and registered in the container at runtime by the
   * Kernel-Spring bridge: it is resolved by type at call time, once
   */
  private UserAccountService getUserAccountService() {
    if (userAccountService == null) {
      userAccountService = ExoContainerContext.getService(UserAccountService.class);
    }
    return userAccountService;
  }

  /**
   * The cache directives of an answer meant for the administrator's session
   * only
   */
  private static CacheControl privateNoStore() {
    CacheControl cacheControl = new CacheControl();
    cacheControl.setPrivate(true);
    cacheControl.setNoStore(true);
    cacheControl.setNoTransform(false);
    return cacheControl;
  }

  private boolean isNestedInGroup(String groupId, String targetGroupId) {
    return isNestedInGroup(groupId, targetGroupId, new HashSet<>());
  }

  private boolean isNestedInGroup(String groupId, String targetGroupId, Set<String> visitedGroupIds) {
    if (!visitedGroupIds.add(groupId)) { // Cycle guard
      return false;
    }
    try {
      Group group = organizationService.getGroupHandler().findGroupById(groupId);
      return group != null
             && CollectionUtils.isNotEmpty(group.getEnclosingMemberships())
             && group.getEnclosingMemberships()
                     .stream()
                     .anyMatch(enclosingMembership -> StringUtils.equals(targetGroupId, enclosingMembership.getGroupId())
                                                      || isNestedInGroup(enclosingMembership.getGroupId(),
                                                                         targetGroupId,
                                                                         visitedGroupIds));
    } catch (Exception e) {
      LOG.warn("Error retrieving enclosing groups of group {}", groupId, e);
      return false;
    }
  }

  /**
   * Check if the authenticated user can list the memberships of the given
   * group, checked through the 'group' ACL plugin contributed at runtime by
   * the social addon
   *
   * @param groupId group id to check
   * @return true if the authenticated user can list the group memberships
   */
  private boolean canListGroupMemberships(String groupId) {
    return StringUtils.isNotBlank(groupId)
           && userACL.hasAclPlugin(GROUP_OBJECT_TYPE)
           && userACL.hasPermission(GROUP_OBJECT_TYPE,
                                    groupId,
                                    GROUP_LIST_MEMBERS_PERMISSION_TYPE,
                                    ConversationState.getCurrent().getIdentity());
  }

}
