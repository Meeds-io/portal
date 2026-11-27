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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
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
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.branding.model.Branding;
import org.exoplatform.portal.branding.model.BrandingFile;
import org.exoplatform.portal.branding.model.ThemeStylesheet;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

import io.meeds.portal.theme.ThemeReadiness;
import io.meeds.portal.theme.model.BrandingState;
import io.meeds.portal.theme.model.ImageBytes;
import io.meeds.portal.theme.rest.model.ThemesView;
import io.meeds.portal.theme.rest.util.ThemeRestUtils;
import io.meeds.portal.theme.service.ThemeService;
import io.meeds.portal.theme.util.BrandingImageValidator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/platform/branding")
@Tag(name = "/v1/platform/branding", description = "Managing branding information")
public class BrandingRestResourcesV1 implements ResourceContainer {

  private static final Log LOG = ExoLogger.getLogger(BrandingRestResourcesV1.class);

  private static final String IMAGE_MIME_TYPE        = "image/png";

  // 1 year
  private static final int    CACHE_IN_SECONDS       = 365 * 24 * 3600;

  private static final long   CACHE_IN_MILLI_SECONDS = CACHE_IN_SECONDS * 1000L;

  private BrandingService     brandingService;

  private ThemeService        themeService;

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
                          @ApiResponse(responseCode = "400", description = "Invalid value, the body is the message code"),
                          @ApiResponse(responseCode = "503", description = "The theme services are still starting"),
  })
  public Response updateBrandingInformation(Branding branding) {
    if (getThemeService() == null) {
      // before readiness no field is validated nor written: the writer needs the theme services
      return themeServicesStarting();
    }
    try {
      brandingService.updateBrandingInformation(branding);
      return Response.noContent().build();
    } catch (IllegalArgumentException e) {
      // expected refusal of an invalid theme or CSS value: the message code lets the Branding UI explain it
      LOG.debug("Branding update refused: {}", e.getMessage());
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
    } catch (IllegalStateException e) {
      int status = ThemeRestUtils.statusOf(e);
      if (status == 500) {
        LOG.warn("Branding update failed", e);
      } else {
        LOG.debug("Branding update refused: {}", e.getMessage());
      }
      return Response.status(status).entity(status == 500 ? ThemeService.PUBLISH_FAILED_MESSAGE : e.getMessage())
                     .type(MediaType.TEXT_PLAIN)
                     .build();
    }
  }

  @GET
  @Path("/themes")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @Operation(summary = "Get the themes and the published branding state",
             description = "Every theme as stored, a configured theme's configured values beside its own, the published state and its version",
             method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "503", description = "The theme services are still starting"),
  })
  public Response getThemes() {
    ThemeService service = getThemeService();
    if (service == null) {
      return themeServicesStarting();
    }
    BrandingState state = service.getState();
    ThemesView view = new ThemesView(state.getVersion(),
                                     state.getCompanyName(),
                                     brandingService.getCompanyName(),
                                     state.getLogoFileId(),
                                     state.getDarkLogoFileId(),
                                     state.getFaviconFileId(),
                                     state.getDarkFaviconFileId(),
                                     state.getThemeInUseId(),
                                     service.getThemeInUse().getId(),
                                     state.getModesOffered(),
                                     service.getModesOffered(),
                                     viewThemes(service));
    return Response.ok(ThemeRestUtils.toJson(view), MediaType.APPLICATION_JSON).cacheControl(noStore()).build();
  }

  @PUT
  @Path("/publication")
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Publish the Branding and Theme page",
             description = "Publishes, in one transaction, every change the page made since it loaded the given version",
             method = "PUT")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Published, the body holds the new version"),
                          @ApiResponse(responseCode = "400", description = "Invalid value, the body is the message code"),
                          @ApiResponse(responseCode = "403", description = "The user is not a platform administrator"),
                          @ApiResponse(responseCode = "404", description = "A theme the publication names does not exist"),
                          @ApiResponse(responseCode = "409", description = "The published state changed since the page loaded"),
                          @ApiResponse(responseCode = "503", description = "The theme services are still starting"),
  })
  public Response publish(String body) {
    ThemeService service = getThemeService();
    if (service == null) {
      return themeServicesStarting();
    }
    try {
      long version = service.publish(ThemeRestUtils.readPublication(body), getCurrentUsername());
      return Response.ok("{\"version\":" + version + "}", MediaType.APPLICATION_JSON).build();
    } catch (IllegalAccessException e) {
      LOG.debug("Branding publication refused", e);
      return Response.status(Response.Status.FORBIDDEN).build();
    } catch (ObjectNotFoundException e) {
      LOG.debug("Branding publication names an unknown theme", e);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (IllegalArgumentException | IllegalStateException e) {
      int status = ThemeRestUtils.statusOf(e);
      if (status == 500) {
        LOG.warn("Branding publication failed", e);
      } else {
        LOG.debug("Branding publication refused: {}", e.getMessage());
      }
      return Response.status(status).entity(status == 500 ? ThemeService.PUBLISH_FAILED_MESSAGE : e.getMessage())
                     .type(MediaType.TEXT_PLAIN)
                     .build();
    }
  }

  @PUT
  @Path("/customCss")
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Save the custom CSS",
             description = "Body {\"customCss\": \"<css, blank to remove it>\"}; saved apart from a publication, "
                 + "which moves the stylesheet version",
             method = "PUT")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "204", description = "Custom CSS saved"),
                          @ApiResponse(responseCode = "400",
                                       description = "Forbidden value (branding.customCss.forbiddenValue) or invalid body"),
                          @ApiResponse(responseCode = "403", description = "The user is not a platform administrator"),
                          @ApiResponse(responseCode = "503", description = "The theme services are still starting"),
  })
  public Response saveCustomCss(String body) {
    ThemeService service = getThemeService();
    if (service == null) {
      return themeServicesStarting();
    }
    try {
      service.saveCustomCss(ThemeRestUtils.readCustomCss(body), getCurrentUsername());
      return Response.noContent().build();
    } catch (IllegalAccessException e) {
      LOG.debug("Custom CSS save refused", e);
      return Response.status(Response.Status.FORBIDDEN).build();
    } catch (IllegalArgumentException | IllegalStateException e) {
      int status = ThemeRestUtils.statusOf(e);
      if (status == 500) {
        LOG.warn("Custom CSS save failed", e);
      } else {
        LOG.debug("Custom CSS save refused: {}", e.getMessage());
      }
      return Response.status(status).entity(status == 500 ? ThemeService.PUBLISH_FAILED_MESSAGE : e.getMessage())
                     .type(MediaType.TEXT_PLAIN)
                     .build();
    }
  }

  @POST
  @Path("/themes/resolve")
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Resolve a theme being edited",
             description = "Both versions' effective values, Auto and Automatic computed, and their contrast ratios, of a theme as the page holds it; nothing is stored",
             method = "POST")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "400", description = "Invalid body, the body is the message code"),
                          @ApiResponse(responseCode = "403", description = "The user is not a platform administrator"),
                          @ApiResponse(responseCode = "404", description = "The theme or its source does not exist"),
                          @ApiResponse(responseCode = "503", description = "The theme services are still starting"),
  })
  public Response resolveTheme(String body) {
    ThemeService service = getThemeService();
    if (service == null) {
      return themeServicesStarting();
    }
    try {
      return Response.ok(ThemeRestUtils.toJson(service.resolve(ThemeRestUtils.readTheme(body), getCurrentUsername())),
                         MediaType.APPLICATION_JSON)
                     .cacheControl(noStore())
                     .build();
    } catch (IllegalAccessException e) {
      LOG.debug("Theme preview refused", e);
      return Response.status(Response.Status.FORBIDDEN).build();
    } catch (ObjectNotFoundException e) {
      LOG.debug("Theme preview names an unknown theme", e);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (IllegalArgumentException e) {
      LOG.debug("Theme preview refused: {}", e.getMessage());
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
    }
  }

  @GET
  @Path("/themes/{themeId}/images/{key}")
  @Operation(summary = "Get a theme's background image",
             description = "The image a theme's version shows for an image key; a dark version without its own image shares the light one",
             method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Image served"),
                          @ApiResponse(responseCode = "404", description = "No such theme, key or image"),
  })
  public Response getThemeImage(@PathParam("themeId")
                                long themeId,
                                @PathParam("key")
                                String key,
                                @QueryParam("version")
                                String version,
                                @Parameter(description = "The file id the stylesheet references: the answer is cached a year when it is the current one")
                                @QueryParam("v")
                                String fileVersion) {
    ThemeService service = getThemeService();
    if (service == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    try {
      Long fileId = service.getThemeImageFileId(themeId, key, "dark".equals(version));
      ImageBytes image = fileId == null ? null : service.readImage(fileId);
      if (image == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      Response.ResponseBuilder builder = imageResponse(image.data(), image.type());
      if (String.valueOf(fileId).equals(fileVersion)) {
        // a file's content never changes: its id in the URL makes it cacheable for good
        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(CACHE_IN_SECONDS);
        return builder.tag(new EntityTag(String.valueOf(fileId)))
                      .expires(new Date(System.currentTimeMillis() + CACHE_IN_MILLI_SECONDS))
                      .cacheControl(cacheControl)
                      .build();
      }
      return fallbackAnswer(builder);
    } catch (ObjectNotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }

  @GET
  @Path("/displayMode")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Get the current user's display mode",
             description = "The mode the user chose, the page's effective mode and the modes offered",
             method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "503", description = "The theme services are still starting"),
  })
  public Response getDisplayMode() {
    ThemeService service = getThemeService();
    if (service == null) {
      return themeServicesStarting();
    }
    String username = getCurrentUsername();
    Map<String, String> answer = new LinkedHashMap<>();
    answer.put("mode", service.getDisplayMode(username).name());
    answer.put("effectiveMode", service.getEffectiveDisplayMode(username).name());
    answer.put("modesOffered", service.getModesOffered().name());
    return Response.ok(ThemeRestUtils.toJson(answer), MediaType.APPLICATION_JSON).cacheControl(noStore()).build();
  }

  @PUT
  @Path("/displayMode")
  @RolesAllowed("users")
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Set the current user's display mode",
             description = "Body {\"mode\": \"AUTOMATIC|LIGHT|DARK\"}; applies to the current user only",
             method = "PUT")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "204", description = "Display mode saved"),
                          @ApiResponse(responseCode = "400",
                                       description = "Not a display mode, or a single mode is offered; the body is the message code"),
                          @ApiResponse(responseCode = "503", description = "The theme services are still starting"),
  })
  public Response setDisplayMode(String body) {
    ThemeService service = getThemeService();
    if (service == null) {
      return themeServicesStarting();
    }
    try {
      service.setDisplayMode(getCurrentUsername(), ThemeRestUtils.readDisplayMode(body));
      return Response.noContent().build();
    } catch (IllegalArgumentException e) {
      LOG.debug("Display mode refused: {}", e.getMessage());
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
    }
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
    ThemeService service = getThemeService();
    if (service != null) {
      return stateFileResponse(service, service.getState().getLogoFileId(), version);
    }
    return getBrandingFileResponse(brandingService.getLogo(), request, version);
  }

  @GET
  @Path("/darkLogo")
  @Operation(summary = "Get the dark logo", description = "The dark logo, the light one when no dark logo is set", method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Logo served"),
                          @ApiResponse(responseCode = "404", description = "No logo"),
  })
  public Response getDarkLogo(@QueryParam("v")
  String version) {
    ThemeService service = getThemeService();
    if (service == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    return stateFileResponse(service, service.getEffectiveDarkLogoFileId(), version);
  }

  @GET
  @Path("/darkFavicon")
  @Operation(summary = "Get the dark favicon", description = "The dark favicon, the light one when no dark favicon is set", method = "GET")
  @ApiResponses(value = {
                          @ApiResponse(responseCode = "200", description = "Favicon served"),
                          @ApiResponse(responseCode = "404", description = "No favicon"),
  })
  public Response getDarkFavicon(@QueryParam("v")
  String version) {
    ThemeService service = getThemeService();
    if (service == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    return stateFileResponse(service, service.getEffectiveDarkFaviconFileId(), version);
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
    ThemeService service = getThemeService();
    if (service != null) {
      return stateFileResponse(service, brandingService.getPageBackgroundId(), version);
    }
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
    ThemeService service = getThemeService();
    if (service != null) {
      return stateFileResponse(service, brandingService.getTopBarBackgroundId(), version);
    }
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
    ThemeService service = getThemeService();
    if (service != null) {
      return stateFileResponse(service, brandingService.getSideBarBackgroundId(), version);
    }
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
    ThemeService service = getThemeService();
    if (service != null) {
      return stateFileResponse(service, brandingService.getDrawerBackgroundId(), version);
    }
    return getBrandingFileResponse(brandingService.getDrawerBackground(), request, version);
  }

  @GET
  @Path("/appBackground")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get Default application Background image", description = "Get the platform-wide default application background image (Branding and Theme > Pages & Apps style)", method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fullfilled"),
          @ApiResponse(responseCode = "404", description = "Object not found"),
  })
  public Response getAppBackground(
          @Context
          Request request,
          @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
          @QueryParam("v")
          String version) {
    ThemeService service = getThemeService();
    if (service != null) {
      return stateFileResponse(service, brandingService.getAppBackgroundId(), version);
    }
    return getBrandingFileResponse(brandingService.getAppBackground(), request, version);
  }

  @GET
  @Path("/appTextTitleBackground")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get Default application title text Background image", description = "Get the platform-wide default application title text background image (Branding and Theme > Pages & Apps style)", method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fullfilled"),
          @ApiResponse(responseCode = "404", description = "Object not found"),
  })
  public Response getAppTextTitleBackground(
          @Context
          Request request,
          @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
          @QueryParam("v")
          String version) {
    ThemeService service = getThemeService();
    if (service != null) {
      return stateFileResponse(service, brandingService.getAppTextTitleBackgroundId(), version);
    }
    return getBrandingFileResponse(brandingService.getAppTextTitleBackground(), request, version);
  }

  @GET
  @Path("/appTextHeaderBackground")
  @Produces(IMAGE_MIME_TYPE)
  @Operation(summary = "Get Default application header text Background image", description = "Get the platform-wide default application header text background image (Branding and Theme > Pages & Apps style)", method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fullfilled"),
          @ApiResponse(responseCode = "404", description = "Object not found"),
  })
  public Response getAppTextHeaderBackground(
          @Context
          Request request,
          @Parameter(description = "The value of version parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'")
          @QueryParam("v")
          String version) {
    ThemeService service = getThemeService();
    if (service != null) {
      return stateFileResponse(service, brandingService.getAppTextHeaderBackgroundId(), version);
    }
    return getBrandingFileResponse(brandingService.getAppTextHeaderBackground(), request, version);
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
    ThemeService service = getThemeService();
    if (service != null) {
      return stateFileResponse(service, service.getState().getFaviconFileId(), version);
    }
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
    ThemeService service = getThemeService();
    if (service != null) {
      return stateFileResponse(service, brandingService.getLoginBackgroundId(), version);
    }
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
    ThemeStylesheet stylesheet = brandingService.getThemeStylesheet();
    ThemeService service = getThemeService();
    if (service == null) {
      // the single-theme stylesheet served while the theme services start: never cached for long
      return fallbackAnswer(Response.ok(stylesheet.css(), "text/css"));
    }
    if (StringUtils.isNotBlank(lastModified) && !lastModified.equals(String.valueOf(stylesheet.stamp()))) {
      // a page rendered by a node that already sees a newer stamp: re-read the state, at most once per window
      if (service.refreshState()) {
        stylesheet = brandingService.getThemeStylesheet();
      }
      if (!lastModified.equals(String.valueOf(stylesheet.stamp()))) {
        return fallbackAnswer(Response.ok(stylesheet.css(), "text/css"));
      }
    }
    long lastUpdated = stylesheet.stamp();
    EntityTag eTag = new EntityTag(String.valueOf(lastUpdated));
    Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
    if (builder == null || StringUtils.isBlank(lastModified)) {
      builder = Response.ok(stylesheet.css(), "text/css");
      builder.tag(eTag);
      CacheControl cc = new CacheControl();
      cc.setMaxAge(86400);
      builder.type("text/css");
      // the exposed time also carries the shipped template's fingerprint (v= parameter, ETag): as a date it may lie
      // in the future, which RFC 9110 forbids for Last-Modified, so the header is capped to now
      builder.lastModified(new Date(Math.min(lastUpdated, System.currentTimeMillis())));
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

  /**
   * The themes as stored, the Default Theme with its configured dark values
   * (exo.branding.theme.dark.*) as its configured settings: the page tells a
   * configured dark value from an Automatic one by them, as for a theme bound
   * to the configuration.
   */
  private List<io.meeds.portal.theme.model.Theme> viewThemes(ThemeService service) {
    return service.getThemes().stream().map(theme -> {
      if (!theme.isDefaultTheme()) {
        return theme;
      }
      return new io.meeds.portal.theme.model.Theme(theme.getId(),
                                                    theme.getName(),
                                                    theme.isNameSetInUI(),
                                                    theme.getOrigin(),
                                                    theme.getConfigKey(),
                                                    theme.getSettings(),
                                                    service.getConfiguredSettings(theme),
                                                    theme.getUpdatedDate());
    }).toList();
  }

  private ThemeService getThemeService() {
    if (!ThemeReadiness.isReady()) {
      return null;
    }
    if (themeService == null) {
      themeService = ExoContainerContext.getService(ThemeService.class);
    }
    return themeService;
  }

  /** For tests: the theme service, still answered only once the readiness flag is set. */
  void setThemeService(ThemeService themeService) {
    this.themeService = themeService;
  }

  /**
   * An answer this node may not be able to give again under the same URL: no
   * Expires, no ETag, kept a minute at most by any cache.
   */
  private static Response fallbackAnswer(Response.ResponseBuilder builder) {
    CacheControl cacheControl = new CacheControl();
    cacheControl.setMaxAge(60);
    return builder.cacheControl(cacheControl).build();
  }

  /**
   * A logo or favicon of the branding state, read by file id through the
   * per-node image cache: the requested id is cacheable for good, any other
   * id gets the current file briefly.
   */
  private static Response stateFileResponse(ThemeService service, Long fileId, String version) {
    ImageBytes image = fileId == null ? null : service.readImage(fileId);
    if (image == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    Response.ResponseBuilder builder = imageResponse(image.data(), image.type());
    if (!String.valueOf(fileId).equals(version)) {
      return fallbackAnswer(builder);
    }
    CacheControl cacheControl = new CacheControl();
    cacheControl.setMaxAge(CACHE_IN_SECONDS);
    return builder.tag(new EntityTag(String.valueOf(fileId)))
                  .expires(new Date(System.currentTimeMillis() + CACHE_IN_MILLI_SECONDS))
                  .cacheControl(cacheControl)
                  .build();
  }

  /**
   * Every branding image is served with the type its bytes are, never sniffed
   * by the browser, and an SVG under a policy that runs no script.
   */
  static Response.ResponseBuilder imageResponse(byte[] data, String detectedType) {
    String type = detectedType == null ? BrandingImageValidator.detectType(data) : detectedType;
    if (type == null) {
      type = "application/octet-stream";
    }
    Response.ResponseBuilder builder = Response.ok(new ByteArrayInputStream(data), type).header("X-Content-Type-Options", "nosniff");
    if (BrandingImageValidator.SVG.equals(type)) {
      builder.header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'");
    }
    return builder;
  }

  private static Response themeServicesStarting() {
    return Response.status(Response.Status.SERVICE_UNAVAILABLE).header("Retry-After", "10").build();
  }

  private static CacheControl noStore() {
    CacheControl cacheControl = new CacheControl();
    cacheControl.setPrivate(true);
    cacheControl.setNoStore(true);
    return cacheControl;
  }

  private static String getCurrentUsername() {
    ConversationState state = ConversationState.getCurrent();
    return state == null || state.getIdentity() == null ? null : state.getIdentity().getUserId();
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
      builder = imageResponse(iconFile.getData(), null);
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
