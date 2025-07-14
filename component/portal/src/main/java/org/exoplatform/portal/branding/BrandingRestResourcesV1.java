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
package org.exoplatform.portal.branding;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.branding.model.Branding;
import org.exoplatform.portal.branding.model.BrandingFile;
import org.exoplatform.services.rest.resource.ResourceContainer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/platform/branding")
@Tag(name = "/v1/platform/branding", description = "Managing branding information")
public class BrandingRestResourcesV1 implements ResourceContainer {

  private static final String IMAGE_MIME_TYPE        = "image/png";

  // 1 year
  private static final int    CACHE_IN_SECONDS       = 365 * 24 * 3600;

  private static final int    CACHE_IN_MILLI_SECONDS = CACHE_IN_SECONDS * 1000;

  private BrandingService     brandingService;

  public BrandingRestResourcesV1(BrandingService brandingService) {
    this.brandingService = brandingService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Get Branding information", description = "Get Branding information", method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
  })
  public Response getBrandingInformation() {
    Branding brandingInformation = brandingService.getBrandingInformation(false);
    return Response.ok(brandingInformation).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/default")
  @RolesAllowed("administrators")
  @Operation(summary = "Get default theme style", description = "Get default theme style", method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
  })
  public Response getDefaultBrandingThemeStyle() {
    Map<String, String> defaultThemeStyle = brandingService.getDefaultThemeStyle();
    return Response.ok(defaultThemeStyle).build();
  }

  @PUT
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Update Branding information", description = "Update Branding information", method = "PUT")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "204", description = "Branding information updated"),
  })
  public Response updateBrandingInformation(Branding branding) {
    brandingService.updateBrandingInformation(branding);
    return Response.noContent().build();
  }

  @GET
  @Path("/logo")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get Branding logo", description = "Get Branding logo", method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Branding logo retrieved"),
                          @ApiResponse(responseCode = "404", description = "Branding logo not found"),
                          @ApiResponse(responseCode = "500", description = "Server error when retrieving branding logo")
  })
  public Response getBrandingLogo(
                                  @Context
                                  Request request,
                                  @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
                                  @QueryParam("v")
                                  String version) {
    return getBrandingFileResponse(brandingService.getLogo(), request, version);
  }

  @GET
  @Path("/pageBackground")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get Default Page Background image", description = "Get Default Page Background image", method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fullfilled"),
                          @ApiResponse(responseCode = "404", description = "Object not found"),
  })
  public Response getPageBackground(
                                    @Context
                                    Request request,
                                    @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
                                    @QueryParam("v")
                                    String version) {
    return getBrandingFileResponse(brandingService.getPageBackground(), request, version);
  }

  @GET
  @Path("/topBarBackground")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get Default top bar Background image", description = "Get Default top bar Background image", method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fullfilled"),
          @ApiResponse(responseCode = "404", description = "Object not found"),
  })
  public Response getTopBarBackground(
          @Context
          Request request,
          @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
          @QueryParam("v")
          String version) {
    return getBrandingFileResponse(brandingService.getTopBarBackground(), request, version);
  }

  @GET
  @Path("/sideBarBackground")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get Default sidebar Background image", description = "Get Default sidebar Background image", method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fullfilled"),
          @ApiResponse(responseCode = "404", description = "Object not found"),
  })
  public Response getSideBarBackground(
          @Context
          Request request,
          @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
          @QueryParam("v")
          String version) {
    return getBrandingFileResponse(brandingService.getSideBarBackground(), request, version);
  }

  @GET
  @Path("/drawerBackground")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get Default drawer Background image", description = "Get Default drawer Background image", method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fullfilled"),
          @ApiResponse(responseCode = "404", description = "Object not found"),
  })
  public Response getDrawerBackground(
          @Context
          Request request,
          @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
          @QueryParam("v")
          String version) {
    return getBrandingFileResponse(brandingService.getDrawerBackground(), request, version);
  }

  @GET
  @Path("/favicon")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get Branding favicon", description = "Get Branding favicon", method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Branding favicon retrieved"),
                          @ApiResponse(responseCode = "404", description = "Branding favicon not found"),
                          @ApiResponse(responseCode = "500", description = "Server error when retrieving branding favicon")
  })
  public Response getBrandingFavicon(
                                     @Context
                                     Request request,
                                     @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
                                     @QueryParam("v")
                                     String version) {
    return getBrandingFileResponse(brandingService.getFavicon(), request, version);
  }

  @GET
  @Path("/loginBackground")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get authentication pages left panel background",
             description = "Get authentication pages left panel background", method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fullfilled"),
                          @ApiResponse(responseCode = "404", description = "Resource not found"),
                          @ApiResponse(responseCode = "500", description = "Server error when retrieving resource")
  })
  public Response getLoginBackground(
                                     @Context
                                     Request request,
                                     @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
                                     @QueryParam("v")
                                     String version) {
    return getBrandingFileResponse(brandingService.getLoginBackground(), request, version);
  }

  @GET
  @Path("/css")
  @Operation(summary = "Get Branding CSS content", description = "Get Branding CSS content", method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Branding css retrieved"),
                          @ApiResponse(responseCode = "304", description = "Branding css not modified"),
                          @ApiResponse(responseCode = "500", description = "Server error when retrieving branding css")
  })
  public Response getBrandingCSS(
                                 @Context
                                 Request request,
                                 @Parameter(description = "The value of lastModified parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
                                 @QueryParam("v")
                                 String lastModified) {
    long lastUpdated = brandingService.getLastUpdatedTime();
    EntityTag eTag = new EntityTag(String.valueOf(lastUpdated));
    Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
    if (builder == null || StringUtils.isBlank(lastModified)) {
      String themeCSS = brandingService.getThemeCSSContent();
      builder = Response.ok(themeCSS, "text/css");
      builder.tag(eTag);
      CacheControl cc = new CacheControl();
      cc.setMaxAge(86400);
      builder.type("text/css");
      builder.lastModified(new Date(lastUpdated));
      if (StringUtils.isNotBlank(lastModified)) {
        builder.expires(new Date(System.currentTimeMillis() + CACHE_IN_MILLI_SECONDS));
      }
      // If the query has a lastModified parameter, it means that the client
      // will change the lastModified entry when it really changes
      // Which means that we can cache the image in browser side
      // for a long time
      builder.cacheControl(cc);
    }
    return builder.build();
  }

  private Response getBrandingFileResponse(BrandingFile iconFile,
                                           Request request,
                                           String version) {
    if (iconFile == null || iconFile.getData() == null) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    long lastUpdated = iconFile.getUpdatedDate();
    EntityTag eTag = new EntityTag(String.valueOf(lastUpdated));
    Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
    if (builder == null || StringUtils.isBlank(version)) {
      InputStream stream = new ByteArrayInputStream(iconFile.getData());
      builder = Response.ok(stream, IMAGE_MIME_TYPE);
      // If the query has a lastModified parameter, it means that the client
      // will change the lastModified entry when it really changes
      // Which means that we can cache the image in browser side
      // for a long time
      if (StringUtils.isNotBlank(version)) {
        builder.tag(eTag);
        builder.lastModified(new Date(lastUpdated));
        builder.expires(new Date(System.currentTimeMillis() + CACHE_IN_MILLI_SECONDS));
        CacheControl cc = new CacheControl();
        cc.setMaxAge(CACHE_IN_SECONDS);
        builder.cacheControl(cc);
      }
    }
    return builder.build();
  }

}
