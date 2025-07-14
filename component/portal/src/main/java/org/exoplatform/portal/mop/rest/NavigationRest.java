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
package org.exoplatform.portal.mop.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.rest.model.UserNodeRestEntity;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.ws.frameworks.json.impl.JsonParserImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Path("/v1/navigations")
@Tag(name = "v1/navigations", description = "Retrieve sites navigations")
public class NavigationRest implements ResourceContainer {

  private static final Log          LOG                  = ExoLogger.getLogger(NavigationRest.class);

  private static final Visibility[] DEFAULT_VISIBILITIES = Visibility.DEFAULT_VISIBILITIES;

  private UserPortalConfigService   portalConfigService;

  private LayoutService             layoutService;

  private OrganizationService       organizationService;

  private UserACL                   userACL;

  public NavigationRest(UserPortalConfigService portalConfigService,
                        LayoutService layoutService,
                        OrganizationService organizationService,
                        UserACL userACL) {
    this.portalConfigService = portalConfigService;
    this.layoutService = layoutService;
    this.organizationService = organizationService;
    this.userACL = userACL;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Gets navigations", description = "Gets navigations", method = "GET")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "404", description = "Navigation does not exist"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getSiteNavigation(
                                    @Context
                                    HttpServletRequest request,
                                    @Parameter(description = "Offset", required = false)
                                    @Schema(defaultValue = "0")
                                    @QueryParam("offset")
                                    int offset,
                                    @Parameter(description = "Limit, if equals to 0, it will use default limit.", required = false)
                                    @Schema(defaultValue = "20")
                                    @QueryParam("limit")
                                    int limit) {
    String portalName = portalConfigService.getMetaPortal();

    ConversationState state = ConversationState.getCurrent();
    Identity userIdentity = state == null ? null : state.getIdentity();
    String username = userIdentity == null ? null : userIdentity.getUserId();
    if (StringUtils.equals(username, IdentityConstants.ANONIM)) {
      username = null;
    }

    try {
      UserPortalConfig userPortalConfig = portalConfigService.getUserPortalConfig(portalName, username);
      List<ResultUserNavigation> allNavs = userPortalConfig.getUserPortal()
                                                           .getNavigations()
                                                           .stream()
                                                           .filter(this::isValidNavigation)
                                                           .map(ResultUserNavigation::new)
                                                           .toList();
      return Response.ok(allNavs).build();

    } catch (Exception e) {
      LOG.error("Error retrieving navigations", e);
      return Response.status(500).build();
    }

  }

  @Path("/{siteType}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Gets navigations of one or multiple site navigations", description = "Gets navigations of one or multiple site navigations", method = "GET")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "404", description = "Navigation does not exist"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getSiteTypeNavigations(
                                         @Context
                                         HttpServletRequest request,
                                         @Parameter(description = "Portal site type, possible values: PORTAL, GROUP or USER", required = true)
                                         @PathParam("siteType")
                                         String siteTypeName,
                                         @Parameter(description = "Portal site name", required = true)
                                         @QueryParam("siteName")
                                         String siteName,
                                         @Parameter(description = "Scope of navigations tree to retrieve, possible values: ALL, CHILDREN, GRANDCHILDREN, SINGLE", required = false)
                                         @Schema(defaultValue = "ALL")
                                         @QueryParam("scope")
                                         String scopeName,
                                         @Parameter(description = "parent navigation node id")
                                         @QueryParam("nodeId")
                                         String nodeId,
                                         @Parameter(description = "Multivalued visibilities of navigation nodes to retrieve, possible values: DISPLAYED, HIDDEN, SYSTEM or TEMPORAL. If empty, all visibilities will be used.", required = false)
                                         @Schema(defaultValue = "All possible values combined")
                                         @QueryParam("visibility")
                                         List<String> visibilityNames,
                                         @Parameter(description = "if to include Global site in results in portal type case", required = false)
                                         @DefaultValue("true")
                                         @QueryParam("includeGlobal")
                                         boolean includeGlobal,
                                         @Parameter(description = "to include extra node page details in results")
                                         @QueryParam("expand")
                                         boolean expand,
                                         @Parameter(description = "to check the navigation nodes scheduling start and end dates")
                                         @DefaultValue("true")
                                         @QueryParam("temporalCheck")
                                         boolean temporalCheck,
                                         @Parameter(description = "to expand the navigation breadcrumb")
                                         @DefaultValue("false")
                                         @QueryParam("expandBreadcrumb")
                                         boolean expandBreadcrumb) {
    // this function return nodes and not navigations
    if (StringUtils.isBlank(siteTypeName)) {
      return Response.status(400).build();
    }

    return getNavigations(request,
                          siteTypeName,
                          siteName,
                          scopeName,
                          nodeId,
                          visibilityNames,
                          includeGlobal,
                          expand,
                          temporalCheck,
                          expandBreadcrumb);
  }

  private Response getNavigations(HttpServletRequest request, // NOSONAR
                                  String siteTypeName,
                                  String siteName,
                                  String scopeName,
                                  String nodeId,
                                  List<String> visibilityNames,
                                  boolean includeGlobal,
                                  boolean expand,
                                  boolean temporalCheck,
                                  boolean expandBreadcrumb) {
    String username = request.getRemoteUser();
    if (StringUtils.equals(username, IdentityConstants.ANONIM)) {
      username = null;
    }

    SiteType siteType = null;
    try {
      siteType = SiteType.valueOf(StringUtils.upperCase(siteTypeName));
    } catch (Exception e) {
      return Response.status(400).entity("Bad site type: " + siteTypeName).build();
    }

    Scope scope = null;
    try {
      scope = getScope(scopeName);
    } catch (Exception e) {
      return Response.status(400).entity(e.getMessage()).build();
    }

    Visibility[] visibilities = null;
    try {
      visibilities = convertVisibilities(visibilityNames);
    } catch (Exception e) {
      return Response.status(400)
                     .entity("Bad visibility names: " + visibilityNames + ". should be one of " + DEFAULT_VISIBILITIES)
                     .build();
    }

    UserNodeFilterConfig userFilterConfig = getUserFilterConfig(visibilities, temporalCheck);

    String portalName = siteName;
    if (siteType != SiteType.PORTAL || StringUtils.isBlank(siteName)) {
      portalName = portalConfigService.getMetaPortal();
    }

    try {
      UserPortalConfig userPortalConfig = portalConfigService.getUserPortalConfig(portalName, username);
      if (userPortalConfig == null) {
        return Response.status(404).build();
      }

      UserPortal userPortal = userPortalConfig.getUserPortal();
      Collection<UserNode> nodes = new ArrayList<>();

      if (nodeId != null && siteName != null) {
        SiteKey siteKey = new SiteKey(siteTypeName, siteName);
        UserNode userNode = userPortal.getNodeById(nodeId, siteKey, scope, userFilterConfig, null);
        nodes.add(userNode);
      } else if (siteType == SiteType.PORTAL || StringUtils.isBlank(siteName)) {
        nodes = userPortal.getNodes(siteType, scope, userFilterConfig, includeGlobal);
      } else {
        UserNavigation navigation = userPortal.getNavigation(new SiteKey(siteType, siteName));
        if (navigation == null) {
          return Response.status(404).build();
        }
        UserNode rootNode = userPortal.getNode(navigation, scope, userFilterConfig, null);
        nodes = rootNode.getChildren();
      }
      List<UserNodeRestEntity> resultNodes = EntityBuilder.toUserNodeRestEntity(nodes, expand, organizationService, layoutService, userACL, userPortal, expandBreadcrumb);
      return Response.ok(resultNodes).build();
    } catch (Exception e) {
      LOG.error("Error retrieving Site {}/{} Navigation", siteTypeName, siteName, e);
      return Response.status(500).build();
    }
  }

  private static UserNodeFilterConfig getUserFilterConfig(Visibility[] visibilities, boolean temporalCheck) {
    UserNodeFilterConfig.Builder builder = UserNodeFilterConfig.builder();
    builder.withReadCheckNoPage().withVisibility(visibilities.length > 0 ? visibilities : DEFAULT_VISIBILITIES).withReadCheckNoPage();
    if (temporalCheck) {
      builder.withTemporalCheck();
    }
    return builder.build();
  }

  private Visibility[] convertVisibilities(List<String> visibilityNames) {
    if (visibilityNames == null) {
      return new Visibility[0];
    }
    return visibilityNames.stream()
                          .map(visibilityName -> Visibility.valueOf(StringUtils.upperCase(visibilityName)))
                          .toList()
                          .toArray(new Visibility[0]);
  }

  private Scope getScope(String scopeName) {
    Scope scope = null;
    if (StringUtils.isBlank(scopeName)) {
      scope = Scope.ALL;
    } else {
      switch (StringUtils.upperCase(scopeName)) {
      case "SINGLE":
        scope = Scope.SINGLE;
        break;
      case "CHILDREN":
        scope = Scope.CHILDREN;
        break;
      case "GRANDCHILDREN":
        scope = Scope.GRANDCHILDREN;
        break;
      case "ALL":
        scope = Scope.ALL;
        break;
      default:
        throw new IllegalStateException("Bad scope name: " + scopeName);
      }
    }
    return scope;
  }

  /**
   * A class to retrieve {@link UserNavigation} attributes by avoiding cyclic
   * JSON parsing of instance when retrieving {@link UserNavigation#getBundle()}
   * attributes using {@link JsonParserImpl}
   */
  public static final class ResultUserNavigation {
    public SiteKey getKey() {
      return key;
    }

    public String getLabel() {
      return label;
    }

    private SiteKey key;

    private String  label;

    public ResultUserNavigation(UserNavigation userNavigation) {
      key = userNavigation.getKey();
      if (key.getType().equals(SiteType.GROUP)) {
        try {
          OrganizationService orgService = ExoContainerContext.getCurrentContainer()
                                                              .getComponentInstanceOfType(OrganizationService.class);
          Group group = orgService.getGroupHandler().findGroupById(key.getName());
          if (group != null) {
            label = group.getLabel();
          }
        } catch (Exception e) {
          LOG.error("Error when getting group label {}", key.getName(), e);
        }
      } else {
        label = key.getName();
      }
    }

  }

  private boolean isValidNavigation(UserNavigation userNavigation) {
    return !userNavigation.getKey().getName().equals(portalConfigService.getGlobalPortal());
  }

}
